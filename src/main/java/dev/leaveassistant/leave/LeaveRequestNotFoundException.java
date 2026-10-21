package dev.leaveassistant.leave;

public class LeaveRequestNotFoundException extends RuntimeException {

    public LeaveRequestNotFoundException(Long id) {
        super("Leave request " + id + " not found");
    }
}
