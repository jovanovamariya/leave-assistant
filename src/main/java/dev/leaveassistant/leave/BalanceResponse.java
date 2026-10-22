package dev.leaveassistant.leave;

public record BalanceResponse(LeaveType leaveType, int allowanceDays, int usedDays, int remainingDays) {

    static BalanceResponse from(LeaveBalance balance) {
        return new BalanceResponse(balance.getLeaveType(), balance.getAllowanceDays(),
                balance.getUsedDays(), balance.remainingDays());
    }
}
