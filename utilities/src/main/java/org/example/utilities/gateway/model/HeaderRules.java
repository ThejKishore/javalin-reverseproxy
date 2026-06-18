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
import java.util.List;

/**
 * Configures header manipulation applied to the upstream request and/or
 * the response returned to the client.
 *
 * <p>{@code addRequest} and {@code addResponse} are lists of {@link HeaderEntry}
 * so that multiple headers can be injected per route.  The JSON shape of each
 * entry is {@code {"name": "X-Header", "value": "..."}} — matching the
 * {@code routes/model/AddRequest} and {@code AddResponse} records in the shared
 * module so that {@code RouteDto} → {@code RouteDefinition} round-trips work
 * transparently.
 */
public class HeaderRules {

    /**
     * A single name/value header pair used in {@link #addRequest} and
     * {@link #addResponse} lists.
     */
    public static class HeaderEntry {
        @JsonProperty("name")
        private String name;

        @JsonProperty("value")
        private String value;

        public HeaderEntry() {}

        public HeaderEntry(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    /** Headers to add to the outgoing upstream request. */
    @JsonProperty("add-request")
    private List<HeaderEntry> addRequest = new ArrayList<>();

    /** Header names to remove from the outgoing upstream request. */
    @JsonProperty("exclude-request")
    private List<String> excludeRequest = new ArrayList<>();

    /** Headers to add to the response sent back to the client. */
    @JsonProperty("add-response")
    private List<HeaderEntry> addResponse = new ArrayList<>();

    /** Header names to remove from the response sent back to the client. */
    @JsonProperty("exclude-response")
    private List<String> excludeResponse = new ArrayList<>();

    /** Response header names whose duplicate values should be collapsed into one. */
    @JsonProperty("dedupe-response-headers")
    private List<String> dedupeResponseHeaders = new ArrayList<>();

    // --- Getters & setters ---

    public List<HeaderEntry> getAddRequest() { return addRequest; }
    public void setAddRequest(List<HeaderEntry> addRequest) { this.addRequest = addRequest; }

    public List<String> getExcludeRequest() { return excludeRequest; }
    public void setExcludeRequest(List<String> excludeRequest) { this.excludeRequest = excludeRequest; }

    public List<HeaderEntry> getAddResponse() { return addResponse; }
    public void setAddResponse(List<HeaderEntry> addResponse) { this.addResponse = addResponse; }

    public List<String> getExcludeResponse() { return excludeResponse; }
    public void setExcludeResponse(List<String> excludeResponse) { this.excludeResponse = excludeResponse; }

    public List<String> getDedupeResponseHeaders() { return dedupeResponseHeaders; }
    public void setDedupeResponseHeaders(List<String> dedupeResponseHeaders) { this.dedupeResponseHeaders = dedupeResponseHeaders; }
}

