### Javalin Reverse Proxy Requirements

Your are required to use the following libraries:
- Javalin
- Reslient4j 
- Jackson
- SLF4J 
- Logback
- database (h2,postgres)
- Junit 5
- jdbi

### Javalin Reverse Proxy

- You are required to create a Javalin reverse proxy. The reverse proxy should be able to forward requests to a target server and return the response to the client. 
- The reverse proxy should also be able to handle errors and return appropriate error messages to the client. The routes should be defined in a separate config file(application.yml) or it should be read from an database storage. 
- The reverse proxy should also be able to handle authentication and should be able to forward the authentication headers to the target server. 
- The reverse proxy should also be able to handle rate limiting and should be able to return appropriate error messages when the rate limit is exceeded. 
- The reverse proxy should also be able to handle caching and should be able to return cached responses when appropriate. 
- The reverse proxy should also be able to handle load balancing and should be able to distribute requests to multiple target servers. 
- The reverse proxy should also be able to handle SSL and should be able to forward SSL requests to the target server. 
- The reverse proxy should also be able to handle WebSocket connections and should be able to forward WebSocket connections to the target server. 
- The reverse proxy should also be able to handle HTTP/2 and should be able to forward HTTP/2 requests to the target server. 
- The reverse proxy would be running in the kubernetes cluster and should be able to handle multiple instances of the reverse proxy running in the cluster. 
- The reverse proxy should also be able to handle multiple target servers and should be able to route requests to the appropriate target server based on the request path or other criteria. 
- The reverse proxy should be able to handle custom timeouts based on the routes and should be able to return appropriate error messages when the timeout is exceeded.
- The reverse proxy should be able to handle custom headers and should be able to forward the custom headers to the target server.
- The reverse proxy should be able to handle exclude headers based on the configuration and also should be able to add new headers both the request before forwarding and the response after receiving from the target server.
- The reverse proxy should be able to handle request and response transformations based on the configuration and should be able to modify the request and response before forwarding and after receiving from the target server. 
- The reverse proxy should be able to handle request and response auditing based on the configuration and should be able to store them in a database or a file for later analysis.
- The reverse proxy should have a health check endpoint that can be used to check the health of the reverse proxy.
- The reverse proxy should be able to do path based routing, regex based routing, traffic splitting, and header based routing.
- The reserse proxy should be able to do strip prefix routing and DedupeResponseHeaders filter.
- The reverse proxy should be able to send the ip address of the client to the target server.
- The reverse proxy should be able to add tracing headers to the request and response and should be able to log the tracing information for later analysis.
- The reverse proxy should be capable of adding new routes dynamically through api call or configuration file refresh or database data refresh. 
- The reverse proxy should be able to enable and disable routes dynamically through api call or configuration file refresh or database data refresh.



