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
import com.vmware.sdk.sddcm.model.ClusterCreationSpec;
import com.vmware.sdk.sddcm.model.ClusterSpec;
import com.vmware.sdk.sddcm.model.ComputeSpec;
import com.vmware.sdk.sddcm.model.DatastoreSpec;
import com.vmware.sdk.sddcm.model.Domain;
import com.vmware.sdk.sddcm.model.HostNetworkSpec;
import com.vmware.sdk.sddcm.model.HostSpec;
import com.vmware.sdk.sddcm.model.NetworkSpec;
import com.vmware.sdk.sddcm.model.NsxClusterSpec;
import com.vmware.sdk.sddcm.model.NsxtClusterSpec;
import com.vmware.sdk.sddcm.model.Personality;
import com.vmware.sdk.sddcm.model.PortgroupSpec;
import com.vmware.sdk.sddcm.model.Task;
import com.vmware.sdk.sddcm.model.Validation;
import com.vmware.sdk.sddcm.model.VdsSpec;
import com.vmware.sdk.sddcm.model.VmNic;
import com.vmware.sdk.sddcm.model.VsanDatastoreSpec;
import com.vmware.sdk.sddcm.v1.V1Factory;

/**
 * Demonstrates adding cluster to the domain by domain name.
 *
 * <p>Prerequisites before adding cluster to domain:
 *
 * <ol>
 *   <li>Create the Network pool using CreateNetworkPool sample
 *   <li>Commission the host using HostCommission sample
 *   <li>Make sure given domain should be present in SDDC inventory
 *   <li>Make sure at least three hosts should be available in SDDC inventory pool
 *   <li>Make sure license keys for the product types(ESX and VSAN) should be present in SDDC license inventory
 * </ol>
 */
public class AddClusterExample {
    private static final Logger log = LoggerFactory.getLogger(AddClusterExample.class);
    /** REQUIRED: SDDC Manager host address or FQDN. */
    public static String sddcManagerHostname = "sddcManagerHostname";
    /** REQUIRED: SDDC Manager SSO username. */
    public static String sddcManagerSsoUserName = "username";
    /** REQUIRED: SDDC Manager SSO password. */
    public static String sddcManagerSsoPassword = "password";

    /** REQUIRED: Domain name to add cluster to given domain. */
    public static String domainName = "wld-1";
    /** REQUIRED: Name of the cluster to be created. */
    public static String clusterName = "wld-1-cluster2";
    /** REQUIRED: VMNIC1 ID provided during cluster creation. */
    public static String hostNetworkVmnic1Id = "vmnic0";
    /** REQUIRED: VMNIC1 VDS name provided during cluster creation. */
    public static String hostNetworkVmnic1Vds = "wld-1-cluster2-vds01";
    /** REQUIRED: VMNIC2 ID provided during cluster creation. */
    public static String hostNetworkVmnic2Id = "vmnic1";
    /** REQUIRED: VMNIC2 VDS name provided during cluster creation. */
    public static String hostNetworkVmnic2Vds = "wld-1-cluster2-vds01";
    /** REQUIRED: Provide FQDN of the ESX Hosts to be added to the cluster. */
    public static String host1 = "esxi-10.vrack.vsphere.local";
    /** REQUIRED: Provide FQDN of the ESX Hosts to be added to the cluster. */
    public static String host2 = "esxi-11.vrack.vsphere.local";
    /** REQUIRED: Provide FQDN of the ESX Hosts to be added to the cluster. */
    public static String host3 = "esxi-12.vrack.vsphere.local";
    /** REQUIRED: Provide cluster failure to tolerate to handle data store failures. */
    public static Long failuresToTolerate = 0L;
    /** REQUIRED: Provide datastore name. */
    public static String datastoreName = "wld-1-cluster2-vSanDatastore1";
    /** REQUIRED: VDS spec name. */
    public static String vdsSpecName = "wld-1-cluster2-vds01";
    /** REQUIRED: VDS spec port group1. */
    public static String portGroupSpecName1 = "wld-1-cluster2-vds01-mgmt";
    /** REQUIRED: VDS spec port transport type1. */
    public static String portGroupTransportType1 = "MANAGEMENT";
    /** REQUIRED: VDS spec port group2. */
    public static String portGroupSpecName2 = "wld-1-cluster2-vds01-vsan";
    /** REQUIRED: VDS spec port transport type2. */
    public static String portGroupTransportType2 = "VSAN";
    /** REQUIRED: VDS spec port group3. */
    public static String portGroupSpecName3 = "wld-1-cluster2-vds01-vmotion";
    /** REQUIRED: VDS spec port transport type3. */
    public static String portGroupTransportType3 = "VMOTION";
    /** REQUIRED: nsxClusterSpec VLAN ID. */
    public static Long geneveVlanId = 0L;
    /** REQUIRED: Name of the personality to create vlcm based cluster. */
    public static String personalityName = "personality-sdk";
    /** REQUIRED: Create cluster without license keys. */
    public static Boolean deployWithoutLicenseKeys = true;

    private static final int TASK_POLL_TIME_IN_SECONDS = 180;

