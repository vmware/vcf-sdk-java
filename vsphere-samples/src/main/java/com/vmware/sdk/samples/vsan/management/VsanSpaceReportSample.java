/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vsan.management;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.CLUSTER_COMPUTE_RESOURCE;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vmware.pbm.PbmPortType;
import com.vmware.pbm.PbmServiceInstanceContent;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.vsan.management.VsanManagedObjectsCatalog;
import com.vmware.sdk.vsphere.utils.vsan.management.VsanUtil;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VsanConfigInfoEx;
import com.vmware.vim25.VsanObjectSpaceSummary;
import com.vmware.vim25.VsanSpaceUsage;
import com.vmware.vim25.VsanSpaceUsageDetailResult;
import com.vmware.vsan.sdk.VsanhealthPortType;

/**
 * This sample demonstrates the vCenter side vSAN space reporting API QuerySpaceUsage. It shows how to get vSAN space
 * usage result, including the following types:
 *
 * <ul>
 *   <li>vSAN Effective Capacity Overview
 *   <li>vSAN Effective Space Usage Breakdown
 *   <li>vSAN Data Reduction (Deduplication / Compression) is Enabled
 *   <li>vSAN Standard Space Usage Overview
 *   <li>vSAN Standard Usage Breakdown View
 *   <li>...
 * </ul>
 *
 * <p>Sample Prerequisites: this sample requires a vCenter server version 9.1 or higher and a vSAN ESA enabled cluster.
 * <p>In addition, the "Apply Auto-RAID to all objects" is enabled in ESA cluster configuration.
 * <p>And the auto RAID policy is enabled on the default vSAN datastore storage policy.
 */
public class VsanSpaceReportSample {

    private static final Logger log = LoggerFactory.getLogger(VsanSpaceReportSample.class);

    /** REQUIRED: vCenter FQDN or IP address. */
    public static String serverAddress = "vcenter1.mycompany.com";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";
    /** REQUIRED: VC cluster name using in cluster health query API */
    public static String clusterName;
    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (do not leave it empty on production environments).
     */
    public static String trustStorePath = null;

