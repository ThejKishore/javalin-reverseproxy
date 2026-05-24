# Context: JWT, CSRF, CSP, HTTPValidation Filters

## Entry Points
- Chain assembly: `app/src/main/java/org/example/gateway/registry/RouteRegistry.java`
- App wiring: `app/src/main/java/org/example/gateway/GatewayApp.java`
- Admin APIs: `app/src/main/java/org/example/gateway/api/AdminController.java`
- Filter implementations: `utilities/src/main/java/org/example/utilities/gateway/security/`

## Key Runtime Behavior
- JWT runs first to fail unauthorized traffic early.
- CSRF applies to unsafe methods (`POST`, `PUT`, `DELETE`, `PATCH`).
- HTTP validation checks inbound content for blacklist regex matches.
- CSP injects response header policy before response is returned.

## Testing Context
- Security filter tests live under `app/src/test/java/org/example/gateway/security/`.
- Full suite currently passes after URI encoding and assertion fixes in tests.

## Pitfalls Learned
- Missing headers in OkHttp test responses are `null` (not empty string).
- Unsafe query examples in tests must be URL-encoded to avoid URI syntax errors.
- Config defaults should be tested against supported values, not a single static source.

