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

import java.security.cert.X509Certificate;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import org.w3c.dom.Element;

import com.vmware.vapi.internal.saml.Constants;
import com.vmware.vapi.internal.saml.SamlTokenImpl;
import com.vmware.vapi.saml.exception.InvalidTokenException;

/**
 * Factory providing methods for creating and parsing SAML tokens.
 *
 * <p>{@link SamlToken} instances produced by this factory are guaranteed to be syntactically and semantically valid
 * when the relevant validation parameters (e.g. {@code trustedRootCertificates}, {@code clockToleranceSec}) are
 * provided.
 *
 * <p>Factory methods that accept signing certificates ({@code trustedRootCertificates}) also ensure that the token
 * signature is valid and that the token is within its valid lifetime, taking into account the specified
 * {@code clockToleranceSec}.
 *
 * <p>All instance and static methods in this class are thread safe.
 */
public final class DefaultTokenFactory implements SamlTokenFactory {

    private static final long DEFAULT_TOKEN_CLOCK_TOLERANCE = 0;

    private static final JAXBContext _jaxbContext = createJaxbContext();

    public DefaultTokenFactory() {}

    /** {@inheritDoc} */
    @Override
    public SamlToken parseToken(Element tokenRoot, X509Certificate[] trustedRootCertificates, long clockToleranceSec)
            throws InvalidTokenException {

        SamlTokenImpl samlTokenImpl = new SamlTokenImpl(tokenRoot, _jaxbContext);
        samlTokenImpl.validate(trustedRootCertificates, clockToleranceSec);
        return samlTokenImpl;
    }
    /** {@inheritDoc} */
    @Override
    public SamlToken parseToken(String tokenXml, X509Certificate[] trustedRootCertificates, long clockToleranceSec)
            throws InvalidTokenException {

        SamlTokenImpl samlTokenImpl = new SamlTokenImpl(tokenXml, _jaxbContext);
        samlTokenImpl.validate(trustedRootCertificates, clockToleranceSec);
        return samlTokenImpl;
    }
    /** {@inheritDoc} */
    @Override
    public SamlToken parseToken(String tokenXml, X509Certificate... trustedRootCertificates)
            throws InvalidTokenException {

        return parseToken(tokenXml, trustedRootCertificates, DEFAULT_TOKEN_CLOCK_TOLERANCE);
    }
    /** {@inheritDoc} */
    @Override
    public SamlToken parseToken(Element tokenRoot, X509Certificate... trustedRootCertificates)
            throws InvalidTokenException {

        return parseToken(tokenRoot, trustedRootCertificates, DEFAULT_TOKEN_CLOCK_TOLERANCE);
    }
    /** {@inheritDoc} */
    @Override
    public SamlToken parseToken(Element tokenRoot) throws InvalidTokenException {
        SamlTokenImpl samlTokenImpl = new SamlTokenImpl(tokenRoot, _jaxbContext);
        samlTokenImpl.allowTokenAccess();
        return samlTokenImpl;
    }
    /** {@inheritDoc} */
    @Override
    public SamlToken parseToken(String tokenXml) throws InvalidTokenException {
        SamlTokenImpl samlTokenImpl = new SamlTokenImpl(tokenXml, _jaxbContext);
        samlTokenImpl.allowTokenAccess();
        return samlTokenImpl;
    }

    /**
     * Static factory method alternative of {@link #parseToken(String, X509Certificate[], long)}.
     *
     * @param tokenXml the XML string of the SAML token
     * @param trustedRootCertificates the public signing certificate(s) of the security token service
     * @param clockToleranceSec tolerance in seconds for clock skew
     * @return a parsed and validated {@link SamlToken}
     * @throws InvalidTokenException if the token is syntactically/semantically invalid or expired
     */
    public static SamlToken createToken(
            String tokenXml, X509Certificate[] trustedRootCertificates, long clockToleranceSec)
            throws InvalidTokenException {

        SamlTokenImpl samlTokenImpl = new SamlTokenImpl(tokenXml, _jaxbContext);
        samlTokenImpl.validate(trustedRootCertificates, clockToleranceSec);
        return samlTokenImpl;
    }

