package dev.leaveassistant.leave;

/** Result of a balance check: whether the request fits, what is left,
 * and why not if it does not. */
public record BalanceDecision(boolean allowed, int remainingAfter, String reason) {

    public static BalanceDecision allowed(int remainingAfter) {
        return new BalanceDecision(true, remainingAfter, null);
    }

    public static BalanceDecision rejected(int remaining, String reason) {
        return new BalanceDecision(false, remaining, reason);
    }
}
