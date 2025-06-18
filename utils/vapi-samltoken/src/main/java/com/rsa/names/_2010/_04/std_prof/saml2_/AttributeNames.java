/*
 * ******************************************************************
 * Copyright (c) 2011-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.rsa.names._2010._04.std_prof.saml2_;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Java class for AttributeNames.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <p>
 *
 * <pre>
 * &lt;simpleType name="AttributeNames">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="http://rsa.com/schemas/attr-names/2009/01/GroupIdentity"/>
 *     &lt;enumeration value="http://vmware.com/schemas/attr-names/2011/07/isSolution"/>
 *     &lt;enumeration value="http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname"/>
 *     &lt;enumeration value="http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 */
@XmlType(name = "AttributeNames", namespace = "http://www.rsa.com/names/2010/04/std-prof/SAML2.0")
@XmlEnum
public enum AttributeNames {
    @XmlEnumValue("http://rsa.com/schemas/attr-names/2009/01/GroupIdentity")
    HTTP_RSA_COM_SCHEMAS_ATTR_NAMES_2009_01_GROUP_IDENTITY("http://rsa.com/schemas/attr-names/2009/01/GroupIdentity"),
    @XmlEnumValue("http://vmware.com/schemas/attr-names/2011/07/isSolution")
    HTTP_VMWARE_COM_SCHEMAS_ATTR_NAMES_2011_07_IS_SOLUTION("http://vmware.com/schemas/attr-names/2011/07/isSolution"),
    @XmlEnumValue("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname")
    HTTP_SCHEMAS_XMLSOAP_ORG_WS_2005_05_IDENTITY_CLAIMS_GIVENNAME(
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname"),
    @XmlEnumValue("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname")
    HTTP_SCHEMAS_XMLSOAP_ORG_WS_2005_05_IDENTITY_CLAIMS_SURNAME(
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname");
    private final String value;

    AttributeNames(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AttributeNames fromValue(String v) {
        for (AttributeNames c : AttributeNames.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
