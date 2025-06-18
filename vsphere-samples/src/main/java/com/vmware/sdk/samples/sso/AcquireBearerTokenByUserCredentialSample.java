/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.sso;

import static com.vmware.sdk.ssoclient.utils.WsTrustAuthenticator.acquireBearerTokenForRegularUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.ssoclient.utils.SoapUtils;
import com.vmware.sdk.utils.ssl.InsecureTrustManager;
import com.vmware.sdk.utils.ssl.vapi.HttpConfigHelper;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;

/** This sample illustrates acquiring a bearer token from SSO server by passing the username and password of the user */
public class AcquireBearerTokenByUserCredentialSample {
    private static final Logger log = LoggerFactory.getLogger(AcquireBearerTokenByUserCredentialSample.class);
    /** REQUIRED: The hostname of the vCenter server. */
    public static String vcHostname = "vcHostname";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";

    public static void main(String[] args) {
        SampleCommandLineParser.load(AcquireBearerTokenByUserCredentialSample.class, args);

        SimpleHttpConfigurer portConfigurer = new SimpleHttpConfigurer(new InsecureTrustManager());

        log.info("Acquiring a bearer token by using user credentials");
        Element token = acquireBearerTokenForRegularUser(
                vcHostname, HttpConfigHelper.DEFAULT_PORT, portConfigurer, username, password, null);

        log.info("Bearer token: {}", SoapUtils.samlTokenToString(token));
    }
}
