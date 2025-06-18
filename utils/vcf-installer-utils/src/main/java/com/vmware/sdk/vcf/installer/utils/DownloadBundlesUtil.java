/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.vcf.installer.utils;

import static java.util.Objects.requireNonNullElse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.vcf.installer.model.BundleDownloadSpec;
import com.vmware.sdk.vcf.installer.model.BundleDownloadStatusInfo;
import com.vmware.sdk.vcf.installer.model.BundleUpdateSpec;
import com.vmware.sdk.vcf.installer.model.CustomPatch;
import com.vmware.sdk.vcf.installer.model.CustomPatchBundleInfo;
import com.vmware.sdk.vcf.installer.model.DepotAccount;
import com.vmware.sdk.vcf.installer.model.DepotSettings;
import com.vmware.sdk.vcf.installer.model.ProductReleaseComponent;
import com.vmware.sdk.vcf.installer.model.ReleaseComponentDetail;
import com.vmware.sdk.vcf.installer.v1.Bundles;
import com.vmware.sdk.vcf.installer.v1.bundles.DownloadStatus;
import com.vmware.sdk.vcf.installer.v1.releases.ReleaseComponents;
import com.vmware.sdk.vcf.installer.v1.system.settings.Depot;
import com.vmware.vapi.client.ApiClient;

/** Demonstrates how to configure online depot and download bundles. */
public class DownloadBundlesUtil {

    private static final Logger log = LoggerFactory.getLogger(DownloadBundlesUtil.class);

    public static int DEFAULT_DEPOT_SYNC_TIMEOUT_MINS = 5;
    public static int DEFAULT_WAIT_BETWEEN_POLLS_DEPOT_SYNC_SEC = 5;
    public static int DEFAULT_POLL_DOWNLOAD_STATUS_TIMEOUT_HOURS = 24;
    public static int DEFAULT_WAIT_BETWEEN_POLLS_DOWNLOAD_STATUS_SEC = 60;

    protected static final String IMAGE_TYPE = "INSTALL";

    protected static final Set<String> START_BUNDLE_DOWNLOAD_ERRORS_TO_IGNORE = Set.of(
            // happens when download has already been initiated, but download has not started yet
            "BUNDLE_DOWNLOAD_ALREADY_SCHEDULED",
            // happens when bundle has already been downloaded
            "BUNDLE_DOWNLOAD_ALREADY_DOWNLOADED",
            // happens when bundle is being validated
            "BUNDLE_DOWNLOAD_NOT_AVAILABLE");

    protected ApiClient client;

    /** @param client Already initialized and logged in client */
    public DownloadBundlesUtil(ApiClient client) {
        this.client = client;
    }

    public void configureOnlineDepot(String depotAccountUsername, String depotAccountPassword)
            throws ExecutionException, InterruptedException {
        Depot depotApi = client.createStub(Depot.class);

        DepotAccount depotAccount = new DepotAccount();
        depotAccount.setUsername(depotAccountUsername);
        depotAccount.setPassword(depotAccountPassword);

        DepotSettings depotSettings = new DepotSettings();
        depotSettings.setVmwareAccount(depotAccount);

        depotApi.updateDepotSettings(depotSettings).invoke().get();
    }

    public boolean isDepotSynced() {
        final com.vmware.sdk.vcf.installer.model.DepotSyncInfo syncInfo;
        try {
            com.vmware.sdk.vcf.installer.v1.system.settings.depot.DepotSyncInfo depotSyncInfoApi =
                    client.createStub(com.vmware.sdk.vcf.installer.v1.system.settings.depot.DepotSyncInfo.class);

            syncInfo = depotSyncInfoApi.getDepotSyncInfo().invoke().get();
        } catch (Exception e) {
            log.warn("Caught exception while trying to retrieve depot sync info.", e);
            return false;
        }

        if (syncInfo.getSyncStatus().equals("SYNC_FAILED")) {
            throw new RuntimeException("Syncing depot failed");
        }

        boolean synced = syncInfo.getSyncStatus().equals("SYNCED");
        if (syncInfo.getSyncStatus().equals("SYNCED")) {
            log.info("Syncing depot succeeded");
        }

        return synced;
    }

