package com.maidbooking.repository.inmemory;

import com.maidbooking.model.Maid;
import com.maidbooking.repository.MaidRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryMaidRepository implements MaidRepository {
    private final Map<UUID, Maid> maids = new ConcurrentHashMap<>();

    @Override
    public Maid save(Maid maid) {
        maids.put(maid.getId(), maid);
        return maid;
    }

    @Override
    public Optional<Maid> findById(UUID maidId) {
        return Optional.ofNullable(maids.get(maidId));
    }

    @Override
    public List<Maid> findAll() {
        return List.copyOf(maids.values());
    }
}
