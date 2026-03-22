/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.transform;

/**
 * Optional hook to modify the raw response body bytes received from the upstream
 * before they are sent back to the client.
 */
@FunctionalInterface
public interface ResponseTransformer {
    /** @return the (possibly modified) response body bytes */
    byte[] transform(byte[] responseBody);
}

