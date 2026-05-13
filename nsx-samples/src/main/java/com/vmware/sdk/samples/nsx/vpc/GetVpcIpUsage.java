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

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.nsx.model.IpAddressBlock;
import com.vmware.sdk.nsx.model.IpAddressBlockUsage;
import com.vmware.sdk.nsx.policy.api.v1.infra.IpBlocks;
import com.vmware.sdk.nsx.policy.api.v1.infra.ip_blocks.Usage;
import com.vmware.sdk.samples.nsx.helpers.NsxHelper;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.vapi.client.ApiClient;

/**
 * The example shows fetching IP usage of a created IP block
 * under a default Project, in Vmware NSX.
 * <p> Sample prerequisites: NSX Manager instance.
 *
 * <p>APIs used
 * <ul>
 *      <li> Create IP block: PUT /policy/api/v1/infra/ip-blocks/{ip-block-id}
 *      <li> Get IP block: GET /policy/api/v1/infra/ip-blocks/{ip-block-id}
 *      <li> GET IP Usage: GET /infra/ip-blocks/usage
 * </ul>
 */

public class GetVpcIpUsage {
    private static final Logger log = LoggerFactory.getLogger(GetVpcIpUsage.class);
    /** REQUIRED: NSX host address or FQDN. */
    public static String nsxHostname = "hostname";
    /** REQUIRED: NSX username. */
    public static String nsxUsername = "username";
    /** REQUIRED: NSX password. */
    public static String nsxPassword = "password";
    /** OPTIONAL: IP Block Cidr used to create an IP Block of Type EXTERNAL. */
    public static String externalIpBlockCidr;
    /** OPTIONAL: ID of IP Block of Type EXTERNAL. */
    public static String externalIpId;

    /* Default values of above optional values, in-case, they are not provided as command line arguments. */
    private static final String EXTERNAL_IP_BLOCK_CIDR = "142.2.3.0/24";

    private static final String EXTERNAL_IP_ID = "external-ip";

    public static void main(String[] args) {
        SampleCommandLineParser.load(GetVpcIpUsage.class, args);
        initUnsetOptionalParameters();
        try (NsxHelper.NsxFactory nsxFactory = new NsxHelper.NsxFactory(nsxHostname, nsxUsername, nsxPassword)) {
            ApiClient apiClient = nsxFactory.getApiClient();
            /* Initialize service classes required to create VPC: Org Service, IpBlock Service, Usage Service. */
            IpBlocks ipBlocks = apiClient.createStub(IpBlocks.class);
            Usage ipblockUsage = apiClient.createStub(Usage.class);

            // Create an external IP block
            IpAddressBlock ipBlock = new IpAddressBlock.Builder()
                    .setCidr(externalIpBlockCidr)
                    .setIpAddressType(IpAddressBlock.IP_ADDRESS_TYPE_IPV4)
                    .setVisibility(IpAddressBlock.VISIBILITY_EXTERNAL)
                    .setId(externalIpId)
                    .build();
            ipBlocks.policyLmCreateOrPatchIpAddressBlock(ipBlock.getId(), ipBlock)
                    .invoke();

            // Get External IP Blocks
            ipBlock = ipBlocks.policyLmReadIpAddressBlock(ipBlock.getId())
                    .invoke()
                    .get();
            log.info("External Ip block: {}", ipBlock);

            IpAddressBlockUsage ipAddressUsage = ipblockUsage
                    .policyLmGetIpAddressBlockUsage(ipBlock.getId())
                    .invoke()
                    .get();
            log.info("External Ip block usage: {}", ipAddressUsage);

        } catch (Exception e) {
            log.error("Unexpected error", e);
        }
    }

    private static void initUnsetOptionalParameters() {
        if (Objects.isNull(externalIpId)) {
            externalIpId = EXTERNAL_IP_ID;
        }
        if (Objects.isNull(externalIpBlockCidr)) {
            externalIpBlockCidr = EXTERNAL_IP_BLOCK_CIDR;
        }
    }
}
