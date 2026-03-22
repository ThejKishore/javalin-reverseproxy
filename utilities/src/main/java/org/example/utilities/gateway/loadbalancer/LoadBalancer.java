/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.loadbalancer;

import org.example.utilities.gateway.model.TargetDefinition;

import java.util.List;

/** Selects one upstream target from a route's list of targets. */
public interface LoadBalancer {
    /**
     * @param targets non-empty list of available targets
     * @return the selected target, or {@code null} if the list is empty
     */
    TargetDefinition pick(List<TargetDefinition> targets);
}

