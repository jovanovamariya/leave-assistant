package dev.leaveassistant.leave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** "Today" is fixed to Wed 21 Oct 2026. The rules are real; only the repositories are mocked. */
@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository requests;
    @Mock
    private LeaveRequestEventRepository events;
    @Mock
    private LeaveBalanceRepository balances;
    @Mock
    private PublicHolidayRepository holidays;

    private LeaveRequestService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);
        service = new LeaveRequestService(requests, events, balances, holidays, clock);
    }

    private static LocalDate oct(int day) {
        return LocalDate.of(2026, 10, day);
    }

    private static LeaveBalance balance(int allowance, int used) {
        LeaveBalance balance = new LeaveBalance();
        ReflectionTestUtils.setField(balance, "leaveType", LeaveType.ANNUAL);
        ReflectionTestUtils.setField(balance, "allowanceDays", allowance);
        ReflectionTestUtils.setField(balance, "usedDays", used);
        return balance;
    }

    private static PublicHoliday holiday(LocalDate date) {
        PublicHoliday holiday = new PublicHoliday();
        ReflectionTestUtils.setField(holiday, "date", date);
        ReflectionTestUtils.setField(holiday, "name", "Test holiday");
        return holiday;
    }

    private static LeaveRequest existing(Long id, LeaveStatus status, int fromDay, int toDay, int days) {
        LeaveRequest request = new LeaveRequest(LeaveType.ANNUAL, oct(fromDay), oct(toDay), days, null, status);
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }

    /** The mocked save assigns an id, like the database would. */
    private void saveAssignsId(long id) {
        when(requests.save(any(LeaveRequest.class))).thenAnswer(invocation -> {
            LeaveRequest request = invocation.getArgument(0);
            ReflectionTestUtils.setField(request, "id", id);
            return request;
        });
    }

    // ---- create ----

    @Test
    void createSavesAPendingRequestAndReservesTheDays() {
        LeaveBalance balance = balance(26, 0);
        when(balances.findById(LeaveType.ANNUAL)).thenReturn(Optional.of(balance));
        saveAssignsId(42L);

        LeaveRequest created = service.create(LeaveType.ANNUAL, oct(26), oct(30), "trip");

        assertEquals(LeaveStatus.PENDING, created.getStatus());
        assertEquals(5, created.getWorkingDays());
        assertEquals(21, balance.remainingDays());

        ArgumentCaptor<LeaveRequestEvent> event = ArgumentCaptor.forClass(LeaveRequestEvent.class);
        verify(events).save(event.capture());
        assertEquals(42L, event.getValue().getRequestId());
        assertNull(event.getValue().getFromStatus());
        assertEquals(LeaveStatus.PENDING, event.getValue().getToStatus());
    }

    @Test
    void createDoesNotCountPublicHolidays() {
        when(holidays.findByDateBetween(oct(26), oct(30))).thenReturn(List.of(holiday(oct(28))));
        when(balances.findById(LeaveType.ANNUAL)).thenReturn(Optional.of(balance(26, 0)));
        saveAssignsId(1L);

        LeaveRequest created = service.create(LeaveType.ANNUAL, oct(26), oct(30), null);

        assertEquals(4, created.getWorkingDays());
    }

    @Test
    void createIsRejectedWhenTheBalanceIsTooLowAndNothingIsSaved() {
        when(balances.findById(LeaveType.ANNUAL)).thenReturn(Optional.of(balance(26, 24)));

        RuleViolationException error = assertThrows(RuleViolationException.class,
                () -> service.create(LeaveType.ANNUAL, oct(26), oct(30), null));

        assertEquals("Requested 5 days but only 2 remaining", error.getMessage());
        verify(requests, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void createIsRejectedWhenItOverlapsAnActiveRequest() {
        LeaveBalance balance = balance(26, 3);
        when(balances.findById(LeaveType.ANNUAL)).thenReturn(Optional.of(balance));
        when(requests.findByStatusIn(any()))
                .thenReturn(List.of(existing(7L, LeaveStatus.PENDING, 28, 30, 3)));

        RuleViolationException error = assertThrows(RuleViolationException.class,
                () -> service.create(LeaveType.ANNUAL, oct(26), oct(28), null));

        assertTrue(error.getMessage().contains("Overlaps with an existing request from 2026-10-28 to 2026-10-30"));
        assertEquals(23, balance.remainingDays());
        verify(requests, never()).save(any());
    }

    @Test
    void createIsRejectedWhenTheDatesHaveNoWorkingDays() {
        RuleViolationException error = assertThrows(RuleViolationException.class,
                () -> service.create(LeaveType.ANNUAL, oct(24), oct(25), null));

        assertTrue(error.getMessage().contains("no working days"));
        verifyNoInteractions(balances);
    }

    // ---- change status ----

    @Test
    void approveChangesTheStatusRecordsTheEventAndLeavesTheBalanceAlone() {
        LeaveRequest request = existing(1L, LeaveStatus.PENDING, 26, 30, 5);
        when(requests.findById(1L)).thenReturn(Optional.of(request));

        service.changeStatus(1L, LeaveStatus.APPROVED);

        assertEquals(LeaveStatus.APPROVED, request.getStatus());
        ArgumentCaptor<LeaveRequestEvent> event = ArgumentCaptor.forClass(LeaveRequestEvent.class);
        verify(events).save(event.capture());
        assertEquals(LeaveStatus.PENDING, event.getValue().getFromStatus());
        assertEquals(LeaveStatus.APPROVED, event.getValue().getToStatus());
        verifyNoInteractions(balances);
    }

    @Test
    void cancellingAPendingRequestGivesTheDaysBack() {
        LeaveRequest request = existing(1L, LeaveStatus.PENDING, 26, 30, 5);
        LeaveBalance balance = balance(26, 5);
        when(requests.findById(1L)).thenReturn(Optional.of(request));
        when(balances.findById(LeaveType.ANNUAL)).thenReturn(Optional.of(balance));

        service.changeStatus(1L, LeaveStatus.CANCELLED);

        assertEquals(LeaveStatus.CANCELLED, request.getStatus());
        assertEquals(26, balance.remainingDays());
    }

    @Test
    void rejectingARequestGivesTheDaysBack() {
        LeaveRequest request = existing(1L, LeaveStatus.PENDING, 26, 30, 5);
        LeaveBalance balance = balance(26, 5);
        when(requests.findById(1L)).thenReturn(Optional.of(request));
        when(balances.findById(LeaveType.ANNUAL)).thenReturn(Optional.of(balance));

        service.changeStatus(1L, LeaveStatus.REJECTED);

        assertEquals(26, balance.remainingDays());
    }

    @Test
    void cancellingAnApprovedRequestAfterItStartedIsRejectedAndKeepsTheBalance() {
        LeaveRequest request = existing(1L, LeaveStatus.APPROVED, 19, 20, 2);
        when(requests.findById(1L)).thenReturn(Optional.of(request));

        RuleViolationException error = assertThrows(RuleViolationException.class,
                () -> service.changeStatus(1L, LeaveStatus.CANCELLED));

        assertEquals("An approved request can only be cancelled before its start date", error.getMessage());
        assertEquals(LeaveStatus.APPROVED, request.getStatus());
        verifyNoInteractions(events);
        verifyNoInteractions(balances);
    }

    @Test
    void aFinalStatusCannotBeChangedAndTheDaysAreNotRestoredTwice() {
        LeaveRequest request = existing(1L, LeaveStatus.CANCELLED, 26, 30, 5);
        when(requests.findById(1L)).thenReturn(Optional.of(request));

        RuleViolationException error = assertThrows(RuleViolationException.class,
                () -> service.changeStatus(1L, LeaveStatus.APPROVED));

        assertEquals("CANCELLED is a final status", error.getMessage());
        verifyNoInteractions(balances);
    }

    @Test
    void changingAnUnknownRequestFails() {
        when(requests.findById(99L)).thenReturn(Optional.empty());

        assertThrows(LeaveRequestNotFoundException.class,
                () -> service.changeStatus(99L, LeaveStatus.APPROVED));
    }
}
