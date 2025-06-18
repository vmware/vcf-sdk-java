/*
 * ******************************************************************
 * Copyright (c) 2011-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.internal.saml;

import java.lang.reflect.Array;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides validate methods for checking the value of given argument. If validation passes the methods will return
 * silently, otherwise - {@link IllegalArgumentException} should be thrown.
 */
public final class ValidateUtil {

    /**
     * Same as {@link #validateNotEmpty(Object, String)} but just for {@code null} value
     *
     * @param fieldValue not to be null
     * @param fieldName field name to log errors if the validation fails
     * @throws IllegalArgumentException on validation failure
     */
    public static void validateNotNull(Object fieldValue, String fieldName) {

        if (fieldValue == null) {
            logAndThrow(String.format("'%s' value should not be NULL", fieldName));
        }
    }

    /**
     * Check whether given object value is empty. Depending on argument runtime type <i>empty</i> means:
     *
     * <ul>
     *   <li>for java.lang.String type - {@code null} value or empty string
     *   <li>for array type - {@code null} value or zero length array
     *   <li>for any other type - {@code null} value
     * </ul>
     *
     * <p>Note that java.lang.String values should be handled by {@link String#isEmpty()}
     *
     * @param obj any object or {@code null}
     * @return {@code true} when the object value is {@code null} or empty array
     */
    public static boolean isEmpty(Object obj) {

        if (obj == null) {
            return true;
        }

        if (obj.getClass().equals(String.class)) {
            return ((String) obj).isEmpty();
        }

        if (obj.getClass().isArray()) {
            return Array.getLength(obj) == 0;
        }

        if (obj instanceof java.util.Collection<?>) {
            return ((java.util.Collection<?>) obj).isEmpty();
        }

        final String message = "String, java.lang.Array or java.util.Collection " + "expected but "
                + obj.getClass().getName() + " was found ";

        getLog().error("Wrong type: {}", message);
        throw new IllegalArgumentException(message);
    }

    /**
     * Validates that given value is not empty (as defined by {@link #isEmpty(Object)} contract). If validation check
     * fails - {@link IllegalArgumentException} will be thrown.
     *
     * <p>Useful for validation of required input arguments passed by end user at public methods, etc.
     *
     * @param fieldValue field value to validate
     * @param fieldName field name
     * @throws IllegalArgumentException on validation failure
     */
    public static void validateNotEmpty(Object fieldValue, String fieldName) {

        if (isEmpty(fieldValue)) {
            logAndThrow(String.format("'%s' value should not be empty", fieldName));
        }
    }

    private static void logAndThrow(String msg) {
        getLog().error(msg);
        throw new IllegalArgumentException(msg);
    }

    private static Logger getLog() {
        return LoggerFactory.getLogger(ValidateUtil.class);
    }

    private ValidateUtil() {
        // prevent instantiation
    }
}
