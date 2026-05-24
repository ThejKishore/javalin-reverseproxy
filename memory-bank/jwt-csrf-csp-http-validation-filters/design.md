# Design: JWT, CSRF, CSP, HTTPValidation Filters

## Architecture Placement
Security filters are integrated in `RouteRegistry.buildChain()` in this order:
1. `JwtAuthFilter` (optional, per-route)
2. `CsrfGatewayFilter` (optional, per-route)
3. Existing tracing/resilience/filter stack
4. `HttpValidationFilter` (optional, per-route)
5. Existing header/transform filters
6. `CspGatewayFilter` (optional, per-route)
7. `ProxyFilter` terminal

## Component Design
- `JwtAuthFilter`: validates Bearer tokens using configured policy.
- `CsrfGatewayFilter`: issues/validates tokens; enforces on unsafe methods only.
- `CspGatewayFilter`: injects CSP response headers based on route policy.
- `HttpValidationFilter`: matches request values against loaded blacklist regex rules.

## Data/Storage
- CSRF token backend supports distributed storage via Hazelcast implementation.
- HTTP validation regex rules are loaded from database and can be reloaded at runtime.

## Admin Integration
Under `/gateway/admin/security/rules`:
- list rules
- create rule
- enable/disable rule
- delete rule
- reload in-memory rules

## Non-Functional Notes
- Filters are stateless per instance; request state stays in `FilterContext`.
- Configuration is route-scoped and reload-safe with existing registry model.

