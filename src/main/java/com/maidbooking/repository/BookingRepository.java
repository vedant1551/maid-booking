package com.maidbooking.repository;

import com.maidbooking.model.Booking;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(UUID bookingId);
}
