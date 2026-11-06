package com.maidbooking.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimeRangeResponse {
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
}
