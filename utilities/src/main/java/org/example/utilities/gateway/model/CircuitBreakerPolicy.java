/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Circuit-breaker policy for a single route. Backed by Resilience4j. */
public class CircuitBreakerPolicy {

    @JsonProperty("enabled")
    private boolean enabled = false;

    /** Percentage of failures that trip the circuit breaker (0–100). */
    @JsonProperty("failure-rate-threshold")
    private int failureRateThreshold = 50;

    /** How long (seconds) the circuit stays open before moving to half-open. */
    @JsonProperty("wait-duration-seconds")
    private int waitDurationSeconds = 60;

    /** Size of the sliding window used to calculate the failure rate. */
    @JsonProperty("sliding-window-size")
    private int slidingWindowSize = 10;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getFailureRateThreshold() { return failureRateThreshold; }
    public void setFailureRateThreshold(int failureRateThreshold) { this.failureRateThreshold = failureRateThreshold; }

    public int getWaitDurationSeconds() { return waitDurationSeconds; }
    public void setWaitDurationSeconds(int waitDurationSeconds) { this.waitDurationSeconds = waitDurationSeconds; }

    public int getSlidingWindowSize() { return slidingWindowSize; }
    public void setSlidingWindowSize(int slidingWindowSize) { this.slidingWindowSize = slidingWindowSize; }
}

