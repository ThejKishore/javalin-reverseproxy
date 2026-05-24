/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.security;

/**
 * Distributed store for CSRF tokens.
 *
 * <p>Tokens are keyed by a client binding key (IP or session ID) and expire
 * after the configured TTL. Implementations must be thread-safe and usable
 * across multiple gateway replicas.
 */
public interface CsrfTokenStore {

    /**
     * Stores a new CSRF token for the given client key.
     * Any existing token for this key is overwritten.
     *
     * @param clientKey  the binding key (IP address or session ID)
     * @param token      the generated CSRF token
     * @param ttlSeconds time-to-live in seconds
     */
    void store(String clientKey, String token, int ttlSeconds);

    /**
     * Returns the stored token for the given client key, or {@code null} if
     * no token exists (or it has expired).
     *
     * @param clientKey the binding key
     * @return the stored token, or {@code null}
     */
    String get(String clientKey);

    /**
     * Removes the token for the given client key.  Called after a successful
     * state-changing request to prevent token reuse.
     *
     * @param clientKey the binding key
     */
    void invalidate(String clientKey);
}

