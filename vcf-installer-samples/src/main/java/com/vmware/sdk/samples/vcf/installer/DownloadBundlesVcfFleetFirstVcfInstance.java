/*
 * ******************************************************************
 * Copyright (c) 2025-2026 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * The term "Broadcom" refers to Broadcom Inc.
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
 * Demonstrates how to configure online depot and download bundles necessary for deploying a new VCF Fleet with its
 * first VCF Instance. This includes the following components: VCF Operations, VCF
 * Operations Collector, VCF Automation, vCenter, NSX, SDDC Manager, VSP platform, VCF fleet lifecycle management,
 * VCF fleet SDDC lifecycle management, SALT raas vmsp component, SALT master vmsp component,
 * Telemetry acceptor component, Fleet depot service component, VIDB component, License server component.
 */
public class DownloadBundlesVcfFleetFirstVcfInstance {
    private static final Logger log = LoggerFactory.getLogger(DownloadBundlesVcfFleetFirstVcfInstance.class);

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
        SampleCommandLineParser.load(DownloadBundlesVcfFleetFirstVcfInstance.class, args);

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

            List<ProductReleaseComponent> latestProductReleaseComponents =
                    downloadLatestBundlesUtil.getLatestProductReleaseComponents(
                            "VCF",
                            versionWithoutBuildNumber,
                            Set.of(
                                // VCF Services Platform
                                "VSP",
                                // VCF fleet lifecycle management
                                "VCF_FLEET_LCM",
                                // VCF fleet SDDC lifecycle management
                                "VCF_SDDC_LCM",
                                // SALT raas vmsp component
                                "VCF_SALT_RAAS",
                                // SALT master vmsp component
                                "VCF_SALT",
                                // Telemetry acceptor component
                                "TELEMETRY_ACCEPTOR",
                                // Fleet depot service component
                                "DEPOT_SERVICE",
                                // VCF Identity Broker component
                                "VIDB",
                                // Migration service engine
                                "VCF_SERVICE_VCD_MIGRATION_BACKEND",
                                // License server component
                                "VCF_LICENSE_SERVER",
                                // VCF Operations
                                "VROPS",
                                // VCF Operations Collector
                                "VCF_OPS_CLOUD_PROXY",
                                // VCF Automation
                                "VRA",
                                // Components that comprise a VCF Instance
                                "VCENTER",
                                "NSX_T_MANAGER",
                                "SDDC_MANAGER"));
            log.info("Retrieved product release components");

            List<String> bundleIdsBeingDownloaded =
                    downloadLatestBundlesUtil.startBundlesDownload(latestProductReleaseComponents);
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
