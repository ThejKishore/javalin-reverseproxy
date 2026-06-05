/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.validationrule;

import org.example.gateway.routes.dao.ValidationRuleEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * EclipseStore root object for the validation-rule object graph.
 *
 * <p>EclipseStore persists the entire Java object graph rooted here.
 * Must be a mutable class (not a record) so that EclipseStore can track changes.
 * All mutations should be made through {@link #getEntries()} and then persisted
 * by calling {@code storageManager.store(root.getEntries())}.
 */
public class ValidationRuleStoreRoot {

    private final List<ValidationRuleEntry> entries = new ArrayList<>();

    public List<ValidationRuleEntry> getEntries() {
        return entries;
    }
}
