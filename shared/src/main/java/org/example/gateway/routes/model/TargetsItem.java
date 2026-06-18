package org.example.gateway.routes.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TargetsItem(

	@JsonProperty("header-match-value")
	String headerMatchValue,

	@JsonProperty("weight")
	int weight,

	@JsonProperty("header-match-name")
	String headerMatchName,

	@JsonProperty("url")
	String url
) {
}