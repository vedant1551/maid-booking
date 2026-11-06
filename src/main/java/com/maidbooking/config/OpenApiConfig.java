package com.maidbooking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI maidBookingApi() {
        return new OpenAPI().info(new Info()
                .title("Maid Booking API")
                .version("1.0.0")
                .description("Find maids, book them (instant, scheduled or recurring), pay per visit, cancel and rate. "
                        + "Money is in rupees with up to 2 decimals. Times are local IST without an offset, e.g. 2030-01-14T10:00."));
    }

    @Bean
    public OpenApiCustomizer errorResponses() {
        return openApi -> openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> operation.getResponses()
                .addApiResponse("400", new ApiResponse().description("Invalid request; the message names the field"))
                .addApiResponse("404", new ApiResponse().description("Maid, booking or visit not found"))
                .addApiResponse("409", new ApiResponse().description("Conflicts with current state, e.g. slot already booked"))));
    }
}
