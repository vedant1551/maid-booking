package com.maidbooking.schedule;

import com.maidbooking.exception.BadRequestException;
import com.maidbooking.model.BookingType;
import com.maidbooking.model.TimeSlot;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RecurringSlotPlanner implements SlotPlanner {
    private static final int MAX_DAYS = 366;

    @Override
    public BookingType getType() {
        return BookingType.RECURRING;
    }

    @Override
    public List<TimeSlot> planSlots(ScheduleDetails details, LocalDateTime now) {
        validate(details);
        LocalDate lastDate = details.getEndDate();
        List<TimeSlot> slots = details.getStartDate().datesUntil(lastDate.plusDays(1))
                .filter(date -> details.getDaysOfWeek().contains(date.getDayOfWeek()))
                .map(date -> new TimeSlot(date.atTime(details.getDailyStartTime()), date.atTime(details.getDailyEndTime())))
                .filter(slot -> slot.getStart().isAfter(now))
                .toList();
        BadRequestException.check(!slots.isEmpty(), "No upcoming visit falls on the selected days in this date range");
        return slots;
    }

    private void validate(ScheduleDetails details) {
        BadRequestException.check(!details.hasDuration() && !details.hasStartOrEndTime(),
                "RECURRING booking takes only startDate, endDate, daysOfWeek, dailyStartTime and dailyEndTime");
        BadRequestException.check(details.getStartDate() != null && details.getEndDate() != null,
                "Recurring booking needs startDate and endDate");
        BadRequestException.check(details.getDailyStartTime() != null && details.getDailyEndTime() != null,
                "Recurring booking needs dailyStartTime and dailyEndTime");
        BadRequestException.check(details.getDailyStartTime().isBefore(details.getDailyEndTime()),
                "dailyEndTime must be after dailyStartTime");
        BadRequestException.check(details.getDaysOfWeek() != null && !details.getDaysOfWeek().isEmpty(),
                "Recurring booking needs at least one day of week");
        BadRequestException.check(!details.getEndDate().isBefore(details.getStartDate()),
                "endDate must not be before startDate");
        BadRequestException.check(ChronoUnit.DAYS.between(details.getStartDate(), details.getEndDate()) < MAX_DAYS,
                "A recurring booking can span at most " + MAX_DAYS + " days");
    }
}
