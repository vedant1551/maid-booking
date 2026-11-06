package com.maidbooking.repository;

import java.util.UUID;

public interface IdempotencyKeyRepository {

    UUID claim(String idempotencyKey, UUID visitId);
}
