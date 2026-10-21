package dev.leaveassistant.leave;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRequestEventRepository extends JpaRepository<LeaveRequestEvent, Long> {

    List<LeaveRequestEvent> findByRequestIdOrderByIdAsc(Long requestId);
}
