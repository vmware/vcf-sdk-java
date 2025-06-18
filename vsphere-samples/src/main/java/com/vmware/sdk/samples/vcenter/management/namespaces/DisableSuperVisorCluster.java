/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.namespaces;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.helpers.ClusterHelper;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.Cluster;
import com.vmware.vcenter.namespace_management.Clusters;
import com.vmware.vcenter.namespace_management.ClustersTypes;

/**
 * Demonstrates how to Disable/Delete vSphere supervisor cluster on given cluster (It can be NSX-T based or vSphere
 * networking based).
 *
 * <p><a
 * href="https://vthinkbeyondvm.com/automating-key-vsphere-supervisor-cluster-workflows-using-java-sdk/">Reference</a>
 *
 * <p>Sample Prerequisites: The sample needs an existing Supervisor cluster (NSX-T based or vSphere networking based)
 * enabled cluster name
 */
public class DisableSuperVisorCluster {
    private static final Logger log = LoggerFactory.getLogger(DisableSuperVisorCluster.class);
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

    /** REQUIRED: Name of the cluster we need to upgrade info. */
    public static String clusterName = "ClusterName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(DisableSuperVisorCluster.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Clusters ppClusterService = client.createStub(Clusters.class);
            Cluster clusterService = client.createStub(Cluster.class);

            String clusterId = ClusterHelper.getCluster(clusterService, clusterName);
            log.info("clusterId::{}", clusterId);

            // Disable Supervisor cluster: It will disable/remove/delete Supervisor cluster and remove all the vSphere
            // pods,
            // Guest clusters/TKC
            ppClusterService.disable(clusterId);
            log.info("Waiting for 1 min to quick-start the disable Supervisor cluster operation");
            Thread.sleep(60 * 1000);

            // Getting cluster configuration status. It must be in "REMOVING" state.
            ClustersTypes.Info info = ppClusterService.get(clusterId);
            String configStatus = info.getConfigStatus().toString();
            log.info("Cluster status::{}", configStatus);

            if ((configStatus != null && configStatus.equalsIgnoreCase("REMOVING"))) {
                log.info(
                        "Supervisor Cluster is being disabled, check H5C further, track the status using GET API in loop");
            }
        }
    }
}
