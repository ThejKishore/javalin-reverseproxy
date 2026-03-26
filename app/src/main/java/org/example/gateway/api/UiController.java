/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.api;

import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.http.NotFoundResponse;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Serves the single-page admin dashboard HTML and its companion static assets
 * (e.g. {@code app.js}) from the classpath directory {@code /gateway-ui/}.
 *
 * <p>Routes are registered <em>before</em> the catch-all proxy so that the
 * proxy never intercepts UI asset requests.
 */
public class UiController {

    private static final String RESOURCE_ROOT = "/gateway-ui";

    /** MIME types for the assets served by this controller. */
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            ".html", "text/html; charset=UTF-8",
            ".js",   "application/javascript; charset=UTF-8",
            ".css",  "text/css; charset=UTF-8",
            ".json", "application/json; charset=UTF-8",
            ".png",  "image/png",
            ".svg",  "image/svg+xml",
            ".ico",  "image/x-icon"
    );

    /**
     * Serves {@code /gateway-ui/index.html} at {@code GET /gateway/admin/ui}.
     */
    public void serveUi(Context ctx) {
        serveClasspathResource(ctx, "/gateway-ui/index.html");
    }

    /**
     * Serves any asset under {@code /gateway-ui/<file>} at its natural URL path.
     * Registered as {@code GET /gateway-ui/<path>} before the proxy catch-all.
     */
    public void serveAsset(Context ctx) {
        String filePath = ctx.path();   // e.g. /gateway-ui/app.js
        serveClasspathResource(ctx, filePath);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void serveClasspathResource(Context ctx, String classpathPath) {
        try (InputStream is = UiController.class.getResourceAsStream(classpathPath)) {
            if (is == null) {
                throw new NotFoundResponse("UI resource not found: " + classpathPath);
            }
            String contentType = resolveContentType(classpathPath);
            ctx.contentType(contentType).result(is.readAllBytes());
        } catch (NotFoundResponse ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalServerErrorResponse("Failed to serve asset: " + ex.getMessage());
        }
    }

    private String resolveContentType(String path) {
        int dot = path.lastIndexOf('.');
        if (dot >= 0) {
            String ext = path.substring(dot).toLowerCase();
            return CONTENT_TYPES.getOrDefault(ext, "application/octet-stream");
        }
        return "application/octet-stream";
    }
}


