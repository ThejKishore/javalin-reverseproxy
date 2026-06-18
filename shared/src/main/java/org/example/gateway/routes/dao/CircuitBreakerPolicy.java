package org.example.gateway.routes.dao;

public record CircuitBreakerPolicy(
	int waitDurationSeconds,
	int slidingWindowSize,
	int failureRateThreshold,
	boolean enabled
) {
}