    /**
     * Static factory method alternative of {@link #parseToken(String, X509Certificate...)}.
     *
     * @param tokenXml the XML string of the SAML token
     * @param trustedRootCertificates the public signing certificate(s) of the security token service
     * @return a parsed and validated {@link SamlToken}
     * @throws InvalidTokenException if the token is syntactically/semantically invalid or expired
     */
    public static SamlToken createToken(String tokenXml, X509Certificate... trustedRootCertificates)
            throws InvalidTokenException {

        return createToken(tokenXml, trustedRootCertificates, DEFAULT_TOKEN_CLOCK_TOLERANCE);
    }

    /**
     * Static factory method alternative of {@link #parseToken(String)}.
     *
     * @param tokenXml the XML string of the SAML token
     * @return a parsed {@link SamlToken}, without validating its signature or expiration
     * @throws InvalidTokenException if the token is syntactically or semantically invalid
     */
    public static SamlToken createToken(String tokenXml) throws InvalidTokenException {
        return new DefaultTokenFactory().parseToken(tokenXml);
    }

    /**
     * Static factory method alternative of {@link #parseToken(Element, X509Certificate[], long)}.
     *
     * @param tokenRoot the DOM {@link Element} containing the SAML token
     * @param trustedRootCertificates the public signing certificate(s) of the security token service
     * @param clockToleranceSec tolerance in seconds for clock skew
     * @return a parsed and validated {@link SamlToken}
     * @throws InvalidTokenException if the token is syntactically/semantically invalid or expired
     */
    public static SamlToken createTokenFromDom(
            Element tokenRoot, X509Certificate[] trustedRootCertificates, long clockToleranceSec)
            throws InvalidTokenException {

        SamlTokenImpl samlTokenImpl = new SamlTokenImpl(tokenRoot, _jaxbContext);
        samlTokenImpl.validate(trustedRootCertificates, clockToleranceSec);
        return samlTokenImpl;
    }

    /**
     * Static factory method alternative of {@link #parseToken(Element, X509Certificate...)}.
     *
     * @param tokenRoot the DOM {@link Element} containing the SAML token
     * @param trustedRootCertificates the public signing certificate(s) of the security token service
     * @return a parsed and validated {@link SamlToken}
     * @throws InvalidTokenException if the token is syntactically/semantically invalid or expired
     */
    public static SamlToken createTokenFromDom(Element tokenRoot, X509Certificate... trustedRootCertificates)
            throws InvalidTokenException {

        return createTokenFromDom(tokenRoot, trustedRootCertificates, DEFAULT_TOKEN_CLOCK_TOLERANCE);
    }

    /**
     * Static factory method alternative of {@link #parseToken(Element)}.
     *
     * @param tokenRoot the DOM {@link Element} containing the SAML token
     * @return a parsed {@link SamlToken}, without validating its signature or expiration
     * @throws InvalidTokenException if the token is syntactically or semantically invalid
     */
    public static SamlToken createTokenFromDom(Element tokenRoot) throws InvalidTokenException {
        return new DefaultTokenFactory().parseToken(tokenRoot);
    }

    /**
     * Initializes and returns a {@link JAXBContext} for {@link Constants#ASSERTION_JAXB_PACKAGE}.
     *
     * @return a {@link JAXBContext} configured for SAML token handling
     * @throws IllegalStateException if the JAXBContext fails to initialize
     */
    private static JAXBContext createJaxbContext() {
        try {
            return JAXBContext.newInstance(Constants.ASSERTION_JAXB_PACKAGE);
        } catch (JAXBException e) {
            throw new IllegalStateException("Cannot initialize JAXBContext.", e);
        }
    }
}
