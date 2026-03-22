/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.model;

/** Strategy used when selecting an upstream target from a route's target list. */
public enum LoadBalancerType {
    ROUND_ROBIN,
    WEIGHTED,
    RANDOM
}

