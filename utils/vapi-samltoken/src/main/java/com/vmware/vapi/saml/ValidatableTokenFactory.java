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

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import org.w3c.dom.Element;

import com.vmware.vapi.internal.saml.Constants;
import com.vmware.vapi.internal.saml.SamlTokenImpl;
import com.vmware.vapi.saml.exception.InvalidTokenException;

/** Factory providing methods for token operations. Not thread safe. */
public final class ValidatableTokenFactory {

    private static final JAXBContext _jaxbContext = createJaxbContext();

    /**
     * Create a ValidatableSamlToken object from DOM Element, performing syntactic and semantical validation of the XML
     * tree. Note that signature validation will not be performed.
     *
     * <p>The token will retain a <i>copy</i> of the original element (not the element itself).
     *
     * @param tokenRoot The root element of the subtree containing the SAML token.
     * @return The parsed and validated Token object
     * @throws InvalidTokenException Indicates syntactic (e.g. contains invalid elements or missing required elements)
     *     or semantic (e.g. subject name in unknown format) error, expired or not yet valid token.
     */
    public ValidatableSamlToken parseValidatableToken(Element tokenRoot) throws InvalidTokenException {
        return new SamlTokenImpl(tokenRoot, _jaxbContext);
    }

    /**
     * Create a ValidatableSamlToken object from DOM Element, performing syntactic and semantical validation of the XML
     * tree. Note that signature validation will not be performed.
     *
     * <p>The token will retain a <i>copy</i> of the original element (not the element itself).
     *
     * @param tokenRoot The root element of the subtree containing the SAML token.
     * @return The parsed and validated Token object
     * @throws InvalidTokenException Indicates syntactic (e.g. contains invalid elements or missing required elements)
     *     or semantic (e.g. subject name in unknown format) error, expired or not yet valid token.
     */
    public ValidatableSamlTokenEx parseValidatableTokenEx(Element tokenRoot) throws InvalidTokenException {
        return new SamlTokenImpl(tokenRoot, _jaxbContext, true);
    }

    /** @return {@link JAXBContext} for {@link Constants#ASSERTION_JAXB_PACKAGE} */
    private static JAXBContext createJaxbContext() {
        try {
            return JAXBContext.newInstance(Constants.ASSERTION_JAXB_PACKAGE);
        } catch (JAXBException e) {
            throw new IllegalStateException("Cannot initialize JAXBContext.", e);
        }
    }
}
