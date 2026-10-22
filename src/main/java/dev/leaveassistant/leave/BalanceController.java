package dev.leaveassistant.leave;

import java.util.Comparator;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BalanceController {

    private final LeaveBalanceRepository balances;

    public BalanceController(LeaveBalanceRepository balances) {
        this.balances = balances;
    }

    @GetMapping("/api/balances")
    public List<BalanceResponse> balances() {
        return balances.findAll().stream()
                .sorted(Comparator.comparing(LeaveBalance::getLeaveType))
                .map(BalanceResponse::from)
                .toList();
    }
}
