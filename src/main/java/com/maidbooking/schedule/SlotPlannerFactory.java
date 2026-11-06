package com.maidbooking.schedule;

import com.maidbooking.exception.BadRequestException;
import com.maidbooking.model.BookingType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SlotPlannerFactory {
    private final Map<BookingType, SlotPlanner> planners = new EnumMap<>(BookingType.class);

    public SlotPlannerFactory(List<SlotPlanner> planners) {
        planners.forEach(planner -> this.planners.put(planner.getType(), planner));
    }

    public SlotPlanner get(BookingType type) {
        SlotPlanner planner = type == null ? null : planners.get(type);
        BadRequestException.check(planner != null, "Unsupported booking type: " + type);
        return planner;
    }
}
