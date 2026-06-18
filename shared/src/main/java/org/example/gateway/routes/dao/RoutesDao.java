package org.example.gateway.routes.dao;

import java.util.ArrayList;
import java.util.List;

public class RoutesDao{
    private final List<RouteDao> routes = new ArrayList<>();

    public List<RouteDao> getRoutes() {
        return routes;
    }
}
