package org.example.gateway.routes.dao;

public record TargetsItem(
	String headerMatchValue,
	int weight,
	String headerMatchName,
	String url
) {
}
