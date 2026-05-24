# Tech Stack & Implementation Context

## Core Technology Stack

| Component | Version | Purpose | Notes |
|---|---|---|---|
| **Java** | 21+ | Language | Modern syntax (records, sealed classes, text blocks) |
| **Javalin** | 7.1.0 | Web framework | Lightweight, config-inside-create pattern |
| **Jetty** | 12 | HTTP server | Modern, supports HTTP/2 |
| **OkHttp** | 4.x | HTTP client | Used for upstream requests, WebSocket support |
| **Resilience4j** | 2.x | Rate limiting, circuit breaker | Per-route policies |
| **Caffeine** | 3.x | In-process caching | Thread-safe, auto-eviction |
| **Jackson** | (bundled) | JSON serialization | Default Javalin serializer |
| **Flyway** | 10.x | Database migrations | Version control for schema |
| **PostgreSQL/H2** | (driver) | Route & audit persistence | PostgreSQL for production, H2 for dev/test |
| **Logback** | (bundled) | SLF4J logging | Configured via `logback.xml` |
| **Gradle** | 8.x | Build tool | Kotlin DSL |

## Database Schema

### routes table
```sql
CREATE TABLE routes (
  id VARCHAR(255) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  config_json JSONB,
  enabled BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);
```

### audit_logs table
```sql
CREATE TABLE audit_logs (
  id BIGSERIAL PRIMARY KEY,
  route_id VARCHAR(255),
  method VARCHAR(10),
  path VARCHAR(1024),
  status INTEGER,
  latency_ms BIGINT,
  request_id UUID,
  trace_id UUID,
  upstream_url VARCHAR(1024),
  response_status INTEGER,
  cache_hit BOOLEAN,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_request_id ON audit_logs(request_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
```

## Configuration File

**Location**: `app/src/main/resources/application.yml`

**Top-level key**: `gateway:`

**Key sections**:
- `port` — Listen port (default 8080)
- `config-source` — "yaml" or "database"
- `refresh-interval-seconds` — How often to reload routes (default 60)
- `datasource` — Connection pool config
- `routes` — List of route definitions (if YAML source)

**Per-route config** includes:
- `routing-type`, `path-pattern`, `strip-prefix`
- `targets` with load balancer strategy
- `rate-limit-policy`, `circuit-breaker-policy`, `cache-policy`
- `header-rules` (add/exclude request & response)
- `audit-enabled`, `audit-store`

## Build System

### Gradle Modules
```
settings.gradle.kts          → Multi-project config
build-logic/build.gradle.kts → Shared plugin setup
app/build.gradle.kts         → Main app (depends on utilities)
utilities/build.gradle.kts   → Filter infrastructure
list/build.gradle.kts        → Utility collections
```

### Key gradle tasks
```bash
./gradlew :app:run           # Run locally (port 8080)
./gradlew test               # All tests
./gradlew :app:installDist   # Build distribution ZIP
./gradlew :app:flywayInfo    # DB migration status
```

## Directory Structure

