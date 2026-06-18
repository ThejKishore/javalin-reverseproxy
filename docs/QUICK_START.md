# Quick Start Guide - Gateway Admin Dashboard

## Access the Dashboard
```
URL: http://localhost:8080/gateway/admin/ui
```

## First-Time Setup

### 1. View Available Routes
When you open the dashboard, you'll see:
- **Total Routes**: 5 (from V3 migration)
- **Active Routes**: 1 (example-service)
- **Disabled Routes**: 4
- **Health Status**: UP/DOWN indicator

### 2. Available Test Routes (Pre-loaded)

#### ✅ ACTIVE Routes
- **example-service** - PATH routing to JSONPlaceholder API
  - Pattern: `/api/example`
  - Target: `https://jsonplaceholder.typicode.com`
  - Load Balancer: ROUND_ROBIN

#### ⏸️ DISABLED Routes (Ready to Enable)
1. **backend-lb** - Load-balanced backend with circuit breaker
   - Pattern: `/api/backend`
   - Targets: backend-1 (weight: 2), backend-2 (weight: 1)
   - Features: Circuit Breaker + Rate Limiting

2. **canary-service** - Header-based routing
   - Pattern: `/api/canary`
   - Header Match: X-Canary = true
   - Load Balancer: RANDOM

3. **traffic-split** - Traffic split routing
   - Pattern: `/api/split`
   - Split: 90% stable, 10% beta
   - Load Balancer: WEIGHTED

4. **regex-route** - Regex pattern routing
   - Pattern: `/api/v[0-9]+/.*`
   - Features: Response caching (60s TTL)
   - Load Balancer: ROUND_ROBIN

## UI Features

### Dashboard
| Feature | Description |
|---------|-------------|
| **Stats Bar** | Shows total, active, disabled routes and health status |
| **Search** | Filter routes by name, path pattern, or type |
| **Sorting** | Click column headers to sort |
| **Pagination** | Select 10, 25, or 50 items per page |

### Actions
| Button | Action |
|--------|--------|
| **New Route** | Create a new routing rule |
| **Reload Gateway** | Reload all routes from database |
| **Edit (✏️)** | Edit route configuration |
| **Toggle (⏯️)** | Enable/disable route without deletion |
| **Delete (🗑️)** | Remove route permanently |

### Route Status
- **ACTIVE** (Green Badge) - Route is enabled and active
- **DISABLED** (Red Badge) - Route is disabled but can be re-enabled

## Creating a New Route

### Step 1: Click "New Route"

### Step 2: Fill Basic Information
- **Name**: Descriptive name (e.g., "user-service")
- **Path Pattern**: URL pattern (e.g., "/api/users")
- **Routing Type**: 
  - `PATH` - Simple URL path matching
  - `REGEX` - Regular expression matching
  - `HEADER` - Header-based routing
  - `TRAFFIC_SPLIT` - Traffic splitting between services
- **Load Balancer**: 
  - `ROUND_ROBIN` - Distribute evenly
  - `WEIGHTED` - Distribute by weight
  - `RANDOM` - Random distribution
- **Timeout**: Request timeout in milliseconds (default: 5000ms)
- **Enabled**: Toggle to enable/disable route

### Step 3: Configure Targets (Required)
- **URL**: Upstream service URL (e.g., "http://backend:8080")
- **Weight**: Load distribution weight (1-100)
- Click "+ Add Target" to add multiple targets

### Step 4: Configure Policies (Optional)

#### Rate Limiting
- Enable/disable rate limiting
- Requests per second
- Burst size
- Timeout duration

#### Circuit Breaker
- Enable/disable circuit breaker
- Failure rate threshold (%)
- Wait duration (seconds)
- Sliding window size

#### Caching
- Enable/disable response caching
- TTL in seconds
- Cache key strategy (METHOD_PATH or METHOD_PATH_QUERY)

#### Audit Logging
- Enable/disable audit logging
- Storage: Database or File

### Step 5: Configure Headers (Optional)
- **Add Request Headers**: Headers to add to upstream requests
- **Add Response Headers**: Headers to add to responses
- **Exclude Request Headers**: Headers to remove from requests
- **Exclude Response Headers**: Headers to remove from responses
- **Auth Forward Headers**: Headers to forward from client

