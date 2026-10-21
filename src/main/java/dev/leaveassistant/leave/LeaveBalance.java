package dev.leaveassistant.leave;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Allowance and used days for one leave type. Remaining days = allowance - used. */
@Entity
@Table(name = "leave_balance")
public class LeaveBalance {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type")
    private LeaveType leaveType;

    @Column(name = "allowance_days", nullable = false)
    private int allowanceDays;

    @Column(name = "used_days", nullable = false)
    private int usedDays;

    protected LeaveBalance() {
        // required by JPA
    }

    public int remainingDays() {
        return allowanceDays - usedDays;
    }

    public void use(int days) {
        if (days <= 0 || days > remainingDays()) {
            throw new IllegalArgumentException("Cannot use " + days + " days, remaining " + remainingDays());
        }
        this.usedDays += days;
    }

    public void restore(int days) {
        if (days <= 0 || days > usedDays) {
            throw new IllegalArgumentException("Cannot restore " + days + " days, used " + usedDays);
        }
        this.usedDays -= days;
    }

    public LeaveType getLeaveType() { return leaveType; }
    public int getAllowanceDays() { return allowanceDays; }
    public int getUsedDays() { return usedDays; }
}
