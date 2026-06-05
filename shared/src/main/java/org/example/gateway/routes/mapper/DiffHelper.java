/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.routes.mapper;

import java.lang.reflect.*;
import java.util.*;

/**
 * Helper utility to detect and report differences between two instances of the same type.
 * Performs deep comparison of all fields including nested objects and collections.
 */
public class DiffHelper {

    /**
     * Compares two objects of the same type and returns a map of differences.
     *
     * @param oldObject the original object (can be null)
     * @param newObject the modified object (can be null)
     * @return Map with property name as key and DiffModel as value
     */
    public static <T> Map<String, DiffModel> findDifferences(T oldObject, T newObject) {
        Map<String, DiffModel> differences = new LinkedHashMap<>();

        if (oldObject == null && newObject == null) {
            return differences;
        }

        Class<?> targetClass = oldObject != null ? oldObject.getClass() : newObject.getClass();

        // Handle case where one is null
        if (oldObject == null) {
            addAllFieldsAsAdded(newObject, targetClass, differences);
            return differences;
        }

        if (newObject == null) {
            addAllFieldsAsDeleted(oldObject, targetClass, differences);
            return differences;
        }

        // Both objects exist, compare fields
        compareFields(oldObject, newObject, targetClass, differences, new HashSet<>());

        return differences;
    }

    /**
     * Compares two collections of the same type and returns differences.
     * Uses object equality for comparison.
     *
     * @param oldCollection the original collection (can be null)
     * @param newCollection the modified collection (can be null)
     * @return Map with collection index/key as key and DiffModel as value
     */
    public static <T> Map<String, DiffModel> findCollectionDifferences(Collection<T> oldCollection, Collection<T> newCollection, Class<?> elementType) {
        Map<String, DiffModel> differences = new LinkedHashMap<>();

        if (oldCollection == null && newCollection == null) {
            return differences;
        }

        Set<T> oldSet = oldCollection != null ? new HashSet<>(oldCollection) : new HashSet<>();
        Set<T> newSet = newCollection != null ? new HashSet<>(newCollection) : new HashSet<>();

        // Find deleted items
        for (T oldItem : oldSet) {
            if (!newSet.contains(oldItem)) {
                String key = "deleted_" + oldItem.hashCode();
                differences.put(key, new DiffModel(
                        elementType,
                        "[deleted]",
                        DiffModel.DiffAction.DELETED,
                        oldItem,
                        null
                ));
            }
        }

        // Find added items
        for (T newItem : newSet) {
            if (!oldSet.contains(newItem)) {
                String key = "added_" + newItem.hashCode();
                differences.put(key, new DiffModel(
                        elementType,
                        "[added]",
                        DiffModel.DiffAction.ADDED,
                        null,
                        newItem
                ));
            }
        }

        return differences;
    }

    private static <T> void compareFields(T oldObject, T newObject, Class<?> targetClass, 
                                         Map<String, DiffModel> differences, Set<Object> visited) {
        if (visited.contains(System.identityHashCode(oldObject) ^ System.identityHashCode(newObject))) {
            return; // Avoid infinite recursion
        }
        visited.add(System.identityHashCode(oldObject) ^ System.identityHashCode(newObject));

        Field[] fields = targetClass.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object oldValue = field.get(oldObject);
                Object newValue = field.get(newObject);

                if (!Objects.deepEquals(oldValue, newValue)) {
                    if (oldValue == null && newValue != null) {
                        differences.put(field.getName(), new DiffModel(
                                targetClass,
                                field.getName(),
                                DiffModel.DiffAction.ADDED,
                                null,
                                newValue
                        ));
                    } else if (oldValue != null && newValue == null) {
                        differences.put(field.getName(), new DiffModel(
                                targetClass,
                                field.getName(),
                                DiffModel.DiffAction.DELETED,
                                oldValue,
                                null
                        ));
                    } else {
                        differences.put(field.getName(), new DiffModel(
                                targetClass,
                                field.getName(),
                                DiffModel.DiffAction.MODIFIED,
                                oldValue,
                                newValue
                        ));

                        // Recursively compare nested objects
                        if (isComplexType(oldValue) && isComplexType(newValue)) {
                            compareNestedObjects(oldValue, newValue, differences);
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                // Skip fields that cannot be accessed
            }
        }

        // Check parent classes
        Class<?> superClass = targetClass.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            compareFields(oldObject, newObject, superClass, differences, visited);
        }
    }

    private static void compareNestedObjects(Object oldValue, Object newValue, Map<String, DiffModel> differences) {
        if (oldValue.getClass() != newValue.getClass()) {
            return;
        }

        Field[] nestedFields = oldValue.getClass().getDeclaredFields();
        for (Field field : nestedFields) {
            field.setAccessible(true);
            try {
                Object oldNested = field.get(oldValue);
                Object newNested = field.get(newValue);

                if (!Objects.deepEquals(oldNested, newNested)) {
                    String nestedKey = field.getDeclaringClass().getSimpleName() + "." + field.getName();
                    differences.put(nestedKey, new DiffModel(
                            field.getDeclaringClass(),
                            field.getName(),
                            DiffModel.DiffAction.MODIFIED,
                            oldNested,
                            newNested
                    ));
                }
            } catch (IllegalAccessException e) {
                // Skip fields that cannot be accessed
            }
        }
    }

    private static void addAllFieldsAsAdded(Object newObject, Class<?> targetClass, Map<String, DiffModel> differences) {
        Field[] fields = targetClass.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(newObject);
                differences.put(field.getName(), new DiffModel(
                        targetClass,
                        field.getName(),
                        DiffModel.DiffAction.ADDED,
                        null,
                        value
                ));
            } catch (IllegalAccessException e) {
                // Skip fields that cannot be accessed
            }
        }
    }

    private static void addAllFieldsAsDeleted(Object oldObject, Class<?> targetClass, Map<String, DiffModel> differences) {
        Field[] fields = targetClass.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(oldObject);
                differences.put(field.getName(), new DiffModel(
                        targetClass,
                        field.getName(),
                        DiffModel.DiffAction.DELETED,
                        value,
                        null
                ));
            } catch (IllegalAccessException e) {
                // Skip fields that cannot be accessed
            }
        }
    }

    private static boolean isComplexType(Object obj) {
        if (obj == null) {
            return false;
        }
        Class<?> type = obj.getClass();
        return !type.isPrimitive() &&
                !type.getName().startsWith("java.lang.") &&
                !type.getName().startsWith("java.util.") &&
                !type.isEnum();
    }
}
