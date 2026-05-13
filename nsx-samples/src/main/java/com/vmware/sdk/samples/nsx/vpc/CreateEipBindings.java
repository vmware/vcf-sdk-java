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

import com.vmware.sdk.nsx.model.PortAddressBindingEntry;
import com.vmware.sdk.nsx.model.VpcSubnetPort;
import com.vmware.sdk.nsx.policy.api.v1.orgs.projects.vpcs.Subnets;
import com.vmware.sdk.nsx.policy.api.v1.orgs.projects.vpcs.subnets.Ports;
import com.vmware.sdk.samples.nsx.helpers.NsxHelper;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.vapi.client.ApiClient;

/**
 * The example shows creation operation of EIP Port Bindings
 * for a give VPC Subnet.
 * <p>
 * Sample prerequisites: NSX Manager instance with default org, Project, VPC, VPC Subnet created.
 * <p> APIs USED:
 * <ul>
 *     <li> Create custom project : PUT /policy/api/v1/orgs/{org-id}/projects/{project-id}/vpcs/{vpc-id}/subnets/{subnet-id}/ports/{port-id}
 * </ul>
 */
public class CreateEipBindings {

    private static final Logger log = LoggerFactory.getLogger(CreateEipBindings.class);
    /** REQUIRED: NSX host address or FQDN. */
    public static String nsxHostname = "hostname";
    /** REQUIRED: NSX username. */
    public static String nsxUsername = "username";
    /** REQUIRED: NSX password. */
    public static String nsxPassword = "password";
    /** OPTIONAL: Custom project ID */
    public static String projectId;
    /** OPTIONAL: VPC ID */
    public static String vpcId;
    /** OPTIONAL: Subnet ID */
    public static String subnetId;
    /** OPTIONAL: Subnet Port ID */
    public static String subnetPortId;
    /** OPTIONAL: External IP CIDR used to create Subnet port */
    public static String externalIpCidr;
    /** OPTIONAL: External MAC address used to create Subnet port */
    public static String externalMacAddress;

    /* Keyword is reserved for Default objects, do not modify. Please create another constant to be used as IDs for custom objects. */
    private static final String DEFAULT = "default";

    /* Default values of above optional values, in-case, they are not provided as command line arguments. */
    private static final String VCF_NSX_SDK_VPC_SUBNET = "VCF-NSX-SDK-vpcSubnet";
    private static final String PROJECT_ID = "VCF-NSX-SDK-Project";
    private static final String VPC_ID = "VCF-NSX-SDK-VPC";
    private static final String VCF_NSX_SDK_VPC_SUBNET_PORT_ID = "VCF-NSX-SDK-vpcSubnetPort";
    private static final String EXTERNAL_IP_CIDR = "142.2.0.0/24";
    private static final String EXTERNAL_MAC_ADDRESS = "aa:bb:cc:dd:ee:f1";

    public static void main(String[] args) {
        SampleCommandLineParser.load(CreateEipBindings.class, args);
        initUnsetOptionalParameters();
        try (NsxHelper.NsxFactory nsxFactory = new NsxHelper.NsxFactory(nsxHostname, nsxUsername, nsxPassword)) {
            ApiClient apiClient = nsxFactory.getApiClient();
            /* Initialize service classes required to create VPC: Project Service, VpcServiceProfile Service, Vpc Service.*/
            Subnets vpcSubnetService = apiClient.createStub(Subnets.class);
            Ports vpcSubnetPortService = apiClient.createStub(Ports.class);

            /* Fetch subnet to create IP bindings */
            vpcSubnetService
                    .policyLmGetVpcSubnet(DEFAULT, projectId, vpcId, subnetId)
                    .invoke()
                    .get();
            PortAddressBindingEntry addressBinding = new PortAddressBindingEntry.Builder()
                    .setIpAddress(externalIpCidr)
                    .setMacAddress(externalMacAddress)
                    .build();
            VpcSubnetPort subnetPort = new VpcSubnetPort.Builder()
                    .setId(subnetPortId)
                    .setAddressBindings(Collections.singletonList(addressBinding))
                    .build();
            subnetPort = vpcSubnetPortService
                    .policyLmUpdateVpcSubnetPort(DEFAULT, projectId, vpcId, subnetId, subnetPortId, subnetPort)
                    .invoke()
                    .get();
            log.info("created Subnet port: {}", subnetPort);
        } catch (Exception exception) {
            log.error("Unexpected error:", exception);
        }
    }

    private static void initUnsetOptionalParameters() {
        if (Objects.isNull(subnetPortId)) {
            subnetPortId = VCF_NSX_SDK_VPC_SUBNET_PORT_ID;
        }
        if (Objects.isNull(externalIpCidr)) {
            externalIpCidr = EXTERNAL_IP_CIDR;
        }
        if (Objects.isNull(externalMacAddress)) {
            externalMacAddress = EXTERNAL_MAC_ADDRESS;
        }
        if (Objects.isNull(subnetId)) {
            subnetId = VCF_NSX_SDK_VPC_SUBNET;
        }
        if (Objects.isNull(projectId)) {
            projectId = PROJECT_ID;
        }
        if (Objects.isNull(vpcId)) {
            vpcId = VPC_ID;
        }
    }
}