    /**
     * Sync the depot and wait until the operation is complete
     *
     * @param timeToWaitForDepotSyncInMinutes Optional - if not provided will default to
     *     {@link #DEFAULT_DEPOT_SYNC_TIMEOUT_MINS}
     * @param timeToWaitInBetweenPollsForDepotSyncInSeconds Optional - if not provided will default to
     *     {@link #DEFAULT_WAIT_BETWEEN_POLLS_DEPOT_SYNC_SEC}
     * @throws ExecutionException if the operation failed
     * @throws InterruptedException if the client is interrupted while waiting
     */
    public void forceSyncDepot(
            Integer timeToWaitForDepotSyncInMinutes, Integer timeToWaitInBetweenPollsForDepotSyncInSeconds)
            throws InterruptedException, ExecutionException {
        int timeoutInMins = requireNonNullElse(timeToWaitForDepotSyncInMinutes, DEFAULT_DEPOT_SYNC_TIMEOUT_MINS);
        int pollIntervalInSec = requireNonNullElse(
                timeToWaitInBetweenPollsForDepotSyncInSeconds, DEFAULT_WAIT_BETWEEN_POLLS_DEPOT_SYNC_SEC);

        com.vmware.sdk.vcf.installer.v1.system.settings.depot.DepotSyncInfo depotSyncInfoApi =
                client.createStub(com.vmware.sdk.vcf.installer.v1.system.settings.depot.DepotSyncInfo.class);
        depotSyncInfoApi.syncDepotMetadata().invoke().get();

        log.info("Polling depot sync.");

        MiscUtil.poll(TimeUnit.MINUTES.toSeconds(timeoutInMins), pollIntervalInSec, this::isDepotSynced);
    }

    public List<ProductReleaseComponent> getLatestProductReleaseComponents(
            String sku, String releaseVersion, Set<String> productReleaseComponentsNamesToInclude)
            throws ExecutionException, InterruptedException {
        ReleaseComponents releaseComponentsApi = client.createStub(ReleaseComponents.class);

        List<ReleaseComponentDetail> releaseComponentDetails = releaseComponentsApi
                .getReleaseComponentsBySku(sku)
                .releaseVersion(releaseVersion)
                .imageType(DownloadBundlesUtil.IMAGE_TYPE)
                .automatedInstall(true)
                .invoke()
                .get()
                .getElements();

        if (releaseComponentDetails.isEmpty()) {
            throw new RuntimeException("No release component details were returned.");
        }

        ReleaseComponentDetail latestReleaseComponentDetail = releaseComponentDetails.get(0);

        return latestReleaseComponentDetail.getComponents().stream()
                .filter(c -> productReleaseComponentsNamesToInclude.contains(c.getName()))
                .collect(Collectors.toList());
    }

