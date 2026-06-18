# System Patterns & Architecture Decisions

## Core Architecture Pattern: Filter Chain

**Why chosen**: Composable, testable, extensible; each concern isolated.

**How it works**:
```
Request → [Filter 1] → [Filter 2] → ... → [Terminal Filter] → Response
```

Each filter processes the request/response, can short-circuit (return early), or pass control to the next filter.

**Key constraints**:
- Filters are **immutable after build time** (thread-safe, no locking at request time)
- Each request gets a fresh `DefaultFilterChain` instance (index-based traversal)
- Filters **cannot spawn blocking threads** (must complete synchronously or use async patterns)

## Routing Strategy Pattern (Sealed Class)

Abstraction over matching logic — allows plugins for custom matchers.

```java
sealed class RouteMatcher permits
    PathRouteMatcher,
    RegexRouteMatcher,
    HeaderRouteMatcher,
    TrafficSplitMatcher { ... }
```

**Per-route matcher** determines if a request belongs to this route.

### Routing Types (from README)
| Type | Matching Logic | Use Case |
|---|---|---|
| **PATH** | Segment-boundary prefix match (e.g., `/api/users` matches `/api/users/123`) | Service grouping |
| **REGEX** | Full Java regex against request path (e.g., `/api/v[0-9]+/.*`) | Complex path patterns |
| **HEADER** | Presence or value of header (e.g., `X-Canary: true`) | Feature flags, A/B testing |
| **TRAFFIC_SPLIT** | Weight-based random distribution (no path constraint) | Canary deployments |

## Load Balancing Pattern (Strategy)

Per-target selection logic, decoupled from routing.

```java
sealed interface LoadBalancer permits
    RoundRobinLoadBalancer,    // AtomicInteger, cycle
    WeightedLoadBalancer,      // Random proportional to weight
    RandomLoadBalancer         // Uniform random
```

**Key fact**: Lock-free round-robin via `AtomicInteger` — no synchronization overhead at request time.

## Resilience Composition

**Pattern**: Stack independent policies (no coupling).

Per route, optionally enable:
1. **Rate Limiter** (Resilience4j) → HTTP 429 when exceeded
2. **Circuit Breaker** (Resilience4j) → HTTP 503 when open
3. **Cache** (Caffeine) → Response header `X-Cache: HIT/MISS`

**Key design**: Each policy is **opt-in** and **independent** of others.

## Security Layering Pattern

Security filters are modeled as optional per-route policies and inserted early in the chain.

Recommended execution order in `RouteRegistry.buildChain()`:
1. `JwtAuthFilter` (fail unauthenticated requests early)
2. `CsrfGatewayFilter` (unsafe method protection)
3. Tracing + resilience filters
4. `HttpValidationFilter` (blacklist validation before proxy)
5. Header/transform filters
6. `CspGatewayFilter` (inject response policy)
7. `ProxyFilter` terminal

This order keeps authn/authz checks before expensive upstream calls and ensures CSP is attached to outgoing responses.

## Dynamic Validation Rules Pattern

HTTP validation regex patterns are stored in DB and loaded into an in-memory provider.
- Admin APIs under `/gateway/admin/security/rules/**` mutate DB rules.
- Reload operation refreshes provider snapshots without restarting gateway.
- `HttpValidationFilter` reads current provider snapshot per request.

## Request/Response Transformation

**Pattern**: Pluggable hooks for custom logic.

- `RequestTransformer` — Modify request body/headers before proxying
- `ResponseTransformer` — Modify response body/headers after receiving from upstream

**Default implementation**: Pass-through (no-op).

**Extension point**: Register custom transformers in `TransformGatewayFilter`.

## Multi-Module Design

**Why modularized**: Separation of concerns, testability, reusability.

```
app/              → Stateful layer (routes, admin API, database)
utilities/        → Reusable filter infrastructure (no app deps)
list/             → Utility collections
build-logic/      → Gradle plugin infrastructure
```

**Dependency rule**: `app` can import `utilities`, but `utilities` CANNOT import `app`.

## Configuration Sources

**Pattern**: Pluggable config loader (abstraction over YAML vs. Database).

- **YAML**: `application.yml` loaded at startup + periodically refreshed
- **Database**: Routes table (PostgreSQL/H2) loaded via Flyway + refresh scheduler

**Key design**: `RouteLoader` is config-source-agnostic; just returns `List<RouteDefinition>`.

## Audit Trail Pattern

**Pattern**: Observer pattern, dual-sink strategy.

Every request is audited with:
- Method, path, status, latency, route ID, upstream URL, trace IDs
- Written to **SLF4J** (always) + **Database** (optional)

**Storage options**:
- `logs/audit.log` via Logback (for immediate inspection)
- `audit_logs` table (for compliance queries & retention)

## Stateless Architecture (Kubernetes-Ready)

**Why**: Horizontal scaling without shared state between replicas.

- **Route state** → PostgreSQL (shared across all replicas)
- **In-memory route registry** → Built from database on startup + periodic refresh
- **No sticky sessions** → Any replica can handle any request
- **Health probes** → Indicate readiness independently

## Header Propagation Strategy

| Header | Behavior | Use Case |
|---|---|---|
| `X-Request-ID` | Generated (UUID) if missing, propagated to upstream | Request tracing |
| `X-Trace-ID` | Generated (UUID) if missing, propagated to upstream | Distributed tracing |
| `X-Forwarded-For` | Appended with client IP | Trust chain for upstream |
| `X-Real-IP` | Set to direct client IP | Upstream IP detection |
| Auth headers | Forwarded verbatim (bypass exclude rules) | Preserves authentication |

## Immutability-First Design

- `RouteDefinition` → immutable record
- `TargetDefinition` → immutable record
- `FilterChain` → immutable, index-based
- Configuration snapshots → no mutations after build

**Benefit**: No synchronization needed at request time; thread-safe by default.

## Error Handling Strategy

**Pattern**: Typed exceptions, propagation through filter chain.

- **GatewayException** (base) → HTTP 500
- **RateLimitException** → HTTP 429
- **CircuitBreakerOpenException** → HTTP 503
- **NotFoundResponse** (typed) → HTTP 404 (+ JSON body if client accepts JSON)

Filters can catch and transform exceptions as needed.

## Testing Patterns

- **Unit tests**: Filter behavior in isolation via `DefaultFilterChain`
- **Integration tests**: End-to-end with `JavalinTest` + `MockWebServer`
- **Config tests**: YAML parsing, field validation
- **Database tests**: H2 in-memory, idempotent migrations

