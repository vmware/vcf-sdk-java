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

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0.AttributedString;
import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0.ObjectFactory;
import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0.PasswordString;
import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0.UsernameTokenType;
import org.w3c.dom.Node;

import com.vmware.sdk.ssoclient.utils.SoapUtils;

/**
 * Handler class to add the Username token element inside the security header
 *
 * @author Ecosystem Engineering
 */
public class UserCredentialHandler extends SSOHeaderHandler {

    private final String userName;
    private final String passwd;

    /**
     * @param username Username to use
     * @param password Password to use
     */
    public UserCredentialHandler(String username, String password) {
        this.userName = username;
        this.passwd = password;
    }

    /**
     * Creates a WS-Security UsernameToken element.
     *
     * @return UsernameToken
     */
    private JAXBElement<UsernameTokenType> createUsernameToken() {
        ObjectFactory objFactory = new ObjectFactory();

        UsernameTokenType userNameToken = objFactory.createUsernameTokenType();
        AttributedString user = objFactory.createAttributedString();
        user.setValue(userName);
        userNameToken.setUsername(user);

        if (passwd != null) {
            // If the password is not specified (i.e. requesting a solution
            // token)
            // do not create the password element
            PasswordString pass = objFactory.createPasswordString();
            pass.setValue(passwd);

            userNameToken.setPassword(pass);
        }
        return objFactory.createUsernameToken(userNameToken);
    }

    @Override
    public boolean handleMessage(SOAPMessageContext smc) {
        if (SoapUtils.isOutgoingMessage(smc)) {
            try {
                Node securityNode = SoapUtils.getSecurityElement(SoapUtils.getSOAPHeader(smc));
                Node usernameNode =
                        SoapUtils.marshallJaxbElement(createUsernameToken()).getDocumentElement();
                securityNode.appendChild(securityNode.getOwnerDocument().importNode(usernameNode, true));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }
}
