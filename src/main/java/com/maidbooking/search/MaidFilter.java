package com.maidbooking.search;

import com.maidbooking.model.Maid;

/** To add a new search filter, add a new class that implements this. */
public interface MaidFilter {

    boolean isApplicable(MaidSearchCriteria criteria);

    boolean matches(Maid maid, MaidSearchCriteria criteria);
}
