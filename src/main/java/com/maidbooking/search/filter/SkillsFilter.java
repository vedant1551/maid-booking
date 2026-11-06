package com.maidbooking.search.filter;

import com.maidbooking.model.Maid;
import com.maidbooking.search.MaidFilter;
import com.maidbooking.search.MaidSearchCriteria;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class SkillsFilter implements MaidFilter {

    @Override
    public boolean isApplicable(MaidSearchCriteria criteria) {
        return criteria.getSkills() != null && !criteria.getSkills().isEmpty();
    }

    @Override
    public boolean matches(Maid maid, MaidSearchCriteria criteria) {
        return criteria.getSkills().stream().allMatch(maid::offers);
    }
}
