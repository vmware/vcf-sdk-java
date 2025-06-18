/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcf.installer;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vcf.installer.model.ProductReleaseComponent;
import com.vmware.sdk.vcf.installer.utils.DownloadBundlesUtil;
import com.vmware.sdk.vcf.installer.utils.MiscUtil;
import com.vmware.sdk.vcf.installer.utils.VcfInstallerClientFactory;
import com.vmware.vapi.client.ApiClient;

/**
 * Demonstrates how to configure online depot and download bundles necessary for deploying VVF assuming you have
 * existing VCF Operations. This includes the following components: vCenter.
 */
public class DownloadBundlesVvfInstanceExistingVcfOps {
    private static final Logger log = LoggerFactory.getLogger(DownloadBundlesVvfInstanceExistingVcfOps.class);

    /** REQUIRED: VCF Installer host address or FQDN. */
    public static String vcfInstallerHostAddress = "vcf-installer.mycompany.com";

    /** REQUIRED: VCF Installer password for admin@local account. */
    public static String vcfInstallerAdminPassword = "admin@local-account-password";

    /** REQUIRED: Depot username for access. */
    public static String depotAccountUsername = "depot-account-username";

    /** REQUIRED: Depot password for access. */
    public static String depotAccountPassword = "depot-account-password";

    /** OPTIONAL: Path to the trust store on this machine. */
    public static String trustStorePath = null;

    /**
     * OPTIONAL: The maximum time that the depot sync status should be polled until it has completed, measured in
     * minutes.
     */
    public static Integer timeToWaitForDepotSyncInMinutes = null;

    /** OPTIONAL: The time to sleep in between each poll when polling the depot sync status, measured in seconds. */
    public static Integer timeToWaitInBetweenPollsForDepotSyncInSeconds = null;

    /**
     * OPTIONAL: The maximum time that the download status should be polled until all bundles have been successfully
     * downloaded, measured in hours.
     */
    public static Integer maxTimeToPollDownloadStatusInHours = null;

    /**
     * OPTIONAL: The time to sleep in between each poll when polling the download status of the bundles, measured in
     * seconds.
     */
    public static Integer timeToSleepInBetweenPollsForDownloadStatusInSeconds = null;

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        SampleCommandLineParser.load(DownloadBundlesVvfInstanceExistingVcfOps.class, args);

        VcfInstallerClientFactory clientFactory = new VcfInstallerClientFactory();

        try (ApiClient client = clientFactory.createClient(
                vcfInstallerHostAddress, vcfInstallerAdminPassword, loadKeystoreOrCreateEmpty(trustStorePath))) {
            DownloadBundlesUtil downloadLatestBundlesUtil = new DownloadBundlesUtil(client);

            downloadLatestBundlesUtil.configureOnlineDepot(depotAccountUsername, depotAccountPassword);
            log.info("Configured online depot");

            downloadLatestBundlesUtil.forceSyncDepot(
                    timeToWaitForDepotSyncInMinutes, timeToWaitInBetweenPollsForDepotSyncInSeconds);
            log.info("Synced online depot");

            String versionWithoutBuildNumber = MiscUtil.getVersionWithoutBuildNumber(client);

            List<ProductReleaseComponent> latestReleaseComponentsToDownload =
                    downloadLatestBundlesUtil.getLatestProductReleaseComponents(
                            "VVF", versionWithoutBuildNumber, Set.of("VCENTER"));
            log.info("Retrieved product release components");

            List<String> bundleIdsBeingDownloaded =
                    downloadLatestBundlesUtil.startBundlesDownload(latestReleaseComponentsToDownload);
            log.info("Started downloading all necessary bundles");

            downloadLatestBundlesUtil.pollBundlesDownloaded(
                    bundleIdsBeingDownloaded,
                    versionWithoutBuildNumber,
                    maxTimeToPollDownloadStatusInHours,
                    timeToSleepInBetweenPollsForDownloadStatusInSeconds);
            log.info("Downloaded all necessary bundles");

            log.info("Sample completed successfully");
        }
    }
}
