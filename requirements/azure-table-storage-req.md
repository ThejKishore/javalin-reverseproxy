### Azure Table Storage Requirements

The following Azure Table Storage tables are required for the application to function properly:

Use the table storage account name `thejdatalake` and the corresponding table names to access the required tables:


| Azure Table name | URL                                                     |
|------------------|---------------------------------------------------------|
| audit            | https://thejdatalake.table.core.windows.net/audit       |
| changelog | https://thejdatalake.table.core.windows.net/changelog   |
| routes | https://thejdatalake.table.core.windows.net/routes      |
| validation | https://thejdatalake.table.core.windows.net/validation  |

Add additional storage logic to the existing codebase to ensure that the application can read from and write to these tables as needed. This may involve implementing Azure Table Storage SDKs or using REST APIs to interact with the tables.
Ensure that proper authentication and authorization mechanisms are in place to secure access to the storage account and its tables.

This should work without breaking the current existing dao,eclipsestore implementation.

To connect to Azure Table Storage from a Java application, use the Azure Data Tables SDK.

1. Add the Maven dependency
```xml
   <dependency>
   <groupId>com.azure</groupId>
   <artifactId>azure-data-tables</artifactId>
   <version>12.5.0</version>
   </dependency>
```
2. Connect using a connection string

Store your connection string securely (for example, in an environment variable).

```java

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;

public class TableStorageExample {

    public static void main(String[] args) {

        String connectionString =
            "DefaultEndpointsProtocol=https;AccountName=yourAccount;" +
            "AccountKey=yourKey;EndpointSuffix=core.windows.net";

        TableClient tableClient = new TableClientBuilder()
                .connectionString(connectionString)
                .tableName("MyTable")
                .buildClient();

        System.out.println("Connected to table: " + tableClient.getTableName());
    }
}
```
3. Create a table if it doesn't exist
```java

   tableClient.createTable();
```

Or:
```java

tableClient.createTableIfNotExists();
```
4. Insert an entity

```java


   import com.azure.data.tables.models.TableEntity;

TableEntity entity = new TableEntity("Customer", "1001")
.addProperty("Name", "John Doe")
.addProperty("Email", "john@example.com");

tableClient.createEntity(entity);
```
5. Retrieve an entity
```java

   TableEntity entity =
   tableClient.getEntity("Customer", "1001");

System.out.println(entity.getProperty("Name"));
```
6. Query entities
```java

tableClient.listEntities().forEach(
   e -> System.out.println(e.getRowKey())
   );
```

Filter example:
```java

String filter = "PartitionKey eq 'Customer'";

tableClient.listEntities(filter, null, null)
.forEach(entity ->
System.out.println(entity.getRowKey()));
```

7. Using Microsoft Entra ID (recommended)

Instead of account keys, you can authenticate with managed identities or Azure credentials:
```java

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;

TableServiceClient serviceClient =
new TableServiceClientBuilder()
.endpoint("https://<storage-account>.table.core.windows.net")
.credential(new DefaultAzureCredentialBuilder().build())
.buildClient();
```

Add the dependency:

```xml
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-identity</artifactId>
    <version>1.18.1</version>
</dependency>
```

Common connection issues
403 Forbidden: Invalid account key, missing RBAC permissions, or incorrect credential type.
404 Not Found: Table doesn't exist.

Connection refused / DNS issues: Verify the endpoint:

https://<storage-account>.table.core.windows.net
Authentication failed with Entra ID: Ensure the identity has the Storage Table Data Contributor role on the storage account.

For modern applications running in Azure (App Service, AKS, Functions, VMs), Microsoft Entra ID with DefaultAzureCredential is generally preferred over connection strings and account keys.