    public List<String> startBundlesDownload(List<ProductReleaseComponent> releaseComponents)
            throws InterruptedException {
        Bundles bundlesApi = client.createStub(Bundles.class);

        BundleDownloadSpec bundleDownloadSpec = new BundleDownloadSpec();
        bundleDownloadSpec.setDownloadNow(true);

        BundleUpdateSpec bundleUpdateSpec = new BundleUpdateSpec();
        bundleUpdateSpec.setBundleDownloadSpec(bundleDownloadSpec);

        List<String> bundleIdsBeingDownloaded = new ArrayList<>();

        for (ProductReleaseComponent component : releaseComponents) {
            CustomPatch latest = component.getVersions().get(0);

            List<String> ids = latest.getArtifacts().getBundles().stream()
                    .map(CustomPatchBundleInfo::getId)
                    .collect(Collectors.toList());

            for (String id : ids) {
                try {
                    bundlesApi
                            .startBundleDownloadByID(id, bundleUpdateSpec)
                            .invoke()
                            .get();

                    bundleIdsBeingDownloaded.add(id);
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof com.vmware.sdk.vcf.installer.model.Error) {
                        com.vmware.sdk.vcf.installer.model.Error vcfInstallerError =
                                (com.vmware.sdk.vcf.installer.model.Error) e.getCause();

                        if (START_BUNDLE_DOWNLOAD_ERRORS_TO_IGNORE.contains(vcfInstallerError.getErrorCode())) {
                            log.info(
                                    "Not downloading bundle with name: '{}' and id: '{}' as download has already been initiated previously.",
                                    component.getName(),
                                    id);

                            bundleIdsBeingDownloaded.add(id);
                        } else {
                            log.error(
                                    "Failed to download bundle with name: '{}' and id: '{}'", component.getName(), id);

                            throw vcfInstallerError;
                        }
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return bundleIdsBeingDownloaded;
    }

    public boolean areBundlesDownloaded(List<String> bundleIdsBeingDownloaded, String releaseVersion) {
        final List<BundleDownloadStatusInfo> bundleDownloadStatusInfos;
        try {
            DownloadStatus downloadStatusApi = client.createStub(DownloadStatus.class);

            bundleDownloadStatusInfos = downloadStatusApi
                    .getBundleDownloadStatus()
                    .releaseVersion(releaseVersion)
                    .imageType(IMAGE_TYPE)
                    .invoke()
                    .get()
                    .getElements();
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Caught exception while retrieving download status", e);
            return false;
        }

        boolean allDownloaded = true;
        for (BundleDownloadStatusInfo statusInfo : bundleDownloadStatusInfos) {
            if (bundleIdsBeingDownloaded.contains(statusInfo.getBundleId())) {
                if (statusInfo.getDownloadStatus().equals("FAILED")) {
                    throw new RuntimeException("Failed to download bundle with id: " + statusInfo.getBundleId());
                } else if (statusInfo.getDownloadStatus().equals("CANCELLED")) {
                    throw new RuntimeException(
                            "Download bundle with id: " + statusInfo.getBundleId() + " has been cancelled.");
                } else if (!statusInfo.getDownloadStatus().equals("SUCCESS")) {
                    allDownloaded = false;
                    break;
                }
            }
        }

        if (allDownloaded) {
            log.info("All bundles have been successfully downloaded.");
            return true;
        } else {
            return false;
        }
    }

    /**
     * Polls until bundles are downloaded or timeout is reached in which case an exception is thrown.
     *
     * @param bundleIdsBeingDownloaded bundles to check
     * @param releaseVersion release version of the bundles to check
     * @param maxTimeToPollDownloadStatusInHours Optional - if not provided will default to
     *     {@link #DEFAULT_POLL_DOWNLOAD_STATUS_TIMEOUT_HOURS}
     * @param timeToSleepInBetweenPollsForDownloadStatusInSeconds Optional - if not provided will default to
     *     {@link #DEFAULT_WAIT_BETWEEN_POLLS_DOWNLOAD_STATUS_SEC}
     * @throws InterruptedException if the client is interrupted while waiting
     */
    public void pollBundlesDownloaded(
            List<String> bundleIdsBeingDownloaded,
            String releaseVersion,
            Integer maxTimeToPollDownloadStatusInHours,
            Integer timeToSleepInBetweenPollsForDownloadStatusInSeconds)
            throws InterruptedException {
        int pollTimeoutInHours =
                requireNonNullElse(maxTimeToPollDownloadStatusInHours, DEFAULT_POLL_DOWNLOAD_STATUS_TIMEOUT_HOURS);
        int pollIntervalInSec = requireNonNullElse(
                timeToSleepInBetweenPollsForDownloadStatusInSeconds, DEFAULT_WAIT_BETWEEN_POLLS_DOWNLOAD_STATUS_SEC);

        log.info("Polling download status.");

        MiscUtil.poll(
                TimeUnit.HOURS.toSeconds(pollTimeoutInHours),
                pollIntervalInSec,
                () -> areBundlesDownloaded(bundleIdsBeingDownloaded, releaseVersion));
    }
}
