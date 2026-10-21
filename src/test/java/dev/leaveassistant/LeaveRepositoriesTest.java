package dev.leaveassistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.leaveassistant.leave.LeaveBalance;
import dev.leaveassistant.leave.LeaveBalanceRepository;
import dev.leaveassistant.leave.LeaveRequest;
import dev.leaveassistant.leave.LeaveRequestEvent;
import dev.leaveassistant.leave.LeaveRequestEventRepository;
import dev.leaveassistant.leave.LeaveRequestRepository;
import dev.leaveassistant.leave.LeaveStatus;
import dev.leaveassistant.leave.LeaveType;
import dev.leaveassistant.leave.PublicHoliday;
import dev.leaveassistant.leave.PublicHolidayRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/** Checks that the entities match the Flyway tables. Each test rolls back, so the data stays clean. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class LeaveRepositoriesTest {

    @Autowired
    private LeaveRequestRepository requests;

    @Autowired
    private LeaveRequestEventRepository events;

    @Autowired
    private LeaveBalanceRepository balances;

    @Autowired
    private PublicHolidayRepository holidays;

    private LeaveRequest request(LeaveStatus status, int fromDay, int toDay) {
        return new LeaveRequest(LeaveType.ANNUAL,
                LocalDate.of(2026, 10, fromDay), LocalDate.of(2026, 10, toDay), 3, "holiday", status);
    }

    @Test
    void seededAnnualBalanceIsTwentySixDays() {
        LeaveBalance annual = balances.findById(LeaveType.ANNUAL).orElseThrow();

        assertEquals(26, annual.getAllowanceDays());
        assertEquals(0, annual.getUsedDays());
        assertEquals(26, annual.remainingDays());
    }

    @Test
    void usingAndRestoringDaysChangesTheBalance() {
        LeaveBalance annual = balances.findById(LeaveType.ANNUAL).orElseThrow();

        annual.use(5);
        balances.flush();
        assertEquals(21, balances.findById(LeaveType.ANNUAL).orElseThrow().remainingDays());

        annual.restore(5);
        assertEquals(26, annual.remainingDays());
    }

    @Test
    void seededHolidaysCanBeFoundByDateRange() {
        List<PublicHoliday> found = holidays.findByDateBetween(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertEquals(2, found.size());
        assertEquals(Set.of(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 11)),
                Set.of(found.get(0).getDate(), found.get(1).getDate()));
    }

    @Test
    void savedRequestCanBeReadBackWithGeneratedValues() {
        LeaveRequest saved = requests.saveAndFlush(request(LeaveStatus.PENDING, 19, 21));

        LeaveRequest found = requests.findById(saved.getId()).orElseThrow();
        assertEquals(LeaveStatus.PENDING, found.getStatus());
        assertEquals(LocalDate.of(2026, 10, 19), found.getStartDate());
        assertEquals(3, found.getWorkingDays());
        assertNotNull(found.getCreatedAt());
        assertNotNull(found.getUpdatedAt());
    }

    @Test
    void findsOnlyRequestsWithTheGivenStatuses() {
        requests.save(request(LeaveStatus.PENDING, 19, 21));
        requests.save(request(LeaveStatus.APPROVED, 26, 28));
        requests.save(request(LeaveStatus.CANCELLED, 5, 7));

        List<LeaveRequest> active = requests.findByStatusIn(List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED));

        assertEquals(2, active.size());
    }

    @Test
    void listsRequestsNewestStartFirst() {
        requests.save(request(LeaveStatus.PENDING, 5, 7));
        requests.save(request(LeaveStatus.PENDING, 26, 28));

        List<LeaveRequest> all = requests.findAllByOrderByStartDateDesc();

        assertEquals(LocalDate.of(2026, 10, 26), all.get(0).getStartDate());
    }

    @Test
    void eventHistoryIsStoredInOrder() {
        LeaveRequest saved = requests.saveAndFlush(request(LeaveStatus.PENDING, 19, 21));
        events.save(new LeaveRequestEvent(saved.getId(), null, LeaveStatus.PENDING));
        events.save(new LeaveRequestEvent(saved.getId(), LeaveStatus.PENDING, LeaveStatus.APPROVED));
        events.flush();

        List<LeaveRequestEvent> history = events.findByRequestIdOrderByIdAsc(saved.getId());

        assertEquals(2, history.size());
        assertNull(history.get(0).getFromStatus());
        assertEquals(LeaveStatus.PENDING, history.get(0).getToStatus());
        assertEquals(LeaveStatus.APPROVED, history.get(1).getToStatus());
        assertNotNull(history.get(1).getAt());
    }
}
