# Admin Dashboard UI — Implementation Plan

## Steps

1. **Create `app/src/main/resources/gateway-ui/index.html`**
   - Single HTML file, Vue 3 + PrimeVue 4 from CDN (no build tool)
   - Dark Aura theme via `@primeuix/themes` UMD + custom fallback CSS
   - Routes **DataTable** with sort, pagination, search filter
   - **Toolbar**: New Route button, Reload Gateway button, search input
   - **Stats bar**: Total / Active / Disabled / Health counters
   - **Dialog** with 4 tabs: Basic | Targets | Policies | Headers
   - All CRUD via `fetch()` against existing REST endpoints

2. **Wire Javalin route in `GatewayApp.java`**
   - Inside `path("/gateway/admin", () -> { ... })` add `get("/ui", ...)`
   - Handler reads `/gateway-ui/index.html` from classpath and returns HTML

3. **Write clean `ui-dashboard.md` documentation**
   - CDN dependency table
   - REST API → UI action mapping
   - JSON field reference (kebab-case keys)
   - Form tab breakdown

## API Endpoints Used

| UI Action        | Method | URL                                    |
|-----------------|--------|----------------------------------------|
| List routes     | GET    | /gateway/admin/routes                  |
| Create route    | POST   | /gateway/admin/routes                  |
| Update route    | PUT    | /gateway/admin/routes/{id}             |
| Delete route    | DELETE | /gateway/admin/routes/{id}             |
| Enable route    | PATCH  | /gateway/admin/routes/{id}/enable      |
| Disable route   | PATCH  | /gateway/admin/routes/{id}/disable     |
| Reload gateway  | POST   | /gateway/admin/reload                  |
| Health check    | GET    | /gateway/health                        |

## Form Tabs

| Tab      | Fields                                                                          |
|----------|---------------------------------------------------------------------------------|
| Basic    | name, path-pattern, routing-type, strip-prefix, enabled, timeout-ms, lb-type, header-match-name/value |
| Targets  | dynamic list of `{ url, weight }` rows                                          |
| Policies | rate-limit-policy, circuit-breaker-policy, cache-policy, audit toggle           |
| Headers  | add-request/response maps, exclude-request/response lists, auth-forward-headers |

