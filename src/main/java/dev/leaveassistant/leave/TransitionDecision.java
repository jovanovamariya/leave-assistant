package dev.leaveassistant.leave;

/** Result of a status change check: allowed, or rejected with a reason. */
public record TransitionDecision(boolean allowed, String reason) {

    public static TransitionDecision ok() {
        return new TransitionDecision(true, null);
    }

    public static TransitionDecision rejected(String reason) {
        return new TransitionDecision(false, reason);
    }
}
