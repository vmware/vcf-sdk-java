/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.storage.sdrs;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.DATACENTER;
import static com.vmware.vim25.ManagedObjectType.DATASTORE;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.DuplicateNameFaultMsg;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidFolderFaultMsg;
import com.vmware.vim25.InvalidNameFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.StorageDrsConfigSpec;
import com.vmware.vim25.StorageDrsIoLoadBalanceConfig;
import com.vmware.vim25.StorageDrsPodConfigSpec;
import com.vmware.vim25.StorageDrsSpaceLoadBalanceConfig;
import com.vmware.vim25.VimPortType;

/** This sample demonstrates how to create Storage DRS. */
public class CreateStorageDRS {
    private static final Logger log = LoggerFactory.getLogger(CreateStorageDRS.class);
    /** REQUIRED: vCenter FQDN or IP address. */
    public static String serverAddress = "vcenter1.mycompany.com";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";
    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (do not leave it empty on production environments).
     */
    public static String trustStorePath = null;

    /** REQUIRED: Datacenter name. */
    public static String datacenterName = "datacenterName";
    /** REQUIRED: Name for the new storage pod. */
    public static String sdrsName = "sdrsName";
    /** OPTIONAL: Storage DRS behavior, true if automated. It is manual by default. */
    public static Boolean behavior = null;
    /**
     * OPTIONAL: Storage DRS makes storage migration recommendations if I/O latency on one (or more) of the datastores
     * is higher than the specified threshold. Range is 5-100 ms, default is 15ms.
     */
    public static String ioLatencyThreshold = null;
    /**
     * OPTIONAL: Storage DRS makes storage migration recommendations if I/O load imbalance level is higher than the
     * specified threshold. Range is 1-100, default is 5.
     */
    public static String ioLoadImbalanceThreshold = null;
    /**
     * OPTIONAL: Specify the interval that storage DRS runs to load balance among datastores within a storage pod. It is
     * 480 by default.
     */
    public static String loadBalanceInterval = null;
    /**
     * OPTIONAL: Storage DRS considers making storage migration recommendations if the difference in space utilization
     * between the source and destination datastores is higher than the specified threshold. Range 1-50%, default is 5%.
     */
    public static String minSpaceUtilizationDifference = null;
    /**
     * OPTIONAL: Storage DRS makes storage migration recommendations if space utilization on one (or more) of the
     * datastores is higher than the specified threshold. Range 50-100%, default is 80%.
     */
    public static String spaceUtilizationThreshold = null;
    /** OPTIONAL: Name of the datastore to be added in StoragePod. */
    public static String datastoreName = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(CreateStorageDRS.class, args);

