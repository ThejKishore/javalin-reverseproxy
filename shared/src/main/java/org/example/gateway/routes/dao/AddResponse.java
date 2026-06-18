package org.example.gateway.routes.dao;

/**
 * A single header entry to be added to the response (persistence layer).
 *
 * @param name  HTTP header name (e.g. {@code X-Powered-By})
 * @param value HTTP header value
 */
public record AddResponse(
	String name,
	String value
) {
}