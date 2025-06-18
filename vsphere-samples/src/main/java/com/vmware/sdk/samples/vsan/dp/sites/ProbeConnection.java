/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vsan.dp.sites;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.createTrustManagerFromFile;
import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.sdk.utils.ssl.vapi.HttpConfigHelper.createDefaultHttpConfiguration;

import java.security.KeyStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;
import com.vmware.sdk.vsphere.utils.vsan.dp.SnapshotServiceClient;
import com.vmware.snapservice.Sites;
import com.vmware.snapservice.SitesTypes;
import com.vmware.vapi.protocol.HttpConfiguration;

/**
 * Demonstrates probing a connection.
 *
 * <p>Sample Prerequisites: vCenter 8.0.3+
 */
public class ProbeConnection {
    private static final Logger log = LoggerFactory.getLogger(ProbeConnection.class);

    /** REQUIRED: vCenter FQDN or IP address. */
    public static String serverAddress = "vcenter1.mycompany.com";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";
    /** REQUIRED: Snapshot service FQDN or IP address. */
    public static String snapServiceAddress = "snapServiceAddress";
    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (do not leave it empty on production environments).
     */
    public static String trustStorePath = null;

    /** REQUIRED: MoRef ID of the cluster whose protection groups to inspect. */
    public static String remoteVcHost = "remote-vc-host";
    /** REQUIRED: MoRef ID of the cluster whose protection groups to inspect. */
    public static String remoteVcUser = "remote-vc-user";
    /** REQUIRED: MoRef ID of the cluster whose protection groups to inspect. */
    public static String remoteVcPassword = "remote-vc-password";
    /** OPTIONAL: Whether probe with vc cert or not. If provided, then probe with vc cert. */
    public static String withVcCert;

    public static void main(String[] args) {
        SampleCommandLineParser.load(ProbeConnection.class, args);

        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);
        SimpleHttpConfigurer portConfigurer = new SimpleHttpConfigurer(createTrustManagerFromFile(trustStorePath));
        HttpConfiguration httpConfiguration =
                createDefaultHttpConfiguration(keyStore).getConfig();

        SnapshotServiceClient snapshotServiceClient = new SnapshotServiceClient(
                snapServiceAddress, httpConfiguration, serverAddress, portConfigurer, username, password);

        SitesTypes.ProbeResult probeResult =
                ProbeConnection.probe(snapshotServiceClient, remoteVcHost, 443L, remoteVcUser, remoteVcPassword, null);
        log.info("Probe result: {}", probeResult);

        if (withVcCert != null && !withVcCert.isBlank()) {
            log.info("Continue to probe with vc cert.");
            SitesTypes.ProbeResult probeWithVcCertResult = ProbeConnection.probe(
                    snapshotServiceClient,
                    remoteVcHost,
                    443L,
                    remoteVcUser,
                    remoteVcPassword,
                    probeResult.getVcenterCertificate().getCertificate());
            log.info("Probe with vc cert result: {}", probeWithVcCertResult);
        }
    }

    public static SitesTypes.ProbeResult probe(
            SnapshotServiceClient snapshotServiceClient,
            String remoteVcHost,
            Long remoteVcPort,
            String remoteVcUser,
            String remoteVcPassword,
            String vcenterCertificate) {
        Sites sites = snapshotServiceClient.createStub(Sites.class);

        SitesTypes.ProbeSpec spec = new SitesTypes.ProbeSpec();

        SitesTypes.VcenterConnectionSpec vcenterConnectionSpec = new SitesTypes.VcenterConnectionSpec();
        vcenterConnectionSpec.setHost(remoteVcHost);
        vcenterConnectionSpec.setPort(remoteVcPort);
        spec.setVcenterConnectionSpec(vcenterConnectionSpec);

        SitesTypes.UserCredentials vcenterCreds = new SitesTypes.UserCredentials();
        vcenterCreds.setUser(remoteVcUser);
        vcenterCreds.setPassword(remoteVcPassword.toCharArray());
        spec.setVcenterCreds(vcenterCreds);

        spec.setVcenterCertificate(vcenterCertificate);

        return sites.probe(spec);
    }
}
