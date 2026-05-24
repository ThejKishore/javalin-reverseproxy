## Requirements for reverse proxy test servers

### Technology Selected
- Go Lang 2.5.1
- Go Fiber 3.1.0

### Projecct Structure
A Go Fiber project with the following structure:

```shell
reverse-proxy-test-servers/
    | --- test_server_1
    | ------ main.go -> Servers 
    |-------------endpoints server at port 8081
                    get: /color (returning red )
                    get: /weight (returning 10) 
                    get: /longProcess has a thread sleep of 25 seconds( returning `servered from primary server after 25 seconds`)
                    get: /circuitBreaker ( give 80% of the time a 500 error and 20% of the time return `servered from primary server for circuit breaker scenario` )
    | --- test_servers_2
    | ------ main.go 
    | ------------------endpoints at port 9091
                    get: /color (returning blue )
                    get: /weight (returning 90)
                    get: /longProcess ( returning `servered from fallback server`) 
                    get: /circuitBreaker (return `servered from fallback server for circuit breaker scenario` )
    
```
