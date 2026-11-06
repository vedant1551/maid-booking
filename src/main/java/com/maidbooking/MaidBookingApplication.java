package com.maidbooking;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MaidBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaidBookingApplication.class, args);
    }

    @Bean
    public Clock clock(@Value("${booking.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
