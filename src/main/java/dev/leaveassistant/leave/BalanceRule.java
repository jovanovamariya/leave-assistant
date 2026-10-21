package dev.leaveassistant.leave;

/**
 * Rule R-3: a leave request may not use more days than the employee has left.
 * The allowance itself comes from the database; this rule only compares numbers.
 */
public class BalanceRule {

    public BalanceDecision check(int remainingDays, int requestedDays) {
        if (remainingDays < 0) {
            throw new IllegalArgumentException("Remaining days must not be negative");
        }
        if (requestedDays <= 0) {
            throw new IllegalArgumentException("Requested days must be at least 1");
        }
        if (requestedDays > remainingDays) {
            return BalanceDecision.rejected(remainingDays,
                    "Requested " + requestedDays + " days but only " + remainingDays + " remaining");
        }
        return BalanceDecision.allowed(remainingDays - requestedDays);
    }
}
