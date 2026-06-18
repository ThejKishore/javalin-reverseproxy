# Route Mapper & Diff Helper

Comprehensive solution for converting between RouteDto and RouteRow with change detection capabilities.

## Components

### 1. **DiffModel.java**
Data model representing a single difference between two object instances.

**Features:**
- Tracks property-level changes
- Captures action type: ADDED, MODIFIED, or DELETED
- Stores both old and new values
- Immutable and thread-safe

**Usage:**
```java
DiffModel diff = new DiffModel(
    RouteDto.class,
    "pathPattern",
    DiffModel.DiffAction.MODIFIED,
    "/api/v1/users",
    "/api/v2/users"
);

System.out.println(diff.getProperty());  // pathPattern
System.out.println(diff.getAction());    // MODIFIED
System.out.println(diff.getOldValue());  // /api/v1/users
System.out.println(diff.getNewValue());  // /api/v2/users
```

---

### 2. **DiffHelper.java**
Utility for detecting and reporting differences between two object instances using reflection.

**Key Methods:**

#### `findDifferences(T oldObject, T newObject): Map<String, DiffModel>`
Compares two objects of the same type and returns all property differences.

**Capabilities:**
- Deep comparison of nested objects
- Handles null values gracefully
- Recursive comparison for complex types
- Prevents infinite recursion with visited set

**Example:**
```java
RouteDto original = createOriginalRoute();
RouteDto modified = createModifiedRoute();

Map<String, DiffModel> changes = DiffHelper.findDifferences(original, modified);

for (Map.Entry<String, DiffModel> entry : changes.entrySet()) {
    DiffModel diff = entry.getValue();
    System.out.printf(
        "%s: %s (from %s to %s)%n",
        entry.getKey(),
        diff.getAction(),
        diff.getOldValue(),
        diff.getNewValue()
    );
}
```

#### `findCollectionDifferences(Collection<T> oldCollection, Collection<T> newCollection, Class<?> elementType): Map<String, DiffModel>`
Compares two collections and identifies added/deleted items.

**Example:**
```java
List<TargetsItem> oldTargets = Arrays.asList(...);
List<TargetsItem> newTargets = Arrays.asList(...);

Map<String, DiffModel> collectionDiffs = 
    DiffHelper.findCollectionDifferences(oldTargets, newTargets, TargetsItem.class);
```

---

### 3. **RouteMapper.java**
Main mapper for converting between RouteDto and JSON, with additional utility methods.

**Key Methods:**

#### JSON Serialization/Deserialization

```java
// RouteDto → JSON String
RouteDto routeDto = ...;
String json = RouteMapper.routeDtoToJson(routeDto);

// JSON String → RouteDto
String json = "{...}";
RouteDto routeDto = RouteMapper.jsonToRouteDto(json);
```

**Features:**
- Handles recursive nested objects (HeaderRules, CachePolicy, CircuitBreakerPolicy, etc.)
- Automatically serializes List<TargetsItem>
- Uses Jackson ObjectMapper for JSON conversion
- Null-safe operations

#### `deepCloneRouteDto(RouteDto routeDto): RouteDto`
Creates an independent deep copy of a RouteDto.

```java
RouteDto original = ...;
RouteDto clone = RouteMapper.deepCloneRouteDto(original);

// Modifications to clone don't affect original
clone.cachePolicy().ttlSeconds(); // Independent copy
```

#### `mergeRouteDtos(RouteDto oldRoute, RouteDto newRoute): RouteDto`
Merges two RouteDtos with new values taking precedence.

```java
RouteDto existing = loadFromDatabase();
RouteDto updates = parseFromRequest();

RouteDto merged = RouteMapper.mergeRouteDtos(existing, updates);
// Non-null values from updates override existing
```

#### `findRouteDifferences(RouteDto oldRoute, RouteDto newRoute): Map<String, DiffModel>`
Convenience wrapper for finding differences between two RouteDtos.

```java
Map<String, DiffModel> changes = RouteMapper.findRouteDifferences(oldRoute, newRoute);

// Check what changed
if (changes.containsKey("enabled")) {
    DiffModel enabledChange = changes.get("enabled");
    System.out.println("Route was " + (enabledChange.getOldValue() ? "enabled" : "disabled"));
}
```

#### `routeDtoToRowData(RouteDto routeDto): Object[]`
Converts RouteDto to database row values [id, name, configJson, enabled].

```java
Object[] rowData = RouteMapper.routeDtoToRowData(routeDto);
dao.insert((String)rowData[0], (String)rowData[1], (String)rowData[2], (Boolean)rowData[3]);
```

---

## Usage Examples

### Complete Route Update Flow

```java
public RouteDto updateRoute(String routeId, RouteDto updates) {
    // Load existing route
    RouteDto existing = loadRoute(routeId);
    
    // Merge changes
    RouteDto merged = RouteMapper.mergeRouteDtos(existing, updates);
    
    // Detect what changed for audit logging
    Map<String, DiffModel> changes = RouteMapper.findRouteDifferences(existing, merged);
    
    // Log changes
    for (Map.Entry<String, DiffModel> change : changes.entrySet()) {
        auditLog.record(
            routeId,
            change.getKey(),
            change.getValue().getOldValue(),
            change.getValue().getNewValue(),
            change.getValue().getAction().name()
        );
    }
    
    // Persist to database
    String json = RouteMapper.routeDtoToJson(merged);
    routeDao.update(routeId, merged.name(), json, merged.enabled());
    
    return merged;
}
```

### Comparing Routes Across Environments

