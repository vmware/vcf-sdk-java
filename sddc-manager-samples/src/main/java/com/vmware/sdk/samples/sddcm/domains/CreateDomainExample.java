/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.sddcm.domains;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.sddcm.constants.ResultStatus;
import com.vmware.sdk.samples.sddcm.helpers.SddcManagerHelper;
import com.vmware.sdk.samples.sddcm.tasks.TaskHelper;
import com.vmware.sdk.samples.sddcm.utils.SddcUtil;
import com.vmware.sdk.samples.sddcm.utils.StringUtil;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.sddcm.model.ClusterSpec;
import com.vmware.sdk.sddcm.model.ComputeSpec;
import com.vmware.sdk.sddcm.model.DatastoreSpec;
import com.vmware.sdk.sddcm.model.DomainCreationSpec;
import com.vmware.sdk.sddcm.model.HostNetworkSpec;
import com.vmware.sdk.sddcm.model.HostSpec;
import com.vmware.sdk.sddcm.model.NetworkDetailsSpec;
import com.vmware.sdk.sddcm.model.NetworkSpec;
import com.vmware.sdk.sddcm.model.NsxClusterSpec;
import com.vmware.sdk.sddcm.model.NsxManagerSpec;
import com.vmware.sdk.sddcm.model.NsxtClusterSpec;
import com.vmware.sdk.sddcm.model.NsxtSpec;
import com.vmware.sdk.sddcm.model.Personality;
import com.vmware.sdk.sddcm.model.PortgroupSpec;
import com.vmware.sdk.sddcm.model.SsoDomainSpec;
import com.vmware.sdk.sddcm.model.Task;
import com.vmware.sdk.sddcm.model.Validation;
import com.vmware.sdk.sddcm.model.VcenterSpec;
import com.vmware.sdk.sddcm.model.VdsSpec;
import com.vmware.sdk.sddcm.model.VmNic;
import com.vmware.sdk.sddcm.model.VsanDatastoreSpec;
import com.vmware.sdk.sddcm.v1.V1Factory;

/**
 * Demonstrates creating new workload domain by domain name.
 *
 * <p>Prerequisites before creating workload domain:
 *
 * <ul>
 *   <li>Create the Network pool using CreateNetworkPool sample
 *   <li>Commission the host using HostCommission sample
 *   <li>Make sure at least three hosts should be available in SDDC inventory pool
 *   <li>Make sure license keys for the product types(ESX, VSAN and NSXT) should be present in SDDC license inventory
 * </ul>
 */
public class CreateDomainExample {
    private static final Logger log = LoggerFactory.getLogger(CreateDomainExample.class);
    /** REQUIRED: SDDC Manager host address or FQDN. */
    public static String sddcManagerHostname = "sddcManagerHostname";
    /** REQUIRED: SDDC Manager SSO username. */
    public static String sddcManagerSsoUserName = "username";
    /** REQUIRED: SDDC Manager SSO password. */
    public static String sddcManagerSsoPassword = "password";

