package dev.leaveassistant.leave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BalanceRuleTest {

    private final BalanceRule rule = new BalanceRule();

    @Test
    void allowsRequestWithinBalance() {
        BalanceDecision decision = rule.check(10, 3);
        assertTrue(decision.allowed());
        assertEquals(7, decision.remainingAfter());
        assertNull(decision.reason());
    }

    @Test
    void allowsRequestThatUsesExactlyTheRemainingDays() {
        BalanceDecision decision = rule.check(5, 5);
        assertTrue(decision.allowed());
        assertEquals(0, decision.remainingAfter());
    }

    @Test
    void rejectsRequestOneDayOverBalance() {
        BalanceDecision decision = rule.check(5, 6);
        assertFalse(decision.allowed());
        assertEquals(5, decision.remainingAfter());
        assertEquals("Requested 6 days but only 5 remaining", decision.reason());
    }

    @Test
    void rejectsAnyRequestWhenNothingIsLeft() {
        assertFalse(rule.check(0, 1).allowed());
    }

    @Test
    void zeroRequestedDaysIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> rule.check(5, 0));
    }

    @Test
    void negativeRequestedDaysIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> rule.check(5, -1));
    }

    @Test
    void negativeRemainingDaysIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> rule.check(-1, 1));
    }
}