    public static void main(String[] args) {
        SampleCommandLineParser.load(AddClusterExample.class, args);

        try (SddcUtil.SddcFactory factory =
                new SddcUtil.SddcFactory(sddcManagerHostname, sddcManagerSsoUserName, sddcManagerSsoPassword)) {
            V1Factory v1Factory = factory.getV1Factory();

            // Create Cluster creation Spec to add cluster to domain
            ClusterCreationSpec clusterCreationSpec = getClusterCreationSpec(v1Factory);

            // Validate Cluster creation spec
            Validation validation = v1Factory
                    .clusters()
                    .validationsService()
                    .validateClusterCreationSpec(clusterCreationSpec)
                    .invoke()
                    .get();

            if (validation.getResultStatus().equals(ResultStatus.SUCCEEDED.name())) {
                log.info("Add cluster spec validation succeeded");
                // Perform Create Cluster
                Task addClusterTask = v1Factory
                        .clustersService()
                        .createCluster(clusterCreationSpec)
                        .invoke()
                        .get();
                // TaskHelper utility to keep track of the add cluster workflow task
                boolean status = new TaskHelper()
                        .monitorTasks(
                                List.of(addClusterTask),
                                sddcManagerHostname,
                                sddcManagerSsoUserName,
                                sddcManagerSsoPassword,
                                TASK_POLL_TIME_IN_SECONDS);

                if (status) {
                    log.info("Add cluster task succeeded");
                } else {
                    log.error("Add cluster task failed");
                }
            } else {
                log.error("Add cluster spec validation failed");
            }
        } catch (Exception exception) {
            log.error("Exception while adding new cluster", exception);
        }
    }

    private static ClusterCreationSpec getClusterCreationSpec(V1Factory v1Factory) throws Exception {
        Domain domain = SddcManagerHelper.getDomainByName(v1Factory, domainName);
        return new ClusterCreationSpec.Builder()
                .setDomainId(domain.getId())
                .setComputeSpec(getComputeSpec(v1Factory))
                .setDeployWithoutLicenseKeys(deployWithoutLicenseKeys)
                .build();
    }

    private static ComputeSpec getComputeSpec(V1Factory v1Factory) throws Exception {
        List<ClusterSpec> clusterSpecs = new ArrayList<ClusterSpec>();
        clusterSpecs.add(new ClusterSpec.Builder()
                .setName(clusterName)
                .setClusterImageId(getPersonalityId(v1Factory))
                .setHostSpecs(getHostSpecs(v1Factory))
                .setDatastoreSpec(getDataStoreSpec())
                .setNetworkSpec(getNetworkSpec())
                .build());
        return new ComputeSpec.Builder().setClusterSpecs(clusterSpecs).build();
    }

    private static String getPersonalityId(V1Factory v1Factory) {
        Personality personality = null;
        try {
            personality = v1Factory
                    .personalitiesService()
                    .getPersonalities()
                    .personalityName(personalityName)
                    .invoke()
                    .get()
                    .getElements()
                    .stream()
                    .findFirst()
                    .orElse(null);
        } catch (Exception exception) {
            log.error("Failed to get personality Id for the personality {}", personalityName);
        }
        return personality != null ? personality.getPersonalityId() : null;
    }

    private static NetworkSpec getNetworkSpec() {
        List<PortgroupSpec> portGroupSpecs = new ArrayList<>();

        portGroupSpecs.add(new PortgroupSpec.Builder()
                .setName(portGroupSpecName1)
                .setTransportType(portGroupTransportType1)
                .build());
        portGroupSpecs.add(new PortgroupSpec.Builder()
                .setName(portGroupSpecName2)
                .setTransportType(portGroupTransportType2)
                .build());
        portGroupSpecs.add(new PortgroupSpec.Builder()
                .setName(portGroupSpecName3)
                .setTransportType(portGroupTransportType3)
                .build());

        NsxtClusterSpec nsxtClusterSpec =
                new NsxtClusterSpec.Builder().setGeneveVlanId(geneveVlanId).build();
        NsxClusterSpec nsxClusterSpec =
                new NsxClusterSpec.Builder().setNsxtClusterSpec(nsxtClusterSpec).build();

        VdsSpec vdsSpec = new VdsSpec.Builder()
                .setName(vdsSpecName)
                .setPortGroupSpecs(portGroupSpecs)
                .build();
        return new NetworkSpec.Builder()
                .setVdsSpecs(List.of(vdsSpec))
                .setNsxClusterSpec(nsxClusterSpec)
                .build();
    }

    private static DatastoreSpec getDataStoreSpec() {
        VsanDatastoreSpec vsanDatastoreSpec = new VsanDatastoreSpec.Builder()
                .setFailuresToTolerate(failuresToTolerate)
                .setDatastoreName(datastoreName)
                .build();
        return new DatastoreSpec.Builder()
                .setVsanDatastoreSpec(vsanDatastoreSpec)
                .build();
    }

    private static List<HostSpec> getHostSpecs(V1Factory v1Factory) throws Exception {
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
        hostSpecs.add(new HostSpec.Builder()
                .setHostNetworkSpec(hostNetworkSpec)
                .setId(SddcManagerHelper.getHostsByName(v1Factory, host1).getId())
                .build());
        hostSpecs.add(new HostSpec.Builder()
                .setHostNetworkSpec(hostNetworkSpec)
                .setId(SddcManagerHelper.getHostsByName(v1Factory, host2).getId())
                .build());
        hostSpecs.add(new HostSpec.Builder()
                .setHostNetworkSpec(hostNetworkSpec)
                .setId(SddcManagerHelper.getHostsByName(v1Factory, host3).getId())
                .build());

        return hostSpecs;
    }
}
