package com.maidbooking.search.filter;

import com.maidbooking.model.Maid;
import com.maidbooking.model.TimeSlot;
import com.maidbooking.schedule.InstantSlotPlanner;
import com.maidbooking.search.MaidFilter;
import com.maidbooking.search.MaidSearchCriteria;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AvailabilityFilter implements MaidFilter {

    @Override
    public boolean isApplicable(MaidSearchCriteria criteria) {
        return criteria.hasTimeFilter();
    }

    @Override
    public boolean matches(Maid maid, MaidSearchCriteria criteria) {
        if (criteria.getStartTime() != null) {
            return maid.isAvailable(new TimeSlot(criteria.getStartTime(), criteria.getEndTime()));
        }
        if (criteria.getDate() != null) {
            return maid.hasFreeTimeOn(criteria.getDate(), criteria.getDuration(), criteria.getSearchedAt());
        }
        TimeSlot fromNow = TimeSlot.of(InstantSlotPlanner.nextWholeMinute(criteria.getSearchedAt()), criteria.getDuration());
        return maid.isAvailable(fromNow);
    }
}
