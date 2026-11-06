package com.maidbooking.controller;

import com.maidbooking.dto.request.CreateBookingRequest;
import com.maidbooking.dto.request.PaymentRequest;
import com.maidbooking.dto.request.RatingRequest;
import com.maidbooking.dto.response.BookingResponse;
import com.maidbooking.dto.response.PaymentResponse;
import com.maidbooking.mapper.BookingMapper;
import com.maidbooking.service.BookingService;
import com.maidbooking.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Bookings")
@RestController
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;
    private final PaymentService paymentService;

    @Operation(summary = "Book a maid (INSTANT, SCHEDULED or RECURRING)")
    @PostMapping("/maids/{maidId}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(@PathVariable UUID maidId, @Valid @RequestBody CreateBookingRequest request) {
        return BookingMapper.toResponse(bookingService.createBooking(
                maidId, request.getCustomerId(), request.getService(), BookingMapper.toSchedule(request)));
    }

    @Operation(summary = "Get a booking with all its visits")
    @GetMapping("/bookings/{bookingId}")
    public BookingResponse getBooking(@PathVariable UUID bookingId) {
        return BookingMapper.toResponse(bookingService.getBooking(bookingId));
    }

    @Operation(summary = "Pay for one visit")
    @PostMapping("/bookings/{bookingId}/visits/{visitId}/payments")
    public PaymentResponse payForVisit(@PathVariable UUID bookingId, @PathVariable UUID visitId,
                                       @Parameter(description = "Same key on retry returns the same receipt")
                                       @RequestHeader("Idempotency-Key") String idempotencyKey,
                                       @Valid @RequestBody PaymentRequest request) {
        return BookingMapper.toResponse(
                paymentService.payForVisit(bookingId, visitId, request.getMode(), idempotencyKey));
    }

    @Operation(summary = "Rate a visit after it ends (1 to 5 stars)")
    @PostMapping("/bookings/{bookingId}/visits/{visitId}/rating")
    public BookingResponse rateVisit(@PathVariable UUID bookingId, @PathVariable UUID visitId,
                                     @Valid @RequestBody RatingRequest request) {
        return BookingMapper.toResponse(bookingService.rateVisit(bookingId, visitId, request.getStars()));
    }

    @Operation(summary = "Cancel one visit")
    @DeleteMapping("/bookings/{bookingId}/visits/{visitId}")
    public BookingResponse cancelVisit(@PathVariable UUID bookingId, @PathVariable UUID visitId) {
        return BookingMapper.toResponse(bookingService.cancelVisit(bookingId, visitId));
    }

    @Operation(summary = "Cancel all upcoming visits of a booking")
    @DeleteMapping("/bookings/{bookingId}")
    public BookingResponse cancelBooking(@PathVariable UUID bookingId) {
        return BookingMapper.toResponse(bookingService.cancelBooking(bookingId));
    }
}
