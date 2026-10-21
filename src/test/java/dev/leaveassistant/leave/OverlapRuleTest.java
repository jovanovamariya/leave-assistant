package dev.leaveassistant.leave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OverlapRuleTest {

    private final OverlapRule rule = new OverlapRule();

    private static LocalDate oct(int day) {
        return LocalDate.of(2026, 10, day);
    }

    /** Existing request: 19 to 21 October with the given status. */
    private static List<LeavePeriod> existing(LeaveStatus status) {
        return List.of(new LeavePeriod(oct(19), oct(21), status));
    }

    private Optional<LeavePeriod> conflictWithApproved(int start, int end) {
        return rule.findConflict(oct(start), oct(end), existing(LeaveStatus.APPROVED));
    }

    @Test
    void rejectsRequestTouchingTheEndDay() {
        assertTrue(conflictWithApproved(21, 23).isPresent());
    }

    @Test
    void rejectsRequestTouchingTheStartDay() {
        assertTrue(conflictWithApproved(17, 19).isPresent());
    }

    @Test
    void allowsRequestStartingTheDayAfter() {
        assertTrue(conflictWithApproved(22, 23).isEmpty());
    }

    @Test
    void allowsRequestEndingTheDayBefore() {
        assertTrue(conflictWithApproved(16, 18).isEmpty());
    }

    @Test
    void rejectsRequestInsideExistingRange() {
        assertTrue(conflictWithApproved(20, 20).isPresent());
    }

    @Test
    void rejectsRequestCoveringExistingRange() {
        assertTrue(conflictWithApproved(17, 24).isPresent());
    }

    @Test
    void pendingRequestsBlockToo() {
        assertTrue(rule.findConflict(oct(20), oct(22), existing(LeaveStatus.PENDING)).isPresent());
    }

    @Test
    void cancelledRequestsAreIgnored() {
        assertTrue(rule.findConflict(oct(20), oct(22), existing(LeaveStatus.CANCELLED)).isEmpty());
    }

    @Test
    void rejectedRequestsAreIgnored() {
        assertTrue(rule.findConflict(oct(20), oct(22), existing(LeaveStatus.REJECTED)).isEmpty());
    }

    @Test
    void draftsAreIgnored() {
        assertTrue(rule.findConflict(oct(20), oct(22), existing(LeaveStatus.DRAFT)).isEmpty());
    }

    @Test
    void noExistingRequestsMeansNoConflict() {
        assertTrue(rule.findConflict(oct(20), oct(22), List.of()).isEmpty());
    }

    @Test
    void returnsTheRequestThatConflicts() {
        LeavePeriod unrelated = new LeavePeriod(oct(1), oct(2), LeaveStatus.APPROVED);
        LeavePeriod blocking = new LeavePeriod(oct(19), oct(21), LeaveStatus.APPROVED);

        Optional<LeavePeriod> conflict = rule.findConflict(oct(20), oct(22), List.of(unrelated, blocking));

        assertEquals(Optional.of(blocking), conflict);
    }

    @Test
    void startAfterEndIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> rule.findConflict(oct(23), oct(20), List.of()));
    }
}
