/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.filter;

import java.util.List;

/**
 * Immutable, index-based implementation of {@link FilterChain}.
 *
 * <p>A new instance is created for each request by the {@code ProxyHandler}.
 * Calling {@link #proceed(FilterContext)} invokes the next filter; when the list
 * is exhausted execution returns without error (the terminal {@code ProxyFilter}
 * never calls proceed).
 */
public class DefaultFilterChain implements FilterChain {

    private final List<GatewayFilter> filters;
    private final int index;

    public DefaultFilterChain(List<GatewayFilter> filters) {
        this(filters, 0);
    }

    private DefaultFilterChain(List<GatewayFilter> filters, int index) {
        this.filters = filters;
        this.index = index;
    }

    @Override
    public void proceed(FilterContext ctx) throws Exception {
        if (index < filters.size()) {
            FilterChain next = new DefaultFilterChain(filters, index + 1);
            filters.get(index).filter(ctx, next);
        }
    }
}

