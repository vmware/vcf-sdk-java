/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.helpers;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

public class ConfigValidator {

    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();

    public static <T> void validate(T config) {
        Set<ConstraintViolation<T>> violations = validator.validate(config);
        if (!violations.isEmpty()) {
            StringBuilder message = new StringBuilder(
                    "Invalid configuration for " + config.getClass().getSimpleName() + System.lineSeparator());
            for (ConstraintViolation<T> violation : violations) {
                message.append(" - ")
                        .append(violation.getPropertyPath())
                        .append(": ")
                        .append(violation.getMessage())
                        .append(System.lineSeparator());
            }
            throw new IllegalArgumentException(message.toString());
        }
    }
}
