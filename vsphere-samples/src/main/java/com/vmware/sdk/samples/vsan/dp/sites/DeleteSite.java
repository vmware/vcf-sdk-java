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
 * Demonstrates deleting a site.
 *
 * <p>Sample Prerequisites: vCenter 8.0.3+
 */
public class DeleteSite {
    private static final Logger log = LoggerFactory.getLogger(DeleteSite.class);

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

    /** REQUIRED: Vc user. */
    public static String vcUser = "vc-user";
    /** REQUIRED: Vc password. */
    public static String vcPassword = "vc-password";
    /** REQUIRED: The site ID to query. */
    public static String siteId = "site-id-to-delete";

    public static void main(String[] args) throws InterruptedException {
        SampleCommandLineParser.load(DeleteSite.class, args);

        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);
        SimpleHttpConfigurer portConfigurer = new SimpleHttpConfigurer(createTrustManagerFromFile(trustStorePath));
        HttpConfiguration httpConfiguration =
                createDefaultHttpConfiguration(keyStore).getConfig();

        SnapshotServiceClient snapshotServiceClient = new SnapshotServiceClient(
                snapServiceAddress, httpConfiguration, serverAddress, portConfigurer, username, password);

        SitesTypes.DeleteSpec spec = new SitesTypes.DeleteSpec();
        SitesTypes.UserCredentials vcenterCreds = new SitesTypes.UserCredentials();
        vcenterCreds.setUser(vcUser);
        vcenterCreds.setPassword(vcPassword.toCharArray());
        spec.setVcenterCreds(vcenterCreds);

        SitesTypes.DeleteOptions options = new SitesTypes.DeleteOptions();
        options.setExcludeHms(Boolean.FALSE);

        String deleteSiteTaskId = deleteTask(snapshotServiceClient, siteId, spec, options);

        log.info("deleteSiteTaskId: {}", deleteSiteTaskId);

        waitForSnapshotServiceTask(snapshotServiceClient, deleteSiteTaskId);
    }

    public static String deleteTask(
            SnapshotServiceClient snapshotServiceClient,
            String siteId,
            SitesTypes.DeleteSpec spec,
            SitesTypes.DeleteOptions options) {
        Sites sites = snapshotServiceClient.createStub(Sites.class);
        return sites.delete_Task(siteId, spec, options);
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
