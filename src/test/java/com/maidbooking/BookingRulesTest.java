package com.maidbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maidbooking.dto.money.Rupees;
import com.maidbooking.exception.BadRequestException;
import com.maidbooking.exception.ConflictException;
import com.maidbooking.exception.PaymentFailedException;
import com.maidbooking.model.BookingType;
import com.maidbooking.model.Gender;
import com.maidbooking.model.Maid;
import com.maidbooking.model.Payment;
import com.maidbooking.model.PaymentMode;
import com.maidbooking.model.ServiceType;
import com.maidbooking.model.TimeSlot;
import com.maidbooking.model.Visit;
import com.maidbooking.model.VisitStatus;
import com.maidbooking.model.WorkingHours;
import com.maidbooking.payment.PaymentGateway;
import com.maidbooking.payment.PaymentGatewayFactory;
import com.maidbooking.pricing.HourlyPricingStrategy;
import com.maidbooking.refund.TieredRefundPolicy;
import com.maidbooking.schedule.InstantSlotPlanner;
import com.maidbooking.schedule.RecurringSlotPlanner;
import com.maidbooking.schedule.ScheduleDetails;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookingRulesTest {
    private static final LocalDateTime MONDAY_9AM = LocalDateTime.parse("2030-01-14T09:00");
    private final TimeSlot nineToTen = TimeSlot.of(MONDAY_9AM, Duration.ofHours(1));

    @Test
    void visitEndingAtTenDoesNotOverlapVisitStartingAtTen() {
        assertThat(nineToTen.overlaps(TimeSlot.of(MONDAY_9AM.plusHours(1), Duration.ofHours(1)))).isFalse();
        assertThat(nineToTen.overlaps(TimeSlot.of(MONDAY_9AM.plusMinutes(30), Duration.ofHours(1)))).isTrue();
    }

    @Test
    void maidIsAvailableOnlyInsideWorkingHours() {
        Maid maid = maidWorking(
                new WorkingHours(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)),
                new WorkingHours(DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(12, 0)),
                new WorkingHours(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(16, 0)));
        assertThat(maid.isAvailable(TimeSlot.of(MONDAY_9AM.plusHours(1), Duration.ofHours(2)))).isTrue();
        assertThat(maid.isAvailable(TimeSlot.of(MONDAY_9AM.plusHours(2), Duration.ofHours(3)))).isFalse();
        assertThat(maid.isAvailable(TimeSlot.of(MONDAY_9AM.plusDays(1), Duration.ofHours(1)))).isFalse();
    }

    @Test
    void bookedTimeIsNoLongerAvailableAndReleasedTimeIsAvailableAgain() {
        Maid maid = maidWorking(new WorkingHours(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0)));
        Visit visit = new Visit(nineToTen, 24_000);
        maid.reserve(List.of(visit));
        assertThat(maid.isAvailable(nineToTen)).isFalse();
        assertThatThrownBy(() -> maid.reserve(List.of(new Visit(nineToTen, 24_000)))).isInstanceOf(ConflictException.class);
        maid.release(visit);
        assertThat(maid.isAvailable(nineToTen)).isTrue();
    }

    @Test
    void recurringBookingCreatesVisitsOnChosenDaysIncludingStartAndEndDate() {
        ScheduleDetails details = ScheduleDetails.builder().type(BookingType.RECURRING)
                .startDate(LocalDate.of(2030, 1, 14)).endDate(LocalDate.of(2030, 1, 28))
                .daysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
                .dailyStartTime(LocalTime.of(9, 0)).dailyEndTime(LocalTime.of(10, 30))
                .build();
        List<TimeSlot> slots = new RecurringSlotPlanner().planSlots(details, MONDAY_9AM.minusDays(1));
        assertThat(slots).extracting(TimeSlot::getStart).containsExactly(
                MONDAY_9AM, MONDAY_9AM.plusDays(2), MONDAY_9AM.plusDays(7), MONDAY_9AM.plusDays(9), MONDAY_9AM.plusDays(14));
        assertThat(slots).allMatch(slot -> slot.getEnd().toLocalTime().equals(LocalTime.of(10, 30)));
    }

    @Test
    void instantBookingStartsAtTheNextWholeMinute() {
        assertThat(InstantSlotPlanner.nextWholeMinute(LocalDateTime.parse("2030-01-14T16:20:35")))
                .isEqualTo(LocalDateTime.parse("2030-01-14T16:21"));
        assertThat(InstantSlotPlanner.nextWholeMinute(MONDAY_9AM)).isEqualTo(MONDAY_9AM);
    }

    @Test
    void priceIsHourlyRateForTheVisitLengthInPaise() {
        Maid maid = maidWorking();
        assertThat(new HourlyPricingStrategy().calculatePrice(maid, ServiceType.COOKING,
                TimeSlot.of(MONDAY_9AM, Duration.ofMinutes(90)))).isEqualTo(36_000);
    }

    @Test
    void refundDependsOnHowEarlyTheVisitIsCancelled() {
        TieredRefundPolicy policy = new TieredRefundPolicy();
        Payment payment = new Payment("key", PaymentMode.UPI, "ref", 10_000, MONDAY_9AM.minusDays(3));
        assertThat(policy.calculateRefund(payment, nineToTen, MONDAY_9AM.minusHours(24))).isEqualTo(10_000);
        assertThat(policy.calculateRefund(payment, nineToTen, MONDAY_9AM.minusHours(2))).isEqualTo(5_000);
        assertThat(policy.calculateRefund(payment, nineToTen, MONDAY_9AM.minusMinutes(119))).isZero();
    }

    @Test
    void failedRefundLeavesTheVisitPaidSoCancelCanBeRetried() {
        PaymentGatewayFactory gateways = new PaymentGatewayFactory(List.of(new RefundFailsGateway()));
        Visit visit = new Visit(nineToTen, 24_000);
        visit.pay("key", gateways.get(PaymentMode.UPI), MONDAY_9AM.minusDays(2));

        assertThatThrownBy(() -> visit.cancel(MONDAY_9AM.minusDays(2), new TieredRefundPolicy(), gateways))
                .isInstanceOf(PaymentFailedException.class);
        assertThat(visit.getStatus()).isEqualTo(VisitStatus.PAID);
        assertThat(visit.getRefund()).isNull();
    }

    @Test
    void maidRatingIsTheAverageOfCustomerRatings() {
        Maid maid = maidWorking();
        assertThat(maid.getRating()).isNull();
        maid.addRating(4);
        maid.addRating(5);
        assertThat(maid.getRating()).isEqualByComparingTo("4.5");
        assertThat(maid.getRatingCount()).isEqualTo(2);
    }

    @Test
    void visitCanBeRatedOnceAndOnlyAfterItEnds() {
        Visit visit = new Visit(nineToTen, 24_000);
        assertThatThrownBy(() -> visit.rate(5, MONDAY_9AM.plusMinutes(30))).isInstanceOf(ConflictException.class);
        visit.rate(5, MONDAY_9AM.plusHours(1));
        assertThat(visit.getRating()).isEqualTo(5);
        assertThatThrownBy(() -> visit.rate(4, MONDAY_9AM.plusHours(2))).isInstanceOf(ConflictException.class);
    }

    @Test
    void rupeesConvertToPaiseAndBack() {
        assertThat(Rupees.toPaise(new BigDecimal("240.50"))).isEqualTo(24_050);
        assertThat(Rupees.fromPaise(24_050)).isEqualByComparingTo("240.50");
        assertThatThrownBy(() -> Rupees.toPaise(new BigDecimal("1.005"))).isInstanceOf(BadRequestException.class);
    }

    private static Maid maidWorking(WorkingHours... hours) {
        return Maid.builder().name("Asha").locality("Ahmedabad").skills(Set.of(ServiceType.COOKING)).gender(Gender.FEMALE)
                .hourlyRatePaise(24_000).workingHours(List.of(hours)).build();
    }

    private static class RefundFailsGateway implements PaymentGateway {
        @Override
        public PaymentMode getMode() {
            return PaymentMode.UPI;
        }

        @Override
        public String charge(UUID visitId, long amountPaise, String idempotencyKey) {
            return "charge-1";
        }

        @Override
        public String refund(String chargeReference, long amountPaise) {
            throw new PaymentFailedException("Refund service is down");
        }
    }
}
