package dev.leaveassistant.leave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StatusRuleTest {

    private final StatusRule rule = new StatusRule();

    private static final LocalDate START = LocalDate.of(2026, 11, 2);
    private static final LocalDate DAY_BEFORE_START = START.minusDays(1);

    @ParameterizedTest
    @CsvSource({
            "DRAFT, PENDING",
            "DRAFT, CANCELLED",
            "PENDING, APPROVED",
            "PENDING, REJECTED",
            "PENDING, CANCELLED",
            "APPROVED, CANCELLED"
    })
    void allowsTheDefinedChanges(LeaveStatus from, LeaveStatus to) {
        TransitionDecision decision = rule.check(from, to, START, DAY_BEFORE_START);
        assertTrue(decision.allowed());
        assertNull(decision.reason());
    }

    @ParameterizedTest
    @CsvSource({
            "DRAFT, DRAFT",
            "DRAFT, APPROVED",
            "DRAFT, REJECTED",
            "PENDING, DRAFT",
            "PENDING, PENDING",
            "APPROVED, DRAFT",
            "APPROVED, PENDING",
            "APPROVED, APPROVED",
            "APPROVED, REJECTED",
            "REJECTED, DRAFT",
            "REJECTED, PENDING",
            "REJECTED, APPROVED",
            "REJECTED, REJECTED",
            "REJECTED, CANCELLED",
            "CANCELLED, DRAFT",
            "CANCELLED, PENDING",
            "CANCELLED, APPROVED",
            "CANCELLED, REJECTED",
            "CANCELLED, CANCELLED"
    })
    void rejectsEverythingElse(LeaveStatus from, LeaveStatus to) {
        TransitionDecision decision = rule.check(from, to, START, DAY_BEFORE_START);
        assertFalse(decision.allowed());
        assertTrue(decision.reason() != null && !decision.reason().isBlank());
    }

    @Test
    void approvedRequestCanBeCancelledTheDayBeforeItStarts() {
        assertTrue(rule.check(LeaveStatus.APPROVED, LeaveStatus.CANCELLED, START, DAY_BEFORE_START).allowed());
    }

    @Test
    void approvedRequestCannotBeCancelledOnItsStartDate() {
        TransitionDecision decision = rule.check(LeaveStatus.APPROVED, LeaveStatus.CANCELLED, START, START);
        assertFalse(decision.allowed());
        assertEquals("An approved request can only be cancelled before its start date", decision.reason());
    }

    @Test
    void approvedRequestCannotBeCancelledAfterItStarted() {
        assertFalse(rule.check(LeaveStatus.APPROVED, LeaveStatus.CANCELLED, START, START.plusDays(3)).allowed());
    }

    @Test
    void pendingRequestCanBeCancelledEvenAfterItsStartDate() {
        assertTrue(rule.check(LeaveStatus.PENDING, LeaveStatus.CANCELLED, START, START.plusDays(3)).allowed());
    }

    @Test
    void finalStatusesSayTheyAreFinal() {
        TransitionDecision decision = rule.check(LeaveStatus.CANCELLED, LeaveStatus.APPROVED, START, DAY_BEFORE_START);
        assertEquals("CANCELLED is a final status", decision.reason());
    }
}
