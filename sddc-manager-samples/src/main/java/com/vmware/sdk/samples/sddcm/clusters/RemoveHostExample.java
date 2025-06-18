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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.sddcm.constants.ResultStatus;
import com.vmware.sdk.samples.sddcm.helpers.SddcManagerHelper;
import com.vmware.sdk.samples.sddcm.tasks.TaskHelper;
import com.vmware.sdk.samples.sddcm.utils.SddcUtil;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.sddcm.model.ClusterCompactionSpec;
import com.vmware.sdk.sddcm.model.ClusterUpdateSpec;
import com.vmware.sdk.sddcm.model.HostReference;
import com.vmware.sdk.sddcm.model.Task;
import com.vmware.sdk.sddcm.model.Validation;
import com.vmware.sdk.sddcm.v1.Tasks;
import com.vmware.sdk.sddcm.v1.V1Factory;

/**
 * Demonstrates how to remove Host from a cluster.
 *
 * <p>Prerequisites before removing host from a cluster:
 *
 * <ul>
 *   <li>Ensure that Hosts with the given name exists inside the chosen cluster.
 *   <li>Ensure that you have enough hosts remaining to facilitate the configured vSAN availability.
 *   <li>Failure to do so might result in the datastore being marked as read-only or in data loss.
 * </ul>
 */
public class RemoveHostExample {
    private static final Logger log = LoggerFactory.getLogger(RemoveHostExample.class);
    /** REQUIRED: SDDC Manager host address or FQDN. */
    public static String sddcManagerHostname = "sddcManagerHostname";
    /** REQUIRED: SDDC Manager SSO username. */
    public static String sddcManagerSsoUserName = "username";
    /** REQUIRED: SDDC Manager SSO password. */
    public static String sddcManagerSsoPassword = "password";
    /** REQUIRED: Cluster name to be deleted. */
    public static String clusterName = "wld-1-cluster2";
    /** REQUIRED: Provide FQDN of the ESXi Host/Hosts to be deleted from the cluster. */
    public static String[] clusterCompactionHostList = new String[] {"esxi-11.sample.local,esxi-12.sample.local"};

    public static void main(String[] args) {
        SampleCommandLineParser.load(RemoveHostExample.class, args);

        try (SddcUtil.SddcFactory factory =
                new SddcUtil.SddcFactory(sddcManagerHostname, sddcManagerSsoUserName, sddcManagerSsoPassword)) {
            V1Factory v1Factory = factory.getV1Factory();

            // Create Cluster update Spec to remove host from the cluster
            ClusterUpdateSpec clusterUpdateSpec = getClusterUpdateSpec(v1Factory);

            // Validate Cluster Update Spec
            Validation validation = v1Factory
                    .clusters()
                    .validationsService()
                    .validateClusterUpdateSpec(
                            SddcManagerHelper.getClusterByName(v1Factory, clusterName)
                                    .getId(),
                            clusterUpdateSpec)
                    .invoke()
                    .get();

            if (validation.getResultStatus().equals(ResultStatus.SUCCEEDED.name())) {
                log.info("Remove host spec validation succeeded");
                // Remove host from the cluster
                Task removeHostTask = v1Factory
                        .clustersService()
                        .updateCluster(
                                SddcManagerHelper.getClusterByName(v1Factory, clusterName)
                                        .getId(),
                                clusterUpdateSpec)
                        .invoke()
                        .get();

                // Use Tasks service TaskHelper utility to keep track of the remove host workflow task
                Tasks taskService = v1Factory.tasksService();
                boolean status = new TaskHelper().monitorTask(removeHostTask, taskService);
                if (status) {
                    log.info("Remove host from cluster task succeeded");
                } else {
                    log.error("Remove host from cluster task failed");
                }
            } else {
                log.error("remove host spec validation failed");
            }
        } catch (Exception exception) {
            log.error("Exception while removing host from the cluster", exception);
        }
    }

    private static ClusterUpdateSpec getClusterUpdateSpec(V1Factory v1Factory) throws Exception {
        List<HostReference> hostReferenceList = new ArrayList<>();
        for (String host : clusterCompactionHostList) {
            hostReferenceList.add(new HostReference.Builder()
                    .setId(SddcManagerHelper.getHostsByName(v1Factory, host).getId())
                    .build());
        }
        return new ClusterUpdateSpec.Builder()
                .setClusterCompactionSpec(new ClusterCompactionSpec.Builder()
                        .setHosts(hostReferenceList)
                        .build())
                .build();
    }
}
