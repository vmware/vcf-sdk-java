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
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.vsan.management.VsanManagedObjectsCatalog;
import com.vmware.sdk.vsphere.utils.vsan.management.VsanUtil;
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.HostConfigManager;
import com.vmware.vim25.HostMaintenanceSpec;
import com.vmware.vim25.HostScsiDisk;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VsanConfigInfoEx;
import com.vmware.vim25.VsanHostDecommissionMode;
import com.vmware.vim25.VsanHostDiskResult;
import com.vmware.vsan.sdk.RuntimeFaultFaultMsg;
import com.vmware.vsan.sdk.VimVsanHostQueryVsanDisksSpec;
import com.vmware.vsan.sdk.VimVsanHostStoragePoolDiskInfo;
import com.vmware.vsan.sdk.VimVsanHostVsanManagedDisksInfo;
import com.vmware.vsan.sdk.VsanAddStoragePoolDiskSpec;
import com.vmware.vsan.sdk.VsanDeleteStoragePoolDiskSpec;
import com.vmware.vsan.sdk.VsanFaultFaultMsg;
import com.vmware.vsan.sdk.VsanStoragePoolDisk;
import com.vmware.vsan.sdk.VsanhealthPortType;

/**
 * This file includes sample code for vCenter to call Single tier storage pool vSAN ESA APIs:
 *
 * <ol>
 *   <li>AddStoragePoolDisks
 *   <li>DeleteStoragePoolDisk
 *   <li>UnmountStoragePoolDisk
 *   <li>QueryVsanManagedDisks
 * </ol>
 *
 * <p>Sample Prerequisites:
 *
 * <ol>
 *   <li>Deployed is a vSAN ESA cluster with minimum node requirement.
 *   <li>There are at least 2 eligible unconsumed disks.
 * </ol>
 */
public class VsanEsaStoragePoolSample {

    private static final Logger log = LoggerFactory.getLogger(VsanEsaStoragePoolSample.class);

    /** REQUIRED: vCenter FQDN or IP address. */
    public static String serverAddress = "vcenter1.mycompany.com";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";
    /** REQUIRED: VC cluster name using in cluster health query API. */
    public static String clusterName;
    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (do not leave it empty on production environments).
     */
    public static String trustStorePath = null;

