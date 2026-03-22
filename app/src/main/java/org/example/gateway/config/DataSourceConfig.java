/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Database / datasource connection settings read from application.yml. */
public class DataSourceConfig {

    /** {@code h2} or {@code postgres}. */
    @JsonProperty("type")
    private String type = "h2";

    @JsonProperty("url")
    private String url = "jdbc:h2:mem:gateway;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

    @JsonProperty("username")
    private String username = "sa";

    @JsonProperty("password")
    private String password = "";

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

