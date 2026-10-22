package dev.leaveassistant.leave;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Body of POST /api/leave-requests. */
public record CreateLeaveRequest(
        @NotNull LeaveType leaveType,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Size(max = 500) String reason) {
}
