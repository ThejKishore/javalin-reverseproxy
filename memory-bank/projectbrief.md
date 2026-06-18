# Javalin Gateway — Project Brief

## Project Identity
- **Name**: Javalin Gateway
- **Type**: Production-ready API reverse proxy
- **Language**: Java 21
- **Framework**: Javalin 7.1.0 (Jetty 12)
- **License**: Apache 2.0
- **Repository**: `/Users/thejkaruneegar/IdeaProjects/javalin-gateway`

## Core Mission
Build a lightweight, fully-featured reverse proxy that routes HTTP/WebSocket traffic to upstream services with resilience, observability, and dynamic configuration capabilities.

## Key Value Propositions
1. **Zero-downtime route management** — Add/update/delete routes without restart
2. **Per-route resilience** — Independent rate limiting, circuit breaking, caching per route
3. **Kubernetes-native** — Health probes, stateless design, multi-replica support via PostgreSQL
4. **Observable** — Full request/response audit trail, tracing headers, structured logging
5. **Lightweight alternative** — Spring Cloud Gateway alternative with simpler operational model

## Key Stakeholders
- **Developers**: Building & extending the gateway, adding new filters/matchers
- **Platform Teams**: Configuring routes, managing policies
- **DevOps/SRE**: Deploying in production (Kubernetes), monitoring health
- **Service Owners**: Understanding proxy behavior, debugging via audit logs

## Project Scope
✅ Reverse proxy core (multi HTTP verb support)  
✅ Multiple routing strategies (path, regex, header, traffic-split)  
✅ Per-route resilience (rate limiting, circuit breaker, caching)  
✅ Dynamic route management via Admin REST API  
✅ Request/response transformation hooks  
✅ Full audit logging (database + SLF4J)  
✅ WebSocket proxying (bidirectional)  
✅ Tracing/forwarded headers (X-Request-ID, X-Trace-ID, X-Forwarded-For)  
✅ Database-backed persistence (PostgreSQL/H2)  
✅ Web UI for route management  

## Success Metrics
- Gateway startup: <5 seconds
- Per-request overhead: <10ms (p50)
- Support 100+ routes in memory
- Handle 10k+ RPS per replica
- Zero data loss on graceful shutdown
- Full audit trail for compliance

## Active Constraints
- Java 21+ only (no legacy version support)
- Stateless design (route state in database)
- No blocking I/O in filter execution path
- Filter isolation & composability

