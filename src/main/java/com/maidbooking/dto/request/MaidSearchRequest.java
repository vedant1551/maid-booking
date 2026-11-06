package com.maidbooking.dto.request;

import com.maidbooking.dto.money.Rupees;
import com.maidbooking.model.Gender;
import com.maidbooking.model.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MaidSearchRequest {
    private String locality;
    private ServiceType service;
    private Set<ServiceType> skills;
    private Gender gender;
    @DecimalMin("0")
    @DecimalMax("5")
    private BigDecimal minRating;
    @DecimalMin("0")
    @Schema(description = "Rupees per hour", example = "300")
    private BigDecimal maxHourlyRate;
    @Schema(description = "Local IST time without offset", example = "2030-01-14T10:00")
    private LocalDateTime startTime;
    @Schema(description = "Local IST time without offset", example = "2030-01-14T12:00")
    private LocalDateTime endTime;
    private LocalDate date;
    private Boolean availableNow;
    @Min(1)
    @Max(720)
    @Schema(description = "Used with date or availableNow. Defaults to 60")
    private Integer durationMinutes;

    public Long maxHourlyRateInPaise() {
        return maxHourlyRate == null ? null : Rupees.toPaise(maxHourlyRate);
    }
}
