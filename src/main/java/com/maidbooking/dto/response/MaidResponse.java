package com.maidbooking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.maidbooking.model.Gender;
import com.maidbooking.model.ServiceType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MaidResponse {
    private final UUID id;
    private final String name;
    private final String locality;
    private final Set<ServiceType> skills;
    private final Gender gender;
    private final BigDecimal rating;
    private final int ratingCount;
    @Schema(description = "Rupees", example = "240.50")
    private final BigDecimal hourlyRate;
    private final List<WorkingHoursResponse> workingHours;
}
