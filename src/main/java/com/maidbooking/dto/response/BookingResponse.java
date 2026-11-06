package com.maidbooking.dto.response;

import com.maidbooking.model.BookingType;
import com.maidbooking.model.ServiceType;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookingResponse {
    private final UUID id;
    private final UUID maidId;
    private final String customerId;
    private final ServiceType service;
    private final BookingType type;
    private final List<VisitResponse> visits;
}
