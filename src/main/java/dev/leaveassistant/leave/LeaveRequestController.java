package dev.leaveassistant.leave;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leave-requests")
public class LeaveRequestController {

    private final LeaveRequestService service;

    public LeaveRequestController(LeaveRequestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<LeaveRequestResponse> create(@Valid @RequestBody CreateLeaveRequest body) {
        LeaveRequest created = service.create(body.leaveType(), body.startDate(), body.endDate(), body.reason());
        return ResponseEntity.created(URI.create("/api/leave-requests/" + created.getId()))
                .body(LeaveRequestResponse.from(created));
    }

    @GetMapping
    public List<LeaveRequestResponse> list() {
        return service.list().stream().map(LeaveRequestResponse::from).toList();
    }

    @GetMapping("/{id}")
    public LeaveRequestResponse get(@PathVariable Long id) {
        return LeaveRequestResponse.from(service.get(id));
    }

    @GetMapping("/{id}/history")
    public List<LeaveEventResponse> history(@PathVariable Long id) {
        return service.history(id).stream().map(LeaveEventResponse::from).toList();
    }

    @PostMapping("/{id}/cancel")
    public LeaveRequestResponse cancel(@PathVariable Long id) {
        return LeaveRequestResponse.from(service.changeStatus(id, LeaveStatus.CANCELLED));
    }

    @PostMapping("/{id}/approve")
    public LeaveRequestResponse approve(@PathVariable Long id) {
        return LeaveRequestResponse.from(service.changeStatus(id, LeaveStatus.APPROVED));
    }

    @PostMapping("/{id}/reject")
    public LeaveRequestResponse reject(@PathVariable Long id) {
        return LeaveRequestResponse.from(service.changeStatus(id, LeaveStatus.REJECTED));
    }
}
