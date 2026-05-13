/*
 * ******************************************************************
 * Copyright (c) 2025-2026 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.nsx.vpc;

import java.util.Collections;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.nsx.model.IpAddressBlock;
import com.vmware.sdk.nsx.model.Org;
import com.vmware.sdk.nsx.model.Project;
import com.vmware.sdk.nsx.model.SubnetDhcpConfig;
import com.vmware.sdk.nsx.model.TransitGatewayListResult;
import com.vmware.sdk.nsx.model.Vpc;
import com.vmware.sdk.nsx.model.VpcAttachment;
import com.vmware.sdk.nsx.model.VpcConnectivityProfile;
import com.vmware.sdk.nsx.model.VpcDhcpAdvancedConfig;
import com.vmware.sdk.nsx.model.VpcDhcpServerConfig;
import com.vmware.sdk.nsx.model.VpcProfileDhcpConfig;
import com.vmware.sdk.nsx.model.VpcServiceProfile;
import com.vmware.sdk.nsx.model.VpcSubnet;
import com.vmware.sdk.nsx.policy.api.v1.Orgs;
import com.vmware.sdk.nsx.policy.api.v1.infra.IpBlocks;
import com.vmware.sdk.nsx.policy.api.v1.orgs.Projects;
import com.vmware.sdk.nsx.policy.api.v1.orgs.projects.TransitGateways;
import com.vmware.sdk.nsx.policy.api.v1.orgs.projects.VpcConnectivityProfiles;
import com.vmware.sdk.nsx.policy.api.v1.orgs.projects.VpcServiceProfiles;
import com.vmware.sdk.nsx.policy.api.v1.orgs.projects.Vpcs;
import com.vmware.sdk.nsx.policy.api.v1.orgs.projects.vpcs.Attachments;
import com.vmware.sdk.nsx.policy.api.v1.orgs.projects.vpcs.Subnets;
import com.vmware.sdk.samples.nsx.helpers.NsxHelper;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.vapi.client.ApiClient;

/**
 * Example to create VPC Subnet and associated objects as, Project, VPC, IPBlocks, VpcServiceProfile, VPCAttachment.
 * <p>
 * The Subnet created is a Public Subnet with DHCP mode as DHCP_SERVER under VPC.
 * <p>
 * The subnet traffic is routed out of VPC via Transit Gateway.
 * <p>
 * The Transit Gateway is attached to VPC using VPCConnectivityProfile.
 * <p>
 * The DHCP Server configurations are provided in VPCServiceProfile.
 *
 * <p>Sample prerequisites: NSX Manager instance with default org created.
 *
 * <p> APIs USED:
 * <ul>
 *     <li>Create IP block : PUT /policy/api/v1/infra/ip-blocks/{ip-block-id}
 *     <li>GET custom project : GET /policy/api/v1/orgs/{org-id}/projects/{project-id}
 *     <li>Create Vpc Service Profile : PUT /policy/api/v1/orgs/{org-id}/projects/{project-id}/vpc-service-profiles/{vpc-service-profile-id}
 *     <li>Create VPC : PUT policy/api/v1/orgs/{org-id}/projects/{project-id}/vpcs/{vpc-id}
 *     <li>Create VPC Connectivity Profile : PUT /policy/api/v1/orgs/{org-id}/projects/{project-id}/vpc-connectivity-profiles/{vpc-connectivity-profile-id}
 *     <li>Create VPC Attachment : PUT /policy/api/v1/orgs/{org-id}/projects/{project-id}/vpcs/{vpc-id}/attachments/{vpc-attachment-id}
 *     <li>Create VPC Subnet : PUT /policy/api/v1/orgs/{org-id}/projects/{project-id}/vpcs/{vpc-id}/subnets/{subnet-id}
 * </ul>
 */
public class CreateVpcSubnet {

    private static final Logger log = LoggerFactory.getLogger(CreateVpcSubnet.class);
    /** REQUIRED: NSX host address or FQDN. */
    public static String nsxHostname = "hostname";
    /** REQUIRED: NSX username. */
    public static String nsxUsername = "username";
    /** REQUIRED: NSX password. */
    public static String nsxPassword = "password";
    /** OPTIONAL: CIDR of External IP block be used by VpcSubnet that is to be created */
    public static String externalIpCidr;
    /** OPTIONAL: Name of External IP block be used by VpcSubnet that is to be created */
    public static String externalIpName;
    /** OPTIONAL: Vpc Service profile ID */
    public static String vpcServiceProfile;
    /** OPTIONAL: Vpc Connectivity profile ID */
    public static String vpcConnectivityProfile;
    /** OPTIONAL: Vpc Attachment Id */
    public static String vpcAttachmentId;
    /** OPTIONAL: Subnet ID for subnet to be created under Vpc */
    public static String vpcSubnetId;
    /** OPTIONAL: Custom Project name */
    public static String customProjectName;
    /** OPTIONAL: Vpc Id */
    public static String vpcId;

