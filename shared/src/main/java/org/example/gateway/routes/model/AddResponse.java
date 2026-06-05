package org.example.gateway.routes.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single header entry to be added to the response returned to the client.
 *
 * <p>Used inside {@link HeaderRules#addResponse()} — a route can specify
 * one or more response headers to inject before replying to the caller.
 *
 * <pre>{@code
 * add-response:
 *   - name: X-Powered-By
 *     value: Javalin-Gateway
 *   - name: Strict-Transport-Security
 *     value: max-age=31536000
 * }</pre>
 */
public record AddResponse(

	@JsonProperty("name")
	String name,

	@JsonProperty("value")
	String value
) {
}