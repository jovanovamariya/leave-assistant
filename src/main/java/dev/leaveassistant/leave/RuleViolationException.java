package dev.leaveassistant.leave;

/** A business rule was broken. The message is meant to be shown to the user. */
public class RuleViolationException extends RuntimeException {

    public RuleViolationException(String message) {
        super(message);
    }
}
