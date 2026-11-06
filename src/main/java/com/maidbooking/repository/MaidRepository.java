package com.maidbooking.repository;

import com.maidbooking.model.Maid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaidRepository {

    Maid save(Maid maid);

    Optional<Maid> findById(UUID maidId);

    List<Maid> findAll();
}
