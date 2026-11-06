package com.maidbooking.model;

import com.maidbooking.exception.BadRequestException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Value;

/** End time is exclusive, so 9-10 and 10-11 do not overlap. */
@Value
public class TimeSlot {
    LocalDateTime start;
    LocalDateTime end;

    public TimeSlot(LocalDateTime start, LocalDateTime end) {
        BadRequestException.check(start != null && end != null, "Start and end time are required");
        BadRequestException.check(end.isAfter(start), "End time must be after start time");
        this.start = start;
        this.end = end;
    }

    public static TimeSlot of(LocalDateTime start, Duration duration) {
        return new TimeSlot(start, start.plus(duration));
    }

    public boolean overlaps(TimeSlot other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public boolean isWithinOneDay() {
        return end.toLocalDate().equals(start.toLocalDate());
    }

    public LocalDate getDate() {
        return start.toLocalDate();
    }

    public Duration getDuration() {
        return Duration.between(start, end);
    }
}
