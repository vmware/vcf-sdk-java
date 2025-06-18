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
import com.vmware.sdk.sddcm.model.ClusterExpansionSpec;
import com.vmware.sdk.sddcm.model.ClusterUpdateSpec;
import com.vmware.sdk.sddcm.model.HostNetworkSpec;
import com.vmware.sdk.sddcm.model.HostSpec;
import com.vmware.sdk.sddcm.model.Task;
import com.vmware.sdk.sddcm.model.Validation;
import com.vmware.sdk.sddcm.model.VmNic;
import com.vmware.sdk.sddcm.v1.Tasks;
import com.vmware.sdk.sddcm.v1.V1Factory;

/**
 * Demonstrates adding host to the cluster by cluster name.
 *
 * <p>Prerequisites before adding host to cluster:
 *
 * <ol>
 *   <li>Create the Network pool using CreateNetworkPool sample
 *   <li>Commission the host using HostCommission sample
 *   <li>Make sure given cluster and domain should be present in SDDC inventory
 *   <li>Make sure at least one host should be available in SDDC inventory pool
 *   <li>Make sure license keys for the product type(ESX) should be present in SDDC license inventory
 * </ol>
 */
public class AddHostExample {
    private static final Logger log = LoggerFactory.getLogger(AddHostExample.class);
    /** REQUIRED: SDDC Manager host address or FQDN. */
    public static String sddcManagerHostname = "sddcManagerHostname";
    /** REQUIRED: SDDC Manager SSO username. */
    public static String sddcManagerSsoUserName = "username";
    /** REQUIRED: SDDC Manager SSO password. */
    public static String sddcManagerSsoPassword = "password";

    /** REQUIRED: Cluster name to add host to the given cluster. */
    public static String clusterName = "wld-1-cluster2";
    /** REQUIRED: VMNIC1 ID provided during cluster creation. */
    public static String hostNetworkVmnic1Id = "vmnic0";
    /** REQUIRED: VMNIC1 VDS name provided during cluster creation. */
    public static String hostNetworkVmnic1Vds = "wld-1-cluster2-vds01";
    /** REQUIRED: VMNIC2 ID provided during cluster creation. */
    public static String hostNetworkVmnic2Id = "vmnic1";
    /** REQUIRED: VMNIC2 VDS name provided during cluster creation. */
    public static String hostNetworkVmnic2Vds = "wld-1-cluster2-vds01";
    /** REQUIRED: Provide FQDN of the ESX Host/Hosts to be added to the cluster from free inventory pool. */
    public static String[] clusterExpansionHostList = new String[] {"esxi-13.vrack.vsphere.local"};
    /** REQUIRED: Add host without license keys. */
    public static Boolean deployWithoutLicenseKeys = true;

    public static void main(String[] args) {
        SampleCommandLineParser.load(AddHostExample.class, args);

        try (SddcUtil.SddcFactory factory =
                new SddcUtil.SddcFactory(sddcManagerHostname, sddcManagerSsoUserName, sddcManagerSsoPassword)) {
            V1Factory v1Factory = factory.getV1Factory();
            // Create Cluster update Spec to add host to cluster
            ClusterUpdateSpec clusterUpdateSpec = getClusterUpdateSpec(v1Factory);

            // Validate Cluster Spec
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
                log.info("Add host spec validation succeeded");
                // Add host to cluster spec
                Task addHostTask = v1Factory
                        .clustersService()
                        .updateCluster(
                                SddcManagerHelper.getClusterByName(v1Factory, clusterName)
                                        .getId(),
                                clusterUpdateSpec)
                        .invoke()
                        .get();

                // Use Tasks service TaskHelper utility to keep track of the add host workflow task
                Tasks taskService = v1Factory.tasksService();
                boolean status = new TaskHelper().monitorTask(addHostTask, taskService);
                if (status) {
                    log.info("Add host to cluster task succeeded");
                } else {
                    log.error("Add host to cluster task failed");
                }
            } else {
                log.error("Add host spec validation failed");
            }
        } catch (Exception exception) {
            log.error("Exception while adding host to the cluster", exception);
        }
    }

    private static ClusterUpdateSpec getClusterUpdateSpec(V1Factory v1Factory) throws Exception {
        return new ClusterUpdateSpec.Builder()
                .setClusterExpansionSpec(getClusterExpansionSpec(v1Factory))
                .build();
    }

    private static ClusterExpansionSpec getClusterExpansionSpec(V1Factory v1Factory) throws Exception {
        List<VmNic> vmNics = new ArrayList<>();
        vmNics.add(new VmNic.Builder()
                .setId(hostNetworkVmnic1Id)
                .setVdsName(hostNetworkVmnic1Vds)
                .build());
        vmNics.add(new VmNic.Builder()
                .setId(hostNetworkVmnic2Id)
                .setVdsName(hostNetworkVmnic2Vds)
                .build());
        HostNetworkSpec hostNetworkSpec =
                new HostNetworkSpec.Builder().setVmNics(vmNics).build();

        List<HostSpec> hostSpecs = new ArrayList<>();
        for (String host : clusterExpansionHostList) {
            hostSpecs.add(new HostSpec.Builder()
                    .setId(SddcManagerHelper.getHostsByName(v1Factory, host).getId())
                    .setHostNetworkSpec(hostNetworkSpec)
                    .build());
        }
        return new ClusterExpansionSpec.Builder()
                .setHostSpecs(hostSpecs)
                .setDeployWithoutLicenseKeys(deployWithoutLicenseKeys)
                .build();
    }
}
