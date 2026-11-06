package com.maidbooking.search.filter;

import com.maidbooking.model.Maid;
import com.maidbooking.search.MaidFilter;
import com.maidbooking.search.MaidSearchCriteria;
import java.math.BigDecimal;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(5)
public class MinRatingFilter implements MaidFilter {

    @Override
    public boolean isApplicable(MaidSearchCriteria criteria) {
        return criteria.getMinRating() != null;
    }

    @Override
    public boolean matches(Maid maid, MaidSearchCriteria criteria) {
        BigDecimal rating = maid.getRating() == null ? BigDecimal.ZERO : maid.getRating();
        return rating.compareTo(criteria.getMinRating()) >= 0;
    }
}
