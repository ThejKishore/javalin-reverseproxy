/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configures header manipulation applied to the upstream request and/or
 * the response returned to the client.
 */
public class HeaderRules {

    /** Headers to add to the outgoing upstream request. */
    @JsonProperty("add-request")
    private Map<String, String> addRequest = new HashMap<>();

    /** Header names to remove from the outgoing upstream request. */
    @JsonProperty("exclude-request")
    private List<String> excludeRequest = new ArrayList<>();

    /** Headers to add to the response sent back to the client. */
    @JsonProperty("add-response")
    private Map<String, String> addResponse = new HashMap<>();

    /** Header names to remove from the response sent back to the client. */
    @JsonProperty("exclude-response")
    private List<String> excludeResponse = new ArrayList<>();

    /** Response header names whose duplicate values should be collapsed into one. */
    @JsonProperty("dedupe-response-headers")
    private List<String> dedupeResponseHeaders = new ArrayList<>();

    // --- Getters & setters ---

    public Map<String, String> getAddRequest() { return addRequest; }
    public void setAddRequest(Map<String, String> addRequest) { this.addRequest = addRequest; }

    public List<String> getExcludeRequest() { return excludeRequest; }
    public void setExcludeRequest(List<String> excludeRequest) { this.excludeRequest = excludeRequest; }

    public Map<String, String> getAddResponse() { return addResponse; }
    public void setAddResponse(Map<String, String> addResponse) { this.addResponse = addResponse; }

    public List<String> getExcludeResponse() { return excludeResponse; }
    public void setExcludeResponse(List<String> excludeResponse) { this.excludeResponse = excludeResponse; }

    public List<String> getDedupeResponseHeaders() { return dedupeResponseHeaders; }
    public void setDedupeResponseHeaders(List<String> dedupeResponseHeaders) { this.dedupeResponseHeaders = dedupeResponseHeaders; }
}

