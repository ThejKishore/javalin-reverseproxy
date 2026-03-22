/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.transform;

import org.example.utilities.gateway.filter.FilterChain;
import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.filter.GatewayFilter;

/**
 * Applies optional {@link RequestTransformer} and {@link ResponseTransformer} hooks.
 * Either transformer may be {@code null} (pass-through).
 */
public class TransformGatewayFilter implements GatewayFilter {

    private final RequestTransformer requestTransformer;
    private final ResponseTransformer responseTransformer;

    public TransformGatewayFilter(RequestTransformer requestTransformer,
                                  ResponseTransformer responseTransformer) {
        this.requestTransformer = requestTransformer;
        this.responseTransformer = responseTransformer;
    }

    @Override
    public void filter(FilterContext ctx, FilterChain chain) throws Exception {
        // Apply request transformation before forwarding
        if (requestTransformer != null) {
            byte[] modified = requestTransformer.transform(ctx.getEffectiveRequestBody());
            ctx.setModifiedRequestBody(modified);
        }

        chain.proceed(ctx);

        // Apply response transformation after receiving upstream response
        if (responseTransformer != null) {
            byte[] source = ctx.getModifiedResponseBody() != null
                    ? ctx.getModifiedResponseBody()
                    : ctx.getUpstreamResponseBody();
            ctx.setModifiedResponseBody(responseTransformer.transform(source));
        }
    }
}

