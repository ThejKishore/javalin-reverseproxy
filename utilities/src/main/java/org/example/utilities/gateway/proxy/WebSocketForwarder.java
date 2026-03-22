/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.proxy;

import io.javalin.websocket.WsConfig;
import okhttp3.*;
import org.example.utilities.gateway.model.RouteDefinition;
import org.example.utilities.gateway.model.TargetDefinition;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Bridges a Javalin WebSocket session to an upstream WebSocket server.
 * Messages flow bidirectionally; close/error events on either side propagate
 * to the other.
 */
public class WebSocketForwarder {

    private static final Logger log = LoggerFactory.getLogger(WebSocketForwarder.class);

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)  // WebSocket — no read timeout
            .build();

    /** Map from Javalin session ID → upstream OkHttp WebSocket. */
    private final ConcurrentHashMap<String, WebSocket> upstreamSockets = new ConcurrentHashMap<>();

    public void configure(WsConfig ws, RouteDefinition route, TargetDefinition target) {
        ws.onConnect(ctx -> {
            String sessionId = ctx.sessionId();
            // Path is available via the Javalin route path param (ws("/<path>", ...))
            String rawPath = ctx.pathParamMap().getOrDefault("path", "");
            String path = rawPath.isEmpty() ? "/" : ("/" + rawPath);
            String upstreamWsUrl = buildWsUrl(target.getUrl(), path);
            log.debug("WS connect {} → {}", sessionId, upstreamWsUrl);

            Request req = new Request.Builder().url(upstreamWsUrl).build();
            WebSocket upstream = httpClient.newWebSocket(req, new WebSocketListener() {
                @Override
                public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                    ctx.send(text);
                }
                @Override
                public void onMessage(@NotNull WebSocket webSocket, @NotNull okio.ByteString bytes) {
                    ctx.send(bytes.toByteArray());
                }
                @Override
                public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, okhttp3.Response resp) {
                    log.warn("Upstream WS error for session {}: {}", sessionId, t.getMessage());
                    ctx.closeSession(1011, "Upstream error");
                    upstreamSockets.remove(sessionId);
                }
                @Override
                public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                    ctx.closeSession(code, reason);
                    upstreamSockets.remove(sessionId);
                }
            });
            upstreamSockets.put(sessionId, upstream);
        });

        ws.onMessage(ctx -> {
            WebSocket upstream = upstreamSockets.get(ctx.sessionId());
            if (upstream != null) upstream.send(ctx.message());
        });

        ws.onClose(ctx -> {
            WebSocket upstream = upstreamSockets.remove(ctx.sessionId());
            if (upstream != null) upstream.close(ctx.status(), ctx.reason());
        });

        ws.onError(ctx -> {
            WebSocket upstream = upstreamSockets.remove(ctx.sessionId());
            if (upstream != null) upstream.cancel();
        });
    }

    private String buildWsUrl(String targetBase, String path) {
        // Convert http(s) scheme to ws(s)
        String base = targetBase
                .replaceFirst("^https://", "wss://")
                .replaceFirst("^http://",  "ws://");
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + path;
    }
}
