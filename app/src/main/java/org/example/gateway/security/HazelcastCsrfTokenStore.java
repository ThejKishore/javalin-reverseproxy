/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.security;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.example.utilities.gateway.security.CsrfTokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Hazelcast-backed {@link CsrfTokenStore}.
 *
 * <p>Tokens are stored in a distributed {@link IMap} named
 * {@code "gateway-csrf-tokens"}, propagated automatically across all gateway
 * replicas in the same Hazelcast cluster. Each entry is created with an
 * entry-level TTL so Hazelcast evicts expired tokens without a background job.
 */
public class HazelcastCsrfTokenStore implements CsrfTokenStore {

    private static final Logger log = LoggerFactory.getLogger(HazelcastCsrfTokenStore.class);
    private static final String MAP_NAME = "gateway-csrf-tokens";

    private final IMap<String, String> tokenMap;

    public HazelcastCsrfTokenStore(HazelcastInstance hazelcast) {
        this.tokenMap = hazelcast.getMap(MAP_NAME);
        log.info("HazelcastCsrfTokenStore initialised (map={})", MAP_NAME);
    }

    @Override
    public void store(String clientKey, String token, int ttlSeconds) {
        tokenMap.put(clientKey, token, ttlSeconds, TimeUnit.SECONDS);
        log.debug("CSRF token stored for key={} ttl={}s", clientKey, ttlSeconds);
    }

    @Override
    public String get(String clientKey) {
        return tokenMap.get(clientKey);
    }

    @Override
    public void invalidate(String clientKey) {
        tokenMap.remove(clientKey);
        log.debug("CSRF token invalidated for key={}", clientKey);
    }
}

