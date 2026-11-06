package com.maidbooking.search.filter;

import com.maidbooking.model.Maid;
import com.maidbooking.search.MaidFilter;
import com.maidbooking.search.MaidSearchCriteria;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(6)
public class MaxPriceFilter implements MaidFilter {

    @Override
    public boolean isApplicable(MaidSearchCriteria criteria) {
        return criteria.getMaxHourlyRatePaise() != null;
    }

    @Override
    public boolean matches(Maid maid, MaidSearchCriteria criteria) {
        return maid.getHourlyRatePaise() <= criteria.getMaxHourlyRatePaise();
    }
}
