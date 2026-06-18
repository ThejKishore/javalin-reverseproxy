# Tasks: JWT, CSRF, CSP, HTTPValidation Filters

## Completed
- [x] Add security policies to route configuration model.
- [x] Implement `JwtAuthFilter` and route-level registration.
- [x] Implement `CsrfGatewayFilter` with token validation and rotation.
- [x] Implement `CspGatewayFilter` for response header policy.
- [x] Implement `HttpValidationFilter` with pluggable rule provider.
- [x] Add Hazelcast-backed CSRF token store wiring in app bootstrap.
- [x] Add validation rule persistence + in-memory store + reload flow.
- [x] Add admin endpoints for validation rule management.
- [x] Add/adjust tests for all four filters.
- [x] Fix post-merge test regressions in YAML/CSP/HTTP validation test cases.

## Follow-Ups
- [ ] Add performance/load tests for validation-heavy routes.
- [ ] Add optional observability metrics per security filter.
- [ ] Add docs examples for production Hazelcast Kubernetes discovery.

