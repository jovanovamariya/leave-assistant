package dev.leaveassistant.leave;

import java.time.Instant;
import java.time.LocalDate;

public record LeaveRequestResponse(
        Long id,
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        int workingDays,
        String reason,
        LeaveStatus status,
        Instant createdAt,
        Instant updatedAt) {

    static LeaveRequestResponse from(LeaveRequest request) {
        return new LeaveRequestResponse(
                request.getId(),
                request.getLeaveType(),
                request.getStartDate(),
                request.getEndDate(),
                request.getWorkingDays(),
                request.getReason(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
