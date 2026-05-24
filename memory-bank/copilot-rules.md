# Copilot Assistance Rules

## Before Starting Any Task
✅ **Always read memory bank files in this order**:
1. `projectbrief.md` — Understand mission & scope
2. `systemPatterns.md` — Grasp architectural patterns
3. `techContext.md` — Review file locations & stack
4. `activeContext.md` — Learn common pitfalls
5. Feature-specific context (if working on `/memory-bank/<feature>/`)

✅ **Never skip this prep phase** — It prevents major rework later.

---

## When Adding a New Feature

### Phase 0: PRD Intake (Use Kiro-Lite)
```
/start feature <name>
```
- Clarify scope with user
- Document acceptance criteria
- Estimate effort
- Identify dependencies

### Phase 1: Design (Architectural Thinking)
- Where does this fit in the filter chain?
- Does it need a new sealed class (RouteMatcher, LoadBalancer, etc.)?
- Will it need persistence (new Flyway migration)?
- Does it touch tracing or audit logging?

### Phase 2: Task Breakdown
- Break into atomic, testable units
- Each task should be implementable in <1 hour
- Identify test requirements first

### Phase 3: Implementation
- Write tests first (arrange-act-assert)
- Reference existing patterns in codebase
- One task at a time (use `/implement <TASK_ID>`)
- Show diffs for review before moving on

---

## Code Style & Best Practices

### Java 21 Features (Expected & Encouraged)
```java
// ✅ Use records for immutable data
public record RouteDefinition(String id, String name, List<TargetDefinition> targets) {}

// ✅ Use sealed classes for restricted hierarchies
public sealed interface RouteMatcher permits PathRouteMatcher, RegexRouteMatcher { }

// ✅ Use text blocks for multi-line strings
String sqlQuery = """
    SELECT id, name, config_json
    FROM routes
    WHERE enabled = true
    ORDER BY name
    """;

// ✅ Use switch expressions
String result = switch (routeType) {
    case PATH -> "path-prefix matching";
    case REGEX -> "regex matching";
    case HEADER -> "header-value matching";
    case TRAFFIC_SPLIT -> "weighted distribution";
};

// ✅ Use pattern matching with instanceof
if (matcher instanceof HeaderRouteMatcher hm && hm.value() != null) {
    // Use hm directly
}
```

### Filter Implementation Pattern
```java
public class YourNewFilter implements GatewayFilter {
    @Override
    public void apply(FilterContext ctx, FilterChain chain) throws Exception {
        // 1. Pre-processing: inspect/modify request
        String originalPath = ctx.request().getPath();
        
        // 2. Proceed to next filter
        try {
            chain.next(ctx);
        } catch (Exception e) {
            // 3. Error handling: optional
            ctx.response().setStatus(500);
            throw e;
        }
        
        // 4. Post-processing: inspect/modify response (optional)
        // Must complete synchronously or use async pattern
    }
}
```

### Javalin Route Handler Pattern (Javalin 7)
**Remember**: Routes MUST be defined inside `Javalin.create(config -> { ... })`.
```java
// ✅ Correct (Javalin 7)
var app = Javalin.create(config -> {
    config.routes.get("/gateway/admin/routes", new AdminController()::listRoutes);
    config.routes.post("/gateway/admin/routes", new AdminController()::createRoute);
}).start(8080);

// ❌ Wrong (routes added after create/start)
var app = Javalin.create().start(8080);
app.get("/path", handler);  // FAILS in Javalin 7
```

### Testing Pattern
```java
// Unit test: Filter in isolation
@Test
void testFilterBehavior() {
    FilterContext ctx = mock(FilterContext.class);
    FilterChain chain = mock(FilterChain.class);
    
    YourNewFilter filter = new YourNewFilter();
    filter.apply(ctx, chain);
    
    assertThat(ctx.response().getStatus()).isEqualTo(expectedStatus);
    verify(chain).next(ctx);
}

// Integration test: With JavalinTest
@Test
void testEndToEnd() {
    var app = Javalin.create(config -> {
        config.routes.get("/test", ctx -> ctx.result("OK"));
    });
    
    JavalinTest.test(app, (server, client) -> {
        assertThat(client.get("/test").code()).isEqualTo(200);
    });
}

// Database test: With H2 in-memory
@Test
void testDatabaseAccess() {
    try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:test")) {
        // Execute test
    }
}
```

### Documentation & JavaDoc
```java
/**
 * Applies this filter to the given request/response context.
 *
 * <p>Filters execute in order and can short-circuit by throwing an exception.
 * Each filter is responsible for calling {@link FilterChain#next(FilterContext)}
 * to proceed to the next filter.
 *
 * @param ctx the filter context containing request/response
 * @param chain the filter chain to proceed to the next filter
 * @throws Exception if processing fails (short-circuits remaining filters)
 */
@Override
public void apply(FilterContext ctx, FilterChain chain) throws Exception {
    // Implementation
}
```

---

## Database Changes

