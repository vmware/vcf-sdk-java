/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.sddcm.clusters;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.sddcm.helpers.SddcManagerHelper;
import com.vmware.sdk.samples.sddcm.tasks.TaskHelper;
import com.vmware.sdk.samples.sddcm.utils.SddcUtil;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.sddcm.model.ClusterUpdateSpec;
import com.vmware.sdk.sddcm.model.Task;
import com.vmware.sdk.sddcm.v1.V1Factory;

/**
 * Demonstrates how to delete a cluster.
 *
 * <p>Prerequisites before deleting the cluster:
 *
 * <ul>
 *   <li>Ensure that a cluster with the given name exists.
 *   <li>Migrate or backup the VMs and data on the data store associated with the cluster to another location.
 * </ul>
 */
public class DeleteClusterExample {
    private static final Logger log = LoggerFactory.getLogger(DeleteClusterExample.class);
    /** REQUIRED: SDDC Manager host address or FQDN. */
    public static String sddcManagerHostname = "sddcManagerHostname";
    /** REQUIRED: SDDC Manager SSO username. */
    public static String sddcManagerSsoUserName = "username";
    /** REQUIRED: SDDC Manager SSO password. */
    public static String sddcManagerSsoPassword = "password";
    /** REQUIRED: Cluster name to be deleted. */
    public static String clusterName = "wld-1-cluster2";

    private static final int TASK_POLL_TIME_IN_SECONDS = 300;

    public static void main(String[] args) {
        SampleCommandLineParser.load(DeleteClusterExample.class, args);

        try (SddcUtil.SddcFactory factory =
                new SddcUtil.SddcFactory(sddcManagerHostname, sddcManagerSsoUserName, sddcManagerSsoPassword)) {
            V1Factory v1Factory = factory.getV1Factory();

            // Get the clusterID which needs to be deleted
            String clusterID =
                    SddcManagerHelper.getClusterByName(v1Factory, clusterName).getId();
            // Create Cluster update Spec to delete a cluster
            ClusterUpdateSpec clusterUpdateSpec =
                    new ClusterUpdateSpec.Builder().setMarkForDeletion(true).build();

            // Cluster deletion involves 2 steps process.This 2-step deletion process ensures that a cluster is not
            // deleted accidentally.
            // Step-1: Initialize the deletion or mark the cluster for deletion.
            v1Factory
                    .clustersService()
                    .updateCluster(clusterID, clusterUpdateSpec)
                    .invoke()
                    .get();
            log.info("Completed marking the cluster for deletion");

            // Step-2: Trigger the cluster deletion
            Task deleteClusterTask = v1Factory
                    .clustersService()
                    .deleteCluster(clusterID)
                    .invoke()
                    .get();
            // TaskHelper utility to keep track of domain deletion workflow task
            boolean status = new TaskHelper()
                    .monitorTasks(
                            List.of(deleteClusterTask),
                            sddcManagerHostname,
                            sddcManagerSsoUserName,
                            sddcManagerSsoPassword,
                            TASK_POLL_TIME_IN_SECONDS);
            if (status) {
                log.info("Cluster deletion task succeeded");
            } else {
                log.error("Cluster deletion task failed");
            }
        } catch (Exception exception) {
            log.error("Exception while running the delete cluster workflow", exception);
        }
    }
}
