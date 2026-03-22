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

/** Selects a target uniformly at random, ignoring weights. */
public class RandomLoadBalancer implements LoadBalancer {

    @Override
    public TargetDefinition pick(List<TargetDefinition> targets) {
        if (targets == null || targets.isEmpty()) return null;
        return targets.get(ThreadLocalRandom.current().nextInt(targets.size()));
    }
}

