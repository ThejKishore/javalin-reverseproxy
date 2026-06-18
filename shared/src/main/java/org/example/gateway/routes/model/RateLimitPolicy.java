package org.example.gateway.routes.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RateLimitPolicy(

	@JsonProperty("timeout-duration-ms")
	int timeoutDurationMs,

	@JsonProperty("burst")
	int burst,

	@JsonProperty("requests-per-second")
	int requestsPerSecond,

	@JsonProperty("enabled")
	boolean enabled
) {
}