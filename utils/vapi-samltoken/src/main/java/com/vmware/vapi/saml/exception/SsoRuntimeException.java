/*
 * ******************************************************************
 * Copyright (c) 2011-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.saml.exception;

import com.vmware.vapi.saml.BundleMessageSource.Key;

/**
 * Base exception indicating bad condition of the system which prevents normal completion of the given request. System
 * errors might be caused by hardware failures, software defects (bugs), network problems or when the service is down.
 */
public abstract class SsoRuntimeException extends RuntimeException {

    private final Key _messageKey;
    private final Object[] _messageDetails;

    public SsoRuntimeException(String message) {
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
    public SsoRuntimeException(String message, Key messageKey, Throwable cause, Object... messageDetails) {
        super(message, cause);
        _messageKey = messageKey;
        _messageDetails = messageDetails;
    }

    public SsoRuntimeException(String message, Throwable cause) {
        this(message, null, cause);
    }

    // we cannot override getLocalizedMessage to throw unsuported
    // operation, because it is used by some JDK facilities

    public Key getMessageKey() {
        return _messageKey;
    }

    public Object[] getMessageDetails() {
        return _messageDetails;
    }

    private static final long serialVersionUID = -5890144949620014726L;
}
