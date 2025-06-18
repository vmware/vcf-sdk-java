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
import static com.vmware.sdk.ssoclient.utils.WsTrustAuthenticator.acquireHokTokenWithUserCredentials;
import static com.vmware.sdk.ssoclient.utils.WsTrustAuthenticator.isTokenValid;
import static com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer.DEFAULT_PORT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.vmware.sdk.samples.helpers.SecurityUtil;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.ssoclient.utils.SoapUtils;
import com.vmware.sdk.utils.ssl.InsecureTrustManager;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;

/**
 * This sample illustrates validating a SAML token received from SSO server. This sample in turn uses the
 * {@link AcquireHoKTokenByUserCredentialSample} and {@link AcquireBearerTokenByUserCredentialSample} samples using the
 * generated key/certificate pair at the runtime to first acquire a token, which are then used to validate
 */
public class ValidateTokenSample {
    private static final Logger log = LoggerFactory.getLogger(ValidateTokenSample.class);
    /** REQUIRED: The hostname of the vCenter server. */
    public static String vcHostname = "vcHostname";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";

    public static void main(String[] args) {
        SampleCommandLineParser.load(ValidateTokenSample.class, args);

        SimpleHttpConfigurer portConfigurer = new SimpleHttpConfigurer(new InsecureTrustManager());

        log.info("Acquiring bearer token");

        Element token =
                acquireBearerTokenForRegularUser(vcHostname, DEFAULT_PORT, portConfigurer, username, password, null);
        log.info("Bearer Token: {}", SoapUtils.samlTokenToString(token));
        log.info("Validating the acquired token");
        log.info(
                "The bearer token is {}",
                (isTokenValid(vcHostname, DEFAULT_PORT, portConfigurer, token) ? "valid" : "invalid"));

        log.info("Acquiring HoK token using");

        /*
         * A pre-generated self-signed certificate and private key pair to
         * be used in the sample. This is to be used for ONLY development
         * purpose.
         */
        SecurityUtil securityUtil = SecurityUtil.loadFromDefaultFiles();
        token = acquireHokTokenWithUserCredentials(
                vcHostname,
                DEFAULT_PORT,
                portConfigurer,
                username,
                password,
                securityUtil.getPrivateKey(),
                securityUtil.getUserCert(),
                null);
        log.info("HOK Token: {}", SoapUtils.samlTokenToString(token));
        log.info("Validating the acquired token");

        /*
         * Validating a Holder-Of-Key token using the
         * AcquireHoKTokenByUserCredentialSample sample
         */
        log.info(
                "The HoK token is {}",
                (isTokenValid(vcHostname, DEFAULT_PORT, portConfigurer, token) ? "valid" : "invalid"));
    }
}