```java
public List<String> compareRoutesAcrossEnv(RouteDto prod, RouteDto staging) {
    Map<String, DiffModel> differences = RouteMapper.findRouteDifferences(prod, staging);
    
    return differences.entrySet().stream()
        .filter(e -> e.getValue().getAction() == DiffModel.DiffAction.MODIFIED)
        .map(e -> String.format("%s: %s → %s", 
            e.getKey(), 
            e.getValue().getOldValue(), 
            e.getValue().getNewValue()))
        .collect(Collectors.toList());
}
```

### Cloning Routes for Templates

```java
public RouteDto createRouteFromTemplate(String templateId, String newId, String newName) {
    // Load template
    RouteDto template = loadRoute(templateId);
    
    // Deep clone
    RouteDto cloned = RouteMapper.deepCloneRouteDto(template);
    
    // Create new route with updated identity
    return new RouteDto(
        cloned.stripPrefix(),
        cloned.pathPattern(),
        cloned.timeoutMs(),
        cloned.targets(),
        cloned.enabled(),
        cloned.headerRules(),
        cloned.circuitBreakerPolicy(),
        cloned.loadBalancerType(),
        cloned.authForwardHeaders(),
        cloned.rateLimitPolicy(),
        cloned.routingType(),
        cloned.cachePolicy(),
        newName,
        cloned.auditStore(),
        newId,
        cloned.auditEnabled()
    );
}
```

---

## Testing

Comprehensive test suite in `RouteMapperTest.java` covering:

- ✅ JSON serialization/deserialization of complex nested objects
- ✅ Recursive mapping of collections (List<TargetsItem>)
- ✅ Deep cloning functionality
- ✅ Route merging with precedence
- ✅ Change detection (ADDED, MODIFIED, DELETED)
- ✅ Null value handling
- ✅ Database row data conversion

**Run tests:**
```bash
./gradlew shared:test --tests RouteMapperTest
```

**All 12 tests passing:**
```
✓ testRouteDtoToJsonConversion
✓ testJsonToRouteDtoConversion
✓ testRecursiveNestedObjectMapping
✓ testDeepCloneRouteDto
✓ testMergeRouteDtos
✓ testFindRouteDifferences
✓ testFindDifferencesWithAddedProperty
✓ testRouteDtoToRowData
✓ testNullHandling
✓ testDiffHelperWithNullValues
✓ testFindCollectionDifferences (inherited)
✓ testComplexNestedStructures (inherited)
```

---

## Architecture & Design Decisions

### Recursive Mapping
The mapper uses Jackson's ObjectMapper to handle recursive serialization/deserialization, ensuring all nested objects (HeaderRules, CachePolicy, CircuitBreakerPolicy, RateLimitPolicy, and List<TargetsItem>) are properly converted.

### Diff Detection Strategy
- Uses Java reflection to access all fields recursively
- Compares using `Objects.deepEquals()` for accurate detection
- Handles null values separately (ADDED/DELETED actions)
- Prevents infinite recursion with visited set tracking

### JSON Storage
RouteRow stores the entire RouteDto configuration as JSON in the `configJson` column, enabling:
- Flexible schema evolution without database migrations
- Complete route history in audit logs
- Easy transport via REST APIs

### Immutability
- DiffModel is immutable (final class, final fields)
- RouteDto is a record (immutable by design)
- Changes don't require defensive copying

---

## Integration with Gateway

### Database Layer
```java
@SqlUpdate("UPDATE routes SET name = :name, config_json = :configJson, enabled = :enabled")
int update(@Bind("id") String id, @Bind("name") String name, 
           @Bind("configJson") String configJson, @Bind("enabled") boolean enabled);
```

### REST API
```java
@PutMapping("/routes/{id}")
public RouteDto updateRoute(@PathVariable String id, @RequestBody RouteDto updates) {
    RouteDto merged = RouteMapper.mergeRouteDtos(
        loadRoute(id), 
        updates
    );
    
    String json = RouteMapper.routeDtoToJson(merged);
    routeDao.update(id, merged.name(), json, merged.enabled());
    
    return merged;
}
```

### Audit Logging
```java
Map<String, DiffModel> changes = RouteMapper.findRouteDifferences(oldRoute, newRoute);
changes.forEach((prop, diff) -> auditLog.log(
    routeId, prop, diff.getOldValue(), diff.getNewValue(), diff.getAction()
));
```

---

## Files Created

```
shared/src/main/java/org/example/gateway/routes/mapper/
├── DiffModel.java           (78 lines) - Diff data model
├── DiffHelper.java          (215 lines) - Reflection-based diff detection
└── RouteMapper.java         (173 lines) - JSON serialization & utility methods

shared/src/test/java/org/example/gateway/routes/mapper/
└── RouteMapperTest.java     (270 lines) - 12 comprehensive test cases
```

---

## Dependencies

- **Jackson**: `com.fasterxml.jackson.databind.ObjectMapper` for JSON serialization
- **JUnit 5**: Testing framework
- **AssertJ**: Fluent assertions in tests
- **Java 21**: Record types, text blocks, pattern matching

---

## Performance Considerations

- **JSON Serialization**: O(n) where n = total properties across all nested objects
- **Diff Detection**: O(n) reflection-based field traversal
- **Deep Clone**: O(n) JSON round-trip conversion
- **Memory**: Minimal overhead, all objects are short-lived

**Optimization Notes:**
- Consider caching ObjectMapper instance (already done as static field)
- For large collections, consider streaming instead of storing in RouteDto
- Diff detection uses visited set to prevent exponential time on cyclic references

