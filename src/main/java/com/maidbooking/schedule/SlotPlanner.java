package com.maidbooking.schedule;

import com.maidbooking.model.BookingType;
import com.maidbooking.model.TimeSlot;
import java.time.LocalDateTime;
import java.util.List;

public interface SlotPlanner {

    BookingType getType();

    List<TimeSlot> planSlots(ScheduleDetails details, LocalDateTime now);
}
