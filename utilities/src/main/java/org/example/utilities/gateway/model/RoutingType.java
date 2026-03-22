/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.model;

/**
 * Determines how an incoming request path is matched against a {@link RouteDefinition}.
 */
public enum RoutingType {
    /** Simple path-prefix or exact-path matching. */
    PATH,
    /** Full regular-expression matching against the request path. */
    REGEX,
    /** Route is selected when a specific HTTP header is present with the expected value. */
    HEADER,
    /** Distributes traffic across multiple targets by weight; no path constraint. */
    TRAFFIC_SPLIT
}

