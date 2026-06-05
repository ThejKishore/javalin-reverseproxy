### Requirements for Javalin EclipseStore

1. Create a contract for peristence which 
    should use JDBI if the config source is set to "database"
    should use EclipseStore local file system if the config source is set to "eclipse-store-lcl"
    should use EclipseStore Azure Blob Storage if the config source is set to "eclipse-store

```yaml
gateway:
  # Where to load routes from: "yaml" (this file) or "database" , "eclipse-store-lcl" , "eclispse-store-azure"
  config-source: database
```

```java
 @RegisterBeanMapper(RouteRow.class)
   public interface RouteDao {

   @SqlQuery("SELECT id, name, config_json, enabled, created_at, updated_at FROM routes WHERE enabled = TRUE ORDER BY created_at")
   List<RouteRow> findAllEnabled();

   @SqlQuery("SELECT id, name, config_json, enabled, created_at, updated_at FROM routes ORDER BY created_at")
   List<RouteRow> findAll();

   @SqlQuery("SELECT id, name, config_json, enabled, created_at, updated_at FROM routes WHERE id = :id")
   Optional<RouteRow> findById(@Bind("id") String id);

   @SqlUpdate("INSERT INTO routes (id, name, config_json, enabled, created_at, updated_at) " +
   "VALUES (:id, :name, :configJson, :enabled, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
   void insert(@Bind("id") String id,
   @Bind("name") String name,
   @Bind("configJson") String configJson,
   @Bind("enabled") boolean enabled);

   @SqlUpdate("UPDATE routes SET name = :name, config_json = :configJson, " +
   "enabled = :enabled, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
   int update(@Bind("id") String id,
   @Bind("name") String name,
   @Bind("configJson") String configJson,
   @Bind("enabled") boolean enabled);

   @SqlUpdate("UPDATE routes SET enabled = :enabled, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
   int setEnabled(@Bind("id") String id, @Bind("enabled") boolean enabled);

   @SqlUpdate("DELETE FROM routes WHERE id = :id")
   int deleteById(@Bind("id") String id);
   }

```

Use the following dependencies for EclipseStore and Azure Blob Storage:
```groovy
    implementation("org.eclipse.store:storage-embedded:4.1.0")
    implementation("org.eclipse.store:afs-azure-storage:4.1.0")
    implementation("com.azure:azure-storage-blob:12.29.0")
```

use the pojo for route configuration in the shared module:
```java
import java.util.List;

public record RoutesDao(
        List<RouteDao> routes
) {
}
// RouteDao represents the configuration for a single route

import java.util.List;

public record RouteDao(
        String stripPrefix,
        String pathPattern,
        int timeoutMs,
        List<TargetsItem> targets,
        boolean enabled,
        HeaderRules headerRules,
        CircuitBreakerPolicy circuitBreakerPolicy,
        String loadBalancerType,
        List<Object> authForwardHeaders,
        RateLimitPolicy rateLimitPolicy,
        String routingType,
        CachePolicy cachePolicy,
        String name,
        String auditStore,
        String id,
        boolean auditEnabled
) {
}

```

Also Update the AdminController and use RouteDto for admin API endpoints. The RouteDto should be converted to RouteDao for persistence and vice versa for API responses. This will allow us to keep the API layer separate from the persistence layer and make it easier to switch between different storage implementations in the future.

