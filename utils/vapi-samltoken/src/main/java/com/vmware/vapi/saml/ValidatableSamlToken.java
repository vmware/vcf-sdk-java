/*
 * ******************************************************************
 * Copyright (c) 2012-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.saml;

import java.security.cert.X509Certificate;

import com.vmware.vapi.saml.exception.InvalidSignatureException;
import com.vmware.vapi.saml.exception.InvalidTimingException;
import com.vmware.vapi.saml.exception.InvalidTokenException;
import com.vmware.vapi.saml.exception.MalformedTokenException;

/**
 * Instances of this interface are not guaranteed to be valid (i.e. signed by a trusted authority, within lifetime
 * range). Trying to invoke any method different than validate should result in a runtime exception.
 */
public interface ValidatableSamlToken extends SamlToken {

    /**
     * Validates that the token is signed using a trusted certificate and is within the lifetime range
     *
     * @param trustedRootCertificates List of trusted root STS certificates that ValidatableSamlToken will use when
     *     validating the token's signature. Required.
     * @param clockToleranceSec Tolerate that many seconds of discrepancy between the token's sender clock and the local
     *     system clock when validating the token's start and expiration time. This effectively "expands" the token's
     *     validity period with the given number of seconds.
     * @throws InvalidSignatureException when the signature cannot be verified.
     * @throws InvalidTimingException when times in the token are malformed, invalid or divergent at the time of
     *     validation
     * @throws MalformedTokenException when the token or some of its elements are malformed
     * @throws InvalidTokenException if the token or some of its elements is invalid or malformed
     */
    public void validate(X509Certificate[] trustedRootCertificates, long clockToleranceSec)
            throws InvalidTokenException;
}
