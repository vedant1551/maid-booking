package com.maidbooking.mapper;

import com.maidbooking.dto.money.Rupees;
import com.maidbooking.dto.request.CreateBookingRequest;
import com.maidbooking.dto.response.BookingResponse;
import com.maidbooking.dto.response.PaymentResponse;
import com.maidbooking.dto.response.RefundResponse;
import com.maidbooking.dto.response.VisitResponse;
import com.maidbooking.model.Booking;
import com.maidbooking.model.Payment;
import com.maidbooking.model.Refund;
import com.maidbooking.model.Visit;
import com.maidbooking.schedule.ScheduleDetails;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BookingMapper {

    public ScheduleDetails toSchedule(CreateBookingRequest request) {
        return ScheduleDetails.builder()
                .type(request.getType())
                .durationMinutes(request.getDurationMinutes())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .daysOfWeek(request.getDaysOfWeek())
                .dailyStartTime(request.getDailyStartTime())
                .dailyEndTime(request.getDailyEndTime())
                .build();
    }

    public BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .maidId(booking.getMaidId())
                .customerId(booking.getCustomerId())
                .service(booking.getService())
                .type(booking.getType())
                .visits(booking.getVisits().stream().map(BookingMapper::toResponse).toList())
                .build();
    }

    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }
        return PaymentResponse.builder()
                .idempotencyKey(payment.getIdempotencyKey())
                .mode(payment.getMode())
                .reference(payment.getReference())
                .amount(Rupees.fromPaise(payment.getAmountPaise()))
                .paidAt(payment.getPaidAt())
                .build();
    }

    private VisitResponse toResponse(Visit visit) {
        return VisitResponse.builder()
                .id(visit.getId())
                .startTime(visit.getSlot().getStart())
                .endTime(visit.getSlot().getEnd())
                .price(Rupees.fromPaise(visit.getPricePaise()))
                .status(visit.getStatus())
                .payment(toResponse(visit.getPayment()))
                .refund(toResponse(visit.getRefund()))
                .rating(visit.getRating())
                .build();
    }

    private RefundResponse toResponse(Refund refund) {
        if (refund == null) {
            return null;
        }
        return RefundResponse.builder()
                .amount(Rupees.fromPaise(refund.getAmountPaise()))
                .reference(refund.getReference())
                .refundedAt(refund.getRefundedAt())
                .build();
    }
}
