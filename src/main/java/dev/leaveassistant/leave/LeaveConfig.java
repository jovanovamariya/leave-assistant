package dev.leaveassistant.leave;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class LeaveConfig {

    /** The clock is injected so tests can fix "today". */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
