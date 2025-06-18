/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.vlcm.inventory.bulktransition;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.cis.Tasks;
import com.vmware.cis.TasksTypes;
import com.vmware.cis.task.Info;
import com.vmware.cis.task.Status;
import com.vmware.esx.settings.Inventory;
import com.vmware.esx.settings.InventoryTypes;
import com.vmware.esx.settings.inventory.reports.transition_summary.Clusters;
import com.vmware.esx.settings.inventory.reports.transition_summary.ClustersTypes;
import com.vmware.esx.settings.inventory.reports.transition_summary.Hosts;
import com.vmware.esx.settings.inventory.reports.transition_summary.HostsTypes;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.services.Service;

/**
 * Demonstrates triggering the "extract" workflow for a group of clusters or standalone hosts for the bulk transition
 * feature, waiting for the task to complete, and getting the result.
 *
 * <p>Prerequisites:
 *
 * <ul>
 *   <li>vCenter Server 9.0.0+
 *   <li>A datacenter
 *   <li>If a cluster is used as an entity, cluster should have at least one host.
 *   <li>Host with version 7.0.2+
 * </ul>
 */
public class BulkExtract {
    private static final Logger log = LoggerFactory.getLogger(BulkExtract.class);
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
    /** REQUIRED: List of entities comma seperated. */
    public static String[] entities = {"domain-c10"};
    /** REQUIRED: EntityType expected CLUSTER/HOST. */
    public static String entityType = "CLUSTER";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(BulkExtract.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {

            Service serviceApiStub = client.createStub(Service.class);
            log.info("Service API stub {}", serviceApiStub);
            Set<String> entitySet = Set.of(entities);

            Inventory inventoryStub = client.createStub(Inventory.class);

            // Create an entity spec
            InventoryTypes.EntitySpec entitySpec;
            if (entityType.equalsIgnoreCase("CLUSTER")) {
                entitySpec = new InventoryTypes.EntitySpec.Builder(InventoryTypes.EntitySpec.InventoryType.CLUSTER)
                        .setClusters(entitySet)
                        .build();
            } else if (entityType.equalsIgnoreCase("HOST")) {
                entitySpec = new InventoryTypes.EntitySpec.Builder(InventoryTypes.EntitySpec.InventoryType.HOST)
                        .setHosts(entitySet)
                        .build();
            } else {
                log.error("Invalid entity type specified, please specify either cluster/host");
                return;
            }
            InventoryTypes.ExtractInstalledImageSpec.Builder extractInstalledImageSpec =
                    new InventoryTypes.ExtractInstalledImageSpec.Builder(entitySpec);

            Tasks tasks = client.createStub(Tasks.class);

            // Invoke extract task
            String taskId = inventoryStub.extractInstalledImage_Task(extractInstalledImageSpec.build());
            log.info("Task invoked successfully {}", taskId);

            // Poll the task
            Info info;
            String res;
            do {
                TasksTypes.GetSpec.Builder taskSpecBuilder = new TasksTypes.GetSpec.Builder();
                info = tasks.get(taskId, taskSpecBuilder.build());
                res = String.valueOf(info.getResult());
                log.info("Task [{}] current status is:{}", taskId, info.getStatus());
                Thread.sleep(10);
            } while (info.getStatus() != Status.FAILED && info.getStatus() != Status.SUCCEEDED);
            log.info("Task result {}", res);

            if (entityType.equalsIgnoreCase("CLUSTER")) {

                // Check clusters status using the summary API, after the task is complete
                ClustersTypes.GetParams getParams = new ClustersTypes.GetParams.Builder(
                                ClustersTypes.GetParams.InventoryType.CLUSTER)
                        .setClusters(entitySet)
                        .build();
                Clusters clustersStub = client.createStub(Clusters.class);
                ClustersTypes.Info clusterInfo = clustersStub.get(getParams, null, null, null);
                List<ClustersTypes.ClusterSummary> clusterSummaries = clusterInfo.getClusterSummaries();

                // Clusters with status ELIGIBLE are eligible for transition, others are not
                log.info("----------Clusters that are eligible for transition----------");
                for (ClustersTypes.ClusterSummary summary : clusterSummaries) {
                    if (summary.getStatus() == ClustersTypes.TransitionStatus.ELIGIBLE) {
                        log.info("Transition Eligible Cluster Details ---------- {}", summary);
                    }
                }
                log.info("----------Clusters that are not eligible for transition----------");
                for (ClustersTypes.ClusterSummary summary : clusterSummaries) {
                    if (summary.getStatus() != ClustersTypes.TransitionStatus.ELIGIBLE) {
                        log.info("Transition Non-eligible Cluster Details {}", summary);
                    }
                }

            } else {

                // Check hosts status using the summary API, after the task is complete
                HostsTypes.GetParams getParams = new HostsTypes.GetParams.Builder(
                                HostsTypes.GetParams.InventoryType.HOST)
                        .setHosts(entitySet)
                        .build();
                Hosts hostsStub = client.createStub(Hosts.class);
                HostsTypes.Info hostInfo = hostsStub.get(getParams, null, null, null);
                List<HostsTypes.HostSummary> hostSummaries = hostInfo.getHostSummaries();

                // Hosts with status ELIGIBLE are eligible for transition, others are not
                log.info("----------Hosts that are eligible for transition----------");
                for (HostsTypes.HostSummary summary : hostSummaries) {
                    if (summary.getStatus() == HostsTypes.TransitionStatus.ELIGIBLE) {
                        log.info("Transition Eligible Host Details ---------- {}", summary);
                    }
                }
                log.info("----------Hosts that are not eligible for transition----------");
                for (HostsTypes.HostSummary summary : hostSummaries) {
                    if (summary.getStatus() != HostsTypes.TransitionStatus.ELIGIBLE) {
                        log.info("Transition Non-eligible Host Details ---------- {}", summary);
                    }
                }
            }
        }
    }
}
