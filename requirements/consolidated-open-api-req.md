### Requirements for OpenAPI Specification

For the gateway project to register with Azure API Management, we need to provide an OpenAPI specification that describes the API endpoints, request/response formats, and other relevant details. Below are the requirements for the OpenAPI specification:
For this we need to ensure that a single consolidated OpenAPI specification is generated that includes all the routes defined in the gateway, including both admin and proxy routes.
This specification should be comprehensive and accurately reflect the API's capabilities.
For this we need to use the metadata from the route definitions in the gateway.
Which provide following details for each route:

| Metadata Key | Metadata value | Description                                                                                     |
|--------------|----------------|-------------------------------------------------------------------------------------------------|
| context-path | /domain/servicename | The base path for the service or domain                                                         |
| health-endpoint | /actuator/health | The endpoint for health checks                                                                  |
| prometheus-endpoint | /actuator/prometheus | The endpoint for Prometheus metrics                                                             |
| openapi-spec-endpoint | /openapi (for Javalin) or /api-docs (for Spring Boot) | The endpoint where the OpenAPI specification is served                                          |
| skip-api | "true" | A flag to indicate if the path /api should be skipped from the OpenAPI specification even if the openapi specification definition is there |
| skip-paths | /admin,/internal | A comma-separated list of paths to be skipped from the OpenAPI specification for that service   |
| skip-openapi-for-service | "true" | A flag to indicate if the OpenAPI specification should be skipped entirely for that service/domain |


* The OpenAPI specification should be generated in a format that is compatible with Azure API Management, which typically supports OpenAPI 3.0 or later.
* The specification should include all relevant details such as endpoint paths, HTTP methods, request/response schemas, authentication requirements, and any other necessary information to allow Azure API Management to properly register and manage the API.
* The OpenAPI specification should be kept up-to-date with any changes to the API routes or metadata to ensure that it remains accurate and reflective of the current state of the API.
* Create an endpoint in the gateway to serve the consolidated OpenAPI specification, which can be accessed by Azure API Management for registration purposes. This endpoint should be secured and only accessible to authorized users or systems.
* If any routes are marked with the `skip-api` flag or listed in the `skip-paths`, they should be excluded from the generated OpenAPI specification to ensure that only the intended endpoints are exposed to Azure API Management.
* The OpenAPI specification should be generated dynamically based on the current route definitions and metadata in the gateway, allowing for flexibility and ease of maintenance as routes are added, modified, or removed.
* If the `skip-openapi-for-service` flag is set to "true" for a particular service or domain, the OpenAPI specification should not be generated for that service/domain, and it should be excluded from the consolidated specification served at the designated endpoint.




