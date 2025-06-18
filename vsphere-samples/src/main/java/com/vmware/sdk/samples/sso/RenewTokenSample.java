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

import static com.vmware.sdk.ssoclient.utils.WsTrustAuthenticator.acquireHokTokenWithUserCredentials;
import static com.vmware.sdk.ssoclient.utils.WsTrustAuthenticator.renewToken;
import static com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer.DEFAULT_PORT;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.vmware.sdk.samples.helpers.SecurityUtil;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.ssoclient.utils.SoapUtils;
import com.vmware.sdk.utils.ssl.InsecureTrustManager;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;

/**
 * This sample illustrates renewing a Holder-Of-Key token from SSO server. The pre-generated key/certificate pair is
 * used to first acquire a Holder-Of-Key token and then it is renewed.
 */
public class RenewTokenSample {
    private static final Logger log = LoggerFactory.getLogger(RenewTokenSample.class);
    /** REQUIRED: The hostname of the vCenter server. */
    public static String vcHostname = "vcHostname";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";

    /** REQUIRED: Token duration in seconds. */
    public static long durationInSeconds = 0L;

    public static void main(String[] args) {
        SampleCommandLineParser.load(RenewTokenSample.class, args);

        Duration tokenDuration = Duration.ofSeconds(durationInSeconds);

        log.info("Acquiring a HoK token using user credentials");

        SimpleHttpConfigurer stsPortConfigurer = new SimpleHttpConfigurer(new InsecureTrustManager());

        /*
         * A pre-generated self-signed certificate and private key pair to
         * be used in the sample. This is to be used for ONLY development
         * purpose.
         */
        SecurityUtil securityUtil = SecurityUtil.loadFromDefaultFiles();

        /* Acquire a HoK token using username & password */
        Element token = acquireHokTokenWithUserCredentials(
                vcHostname,
                DEFAULT_PORT,
                stsPortConfigurer,
                username,
                password,
                securityUtil.getPrivateKey(),
                securityUtil.getUserCert(),
                tokenDuration);

        log.info("Original token issued: {}", SoapUtils.samlTokenToString(token));

        log.info("Renewing the token...");
        Element renewedToken = renewToken(
                vcHostname,
                DEFAULT_PORT,
                stsPortConfigurer,
                token,
                securityUtil.getPrivateKey(),
                securityUtil.getUserCert(),
                tokenDuration);

        log.info("Renewed token: {}", SoapUtils.samlTokenToString(renewedToken));
    }
}
