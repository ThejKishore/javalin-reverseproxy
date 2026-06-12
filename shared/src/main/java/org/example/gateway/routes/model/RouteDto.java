package org.example.gateway.routes.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RouteDto(

	@JsonProperty("strip-prefix")
	String stripPrefix,

	@JsonProperty("path-pattern")
	String pathPattern,

	@JsonProperty("timeout-ms")
	int timeoutMs,

	@JsonProperty("targets")
	List<TargetsItem> targets,

	@JsonProperty("enabled")
	boolean enabled,

	@JsonProperty("header-rules")
	HeaderRules headerRules,

	@JsonProperty("circuit-breaker-policy")
	CircuitBreakerPolicy circuitBreakerPolicy,

	@JsonProperty("load-balancer-type")
	String loadBalancerType,

	@JsonProperty("auth-forward-headers")
	List<String> authForwardHeaders,

	@JsonProperty("rate-limit-policy")
	RateLimitPolicy rateLimitPolicy,

	@JsonProperty("routing-type")
	String routingType,

	@JsonProperty("cache-policy")
	CachePolicy cachePolicy,

	@JsonProperty("name")
	String name,

	@JsonProperty("audit-store")
	String auditStore,

	@JsonProperty("id")
	String id,

	@JsonProperty("audit-enabled")
	boolean auditEnabled,

	/**
	 * This map is used to capture details like
	 *  context-path: /domain/servicename
	 *  health-endpoint: /actuator/health
	 *  prometheus-endpoint: /actuator/prometheus
	 *  openapi-spec-endpoint: /openapi(javalin) or /api-docs(springboot)
	 *  skip-api: "true"
	 *  skip-paths: /admin,/internal
	 */
	@JsonProperty("meta-data")
    Map<String,String> metaData

) {
}