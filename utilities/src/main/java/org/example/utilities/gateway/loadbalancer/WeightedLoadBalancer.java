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
import java.util.concurrent.ThreadLocalRandom;

/**
 * Selects a target proportionally to its configured {@code weight}.
 * Higher-weight targets receive more traffic.
 */
public class WeightedLoadBalancer implements LoadBalancer {

    @Override
    public TargetDefinition pick(List<TargetDefinition> targets) {
        if (targets == null || targets.isEmpty()) return null;
        int totalWeight = targets.stream().mapToInt(TargetDefinition::getWeight).sum();
        if (totalWeight <= 0) return targets.get(0);
        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (TargetDefinition target : targets) {
            cumulative += target.getWeight();
            if (random < cumulative) return target;
        }
        return targets.get(targets.size() - 1);
    }
}

