/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.exception;

/** Base exception for all gateway errors. */
public class GatewayException extends RuntimeException {

    private final int httpStatus;

    public GatewayException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public GatewayException(String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() { return httpStatus; }
}

