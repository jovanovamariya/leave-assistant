package dev.leaveassistant.leave;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Rule R-4: a new request may not share a day with an existing active request.
 * Both ends of a range count as leave days, so touching ranges overlap.
 */
public class OverlapRule {

    public Optional<LeavePeriod> findConflict(LocalDate start, LocalDate end, List<LeavePeriod> existing) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }
        return existing.stream()
                .filter(period -> period.status().isActive())
                .filter(period -> !start.isAfter(period.end()) && !period.start().isAfter(end))
                .findFirst();
    }
}
