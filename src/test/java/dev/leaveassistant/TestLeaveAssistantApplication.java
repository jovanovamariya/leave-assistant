package dev.leaveassistant;

import org.springframework.boot.SpringApplication;

public class TestLeaveAssistantApplication {

	public static void main(String[] args) {
		SpringApplication.from(LeaveAssistantApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