    private static PropertyCollectorHelper propertyCollectorHelper;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VsanSpaceReportSample.class, args);

        VcenterClientFactory clientFactory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = clientFactory.createClient(username, password, null)) {
            ServiceContent serviceContent = client.getVimServiceContent();

            VimPortType vimPort = client.getVimPort();
            propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            var aboutInfo = serviceContent.getAbout();
            if (!aboutInfo.getApiType().equals("VirtualCenter")) {
                log.error("Host {} is not a VC host. Please run on VC", serviceContent);
            }

            var cluster = propertyCollectorHelper.getMoRefByName(clusterName, CLUSTER_COMPUTE_RESOURCE);
            if (cluster == null) {
                log.error("Cluster {} not found for {}", clusterName, serverAddress);
                return;
            }

            VsanhealthPortType vsanPort = client.getVsanPort();

            // Here is an example of how to get space reporting results by vSAN space reporting API.
            VsanSpaceUsage spaceResult = vsanPort.vsanQuerySpaceUsage(
                    VsanManagedObjectsCatalog.getVsanSpaceReportServiceInstanceReference(), cluster, null, false);

            if (spaceResult == null) {
                log.error("Space result is None for the given cluster {}", clusterName);
                return;
            }
            // The effective capacity will be reported with any of the below prerequisites satisfied on 90U1 or higher:
            // 1. The "Apply Auto-RAID to all objects" is enabled in ESA cluster configuration.
            // 2. The auto RAID policy is enabled on the default vSAN datastore storage policy.
            //
            // It shows an example of how to check the prerequisites.
            // NOTE: This sample can help check the cluster configuration for an initial troubleshooting if the
            // effective capacity is not enabled as expected or the effectiveSpaceUsage is not reported in the
            // QuerySpaceUsage API for 9.1U1 or higher version, but precheck is not required if the
            // effectiveSpaceUsage is reported.
            // Same for other features like global deduplication and compression, if the related fields are not reported
            // in the QuerySpaceUsage API, please refer to the sample code below to check the feature enabled status.

            // Get the vSAN cluster configuration
            var clusterConfigs = vsanPort.vsanClusterGetConfig(
                    VsanManagedObjectsCatalog.getVsanVcClusterConfigServiceInstanceReference(), cluster);
            if (clusterConfigs == null) {
                throw new RuntimeException("Failed to get cluster configs for " + clusterName);
            }

            // Get vSAN ESA enabled status
            boolean vsanEsaEnabled = clusterConfigs.isVsanEsaEnabled();

            // Get ESA global deduplication and compression enabled status
            boolean globalDedupEnabled = false;
            boolean compressionEnabled = false;
            var dataEfficiencyConfig = clusterConfigs.getDataEfficiencyConfig();
            if (dataEfficiencyConfig != null) {
                globalDedupEnabled = Boolean.TRUE.equals(dataEfficiencyConfig.isDedupEnabled());
                compressionEnabled = Boolean.TRUE.equals(dataEfficiencyConfig.isCompressionEnabled());
            }
            // Check if effective capacity is enabled in the vSAN cluster configuration
            boolean isEffectiveCapacityEnabled = checkIfEffectiveCapacityEnabled(
                    client.getPbmPort(), client.getPbmServiceInstanceContent(), cluster, clusterConfigs);

            System.out.println("Checking vSAN cluster configuration for " + clusterName);
            System.out.println("vSAN ESA Enabled: " + vsanEsaEnabled);
            System.out.println("vSAN ESA Effective Capacity Enabled: " + isEffectiveCapacityEnabled);
            System.out.println("vSAN ESA Global Deduplication Enabled: " + globalDedupEnabled);
            System.out.println("vSAN ESA Compression Enabled: " + compressionEnabled);
            // End of vSAN cluster configuration precheck

            VsanSpaceUsageDetailResult spaceDetail = spaceResult.getSpaceDetail();

            if (isEffectiveCapacityEnabled) {
                var usage = spaceResult.getEffectiveSpaceUsage();
                if (usage == null) {
                    System.out.println("Effective space usage is not available for the given cluster " + clusterName);
                } else {
                    System.out.println("\nvSAN Effective Capacity Overview");
                    System.out.println("vSAN Effective Total Usable Capacity: "
                            + bytesToTibBytes(usage.getTotalUsableB()) + " TiB");
                    System.out.println("vSAN Effective Used Capacity: "
                            + bytesToTibBytes((usage.getTotalUsableB() - usage.getFreeUsableB())) + " TiB");
                    System.out.println(
                            "vSAN Effective Free Usable Capacity: " + bytesToTibBytes(usage.getFreeUsableB()) + " TiB");
                    System.out.println(
                            "Actual Written Capacity: " + bytesToTibBytes(usage.getActualWrittenB()) + " TiB");
                    System.out.println("Over reserved Capacity: " + bytesToTibBytes(usage.getOverReservedB()) + " TiB");
                    System.out.println(
                            "Total Provisioning Capacity: " + bytesToTibBytes(usage.getTotalProvisionB()) + " TiB");

                    var snapshotSpace = usage.getSnapshotSpace();
                    if (snapshotSpace != null) {
                        System.out.println("Total Snapshot Count: " + snapshotSpace.getSnapshotCount());
                        System.out.println("Actual Snapshot Usage: "
                                + bytesToTibBytes(snapshotSpace.getActualSnapshotUsedB()) + " TiB");
                        System.out.println("Fully Inflated Snapshot Usage: "
                                + bytesToTibBytes(snapshotSpace.getFullyInflatedSnapshotUsedB()) + " TiB");

                        System.out.println("\nvSAN Effective Space Usage Breakdown");
                        String[] objectTypes = {"vdisk", "vmswap", "fileShare", "namespace", "aggregatedSystemObjects"};
                        List<VsanObjectSpaceSummary> spaceUsageByObjectType = new ArrayList<>();
                        if (spaceDetail != null && spaceDetail.getSpaceUsageByObjectType() != null) {
                            spaceUsageByObjectType = spaceDetail.getSpaceUsageByObjectType();
                        }

                        for (var objType : objectTypes) {
                            long total = 0;
                            for (var obj : spaceUsageByObjectType) {
                                if (objType.equals(obj.getObjType())) {
                                    total += obj.getPrimaryCapacityB();
                                }
                            }
                            System.out.printf("%s: %.4f TiB%n", objType, bytesToTibBytes(total));
                        }
                    }
                }
            }
            if (vsanEsaEnabled || compressionEnabled || globalDedupEnabled) {
                var efficientCapacityState = spaceResult.getEfficientCapacity();
                System.out.println("\nvSAN Data Reduction (Deduplication / Compression) is Enabled");

                long savings;
                if (isEffectiveCapacityEnabled) {
                    savings = efficientCapacityState.getEsaCompressionSpaceSaving()
                            + efficientCapacityState.getEsaDedupSpaceSaving();
                } else {
                    savings = efficientCapacityState.getLogicalCapacityUsed()
                            - efficientCapacityState.getPhysicalCapacityUsed();
                }
                System.out.println("Data Reduction Savings: " + bytesToTibBytes(savings) + " TiB");
            }

            var ratio = spaceResult.getSpaceEfficiencyRatio();
            if (ratio != null) {
                System.out.println("Data Reduction Ratio: " + ratio.getOverallRatio() + "x");

                if (globalDedupEnabled) {
                    System.out.println("Overall Global Deduplication Ratio: " + ratio.getDedupRatio() + "x");

                    if (ratio.getDedupEnabledRatio() != null) {
                        System.out.println("Global Deduplication Enabled Ratio: " + ratio.getDedupEnabledRatio() + "x");
                    }
                }

                if ((vsanEsaEnabled || compressionEnabled) && ratio.getCompressionRatio() != null) {
                    System.out.println("Compression Ratio: " + ratio.getCompressionRatio() + "x");
                }

                if (isEffectiveCapacityEnabled
                        && ratio.getThinProvisionRatio() != null
                        && ratio.getSnapshotSavingRatio() != null) {
                    System.out.println("\nOverall Space Efficiency");
                    System.out.println("Thin-provisioning Saving Ratio: " + ratio.getThinProvisionRatio() + "x");
                    System.out.println("Snapshot Saving Ratio: " + ratio.getSnapshotSavingRatio() + "x");
                }
            }

            System.out.println("\nvSAN Standard Space Usage Overview");
            long totalCapacity = spaceResult.getTotalCapacityB();
            long freeCapacity = spaceResult.getFreeCapacityB();
            long usedCapacity = totalCapacity - freeCapacity;

            System.out.println("Total vSAN Capacity: " + bytesToTibBytes(totalCapacity) + " TiB");
            System.out.println("Used vSAN Capacity: " + bytesToTibBytes(usedCapacity) + " TiB");
            System.out.println("Free vSAN Capacity: " + bytesToTibBytes(freeCapacity) + " TiB");

            System.out.println("\nvSAN Standard Usage Breakdown View");
            String[] objectTypes = {
                "vdisk", "vmswap", "statsdb", "namespace", "traceobject", "esaObjectOverhead", "fileSystemOverhead"
            };
            List<VsanObjectSpaceSummary> usageByType = null;
            if (spaceDetail != null) {
                usageByType = spaceDetail.getSpaceUsageByObjectType();
            }
            usageByType = (usageByType == null) ? new ArrayList<>() : usageByType;
            for (var objType : objectTypes) {
                long total = 0;
                for (var obj : usageByType) {
                    if (objType.equals(obj.getObjType())) {
                        total += obj.getUsedB();
                    }
                }
                System.out.printf("%s: %.4f TiB%n", objType, bytesToTibBytes(total));
            }
        }
    }

      /**
     * Check if the effective capacity is enabled in the vSAN cluster configuration
     *
     * @param pbmPort PBM Port
     * @param pbmServiceInstance PBM Service Instance
     * @param cluster: vSAN cluster object
     * @param clusterConfigs: vSAN cluster configurations
     * @return true if effective capacity is enabled, false otherwise
     */
    public static boolean checkIfEffectiveCapacityEnabled(
            PbmPortType pbmPort,
            PbmServiceInstanceContent pbmServiceInstance,
            ManagedObjectReference cluster,
            VsanConfigInfoEx clusterConfigs) {
        // Check if "Apply Auto-RAID to all objects" is enabled in the cluster configuration
        boolean isEffectiveCapacityEnabled = VsanUtil.isEsaAutoRaidEnabledInCluster(clusterConfigs);

        // If "Apply Auto-RAID to all objects" is not enabled, check if the auto RAID policy is
        // enabled on the default vSAN datastore storage policy
        if (!isEffectiveCapacityEnabled) {
            System.out.println("Checking if the effective capacity is enabled in the vSAN storage profile");
            var vsanDs = VsanUtil.getLocalVsanDatastore(propertyCollectorHelper, cluster);
            var vsanProfile = VsanUtil.getVsanStorageProfile(pbmPort, pbmServiceInstance, vsanDs.get(0));
            isEffectiveCapacityEnabled = VsanUtil.isAutoManagedRAIDEnabledInProfile(vsanProfile);
        }

        return isEffectiveCapacityEnabled;
    }

    private static double bytesToTibBytes(long byteSize) {
        double tibSize = byteSize / Math.pow(2, 40);
        return Math.round(tibSize * 10000.0) / 10000.0;
    }
}
