/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Rate-limiting policy attached to a single route. */
public class RateLimitPolicy {

    @JsonProperty("enabled")
    private boolean enabled = false;

    /** Maximum sustained requests per second. */
    @JsonProperty("requests-per-second")
    private int requestsPerSecond = 100;

    /** Maximum burst above the sustained rate (Resilience4j limitForPeriod). */
    @JsonProperty("burst")
    private int burst = 20;

    /**
     * How long (milliseconds) a caller waits for a permit before being rejected.
     * 0 means fail immediately if no permit is available.
     */
    @JsonProperty("timeout-duration-ms")
    private long timeoutDurationMs = 0;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getRequestsPerSecond() { return requestsPerSecond; }
    public void setRequestsPerSecond(int requestsPerSecond) { this.requestsPerSecond = requestsPerSecond; }

    public int getBurst() { return burst; }
    public void setBurst(int burst) { this.burst = burst; }

    public long getTimeoutDurationMs() { return timeoutDurationMs; }
    public void setTimeoutDurationMs(long timeoutDurationMs) { this.timeoutDurationMs = timeoutDurationMs; }
}

