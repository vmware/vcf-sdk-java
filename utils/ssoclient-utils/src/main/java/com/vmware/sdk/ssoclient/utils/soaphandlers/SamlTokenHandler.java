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

import jakarta.xml.ws.handler.soap.SOAPMessageContext;

import org.w3c.dom.Node;

import com.vmware.sdk.ssoclient.utils.Constants;
import com.vmware.sdk.ssoclient.utils.SoapUtils;

/**
 * Handler class to add the SAML token inside the security header
 *
 * @author Ecosystem Engineering
 */
public class SamlTokenHandler extends SSOHeaderHandler {

    private final Node token;

    /** @param token SAML token to be embedded */
    public SamlTokenHandler(Node token) {
        if (!SoapUtils.isSamlToken(token)) {
            throw new IllegalArgumentException(Constants.ERR_NOT_A_SAML_TOKEN);
        }
        this.token = token;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext smc) {
        if (SoapUtils.isOutgoingMessage(smc)) {
            try {
                Node securityNode = SoapUtils.getSecurityElement(SoapUtils.getSOAPHeader(smc));
                securityNode.appendChild(securityNode.getOwnerDocument().importNode(token, true));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }
}
