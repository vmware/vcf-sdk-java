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

/** Thrown if signature of parsed SAML token cannot be verified. */
public final class InvalidSignatureException extends InvalidTokenException {

    private static final long serialVersionUID = -5079962095665524671L;

    public InvalidSignatureException(String message) {
        super(message);
    }

    public InvalidSignatureException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidSignatureException(String message, Key messageKey, Throwable cause) {
        super(message, messageKey, cause);
    }
}
