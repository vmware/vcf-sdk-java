/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.sddcm.networkpools;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.sddcm.utils.SddcUtil;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.sddcm.model.IpPool;
import com.vmware.sdk.sddcm.model.Network;
import com.vmware.sdk.sddcm.model.NetworkPool;
import com.vmware.sdk.sddcm.v1.V1Factory;

/**
 * Sample class to create the networkpool in sddc manager. This is one such example on how to create the Networkpool of
 * Networktype vSAN. Similarly, there are other Networkpool Types we can create like NFS and iSCSI.
 */
public class CreateNetworkPoolExample {
    private static final Logger log = LoggerFactory.getLogger(CreateNetworkPoolExample.class);
    /** REQUIRED: SDDC Manager host address or FQDN. */
    public static String sddcManagerHostname = "sddcManagerHostname";
    /** REQUIRED: SDDC Manager SSO username. */
    public static String sddcManagerSsoUserName = "username";
    /** REQUIRED: SDDC Manager SSO password. */
    public static String sddcManagerSsoPassword = "password";

    /** REQUIRED: Networkpool name. */
    public static String networkPoolName = "sample-networkpool1";
    /** REQUIRED: IP pool1 starting range. */
    public static String ipPool1RangeStart = "10.0.4.51";
    /** REQUIRED: IP pool1 end range. */
    public static String ipPool1RangeEnd = "10.0.4.60";
    /** REQUIRED: IP pool2 starting range. */
    public static String ipPool2RangeStart = "10.0.8.51";
    /** REQUIRED: IP pool2 end range. */
    public static String ipPool2RangeEnd = "10.0.8.60";
    /** REQUIRED: VSAN network information. */
    public static String network1Type = "VSAN";
    /** REQUIRED: Valid new VLAN ID. */
    public static String network1VlanId = "0";
    /** REQUIRED: Valid MTU. */
    public static String network1Mtu = "8940";
    /** REQUIRED: Valid subnet. */
    public static String network1Subnet = "10.0.4.0";
    /** REQUIRED: Valid subnet mask. */
    public static String network1Mask = "255.255.255.0";
    /** REQUIRED: Valid gateway. */
    public static String network1Gateway = "10.0.4.253";
    /** REQUIRED: Vmotion network information. */
    public static String network2Type = "VMOTION";
    /** REQUIRED: Valid new VLAN ID. */
    public static String network2VlanId = "0";
    /** REQUIRED: Valid MTU. */
    public static String network2Mtu = "8940";
    /** REQUIRED: Valid subnet. */
    public static String network2Subnet = "10.0.8.0";
    /** REQUIRED: Valid subnet mask. */
    public static String network2Mask = "255.255.255.0";
    /** REQUIRED: Valid gateway. */
    public static String network2Gateway = "10.0.8.253";

    public static void main(String[] args) {
        SampleCommandLineParser.load(CreateNetworkPoolExample.class, args);

        try (SddcUtil.SddcFactory factory =
                new SddcUtil.SddcFactory(sddcManagerHostname, sddcManagerSsoUserName, sddcManagerSsoPassword)) {
            V1Factory v1Factory = factory.getV1Factory();

            // Get the Networkpool spec
            NetworkPool networkPoolSpec = getNetworkPoolSpec();
            log.info("About to create the Network Pool.");

            // Create the Networkpool from the given Spec
            NetworkPool networkPool = v1Factory
                    .networkPoolsService()
                    .createNetworkPool(networkPoolSpec)
                    .invoke()
                    .get();
            log.info("Completed creating the Network Pool: {}.", networkPool.getName());
        } catch (Exception exception) {
            log.error("Exception while creating the network pool", exception);
        }
    }

    /**
     * Create the Networkpool spec which is required to create the Networkpool.
     *
     * @return NetworkPool spec
     */
    private static NetworkPool getNetworkPoolSpec() {
        IpPool ipPool1 = new IpPool.Builder()
                .setStart(ipPool1RangeStart)
                .setEnd(ipPool1RangeEnd)
                .build();

        IpPool ipPool2 = new IpPool.Builder()
                .setStart(ipPool2RangeStart)
                .setEnd(ipPool2RangeEnd)
                .build();

        Network network1 = new Network.Builder()
                .setType(network1Type)
                .setVlanId(Long.valueOf(network1VlanId))
                .setMtu(Long.valueOf(network1Mtu))
                .setSubnet(network1Subnet)
                .setMask(network1Mask)
                .setGateway(network1Gateway)
                .setIpPools(List.of(ipPool1))
                .build();

        Network network2 = new Network.Builder()
                .setType(network2Type)
                .setVlanId(Long.valueOf(network2VlanId))
                .setMtu(Long.valueOf(network2Mtu))
                .setSubnet(network2Subnet)
                .setMask(network2Mask)
                .setGateway(network2Gateway)
                .setIpPools(List.of(ipPool2))
                .build();

        return new NetworkPool.Builder()
                .setName(networkPoolName)
                .setNetworks(Arrays.asList(network1, network2))
                .build();
    }
}
