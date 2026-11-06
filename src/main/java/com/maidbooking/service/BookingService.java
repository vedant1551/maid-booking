package com.maidbooking.service;

import com.maidbooking.model.Booking;
import com.maidbooking.model.ServiceType;
import com.maidbooking.schedule.ScheduleDetails;
import java.util.UUID;

public interface BookingService {

    Booking createBooking(UUID maidId, String customerId, ServiceType service, ScheduleDetails schedule);

    Booking getBooking(UUID bookingId);

    Booking cancelVisit(UUID bookingId, UUID visitId);

    Booking cancelBooking(UUID bookingId);

    Booking rateVisit(UUID bookingId, UUID visitId, int stars);
}
