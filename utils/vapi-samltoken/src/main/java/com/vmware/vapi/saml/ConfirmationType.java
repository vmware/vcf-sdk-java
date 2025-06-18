/*
 * ******************************************************************
 * Copyright (c) 2010-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.saml;

/** Describes the token's confirmation type. */
public enum ConfirmationType {

    /** Confirmation type indicating that the token does not contain requester-specific confirmation information. */
    BEARER,

    /**
     * Confirmation type indicating that the requester's certificate is embedded into the token. This allows the
     * requester to prove its right to use the token by signing a message containing the token using the private key
     * corresponding to the certificate.
     */
    HOLDER_OF_KEY
}
