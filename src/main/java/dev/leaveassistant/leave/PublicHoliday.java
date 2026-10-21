package dev.leaveassistant.leave;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "public_holiday")
public class PublicHoliday {

    @Id
    @Column(name = "holiday_date")
    private LocalDate date;

    @Column(nullable = false, length = 100)
    private String name;

    protected PublicHoliday() {
        // required by JPA
    }

    public LocalDate getDate() { return date; }
    public String getName() { return name; }
}
