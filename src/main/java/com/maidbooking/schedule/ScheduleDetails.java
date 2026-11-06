package com.maidbooking.schedule;

import com.maidbooking.model.BookingType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ScheduleDetails {
    BookingType type;
    Integer durationMinutes;
    LocalDateTime startTime;
    LocalDateTime endTime;
    LocalDate startDate;
    LocalDate endDate;
    Set<DayOfWeek> daysOfWeek;
    LocalTime dailyStartTime;
    LocalTime dailyEndTime;

    public boolean hasDuration() {
        return durationMinutes != null;
    }

    public boolean hasStartOrEndTime() {
        return startTime != null || endTime != null;
    }

    public boolean hasRecurringFields() {
        return startDate != null || endDate != null || daysOfWeek != null
                || dailyStartTime != null || dailyEndTime != null;
    }
}
