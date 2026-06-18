/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.routes.mapper;

import org.example.gateway.routes.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for RouteMapper and DiffHelper.
 * Demonstrates recursive mapping and change detection.
 */
class RouteMapperTest {

    @Test
    void testRouteDtoToJsonConversion() {
        RouteDto routeDto = createSampleRouteDto();
        
        String json = RouteMapper.routeDtoToJson(routeDto);
        
        assertThat(json).isNotNull();
        assertThat(json).contains("\"id\":\"route-1\"");
        assertThat(json).contains("\"name\":\"Sample Route\"");
        assertThat(json).contains("\"enabled\":true");
        assertThat(json).contains("\"path-pattern\":\"/api/v1/users\"");
    }

    @Test
    void testJsonToRouteDtoConversion() {
        RouteDto original = createSampleRouteDto();
        String json = RouteMapper.routeDtoToJson(original);
        
        RouteDto deserialized = RouteMapper.jsonToRouteDto(json);
        
        assertThat(deserialized).isNotNull();
        assertThat(deserialized.id()).isEqualTo(original.id());
        assertThat(deserialized.name()).isEqualTo(original.name());
        assertThat(deserialized.enabled()).isEqualTo(original.enabled());
        assertThat(deserialized.pathPattern()).isEqualTo(original.pathPattern());
        assertThat(deserialized.timeoutMs()).isEqualTo(original.timeoutMs());
    }

    @Test
    void testRecursiveNestedObjectMapping() {
        RouteDto routeDto = createSampleRouteDto();
        
        String json = RouteMapper.routeDtoToJson(routeDto);
        RouteDto restored = RouteMapper.jsonToRouteDto(json);
        
        // Verify nested CachePolicy is mapped correctly
        assertThat(restored.cachePolicy()).isNotNull();
        assertThat(restored.cachePolicy().enabled()).isTrue();
        assertThat(restored.cachePolicy().ttlSeconds()).isEqualTo(3600);
        assertThat(restored.cachePolicy().cacheKeyStrategy()).isEqualTo("url");
        
        // Verify nested CircuitBreakerPolicy
        assertThat(restored.circuitBreakerPolicy()).isNotNull();
        assertThat(restored.circuitBreakerPolicy().enabled()).isTrue();
        assertThat(restored.circuitBreakerPolicy().failureRateThreshold()).isEqualTo(50);
        
        // Verify nested RateLimitPolicy
        assertThat(restored.rateLimitPolicy()).isNotNull();
        assertThat(restored.rateLimitPolicy().enabled()).isTrue();
        assertThat(restored.rateLimitPolicy().requestsPerSecond()).isEqualTo(100);
        
        // Verify list of targets
        assertThat(restored.targets()).hasSize(2);
        assertThat(restored.targets().get(0).url()).isEqualTo("http://target1:8080");
        assertThat(restored.targets().get(1).url()).isEqualTo("http://target2:8080");
    }

    @Test
    void testDeepCloneRouteDto() {
        RouteDto original = createSampleRouteDto();
        
        RouteDto cloned = RouteMapper.deepCloneRouteDto(original);
        
        assertThat(cloned).isNotNull();
        assertThat(cloned).isNotSameAs(original);
        assertThat(cloned.id()).isEqualTo(original.id());
        assertThat(cloned.cachePolicy()).isEqualTo(original.cachePolicy());
        assertThat(cloned.targets()).isEqualTo(original.targets());
    }

    @Test
    void testMergeRouteDtos() {
        RouteDto oldRoute = createSampleRouteDto();
        RouteDto newRoute = new RouteDto(
                "/new-prefix",  // stripPrefix
                "/api/v2/orders",  // pathPattern
                5000,  // timeoutMs
                List.of(new TargetsItem(null, 1, null, "http://target3:8080")),  // targets
                false,  // enabled
                oldRoute.headerRules(),
                oldRoute.circuitBreakerPolicy(),
                "round-robin",  // loadBalancerType
                List.of(),
                oldRoute.rateLimitPolicy(),
                oldRoute.routingType(),
                oldRoute.cachePolicy(),
                "Updated Route",  // name
                oldRoute.auditStore(),
                oldRoute.id(),
                oldRoute.auditEnabled(),
                oldRoute.version(),
                null
        );
        
        RouteDto merged = RouteMapper.mergeRouteDtos(oldRoute, newRoute);
        
        assertThat(merged.stripPrefix()).isEqualTo("/new-prefix");
        assertThat(merged.pathPattern()).isEqualTo("/api/v2/orders");
        assertThat(merged.timeoutMs()).isEqualTo(5000);
        assertThat(merged.name()).isEqualTo("Updated Route");
        assertThat(merged.enabled()).isFalse();
        assertThat(merged.targets()).hasSize(1);
        assertThat(merged.targets().getFirst().url()).isEqualTo("http://target3:8080");
    }

