package com.maidbooking.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WorkingHoursRequest {
    @NotNull
    private DayOfWeek day;
    @NotNull
    private LocalTime from;
    @NotNull
    private LocalTime to;
}
