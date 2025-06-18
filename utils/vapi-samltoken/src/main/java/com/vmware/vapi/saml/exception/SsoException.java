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

/** Class representing a general SSO exception. SsoException should be sub-classed by more specialized exceptions. */
public abstract class SsoException extends Exception {

    private static final long serialVersionUID = -6028904116919319752L;

    private final Key _messageKey;
    private final Object[] _messageDetails;

    public SsoException(String message) {
        super(message);
        _messageKey = null;
        _messageDetails = null;
    }

    /**
     * Creates an exception
     *
     * @param message, debug message in English, suited for logs optional
     * @param messageKey the locale-neutral message key, optional
     * @param cause, optional
     * @param messageDetails optional: additional data to keep with the exception
     */
    public SsoException(String message, Key messageKey, Throwable cause, Object... messageDetails) {
        super(message, cause);
        _messageKey = messageKey;
        _messageDetails = messageDetails;
    }

    public SsoException(String message, Throwable cause) {
        this(message, null, cause);
    }

    public Key getMessageKey() {
        return _messageKey;
    }

    public Object[] getMessageDetails() {
        return _messageDetails;
    }
}
