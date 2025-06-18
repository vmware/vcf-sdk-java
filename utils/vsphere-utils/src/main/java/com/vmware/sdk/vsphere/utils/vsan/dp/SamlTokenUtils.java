/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.vsphere.utils.vsan.dp;

import static com.vmware.sdk.ssoclient.utils.WsTrustAuthenticator.acquireBearerTokenForRegularUser;

import java.time.Duration;
import java.util.Objects;

import org.w3c.dom.Element;

import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;
import com.vmware.vapi.saml.DefaultTokenFactory;
import com.vmware.vapi.saml.SamlToken;
import com.vmware.vapi.saml.exception.InvalidTokenException;

public class SamlTokenUtils {

    public static SamlToken getSamlToken(
            String vcServer,
            int vcPort,
            SimpleHttpConfigurer portConfigurer,
            String username,
            String password,
            Duration tokenLifetime) {
        Objects.requireNonNull(vcServer);
        Objects.requireNonNull(portConfigurer);
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);

        Element tokenElement =
                acquireBearerTokenForRegularUser(vcServer, vcPort, portConfigurer, username, password, tokenLifetime);

        SamlToken samlBearerToken;
        try {
            samlBearerToken = DefaultTokenFactory.createTokenFromDom(tokenElement);
        } catch (InvalidTokenException e) {
            throw new RuntimeException(e);
        }
        return samlBearerToken;
    }
}
