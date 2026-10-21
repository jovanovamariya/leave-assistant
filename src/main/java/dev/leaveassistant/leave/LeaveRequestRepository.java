package dev.leaveassistant.leave;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    /** Used to collect the active requests (PENDING, APPROVED) for the overlap rule. */
    List<LeaveRequest> findByStatusIn(Collection<LeaveStatus> statuses);

    List<LeaveRequest> findAllByOrderByStartDateDesc();
}
