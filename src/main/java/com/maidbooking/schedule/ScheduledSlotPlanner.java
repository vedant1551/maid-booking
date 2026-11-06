package com.maidbooking.schedule;

import com.maidbooking.exception.BadRequestException;
import com.maidbooking.model.BookingType;
import com.maidbooking.model.TimeSlot;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ScheduledSlotPlanner implements SlotPlanner {

    @Override
    public BookingType getType() {
        return BookingType.SCHEDULED;
    }

    @Override
    public List<TimeSlot> planSlots(ScheduleDetails details, LocalDateTime now) {
        BadRequestException.check(!details.hasDuration() && !details.hasRecurringFields(),
                "SCHEDULED booking takes only startTime and endTime");
        BadRequestException.check(details.getStartTime() != null && details.getEndTime() != null,
                "SCHEDULED booking needs startTime and endTime");
        TimeSlot slot = new TimeSlot(details.getStartTime(), details.getEndTime());
        BadRequestException.check(slot.getStart().isAfter(now), "Start time must be in the future");
        BadRequestException.check(slot.isWithinOneDay(), "A visit must start and end on the same day");
        return List.of(slot);
    }
}
