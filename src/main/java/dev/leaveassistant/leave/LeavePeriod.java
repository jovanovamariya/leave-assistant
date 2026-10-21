package dev.leaveassistant.leave;

import java.time.LocalDate;

/** The dates and status of an existing leave request. Both dates are leave days. */
public record LeavePeriod(LocalDate start, LocalDate end, LeaveStatus status) {

    public LeavePeriod {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }
    }
}
