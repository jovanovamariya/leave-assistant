package dev.leaveassistant.leave;

import java.time.Instant;

public record LeaveEventResponse(LeaveStatus fromStatus, LeaveStatus toStatus, Instant at) {

    static LeaveEventResponse from(LeaveRequestEvent event) {
        return new LeaveEventResponse(event.getFromStatus(), event.getToStatus(), event.getAt());
    }
}
