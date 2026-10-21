package dev.leaveassistant.leave;

/** Lifecycle of a leave request. Only PENDING and APPROVED requests block other requests. */
public enum LeaveStatus {
    DRAFT,
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED;

    public boolean isActive() {
        return this == PENDING || this == APPROVED;
    }
}
