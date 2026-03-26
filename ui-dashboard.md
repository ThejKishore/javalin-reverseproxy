# Gateway Admin Dashboard — Documentation

<!--
  Licensed under the Apache License, Version 2.0
-->

## Overview

The Gateway Admin Dashboard is a single-page application served directly by Javalin at
`GET /gateway/admin/ui`. It requires **no build tool, no Node.js, and no separate
deployment** — the HTML file is bundled inside the application JAR as a classpath resource.

The UI is built with **Vue 3** and **PrimeVue 3 (CDN)** loaded entirely from CDN.

---

## Access

| Gateway port (default) | URL |
|------------------------|-----|
| 7070                   | `http://localhost:7070/gateway/admin/ui` |

---

## CDN Dependencies

| Library | Version | CDN URL | Purpose |
|---------|---------|---------|---------|
| Vue 3 | 3.x | `https://unpkg.com/vue@3/dist/vue.global.prod.js` | Reactivity & template engine |
| PrimeVue core | 3.53.0 | `https://unpkg.com/primevue@3.53.0/core/core.min.js` | PrimeVue plugin/runtime |
| PrimeVue components | 3.53.0 | `https://unpkg.com/primevue@3.53.0/<component>/<component>.min.js` | DataTable, Dialog, etc. |
| PrimeVue theme | 3.53.0 | `https://unpkg.com/primevue@3.53.0/resources/themes/lara-dark-indigo/theme.css` | Dark theme |
| PrimeVue base CSS | 3.53.0 | `https://unpkg.com/primevue@3.53.0/resources/primevue.min.css` | Component styling |
| PrimeIcons | latest | `https://unpkg.com/primeicons/primeicons.css` | Icon font |
| PrimeFlex | 3.x | `https://unpkg.com/primeflex@3/primeflex.min.css` | Flex/grid utilities |

The application includes comprehensive fallback CSS so the dashboard remains fully functional
even if the PrimeVue Aura theme CDN is unavailable (e.g. offline / air-gapped environment).

---

## File Locations

| File | Description |
|------|-------------|
| `app/src/main/resources/gateway-ui/index.html` | Single-page dashboard HTML |
| `app/src/main/java/org/example/gateway/api/UiController.java` | Javalin handler that reads and serves the HTML |
| `app/src/main/java/org/example/gateway/GatewayApp.java` | Route registration (`GET /gateway/admin/ui`) |

---

## REST API — UI Action Mapping

| UI Action | HTTP Method | Endpoint |
|-----------|-------------|----------|
| Load routes table | `GET` | `/gateway/admin/routes` |
| Create route | `POST` | `/gateway/admin/routes` |
| Update route | `PUT` | `/gateway/admin/routes/{id}` |
| Delete route | `DELETE` | `/gateway/admin/routes/{id}` |
| Enable route | `PATCH` | `/gateway/admin/routes/{id}/enable` |
| Disable route | `PATCH` | `/gateway/admin/routes/{id}/disable` |
| Reload gateway | `POST` | `/gateway/admin/reload` |
| Health indicator | `GET` | `/gateway/health` |

All requests use `Content-Type: application/json`. The health indicator in the page header
is refreshed on every page load and on manual refresh.

---

## RouteDefinition JSON Field Reference

The API serialises / deserialises `RouteDefinition` using **kebab-case** keys via
Jackson `@JsonProperty` annotations. The dashboard sends and receives exactly these keys.

### Core fields

| JSON key | Type | Default | Description |
|----------|------|---------|-------------|
| `id` | `string` | auto UUID | Unique route identifier |
| `name` | `string` | — | Human-readable label (**required**) |
| `path-pattern` | `string` | — | Incoming path to match (**required**) |
| `routing-type` | `string` | `PATH` | `PATH` \| `REGEX` \| `HEADER` \| `TRAFFIC_SPLIT` |
| `strip-prefix` | `string` | `null` | Prefix stripped before forwarding |
| `enabled` | `boolean` | `true` | Whether the route is active |
| `timeout-ms` | `int` | `5000` | Per-route upstream timeout in ms |
| `load-balancer-type` | `string` | `ROUND_ROBIN` | `ROUND_ROBIN` \| `WEIGHTED` \| `RANDOM` |
| `targets` | `array` | `[]` | List of upstream targets (see below) |
| `header-match-name` | `string` | `null` | Header name for `HEADER` routing |
| `header-match-value` | `string` | `null` | Expected value for `HEADER` routing |
| `audit-enabled` | `boolean` | `false` | Enable request/response auditing |
| `audit-store` | `string` | `database` | `database` \| `file` |

### Target object (`targets[]`)

| JSON key | Type | Default | Description |
|----------|------|---------|-------------|
| `url` | `string` | — | Upstream base URL (e.g. `http://backend:8080`) |
| `weight` | `int` | `1` | Relative weight for `WEIGHTED` load balancing |

