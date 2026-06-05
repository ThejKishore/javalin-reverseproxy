package org.example.gateway.routes.dao;

public record RateLimitPolicy(
	int timeoutDurationMs,
	int burst,
	int requestsPerSecond,
	boolean enabled
) {
}
