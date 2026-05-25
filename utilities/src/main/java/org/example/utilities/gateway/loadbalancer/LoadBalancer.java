/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.loadbalancer;

import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.model.TargetDefinition;

import java.util.List;

/** Selects one upstream target from a route's list of targets. */
public interface LoadBalancer {

    /**
     * Selects a target without request context (stateless strategies).
     *
     * @param targets non-empty list of available targets
     * @return the selected target, or {@code null} if the list is empty
     */
    TargetDefinition pick(List<TargetDefinition> targets);

    /**
     * Selects a target with access to the inbound request context.
     * Default implementation delegates to {@link #pick(List)} so existing
     * implementations need not override it.
     *
     * @param targets non-empty list of available targets
     * @param ctx     the current filter context (carries Javalin request)
     * @return the selected target, or {@code null} if the list is empty
     */
    default TargetDefinition pick(List<TargetDefinition> targets, FilterContext ctx) {
        return pick(targets);
    }
}