        validate();

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            createSDRS(
                    vimPort,
                    serviceContent,
                    propertyCollectorHelper,
                    datacenterName,
                    sdrsName,
                    Boolean.TRUE.equals(behavior),
                    ioLatencyThreshold,
                    ioLoadImbalanceThreshold,
                    loadBalanceInterval,
                    minSpaceUtilizationDifference,
                    spaceUtilizationThreshold,
                    datastoreName);
        }
    }

    /**
     * Validates the options supplied to the command and throws errors if a good combination has not been entered for
     * the sample to operate.
     */
    private static void validate() {
        if ((sdrsName == null) || (datacenterName == null)) {
            throw new IllegalArgumentException("Expected valid --datacentername, --sdrsname" + " arguments.");
        }
        if (ioLatencyThreshold != null) {
            if (Integer.parseInt(ioLatencyThreshold) < 5 || Integer.parseInt(ioLatencyThreshold) > 50) {
                throw new IllegalArgumentException("Expected valid --iolatencythreshold argument. Range is 5-50 ms.");
            }
        }
        if (ioLoadImbalanceThreshold != null) {
            if (Integer.parseInt(ioLoadImbalanceThreshold) < 1 || Integer.parseInt(ioLoadImbalanceThreshold) > 100) {
                throw new IllegalArgumentException(
                        "Expected valid --ioloadimbalancethreshold argument. Range is 1-100.");
            }
        }
        if (minSpaceUtilizationDifference != null) {
            if (Integer.parseInt(minSpaceUtilizationDifference) < 1
                    || Integer.parseInt(minSpaceUtilizationDifference) > 50) {
                throw new IllegalArgumentException("Expected valid --minutilizationdiff argument. Range is 1-50%.");
            }
        }
        if (spaceUtilizationThreshold != null) {
            if (Integer.parseInt(spaceUtilizationThreshold) < 50 || Integer.parseInt(spaceUtilizationThreshold) > 100) {
                throw new IllegalArgumentException("Expected valid --utilizationthreshold argument. Range is 50-100%.");
            }
        }
    }

    /**
     * @param behavior Storage DRS behavior, true if automated
     * @param ioLatencyThreshold Storage DRS makes storage migration recommendations if I/O latency on one (or more) of
     *     the datastores is higher than the specified threshold
     * @param ioLoadImbalanceThreshold Storage DRS makes storage migration recommendations if I/O load imbalance level
     *     is higher than the specified threshold
     * @param loadBalanceInterval Specify the interval that storage DRS runs to load balance among datastores within a
     *     storage pod
     * @param minSpaceUtilizationDifference Storage DRS considers making storage migration recommendations if the
     *     difference in space utilization between the source and destination datastores is higher than the specified
     *     threshold
     * @param spaceUtilizationThreshold Storage DRS makes storage migration recommendations if space utilization on one
     *     (or more) of the datastores is higher than the specified threshold.
     * @return StorageDrsConfigSpec object
     */
    private static StorageDrsConfigSpec getStorageDrsConfigSpec(
            boolean behavior,
            String ioLatencyThreshold,
            String ioLoadImbalanceThreshold,
            String loadBalanceInterval,
            String minSpaceUtilizationDifference,
            String spaceUtilizationThreshold) {
        StorageDrsPodConfigSpec podConfigSpec = new StorageDrsPodConfigSpec();
        podConfigSpec.setDefaultIntraVmAffinity(true);
        if (behavior) {
            podConfigSpec.setDefaultVmBehavior("automated");
        } else {
            podConfigSpec.setDefaultVmBehavior("manual");
        }
        podConfigSpec.setEnabled(true); // "True" as storage DRS should be enabled by default

        StorageDrsIoLoadBalanceConfig sdrsIoLoadBalanceConfig = new StorageDrsIoLoadBalanceConfig();
        if (ioLatencyThreshold != null) {
            sdrsIoLoadBalanceConfig.setIoLatencyThreshold(Integer.parseInt(ioLatencyThreshold));
        } else {
            sdrsIoLoadBalanceConfig.setIoLatencyThreshold(15);
        }
        if (ioLoadImbalanceThreshold != null) {
            sdrsIoLoadBalanceConfig.setIoLoadImbalanceThreshold(Integer.parseInt(ioLoadImbalanceThreshold));
        } else {
            sdrsIoLoadBalanceConfig.setIoLoadImbalanceThreshold(5);
        }
        podConfigSpec.setIoLoadBalanceConfig(sdrsIoLoadBalanceConfig);

        // SDRS IO functionalities are not supported on VCF 9.0 and above.
        podConfigSpec.setIoLoadBalanceEnabled(false);
        if (loadBalanceInterval != null) {
            podConfigSpec.setLoadBalanceInterval(Integer.parseInt(loadBalanceInterval));
        } else {
            podConfigSpec.setLoadBalanceInterval(480);
        }

        StorageDrsSpaceLoadBalanceConfig sdrsSpaceLoadBalanceConfig = new StorageDrsSpaceLoadBalanceConfig();
        if (minSpaceUtilizationDifference != null) {
            sdrsSpaceLoadBalanceConfig.setMinSpaceUtilizationDifference(
                    Integer.parseInt(minSpaceUtilizationDifference));
        } else {
            sdrsSpaceLoadBalanceConfig.setMinSpaceUtilizationDifference(5);
        }

        if (spaceUtilizationThreshold != null) {
            sdrsSpaceLoadBalanceConfig.setSpaceUtilizationThreshold(Integer.parseInt(spaceUtilizationThreshold));
        } else {
            sdrsSpaceLoadBalanceConfig.setSpaceUtilizationThreshold(80);
        }
        podConfigSpec.setSpaceLoadBalanceConfig(sdrsSpaceLoadBalanceConfig);

        StorageDrsConfigSpec sdrsConfigSpec = new StorageDrsConfigSpec();
        sdrsConfigSpec.setPodConfigSpec(podConfigSpec);
        return sdrsConfigSpec;
    }

    /**
     * For creating customized StorageDRS.
     *
     * @param dcName datacenter name
     * @param behavior Storage DRS behavior, true if automated
     * @param ioLatencyThreshold Storage DRS makes storage migration recommendations if I/O latency on one (or more) of
     *     the datastores is higher than the specified threshold
     * @param ioLoadImbalanceThreshold Storage DRS makes storage migration recommendations if I/O load imbalance level
     *     is higher than the specified threshold
     * @param loadBalanceInterval Specify the interval that storage DRS runs to load balance among datastores within a
     *     storage pod
     * @param minSpaceUtilizationDifference Storage DRS considers making storage migration recommendations if the
     *     difference in space utilization between the source and destination datastores is higher than the specified
     *     threshold
     * @param spaceUtilizationThreshold Storage DRS makes storage migration recommendations if space utilization on one
     *     (or more) of the datastores is higher than the specified threshold
     */
    private static void createSDRS(
            VimPortType vimPort,
            ServiceContent serviceContent,
            PropertyCollectorHelper propertyCollectorHelper,
            String dcName,
            String name,
            boolean behavior,
            String ioLatencyThreshold,
            String ioLoadImbalanceThreshold,
            String loadBalanceInterval,
            String minSpaceUtilizationDifference,
            String spaceUtilizationThreshold,
            String dsName)
            throws DuplicateNameFaultMsg, RuntimeFaultFaultMsg, InvalidNameFaultMsg, InvalidPropertyFaultMsg,
                    InvalidCollectorVersionFaultMsg, InvalidFolderFaultMsg, InvalidStateFaultMsg {
        ManagedObjectReference storageResourceManager = serviceContent.getStorageResourceManager();

        ManagedObjectReference datacenterMoRef = propertyCollectorHelper.getMoRefByName(dcName, DATACENTER);
        if (datacenterMoRef != null) {
            ManagedObjectReference datastoreFolder = propertyCollectorHelper.fetch(datacenterMoRef, "datastoreFolder");
            ManagedObjectReference storagePod = vimPort.createStoragePod(datastoreFolder, name);
            log.info("Success: Creating storagePod.");

            StorageDrsConfigSpec sdrsConfigSpec = getStorageDrsConfigSpec(
                    behavior,
                    ioLatencyThreshold,
                    ioLoadImbalanceThreshold,
                    loadBalanceInterval,
                    minSpaceUtilizationDifference,
                    spaceUtilizationThreshold);

            ManagedObjectReference taskMoRef =
                    vimPort.configureStorageDrsForPodTask(storageResourceManager, storagePod, sdrsConfigSpec, true);
            if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                log.info("Success: Configuring storagePod.");
            } else {
                throw new RuntimeException("Failure: Configuring storagePod.");
            }

            if (dsName != null) {
                ManagedObjectReference datastoreMoRef = propertyCollectorHelper.getMoRefByName(dsName, DATASTORE);
                if (datastoreMoRef != null) {
                    List<ManagedObjectReference> dsList = new ArrayList<>();
                    dsList.add(datastoreMoRef);
                    ManagedObjectReference task = vimPort.moveIntoFolderTask(storagePod, dsList);
                    if (propertyCollectorHelper.awaitTaskCompletion(task)) {
                        log.info("Success: Adding datastore to storagePod.");
                    } else {
                        throw new RuntimeException("Failure: Adding datastore to storagePod.");
                    }
                } else {
                    log.error("Datastore {} Not Found", dsName);
                }
            }
        } else {
            log.error("Datacenter {} Not Found", dcName);
        }
    }
}