### Creating a Migration
1. **File naming**: `V{number}__{description}.sql` (e.g., `V003__add_audit_table.sql`)
2. **Location**: `app/src/main/resources/db/migration/`
3. **Idempotency**: Every migration must be safe to run multiple times
4. **No data loss**: Always add columns as NULLABLE or with DEFAULT

### Example Migration
```sql
-- ✅ Correct: Idempotent, safe
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    route_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

-- ❌ Wrong: Will fail on second run
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    route_at TIMESTAMP DEFAULT NOW()
);

-- ❌ Wrong: Drops data
ALTER TABLE audit_logs DROP COLUMN created_at;
```

### Verify Migrations
```bash
./gradlew :app:flywayInfo        # List migrations
./gradlew :app:flywayValidate    # Check for errors
./gradlew :app:flywayRepair      # Fix checksums (if corrupted)
```

---

## Configuration Changes

### Updating application.yml
```yaml
# ✅ Correct: Nested under gateway key
gateway:
  port: 8080
  new-feature:
    enabled: true
    timeout-ms: 5000

# ❌ Wrong: Top-level without namespace
new-feature:
  enabled: true
```

### Updating Route Configuration
1. Add field to `RouteDefinition` record
2. Add field to YAML parser in `YamlConfigLoader`
3. Add field to `GatewayConfig` model (if it's route-level config)
4. Update Admin API DTO in `AdminController`
5. Write tests: YAML parsing + field validation

---

## Testing Checklist

Before submitting any code, verify:
- [ ] Unit tests pass: `./gradlew :utilities:test`
- [ ] App tests pass: `./gradlew :app:test`
- [ ] All tests pass: `./gradlew test`
- [ ] No warnings: `./gradlew build`
- [ ] Code compiles: `./gradlew :app:compileJava`
- [ ] Integration tests cover happy + error paths
- [ ] Database migrations are idempotent (test with H2)
- [ ] JavaDoc is complete for public APIs
- [ ] Filter chains tested with `DefaultFilterChain`
- [ ] Audit logging verified (if applicable)

---

## Review Checklist

### Code Review Checklist
- [ ] Tests pass locally before requesting review
- [ ] No warnings or deprecation (other than intentional)
- [ ] JavaDoc is clear & complete
- [ ] Memory bank updated (if architecture changed)
- [ ] Database migrations idempotent (if DB schema changed)
- [ ] Filter positioning correct in chain (order matters!)
- [ ] No blocking I/O in filter execution
- [ ] FilterContext used for request-scoped state
- [ ] No shared state in filter instance fields
- [ ] Error handling includes appropriate HTTP status code

### PR Description Should Include
- **What**: Brief description of change
- **Why**: Business reason or problem solved
- **How**: Technical approach (reference patterns)
- **Test Coverage**: What tests were added/modified
- **Impact**: Performance, breaking changes, etc.

---

## Common Implementation Patterns

### Adding a New Routable Type
1. Create sealed subclass of `RouteMatcher` 
2. Add to `RoutingType` enum
3. Update `YamlConfigLoader` to deserialize 
4. Add integration test in `AdminControllerTest`

### Adding a New Resilience Policy
1. Create policy class (extends/implements resilience4j concept)
2. Create filter class implementing `GatewayFilter`
3. Register in `RouteDefinition.buildFilterChain()`
4. Add to YAML parser
5. Test with mock upstream (MockWebServer)

### Adding a New Filter
1. Implement `GatewayFilter`
2. Register in `RouteDefinition.buildFilterChain()` at correct position
3. Test with `DefaultFilterChain` directly
4. Test end-to-end with `JavalinTest`
5. Document filter ordering impact

### Adding a New Admin API Endpoint
1. Add handler method to `AdminController`
2. Register route in `GatewayApp.main()`
3. Write integration test using `JavalinTest`
4. Document in README (Admin API section)

---

## Debugging Tips

### Memory Bank Not Found
**Check**: Is `/memory-bank/` directory in workspace root?  
**Fix**: Use absolute path `/Users/thejkaruneegar/IdeaProjects/javalin-gateway/memory-bank/`

### Filter Chain Order Wrong
**Check**: Print filter chain in `RouteDefinition.buildFilterChain()`  
**Fix**: Audit should be near end; Tracing should be near start; Caching before Proxy

### Tests Failing Randomly
**Check**: Filter state leaking between tests?  
**Fix**: Reset mocks; verify `DefaultFilterChain` creates fresh instances

### Routes Not Loading
**Check**: Is Flyway migration running? Does config parse?  
**Fix**: Check `logs/gateway.log`; verify YAML syntax

### Audit Logs Not Recording
**Check**: Is `AuditGatewayFilter` in the filter chain?  
**Fix**: Check `RouteDefinition.buildFilterChain()` includes audit filter

---

## Questions? Refer To:
- **Architecture questions** → `systemPatterns.md`
- **Code location questions** → `techContext.md`
- **Known pitfalls** → `activeContext.md`
- **Progress questions** → `progress.md`
- **Feature scope** → `projectbrief.md`

