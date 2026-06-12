package org.example.gateway.api;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.example.gateway.config.OpenApiExportConfig;
import org.example.gateway.registry.RouteRegistry;
import org.example.utilities.gateway.model.RouteDefinition;
import org.example.utilities.gateway.model.TargetDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsolidatedOpenApiControllerTest {

    private static final String TOKEN = "apim-registration-token";

    private Javalin buildApp(RouteRegistry registry) {
        OpenApiExportConfig config = new OpenApiExportConfig();
        config.setEnabled(true);
        config.setPath("/gateway/admin/openapi");
        config.setAuthHeader("Authorization");
        config.setAccessToken(TOKEN);

        ConsolidatedOpenApiController controller = new ConsolidatedOpenApiController(registry, config);

        return Javalin.create(cfg -> cfg.routes.get(config.getPath(), controller::getConsolidatedSpec));
    }

    @Test
    void consolidatedSpec_requiresAuthenticationHeader() {
        RouteRegistry registry = new RouteRegistry();
        registry.reload(List.of());

        JavalinTest.test(buildApp(registry), (server, client) -> {
            var response = client.get("/gateway/admin/openapi");
            assertEquals(401, response.code());
        });
    }

    @Test
    void consolidatedSpec_rejectsInvalidToken() {
        RouteRegistry registry = new RouteRegistry();
        registry.reload(List.of());

        JavalinTest.test(buildApp(registry), (server, client) -> {
            OkHttpClient httpClient = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("http://localhost:" + server.port() + "/gateway/admin/openapi")
                    .header("Authorization", "Bearer wrong-token")
                    .build();
            try (var response = httpClient.newCall(request).execute()) {
                assertEquals(403, response.code());
            }
        });
    }

    @Test
    void consolidatedSpec_includesAdminAndProxyPaths_andHonorsSkipMetadata() {
        RouteRegistry registry = new RouteRegistry();
        registry.reload(List.of(
                route("svc-orders", "/svc1/orders", Map.of(
                        "context-path", "/svc1",
                        "health-endpoint", "/actuator/health",
                        "prometheus-endpoint", "/internal/prometheus",
                        "openapi-spec-endpoint", "/openapi",
                        "skip-paths", "/internal",
                        "skip-api", "true"
                )),
                route("svc-api", "/svc1/api", Map.of(
                        "context-path", "/svc1",
                        "skip-api", "true"
                )),
                route("svc-skipped", "/svc2/orders", Map.of(
                        "skip-openapi-for-service", "true"
                ))
        ));

        JavalinTest.test(buildApp(registry), (server, client) -> {
            OkHttpClient httpClient = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("http://localhost:" + server.port() + "/gateway/admin/openapi")
                    .header("Authorization", "Bearer " + TOKEN)
                    .build();

            try (var response = httpClient.newCall(request).execute()) {
                assertEquals(200, response.code());
                assertTrue(response.body() != null);
                String body = response.body().string();

                assertTrue(body.contains("\"/gateway/admin/routes\""));
                assertTrue(body.contains("\"/svc1/orders\""));
                assertTrue(body.contains("\"/svc1/actuator/health\""));
                assertTrue(body.contains("\"/svc1/openapi\""));

                assertFalse(body.contains("\"/svc1/api\""));
                assertFalse(body.contains("\"/svc1/internal/prometheus\""));
                assertFalse(body.contains("\"/svc2/orders\""));
            }
        });
    }

    private static RouteDefinition route(String id, String path, Map<String, String> metadata) {
        RouteDefinition route = new RouteDefinition();
        route.setId(id);
        route.setName(id);
        route.setEnabled(true);
        route.setPathPattern(path);
        route.setTargets(List.of(new TargetDefinition("http://localhost:9090", 1)));
        route.setMetaData(metadata);
        return route;
    }
}
