# Active Context (Current Session)

## Last Updated
May 24, 2026 — Security filter implementation update

## Project Status
✅ **Production deployed** — Fully operational with comprehensive test coverage
✅ **Stable architecture** — Filter chain pattern proven, multi-module design working
✅ **Feature complete** — All roadmap items from initial scope delivered

## What We Know Right Now
- Two active modules: `app` (core), `utilities` (reusable)
- Filter chain pattern is the cornerstone of all extensibility
- Database-backed routes enable true multi-replica deployments
- Admin API supports full CRUD + enable/disable operations
- All routes have optional per-route audit logging
- WebSocket proxying fully bidirectional
- Tracing headers propagated correctly across hops
- Security filters are now available per route: JWT, CSRF, CSP, HTTP blacklist validation
- Validation regex rules are managed via `/gateway/admin/security/rules/**` APIs
- CSRF tokens are backed by Hazelcast storage in app runtime

## Current Development Phase
Feature development mode (ready for PHASE 3 implementations)

## Common Pitfalls to Avoid

### 1. Filter Idempotency Misconception
**Trap**: Assuming filters are called once per request.  
**Reality**: Some filters may be called multiple times (retries, error handling).  
**Fix**: Use `FilterContext` to store state; never assume side effects happen once.

### 2. Shared State in Filters
**Trap**: Storing request context in filter instance fields.  
**Reality**: Filter instances are singletons, reused across requests.  
**Fix**: Always use `FilterContext.getOrSet()` for request-scoped attributes.

### 3. Blocking I/O in Filter Chain
**Trap**: Adding synchronous database calls in a filter.  
**Reality**: Blocks the Jetty thread, reduces throughput.  
**Fix**: Use `ctx.future()` for async operations, or ensure I/O is already parallelized (OkHttp is).

### 4. Database Transaction Scope
**Trap**: Opening transaction in filter, closing in another.  
**Reality**: Transactions span single filter execution; rollback not guaranteed across filters.  
**Fix**: Use `@Transactional` methods or explicitly manage transaction boundaries per filter.

### 5. Route ID Uniqueness
**Trap**: Admin API allows duplicate route IDs in database.  
**Reality**: RouteRegistry will break with conflicting IDs.  
**Fix**: Add unique constraint in Flyway; validate in AdminController before insert.

### 6. Configuration Reload Race
**Trap**: Route reload happens while request is executing.  
**Reality**: Immutable filter chains mean in-flight requests use old chains; new requests use new chains.  
**Fix**: This is actually safe! But know that requests use the configuration snapshot at entry point.

### 7. Forgetting Audit Filter Registration
**Trap**: Adding resilience policy but forgetting to register AuditGatewayFilter.  
**Reality**: Audit logs won't include that policy's effect.  
**Fix**: Always check `RouteDefinition.buildFilterChain()` — AuditFilter should be near the end.

## Key Code Locations (Refer Here First)

| Concern | Location | File |
|---|---|---|
| **Main entry** | GatewayApp | `app/src/main/java/.../GatewayApp.java` |
| **Route matching** | RouteRegistry.match() | `app/src/main/java/.../registry/RouteRegistry.java` |
| **Filter composition** | RouteDefinition.buildFilterChain() | `utilities/src/main/java/.../model/RouteDefinition.java` |
| **Admin CRUD** | AdminController | `app/src/main/java/.../api/AdminController.java` |
| **Database layer** | RouteDao, AuditDao | `app/src/main/java/.../db/` |
| **Filter interfaces** | GatewayFilter, FilterChain | `utilities/src/main/java/.../filter/` |
| **Routing strategies** | RouteMatcher (sealed) | `utilities/src/main/java/.../routing/` |
| **Resilience** | CircuitBreakerGatewayFilter, SlidingWindowRateLimiter | `utilities/src/main/java/.../circuitbreaker/`, `.../ratelimit/` |
| **Caching** | CacheGatewayFilter | `utilities/src/main/java/.../cache/` |
| **Audit** | AuditGatewayFilter | `app/src/main/java/.../audit/` |
| **Tracing** | TracingFilter | `utilities/src/main/java/.../tracing/` |
| **Security filters** | JwtAuthFilter, CsrfGatewayFilter, HttpValidationFilter, CspGatewayFilter | `utilities/src/main/java/.../security/` |
| **Security rule APIs** | AdminController security endpoints | `app/src/main/java/.../api/AdminController.java` |

## Common Next Steps

### To Add a New Filter
1. Create class in `utilities/src/main/java/.../filter/`
2. Implement `GatewayFilter` interface
3. Register in `RouteDefinition.buildFilterChain()` at appropriate index
4. Write unit test for filter behavior with `DefaultFilterChain`
5. Add integration test in `AdminControllerTest` or new route

### To Add a New Routing Type
1. Create sealed subclass of `RouteMatcher` in `.../routing/`
2. Implement `matches(RoutingContext)` method
3. Add to `RoutingType` enum (if creating new YAML type)
4. Update YAML parser in `YamlConfigLoader`
5. Write tests: unit test for matching logic + integration test

### To Add a New Resilience Policy
1. Create policy class in `utilities/src/main/java/.../model/`
2. Create filter class in `utilities/src/main/java/.../` (mirror resilience4j pattern)
3. Register policy in `RouteDefinition`
4. Update YAML parser to deserialize policy config
5. Add to filter chain in `buildFilterChain()`

## Performance Reminders
- Filter chain is built once per route at startup; no overhead at request time
- Round-robin load balancing uses lock-free `AtomicInteger`
- Caching only applies to GET requests (safe by design)
- Audit logging is always enabled for SLF4J; database writes are optional

## Testing Reminders
- Use `JavalinTest.test()` for HTTP integration tests
- Mock upstream with `MockWebServer` from OkHttp
- Test filters in isolation via `DefaultFilterChain` constructor
- Database tests use H2 in-memory by default
- All migrations must be idempotent (Flyway best practice)

## Next Feature Entry Points
When starting a new feature, check these files first:
1. `projectbrief.md` — Confirm scope alignment
2. `systemPatterns.md` — Understand where your change fits
3. `techContext.md` — Review file locations & schema
4. This file (`activeContext.md`) — Avoid known pitfalls

