package com.maidbooking.schedule;

import com.maidbooking.exception.BadRequestException;
import com.maidbooking.model.BookingType;
import com.maidbooking.model.TimeSlot;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InstantSlotPlanner implements SlotPlanner {
    private static final int MAX_MINUTES = 12 * 60;

    @Override
    public BookingType getType() {
        return BookingType.INSTANT;
    }

    @Override
    public List<TimeSlot> planSlots(ScheduleDetails details, LocalDateTime now) {
        BadRequestException.check(!details.hasStartOrEndTime() && !details.hasRecurringFields(),
                "INSTANT booking takes only durationMinutes; it always starts now");
        Integer minutes = details.getDurationMinutes();
        BadRequestException.check(minutes != null && minutes > 0 && minutes <= MAX_MINUTES,
                "Instant booking needs durationMinutes between 1 and " + MAX_MINUTES);
        return List.of(TimeSlot.of(nextWholeMinute(now), Duration.ofMinutes(minutes)));
    }

    // Start on a whole minute so the visit lines up with working hours.
    public static LocalDateTime nextWholeMinute(LocalDateTime time) {
        LocalDateTime truncated = time.truncatedTo(ChronoUnit.MINUTES);
        return truncated.equals(time) ? time : truncated.plusMinutes(1);
    }
}
