package com.maidbooking.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MaidAvailabilityResponse {
    private final UUID maidId;
    private final LocalDate date;
    private final List<TimeRangeResponse> workingHours;
    private final List<TimeRangeResponse> booked;
    private final List<TimeRangeResponse> free;
}
