package com.maidbooking.search.filter;

import com.maidbooking.model.Maid;
import com.maidbooking.search.MaidFilter;
import com.maidbooking.search.MaidSearchCriteria;
import com.maidbooking.util.TextUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class LocalityFilter implements MaidFilter {

    @Override
    public boolean isApplicable(MaidSearchCriteria criteria) {
        return criteria.getLocality() != null;
    }

    @Override
    public boolean matches(Maid maid, MaidSearchCriteria criteria) {
        return maid.getLocality().equals(TextUtils.normalize(criteria.getLocality()));
    }
}
