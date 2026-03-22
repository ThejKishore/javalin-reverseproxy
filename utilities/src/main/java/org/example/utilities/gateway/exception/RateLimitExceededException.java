/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.exception;

/** Thrown when a route's rate limit has been exceeded (HTTP 429). */
public class RateLimitExceededException extends GatewayException {
    public RateLimitExceededException(String routeId) {
        super("Rate limit exceeded for route: " + routeId, 429);
    }
}

