/*
 * ******************************************************************
 * Copyright (c) 2012-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.internal.cis.authn;

import java.security.PrivateKey;

import com.vmware.vapi.dsig.json.SignatureException;
import com.vmware.vapi.internal.cis.authn.json.JsonSignatureAlgorithm;

/** Implementations of this interface should be able to sign a payload */
public interface Signer {

    /**
     * Signs the payload using the private key corresponding to the public key embedded into the token. The token is
     * expected to be HoK token.
     *
     * @param payload the data to be signed. cannot be null.
     * @param privateKey the private key corresponding to the public key embedded into the token. cannot be null.
     * @param algorithm the sign algorithm to be used
     * @return the String representation of the signature data (includes at least signing algorithm, SAML token,
     *     signature value etc.). cannot be null.
     */
    String sign(byte[] payload, PrivateKey privateKey, JsonSignatureAlgorithm algorithm) throws SignatureException;
}
