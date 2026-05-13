/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
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

import com.vmware.sdk.nsx.model.Org;
import com.vmware.sdk.nsx.model.Project;
import com.vmware.sdk.nsx.model.Vpc;
import com.vmware.sdk.nsx.model.VpcDhcpRelayConfig;
import com.vmware.sdk.nsx.model.VpcProfileDhcpConfig;
import com.vmware.sdk.nsx.model.VpcServiceProfile;
import com.vmware.sdk.nsx.policy.api.v1.Orgs;
import com.vmware.sdk.nsx.policy.api.v1.orgs.Projects;
import com.vmware.sdk.nsx.policy.api.v1.orgs.projects.VpcServiceProfiles;
import com.vmware.sdk.nsx.policy.api.v1.orgs.projects.Vpcs;
import com.vmware.sdk.samples.nsx.helpers.NsxHelper;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.vapi.client.ApiClient;

/**
 * The example shows basic creation operation of a Vpc Service Profile and a VPC
 * under a default Project, in Vmware NSX.
 * <p>Sample prerequisites: NSX Manager instance with default org created.
 *
 * <p>APIs used:
 * <ul>
 *      <li> Create custom project: PUT /policy/api/v1/orgs/{org-id}/projects/{project-id}
 *      <li> Get custom project: GET /policy/api/v1/orgs/{org-id}/projects/{project-id}
 *      <li> Create Vpc Service Profile: PUT /policy/api/v1/orgs/{org-id}/projects/{project-id}/vpc-service-profiles/{vpc-service-profile-id}
 *      <li> Create VPC: PUT /policy/api/v1/orgs/{org-id}/projects/{project-id}/vpcs/{vpc-id}
 * </ul>
*/
public class CreateVpc {

    private static final Logger log = LoggerFactory.getLogger(CreateVpc.class);
    /** REQUIRED: NSX host address or FQDN. */
    public static String nsxHostname = "hostname";
    /** REQUIRED: NSX username. */
    public static String nsxUsername = "username";
    /** REQUIRED: NSX password. */
    public static String nsxPassword = "password";
    /** OPTIONAL: DHCP Server address to be set in Vpc Service Config. */
    public static String dhcpServerAddress;
    /** OPTIONAL: Vpc Service Profile name. */
    public static String serviceProfile;
    /** OPTIONAL: Vpc Private Ips. */
    public static String vpcPrivateIps;
    /** OPTIONAL: Project ID for custom project. */
    public static String projectId;
    /** OPTIONAL: Vpc ID to be created. */
    public static String vpcId;

    /* Constants to create VPC, with example values, please modify as required. To be used in case OPTIONAL values are
     * not provided as command line args. */
    private static final String DHCP_SERVER_ADDRESS = "10.24.2.1";
    private static final String SERVICE_PROFILE = "VCF-NSX-SDK-vpc-service-profile";
    private static final String VPC_PRIVATE_IPS = "10.1.1.0/24";
    private static final String PROJECT_ID = "VCF-NSX-SDK-project1";
    private static final String VPC_ID = "VCF-NSX-SDK-custom-vpc1";

    /* Keyword is reserved for Default objects. */
    private static final String DEFAULT = "default";

    public static void main(String[] args) {
        SampleCommandLineParser.load(CreateVpc.class, args);
        initUnsetOptionalParameters();
        try (NsxHelper.NsxFactory nsxFactory = new NsxHelper.NsxFactory(nsxHostname, nsxUsername, nsxPassword)) {
            ApiClient apiClient = nsxFactory.getApiClient();
            /*
              Initialize service classes required to create VPC: Org Service, Project Service, VpcServiceProfile
              Service, Vpc Service.
             */
            Orgs orgService = apiClient.createStub(Orgs.class);
            Vpcs vpcService = apiClient.createStub(Vpcs.class);
            VpcServiceProfiles serviceProfileService = apiClient.createStub(VpcServiceProfiles.class);
            Projects projectService = apiClient.createStub(Projects.class);

            /* Fetch default org to create a custom project. */
            Org defaultOrg = orgService.policyLmGetOrg(DEFAULT).invoke().get();
            Project customProject = new Project.Builder()
                    .setId(projectId)
                    .setActivateDefaultDfwRules(false)
                    .build();
            /*  Create a custom project
             *  Use projectService.policyLmPatchProject() to catch exceptions while creating project.
             */
            customProject = projectService
                    .policyLmUpdateProject(defaultOrg.getId(), customProject.getId(), customProject)
                    .invoke()
                    .get();

            log.info("Custom Project Created under default org: {}", customProject);

            customProject = projectService
                    .policyLmGetProject(defaultOrg.getId(), customProject.getId())
                    .invoke()
                    .get();
            log.info("Custom Project Fetched: {}", customProject);

            // Create VpcDHCPRelayConfig to be used by VpcService Profile
            VpcDhcpRelayConfig vpcDhcpRelayConfig = new VpcDhcpRelayConfig.Builder()
                    .setServerAddresses(Collections.singletonList(dhcpServerAddress))
                    .build();
            VpcProfileDhcpConfig vpcProfileDhcpConfig = new VpcProfileDhcpConfig.Builder()
                    .setDhcpRelayConfig(vpcDhcpRelayConfig)
                    .build();

            // Create VpcServiceProfile to be used by Vpc
            VpcServiceProfile vpcServiceProfile = new VpcServiceProfile.Builder()
                    .setId(serviceProfile)
                    .setDhcpConfig(vpcProfileDhcpConfig)
                    .build();
            vpcServiceProfile = serviceProfileService
                    .policyLmCreateOrReplaceVpcServiceProfile(defaultOrg.getId(), customProject.getId(), serviceProfile, vpcServiceProfile)
                    .invoke()
                    .get();

            // Create VPC Using Above created Service Profile and private IP
            Vpc vpc = new Vpc.Builder()
                    .setId(vpcId)
                    .setPrivateIps(Collections.singletonList(vpcPrivateIps))
                    .setVpcServiceProfile(vpcServiceProfile.getPath())
                    .build();

            vpcService
                    .policyLmPatchVPC(defaultOrg.getId(), customProject.getId(), vpcId, vpc)
                    .invoke();
            vpc = vpcService
                    .policyLmGetVpc(defaultOrg.getId(), customProject.getId(), vpcId)
                    .invoke()
                    .get();
            log.info("vpc Created: {}", vpc);

        } catch (Exception exception) {
            log.error("Unexpected error:", exception);
        }
    }

    private static void initUnsetOptionalParameters() {
        if (Objects.isNull(dhcpServerAddress)) {
            dhcpServerAddress = DHCP_SERVER_ADDRESS;
        }
        if (Objects.isNull(serviceProfile)) {
            serviceProfile = SERVICE_PROFILE;
        }
        if (Objects.isNull(vpcPrivateIps)) {
            vpcPrivateIps = VPC_PRIVATE_IPS;
        }
        if (Objects.isNull(projectId)) {
            projectId = PROJECT_ID;
        }
        if (Objects.isNull(vpcId)) {
            vpcId = VPC_ID;
        }
    }
}
