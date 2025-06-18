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
import com.vmware.sdk.ssoclient.utils.wssecurity.WsSecuritySignatureCertificate;

public class WsSecurityUserCertificateSignatureHandler extends SSOHeaderHandler {

    private final PrivateKey privateKey;
    private final X509Certificate userCert;

    public WsSecurityUserCertificateSignatureHandler(PrivateKey privateKey, X509Certificate userCert) {
        this.privateKey = privateKey;
        this.userCert = userCert;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext smc) {
        // At this stage the raw SOAPMessage is created and now we need to sign
        // the message using the private key and the certificate provided by the
        // user.
        if (SoapUtils.isOutgoingMessage(smc)) {
            WsSecuritySignatureCertificate wsSign = new WsSecuritySignatureCertificate(privateKey, userCert);
            try {
                wsSign.sign(smc.getMessage());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }
}
