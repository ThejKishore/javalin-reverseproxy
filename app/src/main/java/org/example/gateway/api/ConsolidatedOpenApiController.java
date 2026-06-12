package org.example.gateway.api;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnauthorizedResponse;
import org.example.gateway.config.OpenApiExportConfig;
import org.example.gateway.registry.RouteRegistry;
import org.example.utilities.gateway.model.RouteDefinition;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Serves a consolidated OpenAPI document for Azure API Management registration.
 */
public class ConsolidatedOpenApiController {

    private static final String META_CONTEXT_PATH = "context-path";
    private static final String META_HEALTH_ENDPOINT = "health-endpoint";
    private static final String META_PROM_ENDPOINT = "prometheus-endpoint";
    private static final String META_UPSTREAM_OPENAPI = "openapi-spec-endpoint";
    private static final String META_SKIP_API = "skip-api";
    private static final String META_SKIP_PATHS = "skip-paths";
    private static final String META_SKIP_SERVICE = "skip-openapi-for-service";

    private final RouteRegistry registry;
    private final OpenApiExportConfig exportConfig;

    public ConsolidatedOpenApiController(RouteRegistry registry, OpenApiExportConfig exportConfig) {
        this.registry = registry;
        this.exportConfig = exportConfig;
    }

    public void getConsolidatedSpec(Context ctx) {
        if (exportConfig == null || !exportConfig.isEnabled()) {
            throw new NotFoundResponse("OpenAPI export endpoint is disabled");
        }
        authorize(ctx);
        ctx.contentType("application/json");
        ctx.json(buildSpec(ctx));
    }

