package org.example.gateway.routes.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Header manipulation rules applied to the upstream request and/or the
 * response returned to the client.
 *
 * <p>{@code addRequest} / {@code addResponse} each hold a list of
 * {@link AddRequest} / {@link AddResponse} entries so that multiple headers
 * can be injected in a single rule definition.
 */
public record HeaderRules(

	@JsonProperty("exclude-request")
	List<String> excludeRequest,

	@JsonProperty("add-request")
	List<AddRequest> addRequest,

	@JsonProperty("exclude-response")
	List<String> excludeResponse,

	@JsonProperty("add-response")
	List<AddResponse> addResponse
) {
}