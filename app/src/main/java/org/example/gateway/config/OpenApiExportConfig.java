package org.example.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for the consolidated OpenAPI export endpoint.
 */
public class OpenApiExportConfig {

    @JsonProperty("enabled")
    private boolean enabled = true;

    @JsonProperty("path")
    private String path = "/gateway/admin/openapi";

    @JsonProperty("auth-header")
    private String authHeader = "Authorization";

    @JsonProperty("access-token")
    private String accessToken = "";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getAuthHeader() { return authHeader; }
    public void setAuthHeader(String authHeader) { this.authHeader = authHeader; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
}

