package com.maidbooking.dto.response;

import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WorkingHoursResponse {
    private final DayOfWeek day;
    private final LocalTime from;
    private final LocalTime to;
}
