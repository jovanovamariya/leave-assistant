package dev.leaveassistant.leave;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * Counts working days between two dates, both ends included.
 * A working day is Monday to Friday and not a public holiday (rule R-2).
 */
public class WorkingDayCalculator {
public int countWorkingDays(LocalDate from, LocalDate to, Set<LocalDate> holidays)
{
    if (from.isAfter(to)) {
        throw new IllegalArgumentException("Start date must not be after end date");
    }
    int count = 0;
    for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
        if (isWorkingDay(day, holidays)) {
            count++;
        }
    }
    return count;
}
private boolean isWorkingDay(LocalDate day, Set<LocalDate> holidays)
{
    DayOfWeek dow = day.getDayOfWeek();
    boolean weekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    return !weekend && !holidays.contains(day);
}
}
