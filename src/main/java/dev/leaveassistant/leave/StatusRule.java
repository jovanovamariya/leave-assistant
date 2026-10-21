package dev.leaveassistant.leave;

import java.time.LocalDate;

/**
 * Rule R-5: which status changes are allowed.
 * <ul>
 *   <li>DRAFT can be submitted (PENDING) or cancelled.</li>
 *   <li>PENDING can be approved, rejected or cancelled.</li>
 *   <li>APPROVED can be cancelled, but only before its start date.</li>
 *   <li>REJECTED and CANCELLED are final.</li>
 * </ul>
 * Recording the change in the history and restoring the balance is done by the service, not here.
 */
public class StatusRule {

    public TransitionDecision check(LeaveStatus from, LeaveStatus to, LocalDate startDate, LocalDate today) {
        return switch (from) {
            case DRAFT -> to == LeaveStatus.PENDING || to == LeaveStatus.CANCELLED
                    ? TransitionDecision.ok()
                    : notAllowed(from, to);
            case PENDING -> to == LeaveStatus.APPROVED || to == LeaveStatus.REJECTED || to == LeaveStatus.CANCELLED
                    ? TransitionDecision.ok()
                    : notAllowed(from, to);
            case APPROVED -> approvedTo(to, startDate, today);
            case REJECTED, CANCELLED -> TransitionDecision.rejected(from + " is a final status");
        };
    }

    private TransitionDecision approvedTo(LeaveStatus to, LocalDate startDate, LocalDate today) {
        if (to != LeaveStatus.CANCELLED) {
            return notAllowed(LeaveStatus.APPROVED, to);
        }
        if (!today.isBefore(startDate)) {
            return TransitionDecision.rejected("An approved request can only be cancelled before its start date");
        }
        return TransitionDecision.ok();
    }

    private TransitionDecision notAllowed(LeaveStatus from, LeaveStatus to) {
        return TransitionDecision.rejected("A " + from + " request cannot become " + to);
    }
}
