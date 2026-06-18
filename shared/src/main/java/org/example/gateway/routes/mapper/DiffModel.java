/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.routes.mapper;

import java.util.Objects;

/**
 * Model representing a difference between two object instances.
 * Tracks changes at the property level with action type (ADDED, MODIFIED, DELETED).
 */
public class DiffModel {
    private final Class<?> targetClass;
    private final String property;
    private final DiffAction action;
    private final Object oldValue;
    private final Object newValue;

    public enum DiffAction {
        ADDED,
        MODIFIED,
        DELETED
    }

    public DiffModel(Class<?> targetClass, String property, DiffAction action, Object oldValue, Object newValue) {
        this.targetClass = Objects.requireNonNull(targetClass, "targetClass cannot be null");
        this.property = Objects.requireNonNull(property, "property cannot be null");
        this.action = Objects.requireNonNull(action, "action cannot be null");
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public String getProperty() {
        return property;
    }

    public DiffAction getAction() {
        return action;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiffModel diffModel = (DiffModel) o;
        return targetClass.equals(diffModel.targetClass) &&
                property.equals(diffModel.property) &&
                action == diffModel.action &&
                Objects.equals(oldValue, diffModel.oldValue) &&
                Objects.equals(newValue, diffModel.newValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetClass, property, action, oldValue, newValue);
    }

    @Override
    public String toString() {
        return "DiffModel{" +
                "targetClass=" + targetClass.getSimpleName() +
                ", property='" + property + '\'' +
                ", action=" + action +
                ", oldValue=" + oldValue +
                ", newValue=" + newValue +
                '}';
    }
}
