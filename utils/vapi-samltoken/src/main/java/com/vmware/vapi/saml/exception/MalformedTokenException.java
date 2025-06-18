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

/** Thrown if the currently parsed SAML token or some of its elements cannot be parsed or it is malformed. */
public final class MalformedTokenException extends InvalidTokenException {

    private static final long serialVersionUID = 6355024792039464203L;

    public MalformedTokenException(String message) {
        super(message);
    }

    public MalformedTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public MalformedTokenException(String message, Key messageKey, Throwable cause, Object... messageDetails) {
        super(message, messageKey, cause, messageDetails);
    }
}
