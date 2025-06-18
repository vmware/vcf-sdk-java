/*
 * ******************************************************************
 * Copyright (c) 2010-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.internal.saml.exception;

import com.vmware.vapi.saml.exception.SsoException;

/** Parsing related exception. Includes but not limited to XML and SOAP parsing. */
public class ParserException extends SsoException {

    private static final long serialVersionUID = 3496772333648099824L;

    public ParserException(String message) {
        super(message);
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
