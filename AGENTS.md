# AGENTS.md

## Project Snapshot
- Java 21 + Gradle multi-module gateway on Javalin 7 (`settings.gradle.kts`, `app/build.gradle.kts`, `utilities/build.gradle.kts`).
- Main entrypoint is `app/src/main/java/org/example/gateway/GatewayApp.java`; all routes are registered inside `Javalin.create(config -> { ... })`.
- Start context first from memory bank: `memory-bank/projectbrief.md`, `memory-bank/systemPatterns.md`, `memory-bank/techContext.md`, `memory-bank/activeContext.md`, `memory-bank/copilot-rules.md`.

## Architecture You Must Preserve
- Request flow: Javalin handler -> `RouteRegistry.match()` -> prebuilt `DefaultFilterChain` -> terminal `ProxyFilter` -> upstream.
- Filter order matters and is encoded in `RouteRegistry.buildChain(...)` (JWT/CSRF -> tracing -> resilience -> validation/header/transform -> CSP -> proxy).
- `RouteRegistry` keeps `allRoutes` and active `routes` separately; admin APIs use all routes, proxy matching uses enabled routes only.
- `DefaultFilterChain` is immutable/index-based (`utilities/.../filter/DefaultFilterChain.java`); do not add shared mutable request state to filters.
- Route config format is kebab-case JSON/YAML via `YamlConfigLoader.jsonMapper()`; preserve key names like `path-pattern`, `load-balancer-type`.

## Module Boundaries & Ownership
- `app/`: bootstrap, controllers, DB/storage adapters, registry wiring.
- `utilities/`: reusable gateway primitives (filters, routing, load balancing, policies, proxy client).
- `shared/`: DTO/DAO mapping and diff helpers used by storage/admin flows.
- Keep dependency direction one-way: `app` depends on `utilities`/`shared`; avoid importing `app` classes into `utilities`.

## Storage + Integration Points
- Route source is selected by `gateway.config-source` (`yaml`, `database`, `eclipse-store-lcl`, `eclipse-store-azure`) in `application.yml`.
- `RouteStorageProviderFactory` and `RouteLoader` are the seam for adding/changing storage backends.
- Validation blacklist rules are DB-backed and hot-reloaded (`/gateway/admin/security/rules/**`, `ValidationRuleStore`).
- CSRF token storage is Hazelcast-backed when enabled (`HazelcastCsrfTokenStore`); code must still work when Hazelcast is disabled.
- Audit and change logs go through storage providers; do not bypass provider interfaces in controllers.

## Developer Workflow (Known Working Commands)
- Run app: `./gradlew :app:run`
- Full tests: `./gradlew test`
- Module tests: `./gradlew :utilities:test` and `./gradlew :app:test`
- Build distro: `./gradlew :app:installDist`
- Flyway checks: `./gradlew :app:flywayInfo` / `./gradlew :app:flywayValidate`

## Testing & Debugging Conventions
- Integration HTTP tests use `JavalinTest` (see `app/src/test/java/org/example/gateway/api/AdminControllerTest.java`).
- Upstream behavior is simulated with `MockWebServer` (see `app/src/test/java/org/example/gateway/proxy/ProxyHandlerIntegrationTest.java`).
- Keep API payload assertions in kebab-case JSON (example: `"path-pattern"` in `AdminControllerTest`).
- Runtime logs: `app/logs/gateway.log` and `app/logs/audit.log`; health endpoints are `/gateway/health/live` and `/gateway/health/ready`.

