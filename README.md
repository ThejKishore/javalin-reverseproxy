# Javalin Gateway

A **production-ready, fully-featured API reverse proxy** built on
[Javalin 7.1.0](https://javalin.io/) and Java 21.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Javalin](https://img.shields.io/badge/Javalin-7.1.0-blue.svg)](https://javalin.io/)

---

## 📚 Documentation

All supplementary docs live in the [`docs/`](./docs/) folder. Start here and follow the links:

| Document | Description |
|---|---|
| [Quick Start Guide](./docs/QUICK_START.md) | How to run, access the UI, create routes, troubleshoot |
| [Project Summary](./docs/PROJECT_SUMMARY.md) | High-level overview, goals, and scope |
| [Implementation Details](./docs/README_IMPLEMENTATION.md) | Architecture, API endpoints, file structure, color palette |
| [Completion Report](./docs/COMPLETION_REPORT.md) | Executive summary, requirements completed, QA results |
| [UI Light Theme Changes](./docs/UI_LIGHT_THEME_CHANGES.md) | CSS changes, color palette reference |
| [Light Theme Migration](./docs/LIGHT_THEME_MIGRATION.md) | Dark→light migration, component-by-component breakdown |
| [UI Refactoring Summary](./docs/UI_REFACTORING_SUMMARY.md) | UI refactoring changes summary |
| [Visual Comparison](./docs/VISUAL_COMPARISON.md) | Before/after visual comparisons |
| [Verification Checklist](./docs/VERIFICATION_CHECKLIST.md) | Final verification and sign-off checklist |
| [Audit Testing Guide](./docs/AUDIT_TESTING_GUIDE.md) | How to test audit functionality |
| [Audit Checklist](./docs/AUDIT_COMPREHENSIVE_CHECKLIST.md) | Comprehensive audit checklist |
| [Delivery Manifest](./docs/DELIVERY_MANIFEST.md) | Files delivered and their purpose |
| [Implementation File Manifest](./docs/IMPLEMENTATION_FILE_MANIFEST.md) | All implementation files listed |
| [UI Plan](./docs/ui-plan.md) | UI planning notes |
| [UI Dashboard](./docs/ui-dashboard.md) | Dashboard design notes |

---

## Table of Contents

1. [Features](#features)
2. [Architecture](#architecture)
3. [Quick Start](#quick-start)
4. [Configuration Reference](#configuration-reference)
5. [Routing Types](#routing-types)
6. [Load Balancing](#load-balancing)
7. [Rate Limiting](#rate-limiting)
8. [Circuit Breaker](#circuit-breaker)
9. [Caching](#caching)
10. [Header Management](#header-management)
11. [Request & Response Transformation](#request--response-transformation)
12. [Tracing & Forwarded Headers](#tracing--forwarded-headers)
13. [Auditing](#auditing)
14. [Authentication Header Forwarding](#authentication-header-forwarding)
15. [Security Filters](#security-filters)
16. [WebSocket Proxying](#websocket-proxying)
17. [Dynamic Route Management (Admin API)](#dynamic-route-management-admin-api)
18. [Security Validation Rules API](#security-validation-rules-api)
19. [Health Checks](#health-checks)
20. [Database-Backed Routes](#database-backed-routes)
21. [Kubernetes Deployment](#kubernetes-deployment)
22. [Building & Running](#building--running)
23. [Testing](#testing)
24. [Module Structure](#module-structure)

---

## Features

| Capability | Details |
|---|---|
| **Reverse Proxy** | Forwards all HTTP verbs (GET/POST/PUT/PATCH/DELETE/HEAD/OPTIONS) to upstream servers |
| **Routing** | Path-prefix, Regex, Header-based, Traffic-split |
| **Load Balancing** | Round-Robin, Weighted, Random — per route |
| **Rate Limiting** | Sliding-window rate limiter via Resilience4j, per route |
| **Circuit Breaker** | Resilience4j circuit breaker, per route |
| **Caching** | Caffeine in-process cache for GET responses |
| **Header Management** | Add / exclude / deduplicate headers on request & response |
| **Strip Prefix** | Remove a path prefix before forwarding |
| **Body Transform** | Pluggable `RequestTransformer` / `ResponseTransformer` hooks |
| **Tracing** | Propagates / generates `X-Request-ID` and `X-Trace-ID` |
| **Forwarded-For** | Injects `X-Forwarded-For` and `X-Real-IP` |
| **Auth Forwarding** | Configurable list of auth headers forwarded verbatim |
| **Security Filters** | Global or per-route JWT, CSRF, CSP, and HTTP input blacklist validation |
| **Auditing** | Records request/response metadata to database or log file |
| **WebSocket** | Full bidirectional WebSocket proxying via OkHttp |
| **HTTP/2 & HTTPS** | Transparent via OkHttp — no extra configuration needed |
| **Dynamic Routes** | Add/update/delete/enable/disable routes via REST API |
| **Config Reload** | Periodic refresh from YAML file **or** database |
| **Health Checks** | `/gateway/health`, `/gateway/health/live`, `/gateway/health/ready` |
| **Kubernetes-ready** | Stateless; multiple replicas share routes via Postgres |

---

## Architecture

```
Client
  │
  ▼
┌────────────────────────────────────────────────────────────────────────┐
│  Javalin 7.1.0 (Jetty 12)                                              │
│                                                                        │
│  GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS  /<path>  →  ProxyHandler      │
│  WS  /<path>                             →  WebSocketProxyHandler      │
│  GET /gateway/health/**                  →  HealthController           │
│  *   /gateway/admin/**                   →  AdminController            │
└────────────────────────────────────────────────────────────────────────┘
                │
                ▼
         RouteRegistry.match(ctx)
                │
                ▼
   ┌────────── Filter Chain ────────────────────────────────────────┐
   │  JwtAuthFilter         (global or per-route — fail fast)                │
   │  CsrfGatewayFilter     (global or per-route — unsafe methods only)      │
   │  TracingFilter         (inject X-Request-ID, X-Trace-ID)                │
   │  SlidingWindowRateLimiter  (optional, per-route)                        │
   │  CircuitBreakerGatewayFilter  (optional, per-route)                     │
   │  CacheGatewayFilter     (optional, GET only)                            │
   │  ForwardedForFilter     (X-Forwarded-For / X-Real-IP)                   │
   │  HttpValidationFilter   (optional blacklist validation)                  │
   │  HeaderMutationFilter   (add/exclude request & response hdrs)           │
   │  TransformGatewayFilter (pluggable body transforms)                     │
   │  DedupeResponseHeadersFilter  (optional)                                │
   │  CspGatewayFilter       (global or per-route CSP response header)       │
   │  ProxyFilter            ← terminal: LoadBalancer + OkHttp               │
   └────────────────────────────────────────────────────────────────┘
                │
                ▼
         Upstream server(s)
```

Filter chains are **pre-built per route** at startup / reload time and stored in the `RouteRegistry`.
Each incoming request creates a fresh `DefaultFilterChain` instance (immutable, index-based) — no locking needed at request time.

---

## Quick Start

### Prerequisites

- Java 21+
- Gradle 8.x (wrapper included)

### Run

```bash
git clone https://github.com/your-org/javalin-gateway.git
cd javalin-gateway
./gradlew :app:run
```

The gateway starts on **port 8080** with the sample route (`/api/example → http://localhost:9090`).

### Verify

```bash
curl http://localhost:8080/gateway/health
curl http://localhost:8080/gateway/admin/routes
```

---

## Configuration Reference

All configuration lives under the top-level `gateway:` key in
`app/src/main/resources/application.yml`.

```yaml
gateway:
  port: 8080
  config-source: yaml             # "yaml" or "database"
  refresh-interval-seconds: 60

  datasource:
    type: h2                      # "h2" or "postgres"
    url: "jdbc:h2:mem:gateway;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
    username: sa
    password: ""

  routes:
    - id: my-service
      name: My Service
      path-pattern: /api/my
      routing-type: PATH          # PATH | REGEX | HEADER | TRAFFIC_SPLIT
      strip-prefix: /api/my
      enabled: true
      timeout-ms: 5000
      load-balancer-type: ROUND_ROBIN   # ROUND_ROBIN | WEIGHTED | RANDOM

      targets:
        - url: http://my-service-1:8080
          weight: 2
        - url: http://my-service-2:8080
          weight: 1

      rate-limit-policy:
        enabled: true
        requests-per-second: 100
        burst: 20
        timeout-duration-ms: 0

      circuit-breaker-policy:
        enabled: true
        failure-rate-threshold: 50
        wait-duration-seconds: 60
        sliding-window-size: 10

      cache-policy:
        enabled: true
        ttl-seconds: 300
        cache-key-strategy: METHOD_PATH_QUERY

      header-rules:
        add-request:
          X-Gateway: "javalin-gateway"
        exclude-request:
          - Cookie
        add-response:
          X-Powered-By: "javalin-gateway"
        exclude-response:
          - Server
        dedupe-response-headers:
          - Access-Control-Allow-Origin

      auth-forward-headers:
        - Authorization
        - X-API-Key

      audit-enabled: true
      audit-store: database       # "database" or "file"

      jwt-policy:
        enabled: false
        algorithm: HS256
        secret-or-public-key: "change-me-minimum-32-chars-secret!!"
        issuer: ""
        audience: ""
        required-claims: {}
        exclude-paths: []

      csrf-policy:
        enabled: false
        token-ttl-seconds: 3600
        bind-to: IP                # IP | SESSION
        exclude-paths: []

      csp-policy:
        enabled: false
        policy: "default-src 'self'"
        report-only: false
        exclude-paths: []

      http-validation-policy:
        enabled: false
        validate-query-params: true
        validate-headers: true
        validate-cookies: true
        validate-body: false
        exclude-paths: []
```

---

## Routing Types

### PATH
Matches when the request path **starts with** `path-pattern` at a segment boundary.
```yaml
path-pattern: /api/users
# Matches: /api/users, /api/users/123
# Does NOT match: /api/users-v2
```
LoadBalanecer Type `ROUND_ROBIN` or `RANDOM` is recommended for PATH routes.

### REGEX
Full Java regex match against the request path.
```yaml
routing-type: REGEX
path-pattern: "/api/v[0-9]+/.*"
```
LoadBalancer Type `ROUND_ROBIN` or `RANDOM` is recommended for REGEX routes.

### HEADER
Routes only when a specific request header matches a configured value.
```yaml
routing-type: HEADER
header-match-name: X-Canary
header-match-value: "true"
```
LoadBalancer Type `ROUND_ROBIN` or `RANDOM` is recommended for HEADER routes.

### TRAFFIC_SPLIT
Weight-based or header-based distribution across multiple targets.

LoadBalancer Type `WEIGHTED` or `HEADER` is recommended for TRAFFIC_SPLIT routes.

Scenario 1: 90% stable, 10% canary Traffic split:
```yaml
routing-type: TRAFFIC_SPLIT
load-balancer-type: WEIGHTED
targets:
  - url: http://stable:8080
    weight: 9
  - url: http://canary:8080
    weight: 1
```

Scenario 2: Header-based split (e.g., A/B testing):

```yaml
routing-type: TRAFFIC_SPLIT
load-balancer-type: HEADER
header-match-name: X-User-Group
header-match-value: "beta"
targets:
  - url: http://beta-group:8080
    header-match-name: X-User-Group
    header-match-value: "beta"
  - url: http://default-group:8080
    header-match-name: X-User-Group
    header-match-value: "default"
```

---

## Load Balancing

| Strategy      | Behaviour                                                                  |
|---------------|----------------------------------------------------------------------------|
| `ROUND_ROBIN` | Cycles through targets (lock-free AtomicInteger, per-route)                |
| `WEIGHTED`    | Random selection proportional to `weight`                                  |
| `HEADER`      | Routes to the target whose `header-match-value` matches the request header |
| `RANDOM`      | Uniform random selection                                                   |

---

## Rate Limiting

Uses Resilience4j's `RateLimiter`. When exceeded:
```json
HTTP 429  {"status": 429, "error": "Rate limit exceeded for route: my-service"}
```

---

## Circuit Breaker

Uses Resilience4j's `CircuitBreaker` (count-based sliding window). When open:
```json
HTTP 503  {"status": 503, "error": "Circuit breaker is OPEN for route: my-service"}
```

---

## Caching

Caffeine in-process cache for `GET` responses. Only 2xx responses are cached.
Response header `X-Cache: HIT` / `X-Cache: MISS` indicates cache status.

---

## Header Management

- **add-request** / **exclude-request**: Modify headers forwarded to the upstream.
- **add-response** / **exclude-response**: Modify headers returned to the client.
- **dedupe-response-headers**: Collapse duplicate values (e.g. `Access-Control-Allow-Origin`) into one comma-separated value.
- **strip-prefix**: Remove a prefix from the path before forwarding.

---

## Request & Response Transformation

Implement `RequestTransformer` or `ResponseTransformer` (both in the utilities module) and register them via `TransformGatewayFilter`. The default pass-through implementation does nothing.

---

## Tracing & Forwarded Headers

| Header | Behaviour |
|---|---|
| `X-Request-ID` | Propagated or generated (UUID) per request |
| `X-Trace-ID` | Propagated or generated (UUID) per request |
| `X-Forwarded-For` | Client IP appended to any existing chain |
| `X-Real-IP` | Set to the direct client IP |

Both tracing headers are echoed in every response.

---

## Auditing

Enable per route: `audit-enabled: true`. Records are written to:
- **SLF4J** (`logs/audit.log` via Logback) — always
- **Database** (`audit_logs` table) — when `audit-store: database`

---

## Authentication Header Forwarding

```yaml
auth-forward-headers:
  - Authorization
  - X-API-Key
```

These headers are forwarded verbatim to the upstream regardless of `exclude-request` rules.

---

## Security Filters

Security filters can be configured **globally** (applied to all routes) in the
top-level `gateway:` block, or **per-route** to override or disable the global
policy for specific routes. Global policies are hot-reloadable via
`POST /gateway/admin/reload`.

| Filter | Purpose | Failure Status |
|---|---|---|
| `JwtAuthFilter` | Validates Bearer JWT token and required claims | `401` / `403` |
| `CsrfGatewayFilter` | Enforces CSRF token on unsafe methods (`POST`,`PUT`,`DELETE`,`PATCH`) | `403` |
| `HttpValidationFilter` | Validates query/header/cookie/body against blacklist regex rules | `400` |
| `CspGatewayFilter` | Injects `Content-Security-Policy` (or report-only variant) on response | N/A (header injection) |

Global policy example (applies to every route):
```yaml
gateway:
  jwt-policy:
    enabled: true
    algorithm: HS256
    secret-or-public-key: "change-me-minimum-32-chars-secret!!"
    exclude-paths: [/gateway/health, /gateway/admin]
  csrf-policy:
    enabled: true
    bind-to: IP
    exclude-paths: [/gateway/health, /gateway/admin]
  csp-policy:
    enabled: true
    policy: "default-src 'self'"
    exclude-paths: [/gateway/health, /gateway/admin]
```

Per-route override (disable JWT for one route):
```yaml
routes:
  - id: public-service
    jwt-policy: { enabled: false }
```

CSRF tokens are stored via distributed `CsrfTokenStore` (Hazelcast implementation in `app`).

---

## WebSocket Proxying

Bidirectional WebSocket proxy via OkHttp. Any path matching a route's `path-pattern` is automatically eligible for WebSocket proxying — no additional configuration required.

---

## Dynamic Route Management (Admin API)

| Method | Path | Description |
|---|---|---|
| `GET`    | `/gateway/admin/routes`           | List all routes |
| `GET`    | `/gateway/admin/routes/{id}`      | Get a single route |
| `POST`   | `/gateway/admin/routes`           | Create a route |
| `PUT`    | `/gateway/admin/routes/{id}`      | Replace a route |
| `DELETE` | `/gateway/admin/routes/{id}`      | Delete a route |
| `PATCH`  | `/gateway/admin/routes/{id}/enable`  | Enable a route |
| `PATCH`  | `/gateway/admin/routes/{id}/disable` | Disable a route |
| `POST`   | `/gateway/admin/reload`           | Reload all routes from source |

**Create example:**
```bash
curl -X POST http://localhost:8080/gateway/admin/routes \
  -H "Content-Type: application/json" \
  -d '{
    "name": "New Service",
    "path-pattern": "/api/new",
    "routing-type": "PATH",
    "strip-prefix": "/api/new",
    "enabled": true,
    "timeout-ms": 5000,
    "load-balancer-type": "ROUND_ROBIN",
    "targets": [{"url": "http://new-service:8080", "weight": 1}]
  }'
```

---

## Security Validation Rules API

Dynamic blacklist rule management endpoints:

| Method | Path | Description |
|---|---|---|
| `GET`    | `/gateway/admin/security/rules` | List all rules |
| `POST`   | `/gateway/admin/security/rules` | Create a new rule |
| `POST`   | `/gateway/admin/security/rules/reload` | Reload in-memory rules from DB |
| `PATCH`  | `/gateway/admin/security/rules/{id}/enable` | Enable a rule |
| `PATCH`  | `/gateway/admin/security/rules/{id}/disable` | Disable a rule |
| `DELETE` | `/gateway/admin/security/rules/{id}` | Delete a rule |

Example create payload:
```json
{
  "name": "XSS script tag",
  "pattern": "(?i)<script",
  "target": "ALL",
  "enabled": true
}
```

---

## Health Checks

| Endpoint | Purpose | HTTP on success |
|---|---|---|
| `GET /gateway/health` | Combined status | `200` |
| `GET /gateway/health/live` | Kubernetes liveness probe | Always `200` |
| `GET /gateway/health/ready` | Kubernetes readiness probe | `200` when routes loaded |

---

## Database-Backed Routes

```yaml
gateway:
  config-source: database
  datasource:
    type: postgres
    url: "jdbc:postgresql://postgres:5432/gateway"
    username: gateway
    password: secret
```

Flyway migrations create the `routes` and `audit_logs` tables automatically.

**Insert a route via SQL:**
```sql
INSERT INTO routes (id, name, config_json, enabled) VALUES (
  'svc-1', 'My Service',
  '{"id":"svc-1","name":"My Service","path-pattern":"/api/svc","routing-type":"PATH",
    "strip-prefix":"/api/svc","enabled":true,"timeout-ms":5000,
    "load-balancer-type":"ROUND_ROBIN","targets":[{"url":"http://svc:8080","weight":1}]}',
  true
);
```

After inserting, call `POST /gateway/admin/reload` or wait for the next scheduled refresh.

---

## Kubernetes Deployment

Use **PostgreSQL** so all replicas share route state.

```yaml
# deployment.yaml (excerpt)
containers:
  - name: gateway
    image: your-registry/javalin-gateway:latest
    ports:
      - containerPort: 8080
    livenessProbe:
      httpGet:
        path: /gateway/health/live
        port: 8080
    readinessProbe:
      httpGet:
        path: /gateway/health/ready
        port: 8080
```

For YAML-sourced routes in multi-instance deployments, mount `application.yml` as a Kubernetes ConfigMap volume; the gateway re-reads it every `refresh-interval-seconds`.

---

## Building & Running

### Build Tool: Mill

This project uses **Mill**, a modern, lightweight build tool for the JVM that offers:
- **Declarative configuration** via `*.mill.yaml` files (1/10th the size of Maven/Gradle)
- **Aggressive caching & parallelism** — 3-7x faster builds than Maven/Gradle
- **Object-oriented build structure** — easy to understand and extend
- **Multi-module support** with clear module boundaries

#### Prerequisites

- Java 21+
- Mill 1.1.6+ (install via [Mill docs](https://mill-build.org/) or Homebrew: `brew install mill`)

#### Quick Reference: Common Mill Commands

| Command | Purpose |
|---------|---------|
| `mill app.run` | Run the gateway application locally |
| `mill app.test` | Run all app module tests |
| `mill utilities.test` | Run utilities module tests |
| `mill shared.test` | Run shared module tests |
| `mill test` | Run all tests across all modules |
| `mill app.compile` | Compile app module sources |
| `mill app.assembly` | Build executable JAR with all dependencies |
| `mill __.compile` | Compile all modules |
| `mill __.test` | Run all tests (same as `mill test`) |

#### Running the Gateway

```bash
# Start the gateway
mill app.run

# With custom environment (sets JVM args and env variables)
mill app.run
# NB: VM arg `--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED` 
# and env var `app_env=development` are auto-configured
```

#### Running Tests

```bash
# Run all tests across all modules
mill test

# Run app module tests only
mill app.test

# Run utilities module tests only
mill utilities.test

# Run shared module tests only
mill shared.test

# Run tests with watch mode (re-runs on source changes)
mill test -w

# Run a specific test class
mill app.test org.example.gateway.api.AdminControllerTest
```

#### Building Distributions

```bash
# Build a standalone executable JAR
mill app.assembly

# Output: out/app/assembly.dest/out.jar

# Build dist ZIP with launcher scripts
mill app.installDist

# Output: out/app/installDist.dest/
```

#### Module Structure & Dependencies

The project is organized into three modules:

**`app/`** — Main Javalin application
- Depends on: `shared`, `utilities`
- Contains: Gateway entry point, controllers, configuration, database, proxy logic, admin APIs
- Build config: `app/package.mill.yaml`

**`utilities/`** — Reusable filter & routing primitives
- Depends on: `shared`
- Contains: Filter chains, load balancers, rate limiters, circuit breaker, cache, routing matchers
- **No app-layer dependencies** — can be used independently
- Build config: `utilities/package.mill.yaml`

**`shared/`** — Common DTOs and mapper utilities
- Depends on: nothing (leaf module)
- Contains: RouteDefinition, TargetDefinition, and cross-module DAO/mapping helpers
- Build config: `shared/package.mill.yaml`

Module dependency graph:
```
app ──────────┐
              ├──→ shared
utilities ────┘
```

#### Running Tests Per Module

Tests are nested under each module:

```bash
# App tests
mill app.test

# Utilities tests
mill utilities.test

# Shared tests
mill shared.test

# All at once (parallel by default)
mill test
```

Each test module is configured with JUnit 5 (`TestModule.Junit5`) via the base `ProjectBaseModule` in `mill-build/src/ProjectBaseModule.scala`.

#### Troubleshooting Mill

If you encounter issues:

```bash
# Clear Mill's cache
rm -rf out/

# Re-run with verbose output
mill --debug app.test

# Show module dependencies
mill --show app.moduleDeps

# List all available tasks
mill --help
```

For more details, see the [Mill documentation](https://mill-build.org/mill/).

---

## Testing

| Test class | Coverage |
|---|---|
| `PathRouteMatcherTest` | Path-prefix matching, edge cases |
| `RoundRobinLoadBalancerTest` | Cycle, concurrency safety |
| `DefaultFilterChainTest` | Ordering, short-circuit, exception propagation |
| `YamlConfigLoaderTest` | YAML parsing, field mapping |
| `RouteRegistryTest` | Add / update / remove / enable / disable |
| `AdminControllerTest` | Admin REST API (JavalinTest) |
| `HealthControllerTest` | Liveness / readiness probes (JavalinTest) |
| `ProxyHandlerIntegrationTest` | End-to-end with MockWebServer |

---

## Module Structure

```
javalin-gateway/
├── app/               # Main Javalin application
│   └── org.example.gateway
│       ├── GatewayApp           (entry point)
│       ├── api/                 (AdminController, HealthController)
│       ├── audit/               (AuditGatewayFilter)
│       ├── config/              (GatewayConfig, YamlConfigLoader)
│       ├── db/                  (DatabaseManager, RouteDao, AuditDao …)
│       ├── proxy/               (ProxyHandler, WebSocketProxyHandler)
│       └── registry/            (RouteRegistry, RouteLoader)
│
└── utilities/         # Reusable filter infrastructure (no app-layer deps)
    └── org.example.utilities.gateway
        ├── cache/               (CacheStore, CaffeineCache, CacheGatewayFilter)
        ├── circuitbreaker/      (CircuitBreakerFactory, CircuitBreakerGatewayFilter)
        ├── exception/           (GatewayException hierarchy)
        ├── filter/              (GatewayFilter, FilterChain, FilterContext, DefaultFilterChain)
        ├── header/              (HeaderMutationFilter, DedupeResponseHeadersFilter)
        ├── loadbalancer/        (LoadBalancer, RoundRobin, Weighted, Random)
        ├── model/               (RouteDefinition, TargetDefinition, policies …)
        ├── proxy/               (OkHttpUpstreamClient, WebSocketForwarder, ProxyFilter …)
        ├── ratelimit/           (SlidingWindowRateLimiter)
        ├── routing/             (PathRouteMatcher, RegexRouteMatcher, HeaderRouteMatcher …)
        ├── tracing/             (TracingFilter)
        └── transform/           (RequestTransformer, ResponseTransformer, TransformGatewayFilter)
```

---

## License

Apache License 2.0 — see [LICENSE](LICENSE).

```shell

az role assignment create \
--role "Storage Table Data Contributor" \
--assignee $(az ad signed-in-user show --query id -o tsv) \
--scope "/subscriptions/bc01e45b-fca2-4819-ac40-e8d9aeade6a9/resourceGroups/rg-datalake-demo/providers/Microsoft.Storage/storageAccounts/thejdatalake"
```
