/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.ssoclient.utils;

/**
 * Class to capture all the constants used throughout the SDK
 *
 * @author Ecosystem Engineering
 */
public final class Constants {
    public static final String DIGITAL_SIGNATURE_NAMESPACE_PREFIX = "ds";

    public static final String ENCODING_TYPE_BASE64 =
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary";
    public static final String ERR_CREATING_USE_KEY_ELEMENT = "Error creating UseKey element";
    public static final String ERR_INSERTING_SECURITY_HEADER =
            "Error inserting Security header into the SOAP message. Too many Security found.";
    public static final String ERR_NOT_A_SAML_TOKEN = "Token provided is not a SAML token";
    public static final String ERROR_CREATING_BINARY_SECURITY_TOKEN = "Error creating BinarySecurityToken";
    public static final String METHOD = "Method";
    public static final int REQUEST_VALIDITY_IN_MINUTES = 10;
    public static final String RSA_WITH_SHA512 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512";

    public static final String SAML_KEY_ID_TYPE =
            "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLID";
    public static final String SAML_TOKEN_TYPE =
            "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
    public static final String SECURITY_ELEMENT = "Security";

    public static final String SECURITY_ELEMENT_NAME = "Security";
    public static final String SIGNATURE_ELEMENT_NAME = "Signature";
    public static final String SUBJECT_CONFIRMATION = "SubjectConfirmation";
    public static final String URN_OASIS_NAMES_TC_SAML_2_0_ASSERTION = "urn:oasis:names:tc:SAML:2.0:assertion";
    public static final String URN_OASIS_NAMES_TC_SAML_2_0_CM_HOLDER_OF_KEY =
            "urn:oasis:names:tc:SAML:2.0:cm:holder-of-key";
    public static final String WS_1_3_TRUST_JAXB_PACKAGE = "org.oasis_open.docs.ws_sx.ws_trust._200512";

    public static final String WSS_NS =
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    public static final String WSSE_JAXB_PACKAGE =
            "org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0";
    public static final String WSSE_NAMESPACE =
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    public static final String WSSE11_NAMESPACE = "http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd";
    public static final String WSSE11_PREFIX = "wsse11";
    public static final String WSSE11_TOKEN_TYPE_ATTR_NAME = "TokenType";
    public static final String WSSU_JAXB_PACKAGE =
            "org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_utility_1_0";
    public static final String WSU_ID_LOCAL_NAME = "Id";
    public static final String WSU_NAMESPACE =
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    public static final String WSU_PREFIX = "wsu";
    public static final String WSU_TIMESTAMP_LOCAL_NAME = "Timestamp";
    public static final String X509_CERTIFICATE_TYPE =
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3";
    public static final String XML_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
}
