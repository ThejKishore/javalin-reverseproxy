/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.filter;

import io.javalin.http.Context;
import org.example.utilities.gateway.model.RouteDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DefaultFilterChainTest {

    @Mock Context javalinCtx;

    @Test
    void executesFiltersInOrder() throws Exception {
        List<String> order = new ArrayList<>();
        GatewayFilter f1 = (ctx, chain) -> { order.add("f1-before"); chain.proceed(ctx); order.add("f1-after"); };
        GatewayFilter f2 = (ctx, chain) -> { order.add("f2-before"); chain.proceed(ctx); order.add("f2-after"); };
        GatewayFilter f3 = (ctx, chain) -> { order.add("f3-terminal"); /* no proceed */ };

        FilterContext ctx = new FilterContext(javalinCtx, new RouteDefinition());
        new DefaultFilterChain(List.of(f1, f2, f3)).proceed(ctx);

        assertEquals(List.of("f1-before", "f2-before", "f3-terminal", "f2-after", "f1-after"), order);
    }

    @Test
    void emptyChain_doesNothing() throws Exception {
        FilterContext ctx = new FilterContext(javalinCtx, new RouteDefinition());
        assertDoesNotThrow(() -> new DefaultFilterChain(List.of()).proceed(ctx));
    }

    @Test
    void shortCircuit_stopsChain() throws Exception {
        List<String> executed = new ArrayList<>();
        GatewayFilter shortCircuit = (ctx, chain) -> { executed.add("shortCircuit"); /* no proceed */ };
        GatewayFilter shouldNotRun = (ctx, chain) -> executed.add("shouldNotRun");

        FilterContext ctx = new FilterContext(javalinCtx, new RouteDefinition());
        new DefaultFilterChain(List.of(shortCircuit, shouldNotRun)).proceed(ctx);

        assertEquals(List.of("shortCircuit"), executed);
    }

    @Test
    void exceptionPropagates() {
        GatewayFilter boom = (ctx, chain) -> { throw new IllegalStateException("oops"); };
        FilterContext ctx = new FilterContext(javalinCtx, new RouteDefinition());
        assertThrows(IllegalStateException.class,
                () -> new DefaultFilterChain(List.of(boom)).proceed(ctx));
    }
}

