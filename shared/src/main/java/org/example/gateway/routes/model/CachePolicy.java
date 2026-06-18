package org.example.gateway.routes.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CachePolicy(

	@JsonProperty("ttl-seconds")
	int ttlSeconds,

	@JsonProperty("cache-key-strategy")
	String cacheKeyStrategy,

	@JsonProperty("enabled")
	boolean enabled
) {
}