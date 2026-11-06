package com.maidbooking.model;

import com.maidbooking.exception.BadRequestException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.Value;

@Value
public class WorkingHours {
    DayOfWeek day;
    LocalTime from;
    LocalTime to;

    public WorkingHours(DayOfWeek day, LocalTime from, LocalTime to) {
        BadRequestException.check(day != null && from != null && to != null, "Working hours need a day, from and to");
        BadRequestException.check(from.isBefore(to), "Working hours must start before they end");
        this.day = day;
        this.from = from;
        this.to = to;
    }
}
