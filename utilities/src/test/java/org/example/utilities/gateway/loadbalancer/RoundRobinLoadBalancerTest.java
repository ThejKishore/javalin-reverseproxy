/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.loadbalancer;

import org.example.utilities.gateway.model.TargetDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoundRobinLoadBalancerTest {

    private static final List<TargetDefinition> THREE_TARGETS = List.of(
            new TargetDefinition("http://a:8080", 1),
            new TargetDefinition("http://b:8080", 1),
            new TargetDefinition("http://c:8080", 1)
    );

    @Test
    void cyclesAcrossTargets() {
        RoundRobinLoadBalancer lb = new RoundRobinLoadBalancer();
        assertEquals("http://a:8080", lb.pick(THREE_TARGETS).getUrl());
        assertEquals("http://b:8080", lb.pick(THREE_TARGETS).getUrl());
        assertEquals("http://c:8080", lb.pick(THREE_TARGETS).getUrl());
        // wraps around
        assertEquals("http://a:8080", lb.pick(THREE_TARGETS).getUrl());
    }

    @Test
    void singleTarget_alwaysReturnsIt() {
        RoundRobinLoadBalancer lb = new RoundRobinLoadBalancer();
        TargetDefinition t = new TargetDefinition("http://only:8080", 1);
        for (int i = 0; i < 5; i++) {
            assertEquals(t.getUrl(), lb.pick(List.of(t)).getUrl());
        }
    }

    @Test
    void emptyList_returnsNull() {
        assertNull(new RoundRobinLoadBalancer().pick(List.of()));
    }

    @Test
    void concurrentAccess_doesNotThrow() throws InterruptedException {
        RoundRobinLoadBalancer lb = new RoundRobinLoadBalancer();
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) lb.pick(THREE_TARGETS);
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
    }
}

