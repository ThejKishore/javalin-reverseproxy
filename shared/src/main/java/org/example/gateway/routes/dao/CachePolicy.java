package org.example.gateway.routes.dao;

public record CachePolicy(
	int ttlSeconds,
	String cacheKeyStrategy,
	boolean enabled
) {
}
