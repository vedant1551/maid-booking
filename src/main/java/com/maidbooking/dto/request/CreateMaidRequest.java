package com.maidbooking.dto.request;

import com.maidbooking.dto.money.Rupees;
import com.maidbooking.model.Gender;
import com.maidbooking.model.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateMaidRequest {
    @NotBlank
    @Size(max = 100)
    private String name;
    @NotBlank
    @Size(max = 100)
    private String locality;
    @NotEmpty
    private Set<@NotNull ServiceType> skills;
    @NotNull
    private Gender gender;
    @NotNull
    @DecimalMin("1")
    @DecimalMax("100000")
    @Digits(integer = 6, fraction = 2)
    @Schema(description = "Rupees per hour", example = "240.50")
    private BigDecimal hourlyRate;
    @NotEmpty
    private List<@NotNull @Valid WorkingHoursRequest> workingHours;

    public long hourlyRateInPaise() {
        return Rupees.toPaise(hourlyRate);
    }
}
