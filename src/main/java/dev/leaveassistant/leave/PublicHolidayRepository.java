package dev.leaveassistant.leave;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, LocalDate> {

    List<PublicHoliday> findByDateBetween(LocalDate from, LocalDate to);
}
