package com.maidbooking.repository.inmemory;

import com.maidbooking.repository.IdempotencyKeyRepository;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryIdempotencyKeyRepository implements IdempotencyKeyRepository {
    private final Map<String, UUID> keyToVisit = new ConcurrentHashMap<>();

    @Override
    public UUID claim(String idempotencyKey, UUID visitId) {
        UUID owner = keyToVisit.putIfAbsent(idempotencyKey, visitId);
        return owner == null ? visitId : owner;
    }
}
