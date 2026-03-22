/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Caching policy for a single route. Backed by Caffeine. */
public class CachePolicy {

    @JsonProperty("enabled")
    private boolean enabled = false;

    /** How long (seconds) a cached response is considered valid. */
    @JsonProperty("ttl-seconds")
    private int ttlSeconds = 300;

    /**
     * Strategy used to build the cache key.
     * Supported values: METHOD_PATH, METHOD_PATH_QUERY.
     */
    @JsonProperty("cache-key-strategy")
    private String cacheKeyStrategy = "METHOD_PATH_QUERY";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(int ttlSeconds) { this.ttlSeconds = ttlSeconds; }

    public String getCacheKeyStrategy() { return cacheKeyStrategy; }
    public void setCacheKeyStrategy(String cacheKeyStrategy) { this.cacheKeyStrategy = cacheKeyStrategy; }
}

