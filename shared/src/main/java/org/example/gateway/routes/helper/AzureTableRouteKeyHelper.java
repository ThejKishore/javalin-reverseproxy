/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.routes.helper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Utility for encoding route {@code path-pattern} values into Azure Table row keys. */
public final class AzureTableRouteKeyHelper {

    private static final String PREFIX = "path-";

    private AzureTableRouteKeyHelper() {}

    public static String encodeRowKey(String pathPattern) {
        if (pathPattern == null || pathPattern.isBlank()) {
            throw new IllegalArgumentException("path-pattern is required");
        }
        return PREFIX + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(pathPattern.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeRowKey(String rowKey) {
        if (rowKey == null || rowKey.isBlank()) {
            return rowKey;
        }
        if (!rowKey.startsWith(PREFIX)) {
            return rowKey;
        }
        byte[] bytes = Base64.getUrlDecoder().decode(rowKey.substring(PREFIX.length()));
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
