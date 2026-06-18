/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.header;

import org.example.utilities.gateway.filter.FilterChain;
import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.filter.GatewayFilter;
import org.example.utilities.gateway.model.HeaderRules;

import java.util.List;
import java.util.Locale;

/**
 * Adds / removes headers on the upstream request (before forwarding) and on the
 * response received from the upstream (before returning to the client).
 * Configuration is taken from {@link HeaderRules} on the matched route.
 */
public class HeaderMutationFilter implements GatewayFilter {

    private final HeaderRules rules;

    public HeaderMutationFilter(HeaderRules rules) {
        this.rules = rules;
    }

    @Override
    public void filter(FilterContext ctx, FilterChain chain) throws Exception {
        // -- REQUEST: additions & exclusions are applied inside OkHttpUpstreamClient
        //    so we just stash them on the context for the proxy filter to pick up.
        //    Nothing to do here at the filter level for request headers; the proxy
        //    reads rules.getAddRequest() / rules.getExcludeRequest() directly.

        chain.proceed(ctx);

        // -- RESPONSE: add / exclude headers received from upstream ---------------
        List<HeaderRules.HeaderEntry> addResponse = rules.getAddResponse();
        if (addResponse != null) {
            addResponse.forEach(e ->
                    ctx.getUpstreamResponseHeaders().put(e.getName(), List.of(e.getValue())));
        }

        List<String> excludeResponse = rules.getExcludeResponse();
        if (excludeResponse != null) {
            excludeResponse.forEach(name ->
                    ctx.getUpstreamResponseHeaders().remove(name.toLowerCase(Locale.ROOT)));
        }
    }
}

