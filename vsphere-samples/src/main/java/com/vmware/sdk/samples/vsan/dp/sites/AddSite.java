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
import com.vmware.snapservice.Tasks;
import com.vmware.snapservice.tasks.Info;
import com.vmware.snapservice.tasks.Status;
import com.vmware.vapi.protocol.HttpConfiguration;

/**
 * Demonstrates adding a site.
 *
 * <p>Sample Prerequisites: vCenter 8.0.3+
 */
public class AddSite {
    private static final Logger log = LoggerFactory.getLogger(AddSite.class);
    /** REQUIRED: vCenter FQDN or IP address. */
    public static String serverAddress = "vcenter1.mycompany.com";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";
    /** REQUIRED: snapshot service FQDN or IP address. */
    public static String snapServiceAddress = "snapServiceAddress";
    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (do not leave it empty on production environments).
     */
    public static String trustStorePath = null;

    /** REQUIRED: Remote vc host. */
    public static String remoteVcHost = "remote-vc-host";
    /** REQUIRED: Remote vc user. */
    public static String remoteVcUser = "remote-vc-user";
    /** REQUIRED: Remote vc password. */
    public static String remoteVcPassword = "remote-vc-password";

    public static void main(String[] args) throws InterruptedException {
        SampleCommandLineParser.load(AddSite.class, args);

        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);
        SimpleHttpConfigurer portConfigurer = new SimpleHttpConfigurer(createTrustManagerFromFile(trustStorePath));
        HttpConfiguration httpConfiguration =
                createDefaultHttpConfiguration(keyStore).getConfig();

        SnapshotServiceClient snapshotServiceClient = new SnapshotServiceClient(
                snapServiceAddress, httpConfiguration, serverAddress, portConfigurer, username, password);

        SitesTypes.ProbeResult probeResult =
                ProbeConnection.probe(snapshotServiceClient, remoteVcHost, 443L, remoteVcUser, remoteVcPassword, null);
        log.info("Probe result: {}", probeResult);

        log.info("Continue to probe with vc cert.");
        SitesTypes.ProbeResult probeWithVcCertResult = ProbeConnection.probe(
                snapshotServiceClient,
                remoteVcHost,
                443L,
                remoteVcUser,
                remoteVcPassword,
                probeResult.getVcenterCertificate().getCertificate());
        log.info("Probe with vc cert result: {}", probeWithVcCertResult);

        log.info("Continue to add site.");

        String addSiteTaskId = AddSite.addTask(
                snapshotServiceClient,
                remoteVcHost,
                443L,
                remoteVcUser,
                remoteVcPassword,
                probeResult.getVcenterCertificate().getCertificate(),
                probeWithVcCertResult.getVaCertificate().getCertificate());
        log.info("addSiteTaskId: {}", addSiteTaskId);

        waitForSnapshotServiceTask(snapshotServiceClient, addSiteTaskId);
    }

    public static String addTask(
            SnapshotServiceClient snapshotServiceClient,
            String remoteVcHost,
            Long remoteVcPort,
            String remoteVcUser,
            String remoteVcPassword,
            String vcenterCertificate,
            String vaCertificate) {
        Sites sites = snapshotServiceClient.createStub(Sites.class);

        SitesTypes.AddSpec spec = new SitesTypes.AddSpec();

        SitesTypes.VcenterConnectionSpec vcenterConnectionSpec = new SitesTypes.VcenterConnectionSpec();
        vcenterConnectionSpec.setHost(remoteVcHost);
        vcenterConnectionSpec.setPort(remoteVcPort);
        spec.setVcenterConnectionSpec(vcenterConnectionSpec);

        SitesTypes.UserCredentials vcenterCreds = new SitesTypes.UserCredentials();
        vcenterCreds.setUser(remoteVcUser);
        vcenterCreds.setPassword(remoteVcPassword.toCharArray());
        spec.setVcenterCreds(vcenterCreds);

        spec.setVcenterCertificate(vcenterCertificate);

        spec.setVaCertificate(vaCertificate);

        return sites.add_Task(spec);
    }

    private static void waitForSnapshotServiceTask(SnapshotServiceClient snapshotServiceClient, String ssTaskId)
            throws InterruptedException {
        Tasks tasks = snapshotServiceClient.createStub(Tasks.class);
        while (true) {
            Info taskInfo = tasks.get(ssTaskId);

            if (taskInfo.getStatus() == Status.SUCCEEDED) {
                log.info("# Task {} succeeds: {}", ssTaskId, taskInfo);
                return;
            } else if (taskInfo.getStatus() == Status.FAILED) {
                log.error("# Task {} failed.", taskInfo.getDescription().getId());
                log.error("Error: {}", taskInfo.getError().getMessage());
                return;
            } else {
                log.info(
                        "# Task {} progress: {}",
                        taskInfo.getDescription().getId(),
                        taskInfo.getProgress().getCompleted());
                java.util.concurrent.TimeUnit.SECONDS.sleep(5);
            }
        }
    }
}
