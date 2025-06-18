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

import static com.vmware.sdk.ssoclient.utils.WsTrustAuthenticator.acquireHokToken;
import static com.vmware.sdk.ssoclient.utils.WsTrustAuthenticator.acquireHokTokenWithUserCredentials;
import static com.vmware.sdk.utils.ssl.vapi.HttpConfigHelper.DEFAULT_PORT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.vmware.sdk.samples.helpers.SecurityUtil;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.ssoclient.utils.SoapUtils;
import com.vmware.sdk.utils.ssl.InsecureTrustManager;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;

/**
 * This sample illustrates acquiring a Holder-Of-Key token from SSO server by passing an existing Holder-Of-Key token. A
 * sample certificate/private key is used to first acquire a Holder-Of-Key token which is then used to obtain another
 * Holder-Of-Key token
 */
public class AcquireHoKTokenByHoKTokenSample {
    private static final Logger log = LoggerFactory.getLogger(AcquireHoKTokenByHoKTokenSample.class);
    /** REQUIRED: The hostname of the vCenter server. */
    public static String vcHostname = "vcHostname";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";

    public static void main(String[] args) {
        SampleCommandLineParser.load(AcquireHoKTokenByHoKTokenSample.class, args);

        log.info("Acquiring a HoK token by using another HoK token acquired using user credentials");

        SimpleHttpConfigurer portConfigurer = new SimpleHttpConfigurer(new InsecureTrustManager());

        /*
         * A pre-generated self-signed certificate and private key pair to
         * be used in the sample. This is to be used for ONLY development
         * purpose.
         */
        SecurityUtil securityUtil = SecurityUtil.loadFromDefaultFiles();

        /* Acquire a HoK token using username & password */
        Element originalToken = acquireHokTokenWithUserCredentials(
                vcHostname,
                DEFAULT_PORT,
                portConfigurer,
                username,
                password,
                securityUtil.getPrivateKey(),
                securityUtil.getUserCert(),
                null);
        log.info("Original token issued: {}", SoapUtils.samlTokenToString(originalToken));

        Element additionalToken = acquireHokToken(
                vcHostname,
                DEFAULT_PORT,
                portConfigurer,
                originalToken,
                securityUtil.getPrivateKey(),
                securityUtil.getUserCert(),
                null);
        log.info("Additional token issued: {}", SoapUtils.samlTokenToString(additionalToken));
    }
}