    private void authorize(Context ctx) {
        String expected = exportConfig.getAccessToken();
        if (expected == null || expected.isBlank()) {
            throw new ForbiddenResponse("OpenAPI export access token is not configured");
        }

        String authHeaderName = nonBlank(exportConfig.getAuthHeader(), "Authorization");
        String headerValue = ctx.header(authHeaderName);
        if (headerValue == null || headerValue.isBlank()) {
            throw new UnauthorizedResponse("Missing OpenAPI export credentials");
        }

        String providedToken = normalizeToken(headerValue);
        if (!MessageDigest.isEqual(providedToken.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8))) {
            throw new ForbiddenResponse("Invalid OpenAPI export credentials");
        }
    }

    private Map<String, Object> buildSpec(Context ctx) {
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("openapi", "3.0.3");

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", "Javalin Gateway Consolidated API");
        info.put("version", "1.0.0");
        info.put("description", "Dynamically generated gateway OpenAPI including admin and proxy routes");
        info.put("x-generated-at", Instant.now().toString());
        spec.put("info", info);

        spec.put("servers", List.of(Map.of("url", baseServerUrl(ctx))));

        Map<String, Object> paths = new LinkedHashMap<>();
        addAdminPaths(paths);
        addProxyPaths(paths);
        spec.put("paths", paths);

        spec.put("components", Map.of(
                "securitySchemes", Map.of(
                        "OpenApiExportToken", Map.of(
                                "type", "apiKey",
                                "in", "header",
                                "name", nonBlank(exportConfig.getAuthHeader(), "Authorization")
                        )
                )
        ));

        return spec;
    }

    private void addAdminPaths(Map<String, Object> paths) {
        addOperation(paths, "/gateway/admin/routes", "get", "List gateway routes", "Admin");
        addOperation(paths, "/gateway/admin/routes", "post", "Create gateway route", "Admin");
        addOperation(paths, "/gateway/admin/routes/{id}", "get", "Get gateway route", "Admin");
        addOperation(paths, "/gateway/admin/routes/{id}", "put", "Update gateway route", "Admin");
        addOperation(paths, "/gateway/admin/routes/{id}", "delete", "Delete gateway route", "Admin");
        addOperation(paths, "/gateway/admin/routes/{id}/enable", "patch", "Enable gateway route", "Admin");
        addOperation(paths, "/gateway/admin/routes/{id}/disable", "patch", "Disable gateway route", "Admin");
        addOperation(paths, "/gateway/admin/reload", "post", "Reload gateway routes", "Admin");

        addOperation(paths, "/gateway/admin/security/rules", "get", "List validation rules", "Admin Security");
        addOperation(paths, "/gateway/admin/security/rules", "post", "Create validation rule", "Admin Security");
        addOperation(paths, "/gateway/admin/security/rules/reload", "post", "Reload validation rules", "Admin Security");
        addOperation(paths, "/gateway/admin/security/rules/{id}/enable", "patch", "Enable validation rule", "Admin Security");
        addOperation(paths, "/gateway/admin/security/rules/{id}/disable", "patch", "Disable validation rule", "Admin Security");
        addOperation(paths, "/gateway/admin/security/rules/{id}", "delete", "Delete validation rule", "Admin Security");

        addOperation(paths, "/gateway/health", "get", "Gateway combined health", "Health");
        addOperation(paths, "/gateway/health/live", "get", "Gateway liveness", "Health");
        addOperation(paths, "/gateway/health/ready", "get", "Gateway readiness", "Health");
        addSecuredOperation(paths, normalizePath(exportConfig.getPath()), "get",
                "Get consolidated OpenAPI specification", "Admin");
    }

    private void addProxyPaths(Map<String, Object> paths) {
        for (RouteDefinition route : registry.getAllRoutes()) {
            String pathPattern = normalizePath(route.getPathPattern());
            Map<String, String> meta = route.getMetaData() == null ? Map.of() : route.getMetaData();

            if (isTrue(meta.get(META_SKIP_SERVICE))) {
                continue;
            }
            if (shouldSkipPath(pathPattern, meta)) {
                continue;
            }

            String tag = route.getName() == null || route.getName().isBlank() ? "Proxy" : route.getName();
            addOperation(paths, pathPattern, "get", "Proxy GET for " + tag, tag);
            addOperation(paths, pathPattern, "post", "Proxy POST for " + tag, tag);
            addOperation(paths, pathPattern, "put", "Proxy PUT for " + tag, tag);
            addOperation(paths, pathPattern, "patch", "Proxy PATCH for " + tag, tag);
            addOperation(paths, pathPattern, "delete", "Proxy DELETE for " + tag, tag);

            String contextPath = normalizePath(meta.get(META_CONTEXT_PATH));
            addMetadataEndpoint(paths, contextPath, meta.get(META_HEALTH_ENDPOINT), "Service health endpoint", tag, meta);
            addMetadataEndpoint(paths, contextPath, meta.get(META_PROM_ENDPOINT), "Service prometheus endpoint", tag, meta);
            addMetadataEndpoint(paths, contextPath, meta.get(META_UPSTREAM_OPENAPI), "Service OpenAPI endpoint", tag, meta);
        }
    }

    private void addMetadataEndpoint(Map<String, Object> paths,
                                     String contextPath,
                                     String endpoint,
                                     String summary,
                                     String tag,
                                     Map<String, String> meta) {
        if (endpoint == null || endpoint.isBlank()) {
            return;
        }
        String fullPath = combinePaths(contextPath, endpoint);
        if (shouldSkipPath(fullPath, meta)) {
            return;
        }
        addOperation(paths, fullPath, "get", summary, tag);
    }

    private boolean shouldSkipPath(String path, Map<String, String> meta) {
        if (path == null || path.isBlank()) {
            return true;
        }

        if (isTrue(meta.get(META_SKIP_API)) && isApiPath(path, normalizePath(meta.get(META_CONTEXT_PATH)))) {
            return true;
        }

        List<String> skipPaths = parseSkipPaths(meta.get(META_SKIP_PATHS));
        if (skipPaths.isEmpty()) {
            return false;
        }

        String contextPath = normalizePath(meta.get(META_CONTEXT_PATH));
        for (String skipPath : skipPaths) {
            if (pathMatchesOrStartsWith(path, skipPath)) {
                return true;
            }
            if (contextPath != null && pathMatchesOrStartsWith(path, combinePaths(contextPath, skipPath))) {
                return true;
            }
        }
        return false;
    }

    private boolean isApiPath(String path, String contextPath) {
        if (pathMatchesOrStartsWith(path, "/api")) {
            return true;
        }
        return contextPath != null && pathMatchesOrStartsWith(path, combinePaths(contextPath, "/api"));
    }

    private List<String> parseSkipPaths(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> parsed = new ArrayList<>();
        for (String token : value.split(",")) {
            String normalized = normalizePath(token);
            if (normalized != null && !normalized.isBlank()) {
                parsed.add(normalized);
            }
        }
        return parsed;
    }

    private void addOperation(Map<String, Object> paths, String rawPath, String method, String summary, String tag) {
        addOperation(paths, rawPath, method, summary, tag, false);
    }

    private void addSecuredOperation(Map<String, Object> paths, String rawPath, String method, String summary, String tag) {
        addOperation(paths, rawPath, method, summary, tag, true);
    }

    private void addOperation(Map<String, Object> paths,
                              String rawPath,
                              String method,
                              String summary,
                              String tag,
                              boolean secured) {
        String path = normalizePath(rawPath);
        if (path == null || path.isBlank()) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> methodMap = (Map<String, Object>) paths.computeIfAbsent(path, p -> new LinkedHashMap<>());

        Map<String, Object> operation = new LinkedHashMap<>();
        operation.put("summary", summary);
        operation.put("tags", List.of(tag));
        operation.put("responses", Map.of(
                "200", Map.of("description", "Successful response"),
                "400", Map.of("description", "Bad request"),
                "401", Map.of("description", "Unauthorized"),
                "403", Map.of("description", "Forbidden"),
                "500", Map.of("description", "Internal server error")
        ));
        if (secured) {
            operation.put("security", List.of(Map.of("OpenApiExportToken", List.of())));
        }

        methodMap.put(method.toLowerCase(Locale.ROOT), operation);
    }

    private static boolean pathMatchesOrStartsWith(String path, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        if (path.equals(candidate)) {
            return true;
        }
        return path.startsWith(candidate + "/");
    }

    private static String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }
        if (trimmed.length() > 1 && trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String combinePaths(String prefix, String suffix) {
        String left = normalizePath(prefix);
        String right = normalizePath(suffix);
        if (left == null && right == null) {
            return null;
        }
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        if ("/".equals(left)) {
            return right;
        }
        return left + right;
    }

    private static boolean isTrue(String value) {
        return value != null && "true".equalsIgnoreCase(value.trim());
    }

    private static String nonBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static String normalizeToken(String headerValue) {
        String token = headerValue.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return token.substring(7).trim();
        }
        return token;
    }

    private static String baseServerUrl(Context ctx) {
        URI uri = URI.create(ctx.url());
        StringBuilder sb = new StringBuilder();
        sb.append(uri.getScheme()).append("://").append(uri.getHost());
        if (uri.getPort() != -1) {
            sb.append(":").append(uri.getPort());
        }
        return sb.toString();
    }
}
