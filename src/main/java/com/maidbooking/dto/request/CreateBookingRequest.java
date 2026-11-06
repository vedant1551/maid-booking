package com.maidbooking.dto.request;

import com.maidbooking.model.BookingType;
import com.maidbooking.model.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateBookingRequest {
    @NotBlank
    @Size(max = 64)
    private String customerId;
    @NotNull
    private ServiceType service;
    @NotNull
    private BookingType type;

    @Schema(description = "INSTANT only", example = "90")
    private Integer durationMinutes;

    @Schema(description = "SCHEDULED only. Local IST time without offset", example = "2030-01-14T10:00")
    private LocalDateTime startTime;
    @Schema(description = "SCHEDULED only. Local IST time without offset", example = "2030-01-14T12:00")
    private LocalDateTime endTime;

    private LocalDate startDate;
    private LocalDate endDate;
    private Set<DayOfWeek> daysOfWeek;
    private LocalTime dailyStartTime;
    private LocalTime dailyEndTime;
}
