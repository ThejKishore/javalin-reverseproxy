package org.example.gateway.routes.dao;

import java.util.List;

public record RouteDao(
	String stripPrefix,
	String pathPattern,
	int timeoutMs,
	List<TargetsItem> targets,
	boolean enabled,
	HeaderRules headerRules,
	CircuitBreakerPolicy circuitBreakerPolicy,
	String loadBalancerType,
	List<Object> authForwardHeaders,
	RateLimitPolicy rateLimitPolicy,
	String routingType,
	CachePolicy cachePolicy,
	String name,
	String auditStore,
	String id,
	boolean auditEnabled
) {
}