    private static PropertyCollectorHelper propertyCollectorHelper;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VsanEsaStoragePoolSample.class, args);

        VcenterClientFactory clientFactory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = clientFactory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference clusterMoRef =
                    propertyCollectorHelper.getMoRefByName(clusterName, CLUSTER_COMPUTE_RESOURCE);
            if (clusterMoRef == null) {
                log.error("Cannot find cluster: {}", clusterName);
                return;
            }

            List<ManagedObjectReference> hosts = ((ArrayOfManagedObjectReference)
                            propertyCollectorHelper.fetch(clusterMoRef, "host"))
                    .getManagedObjectReference();
            if (hosts.size() < 2) {
                log.error("The cluster does not have enough hosts. Please add at least 2 hosts and try again.");
                return;
            }

            VsanhealthPortType vsanPort = client.getVsanPort();

            // Check if vSAN ESA is configured
            VsanConfigInfoEx configInfoEx = vsanPort.vsanClusterGetConfig(
                    VsanManagedObjectsCatalog.getVsanVcClusterConfigServiceInstanceReference(), clusterMoRef);
            if (configInfoEx == null || !configInfoEx.isVsanEsaEnabled()) {
                log.error("vSAN ESA is not enabled on cluster {}", clusterName);
                return;
            }

            // Step 1: Query vSAN disks and filter out eligible disks for the given host. Select the disk of your choice
            // and add disks to storage pool.
            // Expectation:
            //     This operation will be successful.
            // Reason:
            //     The disk selected to be added is an eligible disk.
            ManagedObjectReference firstHost = hosts.get(0);
            HostConfigManager hostConfigManager = propertyCollectorHelper.fetch(firstHost, "configManager");
            var diskSpec = new VsanAddStoragePoolDiskSpec();
            diskSpec.setHost(firstHost);
            List<HostScsiDisk> eligibleVsanDisks = queryEligibleVsanDisks(vsanPort, hostConfigManager);
            log.info(
                    "Eligible vSAN disks : {}",
                    eligibleVsanDisks.stream()
                            .map(HostScsiDisk::getCanonicalName)
                            .collect(Collectors.toList()));

            HostScsiDisk disk;
            if (!eligibleVsanDisks.isEmpty()) {
                disk = eligibleVsanDisks.remove(eligibleVsanDisks.size() - 1);
                var storagePoolDisk = new VsanStoragePoolDisk();
                storagePoolDisk.setDiskName(disk.getCanonicalName());
                storagePoolDisk.setDiskType("singleTier");
                diskSpec.getDisks().add(storagePoolDisk);
                addDiskToStoragePool(vsanPort, diskSpec);
            }

            // Step 2) Query storage pool disks and remove disk from storage pool with no action on decommissioning.
            // Expectation:
            //         This operation will be successful.
            VsanDeleteStoragePoolDiskSpec deleteStoragePoolDiskSpec = getDeleteStoragePoolDiskSpec(vsanPort, firstHost);
            removeDiskFromStoragePool(vsanPort, clusterMoRef, deleteStoragePoolDiskSpec);

            // Step 3) Add the other disk to the pool,
            //         Query storage pool disks and unmount disk from storage pool with no action on decommissioning and
            //         then remove disk.
            // Expectation:
            //        This operation will be successful.
            // Reason:
            //        The disk can be removed even when it is unmounted from the storage pool.
            ManagedObjectReference secondHost = hosts.get(1);
            hostConfigManager = propertyCollectorHelper.fetch(secondHost, "configManager");
            diskSpec = new VsanAddStoragePoolDiskSpec();
            diskSpec.setHost(secondHost);
            eligibleVsanDisks = queryEligibleVsanDisks(vsanPort, hostConfigManager);

            if (!eligibleVsanDisks.isEmpty()) {
                var otherDiskSpec = new VsanAddStoragePoolDiskSpec();
                otherDiskSpec.setHost(secondHost);
                var otherDisk = eligibleVsanDisks.remove(eligibleVsanDisks.size() - 1);
                var otherStoragePoolDisk = new VsanStoragePoolDisk();
                otherStoragePoolDisk.setDiskName(otherDisk.getCanonicalName());
                otherStoragePoolDisk.setDiskType("singleTier");
                otherDiskSpec.getDisks().add(otherStoragePoolDisk);
                addDiskToStoragePool(vsanPort, otherDiskSpec);

                var otherDeleteStoragePoolDiskSpec = getDeleteStoragePoolDiskSpec(vsanPort, secondHost);
                unmountDiskFromStoragePool(vsanPort, clusterMoRef, otherDeleteStoragePoolDiskSpec);
                removeDiskFromStoragePool(vsanPort, clusterMoRef, otherDeleteStoragePoolDiskSpec);
            }
        }
    }

    /** Get the DeleteStoragePoolDiskSpec calling vsanDeleteStoragePoolDisk API */
    private static VsanDeleteStoragePoolDiskSpec getDeleteStoragePoolDiskSpec(
            VsanhealthPortType vsanPort, ManagedObjectReference firstHost) {
        List<VimVsanHostStoragePoolDiskInfo> storagePoolDisks = queryStoragePoolDisks(vsanPort, firstHost);
        log.info(
                "Storage pool disks: {}",
                storagePoolDisks.stream()
                        .map(VimVsanHostStoragePoolDiskInfo::getDisk)
                        .map(HostScsiDisk::getCanonicalName)
                        .collect(Collectors.toList()));
        if (storagePoolDisks.isEmpty()) {
            log.info("No Storage Pool Disks found");
            return null;
        }

        var vsanDeleteStoragePoolDiskSpec = new VsanDeleteStoragePoolDiskSpec();
        var maintenanceSpec = new HostMaintenanceSpec();
        var vsanMode = new VsanHostDecommissionMode();
        vsanMode.setObjectAction("noAction");
        maintenanceSpec.setVsanMode(vsanMode);
        vsanDeleteStoragePoolDiskSpec
                .getDiskUuids()
                .add(Objects.requireNonNull(storagePoolDisks.get(0).getDisk().getVsanDiskInfo())
                        .getVsanUuid());
        vsanDeleteStoragePoolDiskSpec.setMaintenanceSpec(maintenanceSpec);
        return vsanDeleteStoragePoolDiskSpec;
    }

    /**
     * Demonstrates AddStoragePoolDisks API Add disks to Storage Pool If the task of disk addition fails, any exception
     * will be logged.
     */
    private static void addDiskToStoragePool(VsanhealthPortType vsanPort, VsanAddStoragePoolDiskSpec diskSpec) {
        ManagedObjectReference taskMo;
        try {
            taskMo = vsanPort.vsanAddStoragePoolDisk(
                    VsanManagedObjectsCatalog.getVsanVcDiskMgrServiceInstanceReference(), List.of(diskSpec));
        } catch (RuntimeFaultFaultMsg | VsanFaultFaultMsg e) {
            throw new RuntimeException(e);
        }
        var success = VsanUtil.waitForTasks(propertyCollectorHelper, taskMo);
        if (success) {
            log.info("AddDisk to storage pool operation completed");
        } else {
            log.error("AddDisk to storage pool operation failed");
        }
    }

    /** Demonstrates QueryDisksForVsan API. Query all vSAN disks */
    private static List<VsanHostDiskResult> queryVsanDisks(
            VsanhealthPortType vsanPort, HostConfigManager hostConfigManager) {
        try {
            return vsanPort.queryDisksForVsan(hostConfigManager.getVsanSystem(), null);
        } catch (RuntimeFaultFaultMsg e) {
            throw new RuntimeException(e);
        }
    }

    /** Support method helps filter eligible vSAN disks. Query all vSAN disks */
    private static List<HostScsiDisk> queryEligibleVsanDisks(
            VsanhealthPortType vsanPort, HostConfigManager hostConfigManager) {
        List<VsanHostDiskResult> disks = queryVsanDisks(vsanPort, hostConfigManager);
        List<HostScsiDisk> eligibleDisks = new ArrayList<>();
        for (VsanHostDiskResult disk : disks) {
            if ("eligible".equals(disk.getState()) && disk.getDisk().isSsd()) {
                log.info(
                        "disk {} is eligible, state: {}, ssd: {}",
                        disk.getDisk().getUuid(),
                        disk.getState(),
                        disk.getDisk().isSsd());
                eligibleDisks.add(disk.getDisk());
            } else {
                log.info(
                        "disk {} is not eligible, state: {}, ssd: {}",
                        disk.getDisk().getUuid(),
                        disk.getState(),
                        disk.getDisk().isSsd());
            }
        }
        return eligibleDisks;
    }

    /**
     * Demonstrates QueryVsanManagedDisks API Query Storage Pool disks On success the query returns list of vSAN ESA
     * storage pool disks If the Query fails, any exception will be logged.
     */
    private static List<VimVsanHostStoragePoolDiskInfo> queryStoragePoolDisks(
            VsanhealthPortType vsanPort, ManagedObjectReference host) {
        var vimVsanHostQueryVsanDisksSpec = new VimVsanHostQueryVsanDisksSpec();
        vimVsanHostQueryVsanDisksSpec.setVsanDiskType("storagePool");

        VimVsanHostVsanManagedDisksInfo storagePoolDisks;
        try {
            storagePoolDisks = vsanPort.queryVsanManagedDisks(
                    VsanManagedObjectsCatalog.getVsanVcDiskMgrServiceInstanceReference(),
                    host,
                    vimVsanHostQueryVsanDisksSpec);
        } catch (RuntimeFaultFaultMsg | VsanFaultFaultMsg e) {
            throw new RuntimeException(e);
        }

        List<VimVsanHostStoragePoolDiskInfo> disks = new ArrayList<>();
        for (var storagePool : Objects.requireNonNull(storagePoolDisks.getStoragePools())) {
            disks.addAll(Objects.requireNonNull(storagePool.getStoragePoolDisks()));
        }
        return disks;
    }

    /**
     * Demonstrates unmountDiskFromStoragePool API Unmount disks from storage pool If the task of disk unmount fails,
     * any exception will be logged.
     */
    private static void unmountDiskFromStoragePool(
            VsanhealthPortType vsanPort,
            ManagedObjectReference clusterMoRef,
            VsanDeleteStoragePoolDiskSpec otherDeleteStoragePoolDiskSpec) {
        try {
            var task = vsanPort.vsanUnmountStoragePoolDisks(
                    VsanManagedObjectsCatalog.getVsanVcDiskMgrServiceInstanceReference(),
                    clusterMoRef,
                    otherDeleteStoragePoolDiskSpec);
            var success = VsanUtil.waitForTasks(propertyCollectorHelper, task);
            if (success) {
                log.info("Successfully unmounted storage pool disks");
            } else {
                log.error("Failed to unmount storage pool disks");
            }
        } catch (Exception e) {
            log.error("unmount disk from storage pool operation failed: ", e);
        }
    }

    /*
     * Demonstrates DeleteStoragePoolDisk API
     * Removes disks from storage pool
     * If the task of disk remove fails, any exception will be logged.
     */
    private static void removeDiskFromStoragePool(
            VsanhealthPortType vsanPort,
            ManagedObjectReference clusterMoRef,
            VsanDeleteStoragePoolDiskSpec vsanDeleteStoragePoolDiskSpec) {
        if (vsanDeleteStoragePoolDiskSpec == null) {
            return;
        }
        try {
            ManagedObjectReference taskMo = vsanPort.vsanDeleteStoragePoolDisk(
                    VsanManagedObjectsCatalog.getVsanVcDiskMgrServiceInstanceReference(),
                    clusterMoRef,
                    vsanDeleteStoragePoolDiskSpec);
            var success = VsanUtil.waitForTasks(propertyCollectorHelper, taskMo);
            if (success) {
                log.info("remove disk from storage pool operation completed");
            } else {
                log.error("remove disk from storage pool operation failed");
            }
        } catch (Exception e) {
            log.error("Remove disk from storage pool operation failed: ", e);
        }
    }
}
