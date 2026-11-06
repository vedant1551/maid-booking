package com.maidbooking.model;

import com.maidbooking.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Booking {
    private final UUID id;
    private final UUID maidId;
    private final String customerId;
    private final ServiceType service;
    private final BookingType type;
    private final List<Visit> visits;

    @Builder
    private Booking(UUID maidId, String customerId, ServiceType service, BookingType type, List<Visit> visits) {
        this.id = UUID.randomUUID();
        this.maidId = maidId;
        this.customerId = customerId;
        this.service = service;
        this.type = type;
        this.visits = List.copyOf(visits);
    }

    public Visit findVisit(UUID visitId) {
        return visits.stream()
                .filter(visit -> visit.getId().equals(visitId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Visit", visitId));
    }
}
