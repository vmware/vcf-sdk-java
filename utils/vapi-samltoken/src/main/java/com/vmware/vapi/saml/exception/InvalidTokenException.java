/*
 * ******************************************************************
 * Copyright (c) 2010-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.saml.exception;

import com.vmware.vapi.saml.BundleMessageSource.Key;

/**
 * Thrown if an element of the currently parsed SAML token is not valid. The specific descendants will express the
 * element and/or reason due to which a token is not valid.
 */
public abstract class InvalidTokenException extends SsoException {

    private static final long serialVersionUID = -7205272177400202564L;

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidTokenException(String message, Key messageKey, Throwable cause, Object... messageDetails) {
        super(message, messageKey, cause, messageDetails);
    }
}
