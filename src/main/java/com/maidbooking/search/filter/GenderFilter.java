package com.maidbooking.search.filter;

import com.maidbooking.model.Maid;
import com.maidbooking.search.MaidFilter;
import com.maidbooking.search.MaidSearchCriteria;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(4)
public class GenderFilter implements MaidFilter {

    @Override
    public boolean isApplicable(MaidSearchCriteria criteria) {
        return criteria.getGender() != null;
    }

    @Override
    public boolean matches(Maid maid, MaidSearchCriteria criteria) {
        return maid.getGender() == criteria.getGender();
    }
}
