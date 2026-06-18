package org.example.gateway.routes.model;

import java.util.List;

public record RoutesDto(
        List<RouteDto> routes
) {
}
