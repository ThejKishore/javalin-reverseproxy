package org.example.gateway.routes.dao;

import java.util.List;
import java.util.Map;

public record RouteDao(
	String stripPrefix,
	String pathPattern,
	int timeoutMs,
	List<TargetsItem> targets,
	boolean enabled,
	HeaderRules headerRules,
	CircuitBreakerPolicy circuitBreakerPolicy,
	String loadBalancerType,
	List<String> authForwardHeaders,
	RateLimitPolicy rateLimitPolicy,
	String routingType,
	CachePolicy cachePolicy,
	String name,
	String auditStore,
	String id,
	boolean auditEnabled,
	Map<String,String> metaData

) {
}