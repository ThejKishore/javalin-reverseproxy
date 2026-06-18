# Product Context

## Business Goals
1. **Centralize microservices routing** — Single entry point for all service-to-service traffic
2. **Reduce operational burden** — Replace complex Spring Cloud Gateway setups with simpler, lighter alternative
3. **Enable dynamic route management** — Add/remove services without restarting gateway instances
4. **Support high-throughput scenarios** — Minimize latency overhead, scale horizontally
5. **Provide full observability** — Audit trails, tracing, structured logging for compliance & debugging
6. **Support multi-tenancy** — Per-route policies, traffic splitting, canary deployments

## User Stories (from Platform Team POV)

### Platform Engineer
**As** a platform engineer,  
**I want** to add/remove/update routes without restarting the gateway,  
**So that** service deployments don't cause gateway downtime.

**Acceptance**: Admin API supports CRUD operations; changes apply immediately.

### Developer
**As** a developer,  
**I want** to understand request routing behavior (where is my traffic going?),  
**So that** I can debug issues and understand latency.

**Acceptance**: Request IDs appear in logs; audit logs show route matches & upstream URLs.

### DevOps/SRE
**As** a DevOps engineer,  
**I want** Kubernetes liveness/readiness probes,  
**So that** the orchestrator can auto-heal and drain unhealthy instances.

**Acceptance**: `/gateway/health/live` always returns 200; `/gateway/health/ready` returns 503 if routes not loaded.

### Service Owner
**As** a service owner,  
**I want** per-route rate limiting & circuit breaking,  
**So that** my service doesn't get overwhelmed by traffic spikes.

**Acceptance**: Rate limit: HTTP 429; Circuit breaker: HTTP 503.

## Target Users
- Mid-to-large organizations running microservices
- Teams operating Kubernetes
- Companies needing dynamic service mesh without adding Istio complexity
- Organizations requiring full audit trails for compliance (SOC 2, HIPAA, etc.)

## Market Positioning
- **vs Spring Cloud Gateway**: Lighter, simpler, no Spring dependency
- **vs Istio**: No sidecar complexity, centralized gateway model
- **vs Kong**: Java-native, seamless Javalin extension, lower operational overhead

## Key Success Criteria
✅ Production deployed and stable  
✅ Key routes (at least 20) configured and running  
✅ Sub-10ms gateway overhead  
✅ Complete audit trail functionality  
✅ Support for horizontal scaling (multi-replica + PostgreSQL)  

## Known Limitations (by design)
- No HTTP/1.0 support (Jetty 12 baseline is HTTP/1.1)
- Caching only for GET requests (no state mutation safety risk)
- No built-in OAuth/OIDC (use external auth gateway)
- Max ~5000 routes per instance (JVM heap constraint)
- No plugin system yet (but filter architecture allows future extensibility)

## Future Roadmap
- [ ] Async filter processing (virtual threads, Project Loom)
- [ ] GraphQL gateway extensions
- [ ] OpenTelemetry integration (beyond basic X-Request-ID)
- [ ] Policy-based rate limiting (by user ID, IP ranges, custom attributes)
- [ ] Plugin system for custom transformers

