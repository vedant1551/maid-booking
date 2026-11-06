package com.maidbooking.search;

import com.maidbooking.model.Gender;
import com.maidbooking.model.ServiceType;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class MaidSearchCriteria {
    private static final int DEFAULT_DURATION_MINUTES = 60;

    String locality;
    ServiceType service;
    Set<ServiceType> skills;
    Gender gender;
    BigDecimal minRating;
    Long maxHourlyRatePaise;
    LocalDateTime startTime;
    LocalDateTime endTime;
    LocalDate date;
    boolean availableNow;
    Integer durationMinutes;
    LocalDateTime searchedAt;

    public boolean hasTimeFilter() {
        return startTime != null || endTime != null || date != null || availableNow;
    }

    public Duration getDuration() {
        return Duration.ofMinutes(durationMinutes == null ? DEFAULT_DURATION_MINUTES : durationMinutes);
    }
}
