package dev.leaveassistant.leave;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/** One row of status history (rule R-5). fromStatus is null for the first event of a request. */
@Entity
@Table(name = "leave_request_event")
public class LeaveRequestEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private LeaveStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private LeaveStatus toStatus;

    @Column(name = "at", nullable = false)
    private Instant at;

    protected LeaveRequestEvent() {
        // required by JPA
    }

    public LeaveRequestEvent(Long requestId, LeaveStatus fromStatus, LeaveStatus toStatus) {
        this.requestId = requestId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    @PrePersist
    void onCreate() {
        this.at = Instant.now();
    }

    public Long getId() { return id; }
    public Long getRequestId() { return requestId; }
    public LeaveStatus getFromStatus() { return fromStatus; }
    public LeaveStatus getToStatus() { return toStatus; }
    public Instant getAt() { return at; }
}