    /** REQUIRED: Name of the domain to be created. */
    public static String domainName = "wld-1";
    /** REQUIRED: Name of the organisation. */
    public static String orgName = "vcf";
    /** REQUIRED: New workload vcenter Name. */
    public static String vcenterName = "vcenter-wld-1";
    /** REQUIRED: New workload vcenter Root Password. */
    public static String vcenterRootPassword = "vcenterRootPassword";
    /** REQUIRED: New workload vcenter data center name. */
    public static String vcenterDataCenterName = "wld-1-dc";
    /** REQUIRED: New workload vcenter DNS. */
    public static String networkDetailsVcFqdn = "vcenter-wld-1.vrack.vsphere.local";
    /** REQUIRED: New workload vcenter Gateway. */
    public static String networkDetailsVcGateway = "10.0.0.250";
    /** REQUIRED: New workload vcenter subnet mask. */
    public static String networkDetailsVcSubnetMask = "255.255.255.0";
    /** REQUIRED: SSO Domain Name. */
    public static String ssoDomainName = "wld1.local";
    /** REQUIRED: SSO Domain Password. */
    public static String ssoDomainPassword = "VMware123!VMware123!";
    /** REQUIRED: Name of the cluster to be created. */
    public static String clusterName = "wld-1-cluster1";
    /** REQUIRED: VMNIC1 ID provided during cluster creation. */
    public static String hostNetworkVmnic1Id = "vmnic0";
    /** REQUIRED: VMNIC1 VDS name provided during cluster creation. */
    public static String hostNetworkVmnic1Vds = "wld-1-cluster1-vds01";
    /** REQUIRED: VMNIC2 ID provided during cluster creation. */
    public static String hostNetworkVmnic2Id = "vmnic1";
    /** REQUIRED: VMNIC2 VDS name provided during cluster creation. */
    public static String hostNetworkVmnic2Vds = "wld-1-cluster1-vds01";
    /** REQUIRED: Provide FQDN of the ESX Hosts to be added to the new domain. */
    public static String host1 = "esxi-7.vrack.vsphere.local";
    /** REQUIRED: Provide FQDN of the ESX Hosts to be added to the new domain. */
    public static String host2 = "esxi-8.vrack.vsphere.local";
    /** REQUIRED: Provide FQDN of the ESX Hosts to be added to the new domain. */
    public static String host3 = "esxi-9.vrack.vsphere.local";
    /** REQUIRED: Provide Ip Address of the ESX Hosts to be added to the new domain. */
    public static String host1IpAddress = "10.0.0.106";
    /** REQUIRED: Provide Ip Address of the ESX Hosts to be added to the new domain. */
    public static String host2IpAddress = "10.0.0.107";
    /** REQUIRED: Provide Ip Address of the ESX Hosts to be added to the new domain. */
    public static String host3IpAddress = "10.0.0.108";
    /** REQUIRED: Provide root username of the ESX Hosts to be added to the new domain. */
    public static String hostRootUserName = "root";
    /** REQUIRED: Provide cluster failure to tolerate to handle data store failures. */
    public static Long failuresToTolerate = 0L;
    /** REQUIRED: Provide datastore name. */
    public static String datastoreName = "wld-1-cluster1-vSanDatastore1";
    /** REQUIRED: VDS spec name. */
    public static String vdsSpecName = "wld-1-cluster1-vds01";
    /** REQUIRED: VDS spec port group1. */
    public static String portGroupSpecName1 = "wld-1-cluster1-vds01-mgmt";
    /** REQUIRED: VDS spec port transport type1. */
    public static String portGroupTransportType1 = "MANAGEMENT";
    /** REQUIRED: VDS spec port group2. */
    public static String portGroupSpecName2 = "wld-1-cluster1-vds01-vsan";
    /** REQUIRED: VDS spec port transport type2. */
    public static String portGroupTransportType2 = "VSAN";
    /** REQUIRED: VDS spec port group3. */
    public static String portGroupSpecName3 = "wld-1-cluster1-vds01-vmotion";
    /** REQUIRED: VDS spec port transport type3. */
    public static String portGroupTransportType3 = "VMOTION";
    /** REQUIRED: nsxClusterSpec VLAN ID. */
    public static Long geneveVlanId = 0L;
    /** REQUIRED: NSXT VIP FQDN. */
    public static String nsxtVipFqdn = "vip-wld-1-nsxmanager.vrack.vsphere.local";
    /** REQUIRED: NSX Manager admin password. */
    public static String nsxManagerAdminPassword = "VMware123!VMware123!";
    /** REQUIRED: NSX Manager form factor. */
    public static String nsxFormFactor = "medium";
    /** REQUIRED: NSX manager1 name. */
    public static String nsxtManager1Name = "wld-1-nsxt-manager-1";
    /** REQUIRED: NSX manager1 DNS Name. */
    public static String nsxt1DnsName = "wld-1-nsxt-manager-1.vrack.vsphere.local";
    /** REQUIRED: NSX manager1 gateway. */
    public static String nsxt1Gateway = "10.0.0.250";
    /** REQUIRED: NSX manager1 subnet mask. */
    public static String nsxt1SubnetMask = "255.255.255.0";
    /** REQUIRED: NSX manager2 name. */
    public static String nsxtManager2Name = "wld-1-nsxt-manager-2";
    /** REQUIRED: NSX manager2 DNS Name. */
    public static String nsxt2DnsName = "wld-1-nsxt-manager-2.vrack.vsphere.local";
    /** REQUIRED: NSX manager2 gateway. */
    public static String nsxt2Gateway = "10.0.0.250";
    /** REQUIRED: NSX manager2 subnet mask. */
    public static String nsxt2SubnetMask = "255.255.255.0";
    /** REQUIRED: NSX manager3 name. */
    public static String nsxtManager3Name = "wld-1-nsxt-manager-3";
    /** REQUIRED: NSX manager3 DNS Name. */
    public static String nsxt3DnsName = "wld-1-nsxt-manager-3.vrack.vsphere.local";
    /** REQUIRED: NSX manager3 gateway . */
    public static String nsxt3Gateway = "10.0.0.250";
    /** REQUIRED: NSX manager3 subnet mask. */
    public static String nsxt3SubnetMask = "255.255.255.0";
    /** REQUIRED: Name of the personality to create vlcm based cluster. */
    public static String personalityName = "personality-sdk";
    /** REQUIRED: Deploy workload domain without license keys. */
    public static Boolean deployWithoutLicenseKeys = true;

    private static final int TASK_POLL_TIME_IN_SECONDS = 300;

