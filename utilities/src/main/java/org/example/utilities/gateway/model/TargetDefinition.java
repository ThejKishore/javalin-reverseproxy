/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** A single upstream server URL and its optional routing weight. */
public class TargetDefinition {

    @JsonProperty("url")
    private String url;

    @JsonProperty("weight")
    private int weight = 1;

    public TargetDefinition() {}

    public TargetDefinition(String url, int weight) {
        this.url = url;
        this.weight = weight;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }

    @Override
    public String toString() {
        return "TargetDefinition{url='" + url + "', weight=" + weight + '}';
    }
}

