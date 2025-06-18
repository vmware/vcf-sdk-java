/*
 * ******************************************************************
 * Copyright (c) 2010-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.rsa.names._2009._12.std_ext.saml2_;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

/**
 * This object contains factory methods for each Java content interface and Java element interface generated in the
 * com.rsa.names._2009._12.std_ext.saml2 package.
 *
 * <p>An ObjectFactory allows you to programatically construct new instances of the Java representation for XML content.
 * The Java representation of XML content can consist of schema derived interfaces and classes representing the binding
 * of schema type definitions, element declarations and model groups. Factory methods for each of these are provided in
 * this class.
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName _RSAAdvice_QNAME =
            new QName("http://www.rsa.com/names/2009/12/std-ext/SAML2.0", "RSAAdvice");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package:
     * com.rsa.names._2009._12.std_ext.saml2
     */
    public ObjectFactory() {}

    /** Create an instance of {@link RenewRestrictionType } */
    public RenewRestrictionType createRenewRestrictionType() {
        return new RenewRestrictionType();
    }

    /** Create an instance of {@link RSAAdviceType } */
    public RSAAdviceType createRSAAdviceType() {
        return new RSAAdviceType();
    }

    /** Create an instance of {@link JAXBElement }{@code <}{@link RSAAdviceType }{@code >}} */
    @XmlElementDecl(namespace = "http://www.rsa.com/names/2009/12/std-ext/SAML2.0", name = "RSAAdvice")
    public JAXBElement<RSAAdviceType> createRSAAdvice(RSAAdviceType value) {
        return new JAXBElement<RSAAdviceType>(_RSAAdvice_QNAME, RSAAdviceType.class, null, value);
    }
}
