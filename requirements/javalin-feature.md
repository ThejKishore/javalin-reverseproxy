### Requirements for Javalin Gateway filter Feature

- The Javalin Gateway filter should be able to handle the following features:
    - A JWT filter should be able to validate the authentication jwt and check if thats a valid jwt before forwarding to the other filter chain.
    - A CSRF filter should be able to validate the CSRF token and check if thats a valid token before forwarding to the other filter chain.
    - The CSRF filter should be able to apply to only http methods that are not safe (POST, PUT, DELETE, PATCH) and should be able to exclude certain routes from CSRF validation based on the configuration.
      - The CSRF filter should be able to generate a CSRF token and send it to the client in a cookie or in the response body and should be able to validate the CSRF token sent by the client in the request header or in the request body.
      - The CSRF filter should be able to store the CSRF token in a secure way and should be able to associate the CSRF token with the user session or with the client IP address.
      - Use Hazelcast to store the CSRF tokens in a distributed way so that the CSRF tokens can be shared across multiple instances of the reverse proxy running in the kubernetes cluster.
      - The CSRF filter should be able to handle the case when the CSRF token is missing or invalid and should be able to return appropriate error messages to the client.
      - The CSRF filter should be able to handle the case when the CSRF token is expired and should be able to return appropriate error messages to the client.
      - The CSRF filter should be able to handle the case when the CSRF token is valid but does not match the user session or the client IP address and should be able to return appropriate error messages to the client.
    - A CSP filter should be able to validate the Content Security Policy and check if the request is compliant with the defined Content Security Policy before forwarding to the other filter chain.
      - The CSP filter should be able to apply the Content Security Policy based on the request path or other criteria and should be able to exclude certain routes from the Content Security Policy validation based on the configuration.
      - The CSP filter should be able to generate a Content Security Policy and send it to the client in a response header and should be able to validate the Content Security Policy sent by the client in the request header.
      - The CSP filter should be able to store the Content Security Policy in a secure way and should be able to associate the Content Security Policy with the user session or with the client IP address.
      - The CSP filter should be able to handle the case when the Content Security Policy is missing or invalid and should be able to return appropriate error messages to the client.
      - The CSP filter should be able to handle the case when the Content Security Policy is expired and should be able to return appropriate error messages to the client.
      - The CSP filter should be able to handle the case when the Content Security Policy is valid but does not match the user session or the client IP address and should be able to return appropriate error messages to the client.
    - A HTTPValidation filter that use ESAPI Filter for doing blacklist validation on the request parameters, query parameters, headers, and cookies and should be able to return appropriate error messages to the client when the validation fails.
      - The HTTPValidation filter should be able to apply the validation based on the request path or other criteria and should be able to exclude certain routes from the validation based on the configuration.
      - The HTTPValidation filter should be able to generate a validation report and send it to the client in the response body and should be able to log the validation report for later analysis.
      - The HTTPValidation filter should be able to store the validation report in a secure way and should be able to associate the validation report with the user session or with the client IP address.
      - The HTTPValidation filter should be able to handle the case when the validation fails and should be able to return appropriate error messages to the client.
      - The HTTPValidation filter should be able to handle the case when the validation report is missing or invalid and should be able to return appropriate error messages to the client.
      - The Blacklist validation regex patterns should be stored in a database and should be able to be updated dynamically without restarting the reverse proxy server.
      - The HTTPValidation filter should be able to handle the case when the validation report is valid but does not match the user session or the client IP address and should be able to return appropriate error messages to the client.
      - The filter should be able to turn on and off the blacklist validation dynamically through api call or configuration file refresh or database data refresh.
    
### Technology Selected:
- Javalin 7.1.0
- ESAPI 2.7.0
```groovy
// Source: https://mvnrepository.com/artifact/org.owasp.esapi/esapi
implementation("org.owasp.esapi:esapi:2.7.0.1-RC1")
```
- Hazelcast 5.3.0
```groovy
// Source: https://mvnrepository.com/artifact/com.hazelcast/hazelcast
implementation("com.hazelcast:hazelcast:5.7.0")
```
- JJWT-api
```goorvy
   (// Source: https://mvnrepository.com/artifact/io.jsonwebtoken/jjwt-api
   implementation("io.jsonwebtoken:jjwt-api:0.13.0"))
```

### Checklist:
- [ ] Implement JWT filter for authentication validation.
- [ ] Implement CSRF filter for CSRF token validation and generation.
- [ ] Implement CSP filter for Content Security Policy validation and generation.
- [ ] Implement HTTPValidation filter using ESAPI for blacklist validation.
- [ ] Store CSRF tokens in Hazelcast for distributed storage across multiple instances.
- [ ] Store blacklist validation regex patterns in a database and allow dynamic updates without restarting the server
- [ ] Implement error handling for missing, invalid, or expired tokens and policies.
- [ ] Implement dynamic enabling and disabling of filters through API calls or configuration refresh.
- [ ] Implement logging and reporting for validation results and errors.
- [ ] Ensure that the filters can be applied based on request path or other criteria and can exclude certain routes based on configuration.
- [ ] Implement secure storage and association of tokens and policies with user sessions or client IP addresses.
- [ ] Write unit tests for each filter to ensure proper functionality and error handling.
- [ ] Write integration tests to ensure that the filters work together correctly and that the reverse proxy behaves as expected under various scenarios.
- [ ] Document the implementation and usage of the filters in the reverse proxy, including configuration options and error messages.
- [ ] Ensure that the implementation follows best practices for security and performance, and that it can handle a high volume of requests without significant degradation in performance.
- [ ] Add the filters as a part of the filter chain in the Javalin reverse proxy and ensure that they are executed in the correct order based on the configuration.
- [ ] Implement a mechanism to refresh the filter configuration dynamically without restarting the server, such as watching for changes in the configuration file or providing an API endpoint for updating the configuration.

