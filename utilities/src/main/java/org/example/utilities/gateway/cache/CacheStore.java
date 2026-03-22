/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.cache;

import java.util.Optional;

/** Abstraction over a key/value cache used by {@link CacheGatewayFilter}. */
public interface CacheStore {
    Optional<CachedResponse> get(String key);
    void put(String key, CachedResponse response);
    void invalidate(String key);
    void invalidateAll();

    /** An immutable snapshot of an upstream response that can be cached. */
    record CachedResponse(int statusCode, java.util.Map<String, java.util.List<String>> headers, byte[] body) {}
}