### Rate-limit policy (`rate-limit-policy`)

| JSON key | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | `boolean` | `false` | Toggle |
| `requests-per-second` | `int` | `100` | Sustained request rate |
| `burst` | `int` | `20` | Allowed burst above sustained rate |
| `timeout-duration-ms` | `long` | `0` | Permit-wait timeout; `0` = fail-fast |

### Circuit-breaker policy (`circuit-breaker-policy`)

| JSON key | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | `boolean` | `false` | Toggle |
| `failure-rate-threshold` | `int` | `50` | % failures that trip the breaker |
| `wait-duration-seconds` | `int` | `60` | Seconds before moving to half-open |
| `sliding-window-size` | `int` | `10` | Rolling window size for failure rate |

### Cache policy (`cache-policy`)

| JSON key | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | `boolean` | `false` | Toggle |
| `ttl-seconds` | `int` | `300` | Cache entry TTL |
| `cache-key-strategy` | `string` | `METHOD_PATH_QUERY` | `METHOD_PATH` \| `METHOD_PATH_QUERY` |

### Header rules (`header-rules`)

| JSON key | Type | Description |
|----------|------|-------------|
| `add-request` | `object` | Headers injected into the upstream request |
| `exclude-request` | `array<string>` | Request header names to strip |
| `add-response` | `object` | Headers injected into the client response |
| `exclude-response` | `array<string>` | Response header names to strip |
| `dedupe-response-headers` | `array<string>` | Response headers whose duplicates are collapsed |

### Auth forward headers

| JSON key | Type | Description |
|----------|------|-------------|
| `auth-forward-headers` | `array<string>` | Header names forwarded verbatim to upstream (e.g. `Authorization`) |

---

## Dashboard UI Layout

```
┌─────────────────────────────────────────────────────────────────────┐
│  🖥  Gateway Admin   Route Management Console          [Health] [↻] │  ← sticky header
├─────────────┬─────────────┬─────────────┬─────────────────────────┤
│  Total: N   │  Active: N  │ Disabled: N │  Health: UP/DOWN        │  ← stats bar
├─────────────┴─────────────┴─────────────┴─────────────────────────┤
│  [+ New Route]  [⟳ Reload Gateway]           [🔍 Search routes…]  │  ← toolbar
├───────┬──────────────┬────────┬─────────┬────┬──────┬───────┬─────┤
│ Name  │ Path Pattern │  Type  │ Targets │ LB │Status│Timeout│Acts │  ← table
│ …     │ /api/…       │ [PATH] │ http://…│ RR │[✓]  │ 5000ms│✏ ⏸🗑│
└───────┴──────────────┴────────┴─────────┴────┴──────┴───────┴─────┘
```

### Create / Edit Dialog — Tab Structure

| Tab | Fields |
|-----|--------|
| **Basic** | `name`, `path-pattern`, `routing-type`, `load-balancer-type`, `strip-prefix`, `timeout-ms`, `enabled` toggle, `header-match-name` + `header-match-value` (visible only when `routing-type = HEADER`) |
| **Targets** | Dynamic list of `{ url, weight }` rows with Add / Remove |
| **Policies** | Toggle sections for Rate Limiting, Circuit Breaker, Caching, Audit — each reveals its fields when enabled |
| **Headers** | Add-Request headers map, Add-Response headers map, Exclude-Request chips, Exclude-Response chips, Auth-Forward-Headers chips |

---

## How the HTML is Served

```
GET /gateway/admin/ui
  → GatewayApp.java registers route inside path("/gateway/admin", ...)
  → UiController.serveUi(Context ctx) is invoked
  → reads /gateway-ui/index.html from classpath (bundled in JAR)
  → responds with Content-Type: text/html; charset=UTF-8
```

Because the route is registered **before** the catch-all proxy routes
(`/<path>` wildcard handlers), it is never intercepted by the proxy logic.

---

## Adding a New Route (Step-by-step)

1. Open `http://localhost:7070/gateway/admin/ui`
2. Click **New Route**
3. Fill in **Basic** tab — name and path-pattern are mandatory
4. Switch to **Targets** tab — add at least one upstream URL
5. (Optional) configure Policies and Headers
6. Click **Create Route** — the route is persisted to the database and activated immediately

---

## Known Limitations

- No authentication on admin endpoints — secure the `/gateway/admin/*` path with a
  `before` handler or a network-level policy before exposing to untrusted networks.
- The dashboard does not auto-refresh the table; click the refresh icon in the header
  or reload the page to see changes made by other instances.
- Pagination is client-side (all routes loaded at once); for very large numbers of
  routes consider adding server-side pagination to the admin API.
