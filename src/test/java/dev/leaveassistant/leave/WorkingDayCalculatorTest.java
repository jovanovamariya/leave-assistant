package dev.leaveassistant.leave;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
public class WorkingDayCalculatorTest {
    private final WorkingDayCalculator calculator = new WorkingDayCalculator();
    private final Set<LocalDate> noHolidays = Set.of();

    private static final LocalDate MON = LocalDate.of(2026, 10, 19);
    private static final LocalDate WED = LocalDate.of(2026, 10, 21);
    private static final LocalDate FRI = LocalDate.of(2026, 10, 23);
    private static final LocalDate SAT = LocalDate.of(2026, 10, 24);
    private static final LocalDate SUN = LocalDate.of(2026, 10, 25);
    private static final LocalDate NEXT_MON = LocalDate.of(2026, 10, 26);

    @Test
    void mondayToFridayIsFiveDays() {
        assertEquals(5, calculator.countWorkingDays(MON, FRI, noHolidays));
    }

    @Test
    void fridayToNextMondayIsTwoDays() {
        assertEquals(2, calculator.countWorkingDays(FRI, NEXT_MON, noHolidays));
    }

    @Test
    void weekendOnlyIsZeroDays() {
        assertEquals(0, calculator.countWorkingDays(SAT, SUN, noHolidays));
    }

    @Test
    void holidayOnWeekdayIsNotCounted() {
        assertEquals(4, calculator.countWorkingDays(MON, FRI, Set.of(WED)));
    }

    @Test
    void holidayOnWeekendChangesNothing() {
        assertEquals(5, calculator.countWorkingDays(MON, FRI, Set.of(SAT)));
    }

    @Test
    void singleWorkingDayIsOne() {
        assertEquals(1, calculator.countWorkingDays(MON, MON, noHolidays));
    }

    @Test
    void singleHolidayIsZero() {
        assertEquals(0, calculator.countWorkingDays(MON, MON, Set.of(MON)));
    }

    @Test
    void startAfterEndIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.countWorkingDays(FRI, MON, noHolidays));
    }
}

