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
import static com.vmware.sdk.utils.ssl.vapi.HttpConfigHelper.DEFAULT_PORT;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.vmware.sdk.samples.helpers.SecurityUtil;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.ssoclient.utils.SoapUtils;
import com.vmware.sdk.utils.ssl.InsecureTrustManager;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;

/**
 * This sample illustrates acquiring a Holder-Of-Key token from SSO server by passing the username and password of the
 * user along with using the user's private key and certificate
 */
public class AcquireHoKTokenByUserCredentialSample {
    private static final Logger log = LoggerFactory.getLogger(AcquireHoKTokenByUserCredentialSample.class);
    /** REQUIRED: The hostname of the vCenter server. */
    public static String vcHostname = "vcHostname";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";

    public static void main(String[] args) {
        SampleCommandLineParser.load(AcquireHoKTokenByUserCredentialSample.class, args);

        SecurityUtil securityUtil = SecurityUtil.loadFromDefaultFiles();

        log.info("Acquiring a HoK token by using user credentials, use the pre-generated private key and certificate");

        PrivateKey privateKey = securityUtil.getPrivateKey();
        X509Certificate certificate = securityUtil.getUserCert();

        SimpleHttpConfigurer stsPortConfigurer = new SimpleHttpConfigurer(new InsecureTrustManager());

        Element token = acquireHokTokenWithUserCredentials(
                vcHostname, DEFAULT_PORT, stsPortConfigurer, username, password, privateKey, certificate, null);
        log.info("HoK token: {}", SoapUtils.samlTokenToString(token));
    }
}