    @Test
    void testFindRouteDifferences() {
        RouteDto original = createSampleRouteDto();
        RouteDto modified = new RouteDto(
                "/modified-prefix",  // Changed
                "/api/v1/users",
                5000,  // Changed
                original.targets(),
                false,  // Changed
                original.headerRules(),
                original.circuitBreakerPolicy(),
                original.loadBalancerType(),
                original.authForwardHeaders(),
                original.rateLimitPolicy(),
                original.routingType(),
                original.cachePolicy(),
                "Modified Name",  // Changed
                original.auditStore(),
                original.id(),
                original.auditEnabled(),
                original.version(),
                null
        );
        
        Map<String, DiffModel> differences = RouteMapper.findRouteDifferences(original, modified);
        
        assertThat(differences).isNotEmpty();
        assertThat(differences.keySet())
                .contains("stripPrefix", "timeoutMs", "enabled", "name");
        
        // Verify stripPrefix change
        DiffModel stripPrefixDiff = differences.get("stripPrefix");
        assertThat(stripPrefixDiff.getAction()).isEqualTo(DiffModel.DiffAction.MODIFIED);
        assertThat(stripPrefixDiff.getOldValue()).isEqualTo("/prefix");
        assertThat(stripPrefixDiff.getNewValue()).isEqualTo("/modified-prefix");
        
        // Verify timeoutMs change
        DiffModel timeoutDiff = differences.get("timeoutMs");
        assertThat(timeoutDiff.getAction()).isEqualTo(DiffModel.DiffAction.MODIFIED);
        assertThat(timeoutDiff.getOldValue()).isEqualTo(3000);
        assertThat(timeoutDiff.getNewValue()).isEqualTo(5000);
        
        // Verify enabled change
        DiffModel enabledDiff = differences.get("enabled");
        assertThat(enabledDiff.getAction()).isEqualTo(DiffModel.DiffAction.MODIFIED);
        assertThat(enabledDiff.getOldValue()).isEqualTo(true);
        assertThat(enabledDiff.getNewValue()).isEqualTo(false);
    }

    @Test
    void testFindDifferencesWithAddedProperty() {
        RouteDto original = createSampleRouteDto();
        RouteDto modified = new RouteDto(
                original.stripPrefix(),
                original.pathPattern(),
                original.timeoutMs(),
                original.targets(),
                original.enabled(),
                original.headerRules(),
                original.circuitBreakerPolicy(),
                "different-load-balancer",  // Different value
                original.authForwardHeaders(),
                original.rateLimitPolicy(),
                original.routingType(),
                original.cachePolicy(),
                original.name(),
                original.auditStore(),
                original.id(),
                original.auditEnabled(),
                original.version(),
                null
        );
        
        Map<String, DiffModel> differences = RouteMapper.findRouteDifferences(original, modified);
        
        DiffModel loadBalancerDiff = differences.get("loadBalancerType");
        assertThat(loadBalancerDiff).isNotNull();
        assertThat(loadBalancerDiff.getAction()).isEqualTo(DiffModel.DiffAction.MODIFIED);
        assertThat(loadBalancerDiff.getOldValue()).isEqualTo("least-connections");
        assertThat(loadBalancerDiff.getNewValue()).isEqualTo("different-load-balancer");
    }

    @Test
    void testRouteDtoToRowData() {
        RouteDto routeDto = createSampleRouteDto();
        
        Object[] rowData = RouteMapper.routeDtoToRowData(routeDto);
        
        assertThat(rowData).hasSize(5);
        assertThat(rowData[0]).isEqualTo("/api/v1/users");  // pathPattern
        assertThat(rowData[1]).isEqualTo("Sample Route");  // name
        assertThat(rowData[2]).isNotNull();  // configJson
        assertThat(rowData[3]).isEqualTo(true);  // enabled
        assertThat(rowData[4]).isEqualTo(1L);  // version
    }

    @Test
    void testNullHandling() {
        String json = RouteMapper.routeDtoToJson(null);
        assertThat(json).isNull();
        
        RouteDto dto = RouteMapper.jsonToRouteDto(null);
        assertThat(dto).isNull();
        
        RouteDto cloned = RouteMapper.deepCloneRouteDto(null);
        assertThat(cloned).isNull();
        
        Object[] rowData = RouteMapper.routeDtoToRowData(null);
        assertThat(rowData).hasSize(5);
        assertThat(rowData[3]).isEqualTo(false);
        assertThat(rowData[4]).isEqualTo(0L);
    }

    @Test
    void testDiffHelperWithNullValues() {
        RouteDto nullDto = null;
        RouteDto routeDto = createSampleRouteDto();
        
        Map<String, DiffModel> differences = DiffHelper.findDifferences(nullDto, routeDto);
        
        assertThat(differences).isNotEmpty();
        assertThat(differences.values()).allMatch(diff -> 
                diff.getAction() == DiffModel.DiffAction.ADDED
        );
    }

    private RouteDto createSampleRouteDto() {
        return new RouteDto(
                "/prefix",
                "/api/v1/users",
                3000,
                List.of(
                        new TargetsItem(null, 1, null, "http://target1:8080"),
                        new TargetsItem(null, 1, null, "http://target2:8080")
                ),
                true,
                new HeaderRules(
                        List.of(),
                        List.of(new AddRequest("X-Custom-Header", "custom-value")),
                        List.of(),
                        List.of(new AddResponse("X-Response-Header", "response-value"))
                ),
                new CircuitBreakerPolicy(30, 100, 50, true),
                "least-connections",
                List.of(),
                new RateLimitPolicy(1000, 10, 100, true),
                "path",
                new CachePolicy(3600, "url", true),
                "Sample Route",
                "database",
                "route-1",
                true,
                1L,
                null
        );
    }
}
