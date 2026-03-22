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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Distributes requests evenly across targets using a lock-free round-robin counter.
 * Each {@code RoundRobinLoadBalancer} instance tracks its own counter, so one
 * instance per route is required to ensure correct per-route balancing.
 */
public class RoundRobinLoadBalancer implements LoadBalancer {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public TargetDefinition pick(List<TargetDefinition> targets) {
        if (targets == null || targets.isEmpty()) return null;
        int idx = Math.abs(counter.getAndIncrement() % targets.size());
        return targets.get(idx);
    }
}

