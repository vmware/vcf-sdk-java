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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;

import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appliance.system.Version;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.wstrust.AbstractHokTokenAuthenticator;
import com.vmware.sdk.vsphere.utils.wstrust.HokTokenAuthenticator;
import com.vmware.sdk.vsphere.utils.wstrust.HokTokenForTokenAuthenticator;
import com.vmware.vim25.VimPortType;

/**
 * This example demonstrates how to perform STS authentication, log into vCenter and use the authenticated
 * {@link VimPortType} and {@link Version} bindings to retrieve the server time and version. The former uses th
 * WSDL-based APIs, while the latter uses the vCenter REST APIs.
 *
 * <p>The first authentication issues a brand new HoK token. The second one demonstrates how to obtain a SAML token
 * ({@link AbstractHokTokenAuthenticator#login()}) and how to use it to issue a secondary token.
 *
 * <p>This sample requires a single vCenter and should work on all versions.
 */
public class DomainUserHokTokenAuthenticationExample {

    private static final Logger log = LoggerFactory.getLogger(DomainUserHokTokenAuthenticationExample.class);

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

    /** REQUIRED: A PKCS12 keystore which contains a single entry - a certificate and its corresponding private key. */
    public static String userKeystore = "path/to/keystore.p12";

    /** REQUIRED: Password for {@link #userKeystore}. */
    public static String userKeystorePassword = "password";

    public static void main(String[] args) throws Exception {

        SampleCommandLineParser.load(DomainUserHokTokenAuthenticationExample.class, args);

        KeyStore userKeyStore = loadKeystore();
        PrivateKey privateKey = getPrivateKey(userKeyStore);
        X509Certificate certificate = getCertificate(userKeyStore);

        // This authenticator is going to log into the STS & acquire an HoK token.
        AbstractHokTokenAuthenticator authenticator = new HokTokenAuthenticator(
                serverAddress,
                DEFAULT_PORT,
                new SimpleHttpConfigurer(createTrustManagerFromFile(trustStorePath)),
                username,
                password,
                privateKey,
                certificate,
                null);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        // Create configured and authenticated clients, ready for API invocations.
        try (VcenterClient client = factory.createClient(authenticator)) {
            fetchTimeAndVersion(client);
        }

        // Create a new authenticator & new clients and exercise the APIs.
        authenticator = new HokTokenForTokenAuthenticator(
                serverAddress,
                DEFAULT_PORT,
                authenticator.getPortConfigurer(),
                authenticator.login(),
                privateKey,
                certificate,
                null);

        try (VcenterClient client = factory.createClient(authenticator)) {
            fetchTimeAndVersion(client);
        }
    }

    private static void fetchTimeAndVersion(VcenterClient vimClient) {
        try {
            VimPortType vimPort = vimClient.getVimPort();

            XMLGregorianCalendar time = vimPort.currentTime(getVimServiceInstanceRef());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd 'T' HH:mm:ss.SSSZ");
            log.info(
                    "Server current time: {}",
                    sdf.format(time.toGregorianCalendar().getTime()));

            Version version = vimClient.createStub(Version.class);
            log.info("Server version: {}", version.get().getVersion());
        } catch (Exception e) {
            log.error("An unexpected error occurred", e);
        }
    }

    private static KeyStore loadKeystore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = Files.newInputStream(Paths.get(userKeystore))) {
            keyStore.load(is, userKeystorePassword.toCharArray());
        }

        return keyStore;
    }

    private static PrivateKey getPrivateKey(KeyStore keyStore) throws Exception {
        String keystoreAlias = keyStore.aliases().nextElement();
        return (PrivateKey) keyStore.getKey(keystoreAlias, userKeystorePassword.toCharArray());
    }

    private static X509Certificate getCertificate(KeyStore keyStore) throws Exception {
        String keystoreAlias = keyStore.aliases().nextElement();
        return (X509Certificate) keyStore.getCertificate(keystoreAlias);
    }
}
