/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Hazelcast cluster configuration used for the distributed CSRF token store.
 *
 * <p>When {@code members} is empty the instance runs in standalone mode
 * (suitable for single-replica dev/test). For Kubernetes multi-replica
 * deployments, set {@code members} to the Hazelcast service DNS names or use
 * the {@code hazelcast-kubernetes} auto-discovery plugin.
 */
public class HazelcastConfig {

    @JsonProperty("cluster-name")
    private String clusterName = "gateway-cluster";

    /** Explicit member addresses, e.g. {@code ["hazelcast-service:5701"]}. Empty = standalone. */
    @JsonProperty("members")
    private List<String> members = new ArrayList<>();

    /** TTL in seconds for CSRF tokens stored in the distributed map. */
    @JsonProperty("csrf-map-ttl-seconds")
    private int csrfMapTtlSeconds = 3600;

    /** When {@code true}, skip Hazelcast initialisation (useful in pure YAML-only test setups). */
    @JsonProperty("enabled")
    private boolean enabled = true;

    // --- Getters & setters ---

    public String getClusterName() { return clusterName; }
    public void setClusterName(String clusterName) { this.clusterName = clusterName; }

    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }

    public int getCsrfMapTtlSeconds() { return csrfMapTtlSeconds; }
    public void setCsrfMapTtlSeconds(int csrfMapTtlSeconds) { this.csrfMapTtlSeconds = csrfMapTtlSeconds; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}

