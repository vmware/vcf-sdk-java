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

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.vsan.management.VsanManagedObjectsCatalog;
import com.vmware.sdk.vsphere.utils.vsan.management.VsanUtil;
import com.vmware.vim25.AboutInfo;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimVsanReconfigSpec;
import com.vmware.vim25.VsanConfigInfoEx;
import com.vmware.vsan.sdk.InvalidStateFaultMsg;
import com.vmware.vsan.sdk.VsanAdvancedDatastoreConfig;
import com.vmware.vsan.sdk.VsanFaultFaultMsg;
import com.vmware.vsan.sdk.VsanMountPrecheckResult;
import com.vmware.vsan.sdk.VsanhealthPortType;

/**
 * This sample demonstrates how to run Mount Precheck, Mount, and Unmount a remote vSAN datastore using the
 * VsanRemoteDatastoreSystem MO.
 *
 * <p>Sample Prerequisites: The sample needs a vCenter server with a standard vSAN cluster configured and another vSAN
 * cluster with compute only enabled. The standard vSAN cluster is passed to this sample as the server cluster, while
 * the compute-only cluster as the client.
 */
public class RemoteVsanSample {
    private static final Logger log = LoggerFactory.getLogger(RemoteVsanSample.class);

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
    /** REQUIRED: Name of the vSAN cluster in the server mode */
    public static String serverClusterName = "serverVsanDatastore";
    /** REQUIRED: Name of the vSAN cluster in the client mode. */
    public static String clientClusterName = "clientVsanDatastore";

    private static PropertyCollectorHelper propertyCollectorHelper;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(RemoteVsanSample.class, args);

        VcenterClientFactory clientFactory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = clientFactory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            VsanhealthPortType vsanPort = client.getVsanPort();

            ServiceContent serviceContent = client.getVimServiceContent();
            propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            // Detecting whether the host is vCenter or ESXi.
            AboutInfo aboutInfo = serviceContent.getAbout();
            if ("VirtualCenter".equals(aboutInfo.getApiType())) {

                // Get server and client cluster instances
                var serverCluster = getClusterInstanceHelper(serverClusterName);
                if (serverCluster == null) {
                    log.error("Unable to get the server cluster {}", serverClusterName);
                    return;
                }
                var clientCluster = getClusterInstanceHelper(clientClusterName);
                if (clientCluster == null) {
                    log.error("Unable to get the client cluster {}", clientClusterName);
                    return;
                }

                // Mount/Unmount work with desired state mechanism. Spec needs to contain the
                // list of existing remote datastores. For a given spec:
                // Mount: The provided remote vSAN datastore(s) will be mounted to the client cluster.
                //        If the client cluster is already mounted, then it will be skipped.
                // Unmount: All in use remote vSAN datastores of target vSAN cluster will be
                //          unmounted if not specified in desired spec.

                // Get local vSAN datastore from the server cluster
                var localDatastore = VsanUtil.getLocalVsanDatastore(propertyCollectorHelper, serverCluster);

                // Run MountPrecheck API and verify the result for failures
                if (!localDatastore.isEmpty()) {
                    log.info("Running MountPrecheck on cluster: {}", clientClusterName);
                    var result = vsanPort.mountPrecheck(
                            VsanManagedObjectsCatalog.getVsanRemoteDatastoreSystem(),
                            clientCluster,
                            localDatastore.get(0),
                            null);

                    if (verifyPrecheckResult(result)) {
                        var vsanConfig = new VsanConfigInfoEx();
                        vsanConfig.setEnabled(null);

                        String datastoreName = propertyCollectorHelper.fetch(localDatastore.get(0), "name");

                        // Mounting a remote datastore
                        log.info("Mounting remote datastore on cluster: {}", clientClusterName);
                        var dsConfig = new VsanAdvancedDatastoreConfig();
                        dsConfig.getRemoteDatastores().add(localDatastore.get(0));
                        applyVsanConfig(vsanPort, clientCluster, vsanConfig, dsConfig, datastoreName);

                        // Unmounting a remote datastore
                        log.info("Unmounting remote datastore from cluster: {}", clientClusterName);
                        dsConfig = new VsanAdvancedDatastoreConfig();
                        dsConfig.getRemoteDatastores().clear();
                        applyVsanConfig(vsanPort, clientCluster, vsanConfig, dsConfig, datastoreName);
                    }
                } else {
                    log.error("Error: No local vSAN datastore found for server cluster {}", serverClusterName);
                }
            } else {
                log.error("Host provided should be a Virtual Center");
            }
        }
    }

    /**
     * For checking the MountPrecheck failed result in detail E.g. Some connectivity issue in a cluster Like, cluster
     * partition, etc. Red: Indicates severe warnings Yellow: Indicates light warnings Green: Indicates no warnings
     */
    private static boolean verifyPrecheckResult(VsanMountPrecheckResult result) {
        boolean status = true;
        for (var precheckItem : Objects.requireNonNull(result.getResult())) {
            if (Objects.equals(precheckItem.getStatus(), "red")) {
                log.error("Precheck Item failed: {}", precheckItem);
                log.error("Precheck Item failure reason: {}", precheckItem.getReason());
                status = false;
            }
        }
        return status;
    }

    private static ManagedObjectReference getClusterInstanceHelper(String clusterName) {
        if (clusterName != null) {
            ManagedObjectReference clusterInstance;
            try {
                clusterInstance = propertyCollectorHelper.getMoRefByName(clusterName, CLUSTER_COMPUTE_RESOURCE);
            } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e) {
                throw new RuntimeException(e);
            }
            if (clusterInstance == null) {
                log.error("Cluster {} is not found for {}", clusterName, serverAddress);
                return null;
            }
            return clusterInstance;
        } else {
            log.error("Server or Client cluster name argument is not provided");
            return null;
        }
    }

    private static void applyVsanConfig(
            VsanhealthPortType vsanPort,
            ManagedObjectReference cluster,
            VsanConfigInfoEx vsanConfig,
            VsanAdvancedDatastoreConfig dsConfig,
            String datastoreName) {
        var spec = new VimVsanReconfigSpec();

        spec.setVsanClusterConfig(vsanConfig);
        spec.setDatastoreConfig(dsConfig);
        spec.setModify(true);
        ManagedObjectReference task;
        try {
            task = vsanPort.vsanClusterReconfig(
                    VsanManagedObjectsCatalog.getVsanVcClusterConfigServiceInstanceReference(), cluster, spec);
        } catch (InvalidStateFaultMsg | com.vmware.vsan.sdk.RuntimeFaultFaultMsg | VsanFaultFaultMsg e) {
            throw new RuntimeException(e);
        }

        boolean status = VsanUtil.waitForTasks(propertyCollectorHelper, task);
        if (status) {
            log.info("(un) mount remote datastore task completed");
        } else {
            log.error("Failed to (un) mount remote datastore");
        }

        log.info("Successfully (un)mounted remote vSAN datastore {} on cluster {}", datastoreName, cluster.getValue());
    }
}