    public static void main(String[] args) {
        SampleCommandLineParser.load(CreateDomainExample.class, args);

        try (SddcUtil.SddcFactory factory =
                new SddcUtil.SddcFactory(sddcManagerHostname, sddcManagerSsoUserName, sddcManagerSsoPassword)) {
            V1Factory v1Factory = factory.getV1Factory();
            // Prepare workload domain creation spec
            DomainCreationSpec domainCreationSpec = getDomainCreationSpec(v1Factory);
            log.info("Domain Spec {}", StringUtil.toPrettyString(domainCreationSpec));

            // Validate workload domain creation spec
            Validation validation = v1Factory
                    .domains()
                    .validationsService()
                    .validateDomainCreationSpec(domainCreationSpec)
                    .invoke()
                    .get();
            if (validation.getResultStatus().equals(ResultStatus.SUCCEEDED.name())) {
                log.info("Create domain spec validation succeeded");
                // Create workload domain
                Task createDomainTask = v1Factory
                        .domainsService()
                        .createDomain(domainCreationSpec)
                        .invoke()
                        .get();
                // TaskHelper utility to keep track of domain creation workflow task
                boolean status = new TaskHelper()
                        .monitorTasks(
                                List.of(createDomainTask),
                                sddcManagerHostname,
                                sddcManagerSsoUserName,
                                sddcManagerSsoPassword,
                                TASK_POLL_TIME_IN_SECONDS);
                if (status) {
                    log.info("Create domain task succeeded");
                } else {
                    log.error("Create domain task failed");
                }
            } else {
                log.error("Create domain spec validation failed");
            }
        } catch (Exception exception) {
            log.error("Exception while creating domain", exception);
        }
    }

    private static DomainCreationSpec getDomainCreationSpec(V1Factory v1Factory) throws Exception {
        return new DomainCreationSpec.Builder()
                .setDomainName(domainName)
                .setOrgName(orgName)
                .setVcenterSpec(getVcenterSpec())
                .setSsoDomainSpec(getSsoDomainSpec())
                .setComputeSpec(getComputeSpec(v1Factory))
                .setNsxtSpec(getNsxtSpec())
                .setDeployWithoutLicenseKeys(deployWithoutLicenseKeys)
                .build();
    }

    private static SsoDomainSpec getSsoDomainSpec() {
        return new SsoDomainSpec.Builder()
                .setSsoDomainName(ssoDomainName)
                .setSsoDomainPassword(ssoDomainPassword)
                .build();
    }

    private static VcenterSpec getVcenterSpec() {
        NetworkDetailsSpec networkDetailsSpec = new NetworkDetailsSpec.Builder()
                .setDnsName(networkDetailsVcFqdn)
                .setGateway(networkDetailsVcGateway)
                .setSubnetMask(networkDetailsVcSubnetMask)
                .build();
        return new VcenterSpec.Builder()
                .setName(vcenterName)
                .setNetworkDetailsSpec(networkDetailsSpec)
                .setRootPassword(vcenterRootPassword)
                .setDatacenterName(vcenterDataCenterName)
                .build();
    }

    private static ComputeSpec getComputeSpec(V1Factory v1Factory) throws Exception {
        List<ClusterSpec> clusterSpecs = new ArrayList<>();
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
                .setHostName(host1)
                .setIpAddress(host1IpAddress)
                .setUsername(hostRootUserName)
                .build());
        hostSpecs.add(new HostSpec.Builder()
                .setHostNetworkSpec(hostNetworkSpec)
                .setId(SddcManagerHelper.getHostsByName(v1Factory, host2).getId())
                .setHostName(host2)
                .setIpAddress(host2IpAddress)
                .setUsername(hostRootUserName)
                .build());
        hostSpecs.add(new HostSpec.Builder()
                .setHostNetworkSpec(hostNetworkSpec)
                .setId(SddcManagerHelper.getHostsByName(v1Factory, host3).getId())
                .setHostName(host3)
                .setIpAddress(host3IpAddress)
                .setUsername(hostRootUserName)
                .build());

        return hostSpecs;
    }

    private static NsxtSpec getNsxtSpec() {
        return new NsxtSpec.Builder()
                .setNsxManagerSpecs(getNsxManagerSpecs())
                .setVipFqdn(nsxtVipFqdn)
                .setNsxManagerAdminPassword(nsxManagerAdminPassword)
                .setFormFactor(nsxFormFactor)
                .build();
    }

    private static List<NsxManagerSpec> getNsxManagerSpecs() {
        List<NsxManagerSpec> nsxManagerSpecs = new ArrayList<>();
        nsxManagerSpecs.add(new NsxManagerSpec.Builder()
                .setName(nsxtManager1Name)
                .setNetworkDetailsSpec(createNetworkDetailSpec(nsxt1DnsName, nsxt1Gateway, nsxt1SubnetMask))
                .build());
        nsxManagerSpecs.add(new NsxManagerSpec.Builder()
                .setName(nsxtManager2Name)
                .setNetworkDetailsSpec(createNetworkDetailSpec(nsxt2DnsName, nsxt2Gateway, nsxt2SubnetMask))
                .build());
        nsxManagerSpecs.add(new NsxManagerSpec.Builder()
                .setName(nsxtManager3Name)
                .setNetworkDetailsSpec(createNetworkDetailSpec(nsxt3DnsName, nsxt3Gateway, nsxt3SubnetMask))
                .build());
        return nsxManagerSpecs;
    }

    private static NetworkDetailsSpec createNetworkDetailSpec(String dnsName, String gateway, String subnetMask) {
        return new NetworkDetailsSpec.Builder()
                .setDnsName(dnsName)
                .setGateway(gateway)
                .setSubnetMask(subnetMask)
                .build();
    }
}
