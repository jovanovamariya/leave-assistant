package dev.leaveassistant.leave;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, LeaveType> {
}
