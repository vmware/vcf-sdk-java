/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.ssoclient.utils.wssecurity;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.xml.namespace.QName;

import jakarta.xml.soap.SOAPMessage;

import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0.KeyIdentifierType;
import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0.ObjectFactory;
import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0.SecurityTokenReferenceType;
import org.w3c.dom.Node;

import com.vmware.sdk.ssoclient.utils.Constants;
import com.vmware.sdk.ssoclient.utils.SoapUtils;

public class WsSecuritySignatureAssertion extends WsSecuritySignatureImpl {

    private final String _assertionId;

    public WsSecuritySignatureAssertion(PrivateKey privateKey, X509Certificate userCert, String assertionId) {
        super(privateKey, userCert);
        if (assertionId == null) {
            throw new IllegalArgumentException("assertionId is null");
        }
        _assertionId = assertionId;
    }

    @Override
    protected String addUseKeySignatureId(SOAPMessage message) {
        return null;
    }

    @Override
    protected Node createKeyInfoContent(SOAPMessage message) {
        return createSecurityTokenReference();
    }

    /**
     * Creates SecurityTokenReference element that points to the refId parameter.
     *
     * @return Node
     */
    private Node createSecurityTokenReference() {
        ObjectFactory secExtFactory = new ObjectFactory();
        SecurityTokenReferenceType stRef = secExtFactory.createSecurityTokenReferenceType();
        KeyIdentifierType ki = secExtFactory.createKeyIdentifierType();
        ki.setValue(_assertionId);
        ki.setValueType(Constants.SAML_KEY_ID_TYPE);
        stRef.getAny().add(secExtFactory.createKeyIdentifier(ki));
        stRef.getOtherAttributes()
                .put(
                        new QName(
                                Constants.WSSE11_NAMESPACE,
                                Constants.WSSE11_TOKEN_TYPE_ATTR_NAME,
                                Constants.WSSE11_PREFIX),
                        Constants.SAML_TOKEN_TYPE);

        return SoapUtils.marshallJaxbElement(secExtFactory.createSecurityTokenReference(stRef))
                .getFirstChild();
    }
}
