package org.example.gateway.routes.dao;

/**
 * A single header entry to be added to the upstream request (persistence layer).
 *
 * @param name  HTTP header name (e.g. {@code X-Custom-Header})
 * @param value HTTP header value
 */
public record AddRequest(
	String name,
	String value
) {
}