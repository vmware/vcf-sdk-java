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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.helpers.ClusterHelper;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.Cluster;
import com.vmware.vcenter.namespace_management.Clusters;
import com.vmware.vcenter.namespace_management.software.ClustersTypes;

/**
 * Demonstrates how to Update/Upgrade vSphere supervisor cluster (It can be NSX-T based or vSphere networking based).
 *
 * <p><a href="https://vthinkbeyondvm.com/">vThinkBeyondVM.com</a>
 *
 * <p>Sample Prerequisites: The sample needs an existing Supervisor cluster enabled cluster name
 */
public class UpgradeSupervisorCluster {
    private static final Logger log = LoggerFactory.getLogger(UpgradeSupervisorCluster.class);
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
    public static String clusterName = "clusterName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(UpgradeSupervisorCluster.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Clusters ppClusterService = client.createStub(Clusters.class);
            Cluster clusterService = client.createStub(Cluster.class);

            // Supervisor cluster update service object
            var wcpUpdateService = client.createStub(com.vmware.vcenter.namespace_management.software.Clusters.class);

            log.info("Getting cluster identifier as part of setup");
            String clusterId = ClusterHelper.getCluster(clusterService, clusterName);

            com.vmware.vcenter.namespace_management.software.ClustersTypes.Info clusterinfo =
                    wcpUpdateService.get(clusterId);
            String status = clusterinfo.getState().toString();
            String expectedStatus = "READY";

            if (status.equalsIgnoreCase(expectedStatus)) {
                log.info("Cluster is ready for upgrade as state is::{}", status);
            } else {
                log.info("Cluster is not ready for upgrade as actual state is::{}", status);
                System.exit(0);
            }

            com.vmware.vcenter.namespace_management.ClustersTypes.Info info = ppClusterService.get(clusterId);

            boolean k8sStatus = !(info.getKubernetesStatus().toString().equals("READY"));
            boolean clusterStatus = !(info.getConfigStatus().toString().equals("RUNNING"));

            if (k8sStatus && clusterStatus) {
                log.info("Cluster is NOT in good condition, hence skipping the upgrade");
                System.exit(0);
            }

            // This gives summary for all the clusters
            List<ClustersTypes.Summary> clusterSummary = wcpUpdateService.list();
            // Fetching the desired version for upgrade
            String desiredVersion = null;
            for (ClustersTypes.Summary summary : clusterSummary) {
                if (summary.getCluster().equals(clusterId)) {
                    List<String> availableVersion = summary.getAvailableVersions();

                    boolean isVersionEmpty = availableVersion.isEmpty();
                    if (isVersionEmpty) {
                        log.info("No updates available, need not update");
                        System.exit(0);
                    } else {
                        // Note that just taking the latest available version for supervisor cluster
                        // upgrade, there could be another version as well
                        desiredVersion = availableVersion.get(0);
                    }
                    break;
                }
            }

            log.info("We are building the Spec for upgrading vSphere supervisor cluster");
            com.vmware.vcenter.namespace_management.software.ClustersTypes.UpgradeSpec spec =
                    new ClustersTypes.UpgradeSpec();
            spec.setIgnorePrecheckWarnings(true);
            spec.setDesiredVersion(desiredVersion);

            wcpUpdateService.upgrade(clusterId, spec);
            log.info(
                    "Invocation is successful for updating vSphere supervisor cluster, check H5C, track the status using GET API");
            // Note: There is another API, which takes multiple clusters for upgrade, so we can upgrade multiple
            // clusters at a time
            // API name:
            // upgradeMultiple(java.util.Map<java.lang.String,ClustersTypes.UpgradeSpec> specs)
        }
    }
}
