/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.transform;

/**
 * Optional hook to modify the raw request body bytes before they are forwarded
 * to the upstream server.
 */
@FunctionalInterface
public interface RequestTransformer {
    /** @return the (possibly modified) request body bytes */
    byte[] transform(byte[] requestBody);
}

