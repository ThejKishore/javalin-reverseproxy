package org.example.gateway.routes.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CircuitBreakerPolicy(

	@JsonProperty("wait-duration-seconds")
	int waitDurationSeconds,

	@JsonProperty("sliding-window-size")
	int slidingWindowSize,

	@JsonProperty("failure-rate-threshold")
	int failureRateThreshold,

	@JsonProperty("enabled")
	boolean enabled
) {
}