```
app/src/main/java/org.example.gateway/
├── GatewayApp                     # Entry point, Javalin.create()
├── api/
│   ├── AdminController            # Route CRUD API
│   └── HealthController           # Health probes
├── audit/
│   └── AuditGatewayFilter         # Audit recording
├── config/
│   ├── GatewayConfig              # Configuration model
│   └── YamlConfigLoader           # YAML parser
├── db/
│   ├── DatabaseManager            # Connection pool
│   ├── RouteDao                   # Persistence for routes
│   ├── AuditDao                   # Persistence for audit logs
│   └── Flyway integration
├── proxy/
│   ├── ProxyHandler               # Main request handler
│   └── WebSocketProxyHandler      # WebSocket handler
└── registry/
    ├── RouteRegistry              # In-memory route index
    └── RouteLoader                # Config source abstraction

utilities/src/main/java/org.example.utilities.gateway/
├── cache/
│   ├── CacheStore, CaffeineCache
│   └── CacheGatewayFilter
├── circuitbreaker/
│   ├── CircuitBreakerFactory
│   └── CircuitBreakerGatewayFilter
├── exception/
│   └── GatewayException hierarchy
├── filter/
│   ├── GatewayFilter              # Base interface
│   ├── FilterChain                # Base interface
│   ├── FilterContext              # Request-scoped attributes
│   └── DefaultFilterChain         # Implementation
├── header/
│   ├── HeaderMutationFilter
│   └── DedupeResponseHeadersFilter
├── loadbalancer/
│   ├── LoadBalancer (sealed)
│   ├── RoundRobinLoadBalancer
│   ├── WeightedLoadBalancer
│   └── RandomLoadBalancer
├── model/
│   ├── RouteDefinition            # Route config (immutable)
│   ├── TargetDefinition           # Upstream target
│   └── Policy classes (RateLimitPolicy, etc.)
├── proxy/
│   ├── OkHttpUpstreamClient
│   ├── WebSocketForwarder
│   └── ProxyFilter                # Terminal filter
├── ratelimit/
│   └── SlidingWindowRateLimiter
├── routing/
│   ├── RouteMatcher (sealed)
│   ├── PathRouteMatcher
│   ├── RegexRouteMatcher
│   ├── HeaderRouteMatcher
│   ├── TrafficSplitMatcher
│   └── RoutingContext
├── tracing/
│   └── TracingFilter
└── transform/
    ├── RequestTransformer
    ├── ResponseTransformer
    └── TransformGatewayFilter
```

## Testing Stack

| Framework | Purpose | Usage |
|---|---|---|
| **JUnit 5** | Test framework | All tests |
| **AssertJ** | Fluent assertions | Readable assertions |
| **Mockito** | Mocking | External dependencies |
| **JavalinTest** | Integration testing | HTTP endpoint tests |
| **OkHttp MockServer** | Mock upstream | End-to-end tests |
| **H2 Database** | In-memory DB | Database tests |

## Performance Profile

| Metric | Baseline | Notes |
|---|---|---|
| **Startup time** | ~2-3 seconds | Jetty + route building |
| **Per-request overhead** | 2-5ms (p50) | Filter chain execution |
| **Memory footprint** | ~200MB base | JVM minimum + Jetty |
| **Per-route memory** | ~500-800KB | Route config + filters |
| **GC pause** | <50ms | Generational ZGC recommended |
| **Max routes** | ~5000 | JVM heap constraint (512MB) |
| **Max RPS/replica** | 10k+ | Depends on transformation logic |

## Key Architectural Constraints

1. **Stateless**: Route state lives in database, not in-process memory (multi-replica safe)
2. **Lock-free at runtime**: Filter chains immutable after build; AtomicInteger for round-robin
3. **No blocking I/O in filters**: Must be async or thread-pool based (OkHttp handles this)
4. **Filter isolation**: Each filter independent, composable, testable
5. **Javalin v7 requirement**: All routes MUST be inside `config.routes` (no dynamic route addition after `.start()`)

## Dependencies: Import Paths

**From app layer** (stateful):
- `org.example.gateway.*` (GatewayApp, AdminController, etc.)

**From utilities layer** (reusable):
- `org.example.utilities.gateway.*` (filters, models, routers, etc.)

**External**:
- `Javalin.create()` — Web framework config
- `Resilience4j` — Rate limiting, circuit breaking
- `Caffeine` — Caching
- `OkHttp` — HTTP client
- `Jackson` — JSON serialization

## Deployment Profile

| Environment | Config | Notes |
|---|---|---|
| **Local Dev** | H2 in-memory, YAML config | Single instance, one-time refresh |
| **Staging** | PostgreSQL, Database config | Multi-instance setup |
| **Production** | PostgreSQL, Database config + Kubernetes | Replicas share route state |

**Kubernetes deployment**:
- Liveness probe: `GET /gateway/health/live` (always 200)
- Readiness probe: `GET /gateway/health/ready` (200 when routes loaded)
- No sticky sessions (stateless)

## Security Considerations

- **No built-in auth**: Use external auth gateway or reverse proxy
- **Auth header forwarding**: Configured per-route, bypasses exclude rules
- **SQL injection**: Prepared statements via Flyway + JDBC drivers
- **XSS**: Admin API requests must validate JSON
- **CSRF**: Stateless API, could add CSRF token if needed

