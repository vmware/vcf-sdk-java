/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.helpers;

import com.vmware.vim25.DynamicData;
import com.vmware.vim25.ManagedObjectReference;

/**
 * Helper class for pretty printing vSphere objects in JSON-like format.
 */
public class PrettyPrintHelper {

    /**
     * Format object values for display, with special handling for DataObjects.
     */
    public static String formatObjectValue(Object value) {
        return formatObjectValue(value, 0);
    }

    /**
     * Extract properties from a DataObject in JSON-like format.
     */
    public static String extractDataObjectProperties(Object dataObject) {
        return extractDataObjectProperties(dataObject, 0);
    }

    /**
     * Recursive method to extract DataObject properties with JSON-like formatting.
     */
    public static String extractDataObjectProperties(Object dataObject, int indentLevel) {
        try {
            StringBuilder result = new StringBuilder();
            java.lang.reflect.Method[] methods = dataObject.getClass().getMethods();
            boolean hasContent = false;

            result.append("{\n");

            for (java.lang.reflect.Method method : methods) {
                String methodName = method.getName();
                if (method.getParameterCount() == 0 && (methodName.startsWith("get")
                        && !methodName.equals("getClass")
                     ) || methodName.startsWith("is")) {
                    try {
                        Object propValue = method.invoke(dataObject);

                        if (hasContent) result.append(",\n");

                        // Add proper indentation
                        for (int i = 0; i <= indentLevel; i++) {
                            result.append("  ");
                        }

                        String propName = methodName.startsWith("get") ? methodName.substring(3) : methodName.substring(2);
                        // Pascal to camel case
                        propName = propName.substring(0, 1).toLowerCase() + propName.substring(1);
                        result.append("\"").append(propName).append("\": ");

                        // Format the property value with proper JSON-like structure
                        String formattedValue = formatObjectValue(propValue, indentLevel + 1);
                        result.append(formattedValue);
                        hasContent = true;
                    } catch (Exception e) {
                        // Ignore reflection errors
                    }
                }
            }

            if (hasContent) {
                result.append("\n");
                // Add closing indentation
                for (int i = 0; i < indentLevel; i++) {
                    result.append("  ");
                }
            }
            result.append("}");

            return result.toString();
        } catch (Exception e) {
            return dataObject.toString();
        }
    }

    /**
     * Format object values with JSON-like structure and proper indentation.
     */
    public static String formatObjectValue(Object value, int indentLevel) {
        if (value == null) {
            return "null";
        }

        // Handle ManagedObjectReference
        if (value instanceof ManagedObjectReference) {
            ManagedObjectReference mor = (ManagedObjectReference) value;
            if (indentLevel == 0) {
                return mor.getValue(); // Use the ID instead of full object reference for top-level
            } else {
                return "\"" + mor.getValue() + "\""; // JSON format for nested
            }
        }

        // Handle basic types
        if (value instanceof String) {
            if (indentLevel == 0) {
                return value.toString(); // Plain string for top-level
            } else {
                return "\"" + value.toString().replace("\"", "\\\"") + "\""; // JSON format for nested
            }
        }

        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }

        if (value instanceof java.util.Date) {
            if (indentLevel == 0) {
                return value.toString(); // Plain date for top-level
            } else {
                return "\"" + value.toString() + "\""; // JSON format for nested
            }
        }

        // Handle arrays
        if (value.getClass().isArray()) {
            StringBuilder result = new StringBuilder("[");
            int length = java.lang.reflect.Array.getLength(value);
            if (length > 0) {
                result.append("\n");
                for (int i = 0; i < length && i < 5; i++) { // Limit to 5 items
                    if (i > 0) result.append(",\n");
                    // Add indentation for array items
                    for (int j = 0; j <= indentLevel; j++) {
                        result.append("  ");
                    }
                    result.append(formatObjectValue(java.lang.reflect.Array.get(value, i), indentLevel + 1));
                }
                if (length > 5) {
                    result.append(",\n");
                    for (int j = 0; j <= indentLevel; j++) {
                        result.append("  ");
                    }
                    result.append("...");
                }
                result.append("\n");
                // Add closing indentation
                for (int i = 0; i < indentLevel; i++) {
                    result.append("  ");
                }
            }
            result.append("]");
            return result.toString();
        }

        // Handle ArrayOfXXX objects
        String className = value.getClass().getSimpleName();
        if (className.startsWith("ArrayOf")) {
            String methodName =
                    className.substring(className.indexOf("ArrayOf") + "ArrayOf".length(), className.length());
            /*
             * If object is ArrayOfXXX object, then get the XXX[] by
             * invoking getXXX() on the object.
             * For Ex:
             * ArrayOfManagedObjectReference.getManagedObjectReference()
             * returns ManagedObjectReference[] array.
             */
            try {
                java.lang.reflect.Method method = value.getClass().getMethod("get" + methodName);
                if (method != null) {
                    Object result = method.invoke(value);
                    return formatObjectValue(result, indentLevel);
                }
            } catch (Exception e) {
                // Ignore reflection errors
            }
        }
        // Handle collections
        if (value instanceof java.util.Collection) {
            java.util.Collection<?> collection = (java.util.Collection<?>) value;
            StringBuilder result = new StringBuilder("[");
            if (!collection.isEmpty()) {
                result.append("\n");
                int count = 0;
                for (Object item : collection) {
                    if (count > 0) result.append(",\n");
                    if (count >= 5) { // Limit to 5 items
                        // Add indentation for ellipsis
                        for (int j = 0; j <= indentLevel; j++) {
                            result.append("  ");
                        }
                        result.append("...");
                        break;
                    }
                    // Add indentation for collection items
                    for (int j = 0; j <= indentLevel; j++) {
                        result.append("  ");
                    }
                    result.append(formatObjectValue(item, indentLevel + 1));
                    count++;
                }
                result.append("\n");
                // Add closing indentation
                for (int i = 0; i < indentLevel; i++) {
                    result.append("  ");
                }
            }
            result.append("]");
            return result.toString();
        }

        // Handle DataObjects recursively
        if (value instanceof DynamicData) {
            return extractDataObjectProperties(value, indentLevel);
        }

        // For all other objects, use toString() with quotes
        if (indentLevel == 0) {
            return value.toString(); // Plain toString for top-level
        } else {
            return "\"" + value.toString().replace("\"", "\\\"") + "\""; // JSON format for nested
        }
    }
}