### Step 6: Save
Click "Create Route" or "Save Changes"

## Testing Routes

### Test example-service
```bash
curl http://localhost:8080/api/example/posts/1

# Response from JSONPlaceholder
{
  "userId": 1,
  "id": 1,
  "title": "sunt aut facere repellat provident...",
  "body": "quia et suscipit..."
}
```

### Enable and Test backend-lb
1. Click toggle on "backend-lb" to enable
2. Update target URLs to real backends
3. Make requests to `/api/backend/*`

## API Endpoints Reference

### Route Management
```bash
# List all routes
GET /gateway/admin/routes

# Get specific route
GET /gateway/admin/routes/{id}

# Create route
POST /gateway/admin/routes
Content-Type: application/json
{route-json}

# Update route
PUT /gateway/admin/routes/{id}
Content-Type: application/json
{route-json}

# Delete route
DELETE /gateway/admin/routes/{id}

# Enable route
PATCH /gateway/admin/routes/{id}/enable

# Disable route
PATCH /gateway/admin/routes/{id}/disable

# Reload gateway
POST /gateway/admin/reload
```

### Health Checks
```bash
# Overall health
GET /gateway/health

# Liveness probe
GET /gateway/health/live

# Readiness probe
GET /gateway/health/ready
```

## Light Theme Colors

### Key Colors
- **Primary Blue**: Used for active elements and buttons
- **Success Green**: Used for enabled/success status
- **Error Red**: Used for disabled/error status
- **Info Cyan**: Used for informational badges
- **Warning Amber**: Used for warnings
- **Light Gray**: Backgrounds and borders
- **Dark Slate**: Primary text

## Troubleshooting

### Routes Not Loading
1. Check application is running on port 8080
2. Verify database migrations ran successfully
3. Check application logs

### API Calls Failing
1. Verify you're calling `/gateway/admin/routes` (not other paths)
2. Check network tab in browser developer tools
3. Review application logs

### Routes Not Proxying
1. Ensure route is ENABLED (green badge)
2. Verify target URLs are reachable
3. Check timeout is appropriate for upstream service
4. Review application logs for routing errors

### Cannot Create/Edit Routes
1. Ensure all required fields are filled (Name, Path Pattern, Targets)
2. Check for validation errors in toast notifications
3. Verify targets have valid URLs

## Browser Developer Tools

### Check Network Requests
1. Press F12 to open Developer Tools
2. Go to Network tab
3. Make a request through the UI
4. Check request/response details

### Check Console Logs
1. Press F12 to open Developer Tools
2. Go to Console tab
3. Look for any JavaScript errors

## Best Practices

### Route Configuration
- ✅ Use descriptive route names
- ✅ Set appropriate timeouts for your services
- ✅ Use weights for load balancing
- ✅ Enable audit logging for important routes
- ✅ Test routes before enabling in production

### Testing
- ✅ Test with real upstream services
- ✅ Verify health endpoints
- ✅ Monitor response times
- ✅ Test circuit breaker scenarios
- ✅ Validate caching behavior

### Security
- ✅ Restrict gateway admin access (add auth later)
- ✅ Validate upstream service URLs
- ✅ Use HTTPS for sensitive routes
- ✅ Forward authentication headers properly
- ✅ Enable audit logging

## Next Steps

1. **Enable test routes**: Toggle "backend-lb" to see it in action
2. **Create custom routes**: Add routes for your services
3. **Monitor traffic**: Check health endpoint regularly
4. **Scale up**: Configure multiple targets with weights
5. **Optimize**: Use caching and circuit breakers

## Support & Debugging

### Check Application Logs
```bash
# View gateway logs
tail -f logs/gateway.log

# View audit logs
tail -f logs/audit.log
```

### Database Inspection
The application uses H2 database with Flyway migrations:
- Schema: V1__create_routes.sql
- Audit: V2__create_audit_log.sql
- Data: V3__insert_routes.sql

### Verify Installation
1. Check routes loaded: `GET /gateway/admin/routes`
2. Check health: `GET /gateway/health`
3. Test proxy: `GET /gateway/admin/routes` (through example-service)

---

**Last Updated**: March 26, 2026
**Version**: 1.0.0
**Status**: ✅ Ready for Use

