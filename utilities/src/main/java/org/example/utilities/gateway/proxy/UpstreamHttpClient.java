/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.proxy;

import org.example.utilities.gateway.filter.FilterContext;

/** Abstraction over the upstream HTTP transport used by {@code ProxyFilter}. */
public interface UpstreamHttpClient {
    /**
     * Forwards the request described by {@code ctx} to the selected upstream target,
     * then populates {@code ctx} with the upstream status code, headers and body.
     */
    void forward(FilterContext ctx) throws Exception;
}

