package com.maidbooking.service;

import com.maidbooking.model.Maid;
import com.maidbooking.search.MaidSearchCriteria;
import java.util.List;
import java.util.UUID;

public interface MaidService {

    Maid onboard(Maid maid);

    Maid getMaid(UUID maidId);

    List<Maid> search(MaidSearchCriteria criteria);
}
