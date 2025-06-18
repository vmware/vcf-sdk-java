/*
 * ******************************************************************
 * Copyright (c) 2011-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.saml;

import java.security.cert.X509Certificate;

import org.w3c.dom.Element;

import com.vmware.vapi.saml.exception.InvalidTokenException;

/** Implementations of this interface will serve as a SAML token factories. */
public interface SamlTokenFactory {

    /**
     * Create a {@code SamlToken} object from DOM {@code Element}, performing syntactic and semantical validation of the
     * XML tree.
     *
     * <p>The token will retain a <i>copy</i> of the original element (not the element itself).
     *
     * @param tokenRoot The root element of the subtree containing the SAML token.
     * @param trustedRootCertificates The public signing certificate(s) of the security token service, needed for token
     *     validation. Must not be {@code null}, there must be at least one certificate, and none of the supplied
     *     certificates may be {@code null}.
     * @param clockToleranceSec Tolerate that many seconds of discrepancy between the token's sender clock and the local
     *     system clock when validating the token's start and expiration time. This effectively "expands" the token's
     *     validity period with the given number of seconds.
     * @return The parsed and validated Token object
     * @throws InvalidTokenException Indicates syntactic (e.g. contains invalid elements or missing required elements)
     *     or semantic (e.g. subject name in unknown format) error, expired or not yet valid token or failure to
     *     validate the signature against the trustedRootCertificates.
     */
    public SamlToken parseToken(Element tokenRoot, X509Certificate[] trustedRootCertificates, long clockToleranceSec)
            throws InvalidTokenException;

    /**
     * Create a {@code SamlToken} object from a DOM {@code Element} with zero clock tolerance (i.e., no skew allowed).
     * This is a convenience overload of {@link #parseToken(Element, X509Certificate[], long)}.
     *
     * @param tokenRoot The root element of the subtree containing the SAML token. Must not be {@code null}.
     * @param trustedRootCertificates The public signing certificate(s) of the security token service, used for token
     *     signature validation. Must not be {@code null} or empty, and none of the supplied certificates may be
     *     {@code null}.
     * @return A parsed and validated {@link SamlToken} object.
     * @throws InvalidTokenException Indicates syntactic (e.g. contains invalid elements or missing required elements)
     *     or semantic (e.g. subject name in unknown format) error, expired or not yet valid token or failure to
     *     validate the signature against the trustedRootCertificates.
     */
    public SamlToken parseToken(Element tokenRoot, X509Certificate... trustedRootCertificates)
            throws InvalidTokenException;

    /**
     * Create a {@code SamlToken} object from DOM {@code Element}, performing syntactic and semantical validation of the
     * XML tree.
     *
     * <p>The token signature and expiration status are <i>not</i> validated.
     *
     * <p>The token will retain a <i>copy</i> of the original element (not the element itself).
     *
     * @param tokenRoot The root element of the subtree containing the SAML token.
     * @return The parsed and validated Token object
     * @throws InvalidTokenException Indicates syntactic (e.g. contains invalid elements or missing required elements)
     *     or semantic (e.g. subject name in unknown format) error.
     */
    public SamlToken parseToken(Element tokenRoot) throws InvalidTokenException;

    /**
     * Create a {@code SamlToken} object from string representation, performing syntactic and semantical validation of
     * the XML tree.
     *
     * @param tokenXml The xml representation of a SAML token. Not {@code null}.
     * @param trustedRootCertificates The public signing certificate(s) of the security token service, needed for token
     *     validation. Must not be {@code null}, there must be at least one certificate, and none of the supplied
     *     certificates may be {@code null}.
     * @param clockToleranceSec Tolerate that many seconds of discrepancy between the token's sender clock and the local
     *     system clock when validating the token's start and expiration time. This effectively "expands" the token's
     *     validity period with the given number of seconds.
     * @return The parsed and validated Token object
     * @throws InvalidTokenException Indicates syntactic (e.g. contains invalid elements or missing required elements)
     *     or semantic (e.g. subject name in unknown format) error, expired or not yet valid token or failure to
     *     validate the signature against the trustedRootCertificates.
     */
    public SamlToken parseToken(String tokenXml, X509Certificate[] trustedRootCertificates, long clockToleranceSec)
            throws InvalidTokenException;

    /**
     * Create a {@code SamlToken} object from a string representation of the token with zero clock tolerance (i.e., no
     * skew allowed). This is a convenience overload of {@link #parseToken(String, X509Certificate[], long)}.
     *
     * @param tokenXml The XML string representation of a SAML token. Must not be {@code null}.
     * @param trustedRootCertificates The public signing certificate(s) of the security token service, used for token
     *     signature validation. Must not be {@code null} or empty, and none of the supplied certificates may be
     *     {@code null}.
     * @return A parsed and validated {@link SamlToken} object.
     * @throws InvalidTokenException Indicates syntactic (e.g. contains invalid elements or missing required elements)
     *     or semantic (e.g. subject name in unknown format) error, expired or not yet valid token or failure to
     *     validate the signature against the trustedRootCertificates.
     */
    public SamlToken parseToken(String tokenXml, X509Certificate... trustedRootCertificates)
            throws InvalidTokenException;

    /**
     * Create a {@code SamlToken} object from string representation, performing syntactic and semantical validation of
     * the XML tree.
     *
     * <p>The token signature and expiration status are <i>not</i> validated.
     *
     * @param tokenXml The xml representation of a SAML token. Not {@code null}.
     * @return The parsed and validated Token object
     * @throws InvalidTokenException Indicates syntactic (e.g. contains invalid elements or missing required elements)
     *     or semantic (e.g. subject name in unknown format) error.
     */
    public SamlToken parseToken(String tokenXml) throws InvalidTokenException;
}
