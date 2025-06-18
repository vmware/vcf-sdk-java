/*
 * ******************************************************************
 * Copyright (c) 2010-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.saml.util.exception;

import com.vmware.vapi.saml.exception.SsoException;

/**
 * Signals that a keystore operation has failed (i.e. certificate or private key extraction has failed, or the keystore
 * cannot be opened etc.)
 */
public class SsoKeyStoreOperationException extends SsoException {

    private static final long serialVersionUID = 4328081480901343493L;

    public SsoKeyStoreOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
