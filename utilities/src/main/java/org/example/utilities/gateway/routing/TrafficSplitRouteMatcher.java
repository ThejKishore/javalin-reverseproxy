/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.routing;

import io.javalin.http.Context;
import org.example.utilities.gateway.model.RouteDefinition;
import org.example.utilities.gateway.model.RoutingType;

/**
 * Always matches routes whose type is {@link RoutingType#TRAFFIC_SPLIT}.
 * Traffic-split routes have no path constraint; the load-balancer distributes
 * traffic across targets by weight.
 */
public class TrafficSplitRouteMatcher implements RouteMatcher {

    @Override
    public boolean matches(RouteDefinition route, Context ctx) {
        return route.getRoutingType() == RoutingType.TRAFFIC_SPLIT
                && route.getTargets() != null
                && !route.getTargets().isEmpty();
    }
}

