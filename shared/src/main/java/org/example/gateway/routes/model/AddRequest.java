package org.example.gateway.routes.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single header entry to be added to the upstream request.
 *
 * <p>Used inside {@link HeaderRules#addRequest()} — a route can specify
 * one or more request headers to inject before forwarding to the upstream.
 *
 * <pre>{@code
 * add-request:
 *   - name: X-Custom-Header
 *     value: my-value
 *   - name: Authorization
 *     value: Bearer token
 * }</pre>
 */
public record AddRequest(

	@JsonProperty("name")
	String name,

	@JsonProperty("value")
	String value
) {
}