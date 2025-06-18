/*
 * ******************************************************************
 * Copyright (c) 2012-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.saml.exception;

import com.vmware.vapi.saml.BundleMessageSource.Key;

/**
 * Thrown when the token lifetime or other time related elements in the token are invalid, malformed, divergent at the
 * moment of parsing.
 */
public final class InvalidTimingException extends InvalidTokenException {

    private static final long serialVersionUID = -3532637964719201310L;

    public InvalidTimingException(String message) {
        super(message);
    }

    public InvalidTimingException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidTimingException(String message, Key messageKey, Throwable cause, Object... messageDetails) {
        super(message, messageKey, cause, messageDetails);
    }
}
