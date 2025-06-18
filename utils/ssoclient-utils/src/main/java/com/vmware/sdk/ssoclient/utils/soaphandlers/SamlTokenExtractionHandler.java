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

import jakarta.xml.soap.SOAPBody;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.vmware.sdk.ssoclient.utils.SoapUtils;

/**
 * Handler class to extract the Saml token from the response stream in the raw form before JAX-WS deserializes it. This
 * is needed because the default deserializer with JAX-WS does not maintain the line separators present inside the token
 * while deserializing and later serializing the SAML token. Thus we have introduced this crude way to extracting the
 * raw token to be used for other operations
 *
 * @author Ecosystem Engineering
 */
public class SamlTokenExtractionHandler extends SSOHeaderHandler {

    private Node token;

    @Override
    public boolean handleMessage(SOAPMessageContext smc) {
        if (!SoapUtils.isOutgoingMessage(smc)) {
            try {
                // Extract the Token
                SOAPBody responseBody = smc.getMessage().getSOAPBody();
                Node firstChild = responseBody.getFirstChild();
                if (firstChild != null
                        && "RequestSecurityTokenResponseCollection".equalsIgnoreCase(firstChild.getLocalName())) {
                    if (firstChild.getFirstChild() != null
                            && "RequestSecurityTokenResponse"
                                    .equalsIgnoreCase(firstChild.getFirstChild().getLocalName())) {
                        Node rstrNode = firstChild.getFirstChild();
                        if (rstrNode.getFirstChild() != null
                                && "RequestedSecurityToken"
                                        .equalsIgnoreCase(
                                                rstrNode.getFirstChild().getLocalName())) {
                            Node rstNode = rstrNode.getFirstChild();
                            if (rstNode.getFirstChild() != null
                                    && "Assertion"
                                            .equalsIgnoreCase(
                                                    rstNode.getFirstChild().getLocalName())) {
                                token = rstNode.getFirstChild();
                            }
                        }
                    }
                } else {
                    if (firstChild != null
                            && "RequestSecurityTokenResponse".equalsIgnoreCase(firstChild.getLocalName())) {
                        if (firstChild.getFirstChild() != null
                                && "RequestedSecurityToken"
                                        .equalsIgnoreCase(
                                                firstChild.getFirstChild().getLocalName())) {
                            Node rstNode = firstChild.getFirstChild();
                            if (rstNode.getFirstChild() != null
                                    && "Assertion"
                                            .equalsIgnoreCase(
                                                    rstNode.getFirstChild().getLocalName())) {
                                token = rstNode.getFirstChild();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    public Element getToken() {
        return (Element) token;
    }
}
