package com.maidbooking.service.impl;

import com.maidbooking.exception.BadRequestException;
import com.maidbooking.exception.ConflictException;
import com.maidbooking.exception.NotFoundException;
import com.maidbooking.model.Booking;
import com.maidbooking.model.Maid;
import com.maidbooking.model.ServiceType;
import com.maidbooking.model.TimeSlot;
import com.maidbooking.model.Visit;
import com.maidbooking.payment.PaymentGatewayFactory;
import com.maidbooking.pricing.PricingStrategy;
import com.maidbooking.refund.RefundPolicy;
import com.maidbooking.repository.BookingRepository;
import com.maidbooking.schedule.ScheduleDetails;
import com.maidbooking.schedule.SlotPlannerFactory;
import com.maidbooking.service.BookingService;
import com.maidbooking.service.MaidService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final MaidService maidService;
    private final BookingRepository bookingRepository;
    private final SlotPlannerFactory slotPlannerFactory;
    private final PricingStrategy pricingStrategy;
    private final RefundPolicy refundPolicy;
    private final PaymentGatewayFactory paymentGatewayFactory;
    private final Clock clock;

    @Override
    public Booking createBooking(UUID maidId, String customerId, ServiceType service, ScheduleDetails schedule) {
        Maid maid = maidService.getMaid(maidId);
        BadRequestException.check(maid.offers(service), "Maid does not offer " + service);

        List<TimeSlot> slots = slotPlannerFactory.get(schedule.getType()).planSlots(schedule, LocalDateTime.now(clock));
        checkNoOverlap(slots);
        List<Visit> visits = slots.stream()
                .map(slot -> new Visit(slot, pricingStrategy.calculatePrice(maid, service, slot)))
                .toList();

        maid.reserve(visits);
        Booking booking = Booking.builder()
                .maidId(maidId)
                .customerId(customerId.strip())
                .service(service)
                .type(schedule.getType())
                .visits(visits)
                .build();
        return bookingRepository.save(booking);
    }

    @Override
    public Booking getBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking", bookingId));
    }

    @Override
    public Booking cancelVisit(UUID bookingId, UUID visitId) {
        Booking booking = getBooking(bookingId);
        Visit visit = booking.findVisit(visitId);
        visit.cancel(LocalDateTime.now(clock), refundPolicy, paymentGatewayFactory);
        maidService.getMaid(booking.getMaidId()).release(visit);
        return booking;
    }

    @Override
    public Booking cancelBooking(UUID bookingId) {
        Booking booking = getBooking(bookingId);
        Maid maid = maidService.getMaid(booking.getMaidId());
        LocalDateTime now = LocalDateTime.now(clock);
        boolean cancelledAny = false;
        // If a refund fails midway, a retry skips the visits already cancelled.
        for (Visit visit : booking.getVisits()) {
            if (visit.cancelIfActive(now, refundPolicy, paymentGatewayFactory)) {
                maid.release(visit);
                cancelledAny = true;
            }
        }
        if (!cancelledAny) {
            throw new ConflictException("Booking has no visits left that have not started");
        }
        return booking;
    }

    @Override
    public Booking rateVisit(UUID bookingId, UUID visitId, int stars) {
        Booking booking = getBooking(bookingId);
        booking.findVisit(visitId).rate(stars, LocalDateTime.now(clock));
        maidService.getMaid(booking.getMaidId()).addRating(stars);
        return booking;
    }

    private void checkNoOverlap(List<TimeSlot> slots) {
        List<TimeSlot> sorted = slots.stream().sorted(Comparator.comparing(TimeSlot::getStart)).toList();
        for (int i = 1; i < sorted.size(); i++) {
            BadRequestException.check(!sorted.get(i - 1).overlaps(sorted.get(i)), "Requested visits overlap each other");
        }
    }
}
