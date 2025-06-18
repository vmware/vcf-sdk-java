/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.authentication;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.createTrustManagerFromFile;
import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer.DEFAULT_PORT;
import static com.vmware.sdk.vsphere.utils.VimClient.getVimServiceInstanceRef;

import java.text.SimpleDateFormat;

import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appliance.system.Version;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.wstrust.BearerTokenAuthenticator;
import com.vmware.vim25.VimPortType;

/**
 * This example demonstrates how to perform STS authentication, log into vCenter and use the authenticated
 * {@link VimPortType} and {@link Version} bindings to retrieve the server time and version. The former uses th
 * WSDL-based APIs, while the latter uses the vCenter REST APIs.
 *
 * <p>This sample requires a single vCenter and should work on all versions.
 */
public class DomainUserBearerTokenAuthenticationExample {

    private static final Logger log = LoggerFactory.getLogger(DomainUserBearerTokenAuthenticationExample.class);

    /** REQUIRED: vCenter FQDN or IP address. */
    public static String serverAddress = "vcenter1.mycompany.com";

    /** REQUIRED: STS username used for authentication. */
    public static String username = "Administrator@vsphere.local";

    /** REQUIRED: Password for the {@link #username}. */
    public static String password = "password";

    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (DO NOT USE IN PRODUCTION ENVIRONMENTS).
     */
    public static String trustStorePath = null;

    public static void main(String[] args) throws Exception {

        SampleCommandLineParser.load(DomainUserBearerTokenAuthenticationExample.class, args);

        // This authenticator is going to log into the STS & acquire a Bearer token.
        BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(
                serverAddress,
                DEFAULT_PORT,
                new SimpleHttpConfigurer(createTrustManagerFromFile(trustStorePath)),
                username,
                password,
                null);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        // Create configured and authenticated clients, ready for API invocations.
        try (VcenterClient vimClient = factory.createClient(authenticator)) {
            VimPortType vimPort = vimClient.getVimPort();

            XMLGregorianCalendar time = vimPort.currentTime(getVimServiceInstanceRef());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd 'T' HH:mm:ss.SSSZ");
            log.info(
                    "Server current time: {}",
                    sdf.format(time.toGregorianCalendar().getTime()));

            Version version = vimClient.createStub(Version.class);
            log.info("Server version: {}", version.get().getVersion());
        }
    }
}