    /* Default values of above optional values, in-case, they are not provided as command line arguments. */
    private static final String EXTERNAL_IP_CIDR = "142.2.0.0/24";

    private static final String VCF_NSX_SDK_EXTERNAL_IP = "VCF-NSX-SDK-external-ip";
    private static final String VCF_NSX_SDK_SERVICE_PROFILE = "VCF-NSX-SDK-serviceProfile";
    private static final String VCF_NSX_SDK_CONNECTIVITY_PROFILE = "VCF-NSX-SDK-connectivity-profile";
    private static final String VCF_NSX_SDK_VPC_ATTACHMENT_ID = "VCF-NSX-SDK-vpcAttachment";
    private static final String VCF_NSX_SDK_VPC_SUBNET = "VCF-NSX-SDK-vpcSubnet";
    private static final String PROJECT_NAME = "VCF-NSX-SDK-Project";
    private static final String VPC_ID = "VCF-NSX-SDK-VPC";

    /* Keyword reserved for Pre defined values. */
    private static final String DEFAULT_NAME = "default";

    public static void main(String[] args) {
        SampleCommandLineParser.load(CreateVpcSubnet.class, args);
        initUnsetOptionalParameters();
        try (NsxHelper.NsxFactory nsxFactory = new NsxHelper.NsxFactory(nsxHostname, nsxUsername, nsxPassword)) {
            ApiClient apiClient = nsxFactory.getApiClient();
            Orgs orgService = apiClient.createStub(Orgs.class);
            Projects projectService = apiClient.createStub(Projects.class);
            Vpcs vpcService = apiClient.createStub(Vpcs.class);
            VpcServiceProfiles serviceProfileService = apiClient.createStub(VpcServiceProfiles.class);
            TransitGateways transitGatewayService = apiClient.createStub(TransitGateways.class);
            IpBlocks ipBlocks = apiClient.createStub(IpBlocks.class);
            Subnets vpcSubnetService = apiClient.createStub(Subnets.class);

            // Read default org, make sure it exists.
            Org defaultOrg = orgService.policyLmGetOrg(DEFAULT_NAME).invoke().get();

            IpAddressBlock externalIpBlock = new IpAddressBlock.Builder()
                    .setCidr(externalIpCidr)
                    .setIpAddressType(IpAddressBlock.IP_ADDRESS_TYPE_IPV4)
                    .setVisibility(IpAddressBlock.VISIBILITY_EXTERNAL)
                    .setId(externalIpName)
                    .build();
            // Please use ipBlocks.policyLmCreateOrReplaceIpAddressBlock() to catch exceptions while creating IP Blocks
            ipBlocks.policyLmCreateOrPatchIpAddressBlock(externalIpBlock.getId(), externalIpBlock)
                    .invoke();
            externalIpBlock = ipBlocks.policyLmReadIpAddressBlock(externalIpBlock.getId())
                    .invoke()
                    .get();
            log.info("External IP block path :{}", externalIpBlock.getPath());

            Project customProject = new Project.Builder()
                    .setId(customProjectName)
                    .setExternalIpv4Blocks(Collections.singletonList(externalIpBlock.getPath()))
                    .build();
            projectService
                    .policyLmUpdateProject(defaultOrg.getId(), customProject.getId(), customProject)
                    .invoke()
                    .get();
            customProject = projectService
                    .policyLmGetProject(defaultOrg.getId(), customProject.getId())
                    .invoke()
                    .get();
            customProject.setExternalIpv4Blocks(Collections.singletonList(externalIpBlock.getPath()));
            projectService
                    .policyLmUpdateProject(defaultOrg.getId(), customProject.getId(), customProject)
                    .invoke()
                    .get();

            VpcDhcpAdvancedConfig vpcDhcpAdvancedConfig = new VpcDhcpAdvancedConfig.Builder()
                    .setIsDistributedDhcp(false)
                    .build();
            VpcDhcpServerConfig vpcDhcpServerConfig = new VpcDhcpServerConfig.Builder()
                    .setLeaseTime(86400L)
                    .setAdvancedConfig(vpcDhcpAdvancedConfig)
                    .build();
            VpcProfileDhcpConfig vpcProfileDhcpConfig = new VpcProfileDhcpConfig.Builder()
                    .setDhcpServerConfig(vpcDhcpServerConfig)
                    .build();

            VpcServiceProfile vpcServiceProfile = new VpcServiceProfile.Builder()
                    .setId(CreateVpcSubnet.vpcServiceProfile)
                    .setDhcpConfig(vpcProfileDhcpConfig)
                    .build();
            serviceProfileService
                    .policyLmPatchVpcServiceProfile(
                            defaultOrg.getId(), customProject.getId(), CreateVpcSubnet.vpcServiceProfile, vpcServiceProfile)
                    .invoke();

            Vpc vpc = new Vpc.Builder()
                    .setId(vpcId)
                    .setVpcServiceProfile(vpcServiceProfile.getPath())
                    .build();
            vpcService
                    .policyLmUpdateVpc(defaultOrg.getId(), customProject.getId(), vpcId, vpc)
                    .invoke()
                    .get();
            TransitGatewayListResult transitGatewayListResult =
                    transitGatewayService.policyLmListTransitGateway(defaultOrg.getId(), customProject.getId()).invoke().get();
            String DEFAULT_TRANSIT_GATEWAY_PATH = transitGatewayListResult.getResults().get(0).getPath();
            VpcConnectivityProfiles vpcConnectivityProfileService = apiClient.createStub(VpcConnectivityProfiles.class);
            VpcConnectivityProfile connectivityProfile = new VpcConnectivityProfile.Builder()
                    .setId(vpcConnectivityProfile)
                    .setTransitGatewayPath(DEFAULT_TRANSIT_GATEWAY_PATH)
                    .setExternalIpBlocks(Collections.singletonList(externalIpBlock.getPath()))
                    .build();
            connectivityProfile = vpcConnectivityProfileService
                    .policyLmCreateOrReplaceVpcConnectivityProfile(
                            defaultOrg.getId(), customProject.getId(), connectivityProfile.getId(), connectivityProfile)
                    .invoke()
                    .get();

            Attachments vpcAttachmentService = apiClient.createStub(Attachments.class);
            VpcAttachment vpcAttachment = new VpcAttachment.Builder()
                    .setId(vpcAttachmentId)
                    .setVpcConnectivityProfile(connectivityProfile.getPath())
                    .build();
            vpcAttachmentService
                    .policyLmUpdateVpcAttachment(
                            defaultOrg.getId(), customProject.getId(), vpc.getId(), vpcAttachment.getId(), vpcAttachment)
                    .invoke()
                    .get();

            SubnetDhcpConfig subnetDhcpConfig = new SubnetDhcpConfig.Builder()
                    .setMode(SubnetDhcpConfig.MODE_DHCP_SERVER)
                    .build();
            VpcSubnet vpcSubnet = new VpcSubnet.Builder()
                    .setId(vpcSubnetId)
                    .setAccessMode(VpcSubnet.ACCESS_MODE_PUBLIC)
                    .setIpBlocks(Collections.singletonList(externalIpBlock.getPath()))
                    .setIpv4SubnetSize(16L)
                    .setSubnetDhcpConfig(subnetDhcpConfig)
                    .build();
            vpcSubnet = vpcSubnetService
                    .policyLmUpdateVpcSubnet(defaultOrg.getId(), customProject.getId(), vpc.getId(), vpcSubnet.getId(), vpcSubnet)
                    .invoke()
                    .get();
            log.info("Subnet Created : {}", vpcSubnet);

        } catch (Exception e) {
            log.error("Unexpected error", e);
        }
    }

    private static void initUnsetOptionalParameters() {
        if (Objects.isNull(externalIpCidr)) {
            externalIpCidr = EXTERNAL_IP_CIDR;
        }
        if (Objects.isNull(externalIpName)) {
            externalIpName = VCF_NSX_SDK_EXTERNAL_IP;
        }
        if (Objects.isNull(vpcServiceProfile)) {
            vpcServiceProfile = VCF_NSX_SDK_SERVICE_PROFILE;
        }
        if (Objects.isNull(vpcConnectivityProfile)) {
            vpcConnectivityProfile = VCF_NSX_SDK_CONNECTIVITY_PROFILE;
        }
        if (Objects.isNull(vpcAttachmentId)) {
            vpcAttachmentId = VCF_NSX_SDK_VPC_ATTACHMENT_ID;
        }
        if (Objects.isNull(vpcSubnetId)) {
            vpcSubnetId = VCF_NSX_SDK_VPC_SUBNET;
        }
        if (Objects.isNull(customProjectName)) {
            customProjectName = PROJECT_NAME;
        }
        if (Objects.isNull(vpcId)) {
            vpcId = VPC_ID;
        }
    }
}
