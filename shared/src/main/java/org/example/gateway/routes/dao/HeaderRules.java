package org.example.gateway.routes.dao;

import java.util.List;

/**
 * Header manipulation rules (persistence layer).
 *
 * <p>{@code addRequest} and {@code addResponse} are lists so that multiple
 * headers can be configured per route.
 */
public record HeaderRules(
	List<String> excludeRequest,
	List<AddRequest> addRequest,
	List<String> excludeResponse,
	List<AddResponse> addResponse
) {
}