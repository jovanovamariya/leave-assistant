package dev.leaveassistant.leave;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Brings the leave rules together. The rules decide; this class loads the data, saves the result,
 * records the history and keeps the balance in step.
 */
@Service
public class LeaveRequestService {

    private static final List<LeaveStatus> ACTIVE = List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);

    private final LeaveRequestRepository requests;
    private final LeaveRequestEventRepository events;
    private final LeaveBalanceRepository balances;
    private final PublicHolidayRepository holidays;
    private final Clock clock;

    private final WorkingDayCalculator calculator = new WorkingDayCalculator();
    private final BalanceRule balanceRule = new BalanceRule();
    private final OverlapRule overlapRule = new OverlapRule();
    private final StatusRule statusRule = new StatusRule();

    public LeaveRequestService(LeaveRequestRepository requests,
                               LeaveRequestEventRepository events,
                               LeaveBalanceRepository balances,
                               PublicHolidayRepository holidays,
                               Clock clock) {
        this.requests = requests;
        this.events = events;
        this.balances = balances;
        this.holidays = holidays;
        this.clock = clock;
    }

    /** Creates a PENDING request and reserves its days in the balance (rules R-2, R-3, R-4). */
    @Transactional
    public LeaveRequest create(LeaveType type, LocalDate start, LocalDate end, String reason) {
        Set<LocalDate> holidayDates = holidays.findByDateBetween(start, end).stream()
                .map(PublicHoliday::getDate)
                .collect(Collectors.toSet());
        int workingDays = calculator.countWorkingDays(start, end, holidayDates);
        if (workingDays == 0) {
            throw new RuleViolationException("The selected dates contain no working days");
        }

        LeaveBalance balance = balances.findById(type)
                .orElseThrow(() -> new IllegalStateException("No balance configured for " + type));
        BalanceDecision balanceDecision = balanceRule.check(balance.remainingDays(), workingDays);
        if (!balanceDecision.allowed()) {
            throw new RuleViolationException(balanceDecision.reason());
        }

        List<LeavePeriod> active = requests.findByStatusIn(ACTIVE).stream()
                .map(r -> new LeavePeriod(r.getStartDate(), r.getEndDate(), r.getStatus()))
                .toList();
        overlapRule.findConflict(start, end, active).ifPresent(conflict -> {
            throw new RuleViolationException("Overlaps with an existing request from "
                    + conflict.start() + " to " + conflict.end());
        });

        LeaveRequest saved = requests.save(
                new LeaveRequest(type, start, end, workingDays, reason, LeaveStatus.PENDING));
        events.save(new LeaveRequestEvent(saved.getId(), null, LeaveStatus.PENDING));
        balance.use(workingDays);
        return saved;
    }

    /** Approves, rejects or cancels a request (rule R-5). Rejecting or cancelling gives the days back. */
    @Transactional
    public LeaveRequest changeStatus(Long id, LeaveStatus newStatus) {
        LeaveRequest request = requests.findById(id)
                .orElseThrow(() -> new LeaveRequestNotFoundException(id));
        LeaveStatus oldStatus = request.getStatus();

        TransitionDecision decision = statusRule.check(
                oldStatus, newStatus, request.getStartDate(), LocalDate.now(clock));
        if (!decision.allowed()) {
            throw new RuleViolationException(decision.reason());
        }

        request.changeStatus(newStatus);
        events.save(new LeaveRequestEvent(id, oldStatus, newStatus));

        boolean daysWereReserved = oldStatus.isActive();
        boolean daysAreFreed = newStatus == LeaveStatus.REJECTED || newStatus == LeaveStatus.CANCELLED;
        if (daysWereReserved && daysAreFreed) {
            LeaveBalance balance = balances.findById(request.getLeaveType())
                    .orElseThrow(() -> new IllegalStateException(
                            "No balance configured for " + request.getLeaveType()));
            balance.restore(request.getWorkingDays());
        }
        return requests.save(request);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> list() {
        return requests.findAllByOrderByStartDateDesc();
    }

    @Transactional(readOnly = true)
    public LeaveRequest get(Long id) {
        return requests.findById(id).orElseThrow(() -> new LeaveRequestNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestEvent> history(Long id) {
        get(id);
        return events.findByRequestIdOrderByIdAsc(id);
    }
}
