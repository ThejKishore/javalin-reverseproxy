/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.security;

import org.example.gateway.storage.validationrule.ValidationRuleStorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * Thread-safe, hot-reloadable store of compiled validation {@link Pattern}s.
 *
 * <p>Patterns are loaded from the configured {@link ValidationRuleStorageProvider}.
 * Calling {@link #reload()} re-fetches the enabled patterns without gateway restart.
 * The {@link org.example.gateway.api.AdminController} exposes a dedicated endpoint
 * ({@code POST /gateway/admin/security/rules/reload}) that delegates to this method.
 */
public class ValidationRuleStore {

    private static final Logger log = LoggerFactory.getLogger(ValidationRuleStore.class);

    public record CompiledRule(String id, String name, String target, Pattern pattern) {}

    private final ValidationRuleStorageProvider storageProvider;
    private final CopyOnWriteArrayList<CompiledRule> rules = new CopyOnWriteArrayList<>();

    public ValidationRuleStore(ValidationRuleStorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    /** Returns an unmodifiable snapshot of the currently loaded rules. */
    public List<CompiledRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    /**
     * Reloads enabled rules from the storage provider and recompiles all patterns.
     * Thread-safe — uses a {@link CopyOnWriteArrayList} atomic replace.
     */
    public synchronized void reload() {
        if (storageProvider == null) {
            log.warn("ValidationRuleStore: storageProvider is null — skipping reload");
            return;
        }
        try {
            List<CompiledRule> compiled = storageProvider.findAllEnabled().stream()
                    .map(entry -> {
                        try {
                            Pattern p = Pattern.compile(entry.pattern(),
                                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                            return new CompiledRule(entry.id(), entry.name(), entry.target(), p);
                        } catch (Exception e) {
                            log.warn("Invalid validation rule pattern id={} pattern='{}': {}",
                                    entry.id(), entry.pattern(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(r -> r != null)
                    .toList();

            rules.clear();
            rules.addAll(compiled);
            log.info("ValidationRuleStore reloaded — {} rule(s) active", rules.size());
        } catch (Exception e) {
            log.error("Failed to reload validation rules: {}", e.getMessage(), e);
        }
    }
}

