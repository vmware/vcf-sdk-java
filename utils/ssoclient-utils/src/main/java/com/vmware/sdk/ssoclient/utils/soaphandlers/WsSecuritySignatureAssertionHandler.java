/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.ssoclient.utils.soaphandlers;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import jakarta.xml.ws.handler.soap.SOAPMessageContext;

import com.vmware.sdk.ssoclient.utils.SoapUtils;
import com.vmware.sdk.ssoclient.utils.wssecurity.WsSecuritySignatureAssertion;

/**
 * Handler class to sign the SOAP message using the assertionId of the SAML token along with the private key and
 * certificate of the user / solution This handler is to be used only when acquiring a new token using an existing token
 *
 * @author Ecosystem Engineering
 */
public class WsSecuritySignatureAssertionHandler extends SSOHeaderHandler {
    private final String _assertionId;
    private final PrivateKey _privateKey;
    private final X509Certificate _userCert;

    public WsSecuritySignatureAssertionHandler(PrivateKey privateKey, X509Certificate userCert, String assertionId) {
        _privateKey = privateKey;
        _userCert = userCert;
        _assertionId = assertionId;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext smc) {
        // At this stage the raw SOAPMessage is created and now we need to sign
        // the message using the private key and the certificate provided by the
        // user.
        if (SoapUtils.isOutgoingMessage(smc)) {
            WsSecuritySignatureAssertion wsSign =
                    new WsSecuritySignatureAssertion(_privateKey, _userCert, _assertionId);
            try {
                wsSign.sign(smc.getMessage());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }
}
