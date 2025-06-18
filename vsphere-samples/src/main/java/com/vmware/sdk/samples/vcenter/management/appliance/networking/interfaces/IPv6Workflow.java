/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.appliance.networking.interfaces;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static java.util.Objects.requireNonNullElse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appliance.networking.interfaces.Ipv6;
import com.vmware.appliance.networking.interfaces.Ipv6Types.Address;
import com.vmware.appliance.networking.interfaces.Ipv6Types.AddressInfo;
import com.vmware.appliance.networking.interfaces.Ipv6Types.Config;
import com.vmware.appliance.networking.interfaces.Ipv6Types.Info;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.management.appliance.helpers.NetworkingHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;

/**
 *
 *
 * <ol>
 *   <li>Demonstrates getting IPv6 information for specific nic
 *   <li>Demonstrates setting DHCP/STATIC IPv6 for specific nic
 * </ol>
 */
public class IPv6Workflow {
    private static final Logger log = LoggerFactory.getLogger(IPv6Workflow.class);
    /** REQUIRED: vCenter FQDN or IP address. */
    public static String serverAddress = "vcenter1.mycompany.com";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";
    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (do not leave it empty on production environments).
     */
    public static String trustStorePath = null;

    /** REQUIRED: Specify the interface name. */
    public static String nic = "nic0";
    /** OPTIONAL: Specify this option to enable Autoconf. Default value is false. */
    public static Boolean autoconf = null;
    /** OPTIONAL: Specify this option to enable DHCP. Default value is false. */
    public static Boolean dhcp = null;
    /**
     * OPTIONAL: Specify the IPv6 address as comma separated values and specify address as address-prefix format ex:
     * '[IPv6 address]-[prefix]' if dhcp is false.
     */
    public static String[] addresses = null;
    /** OPTIONAL: Specify the default gateway. Default value is "". */
    public static String defaultGateway = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(IPv6Workflow.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            List<Address> initialAddresses = new ArrayList<>();
            String initialDefaultGateway;

            Ipv6 ipv6Service = client.createStub(Ipv6.class);
            Info ipv6Info = ipv6Service.get(nic);
            boolean initialDhcp = ipv6Info.getDhcp();
            boolean initialAutoconf = ipv6Info.getAutoconf();

            if (!initialDhcp) {
                List<AddressInfo> addressesInfos = ipv6Info.getAddresses();
                for (AddressInfo addressInfo : addressesInfos) {
                    Address address = new Address();
                    address.setAddress(addressInfo.getAddress());
                    address.setPrefix(addressInfo.getPrefix());

                    initialAddresses.add(address);
                }
                initialDefaultGateway = ipv6Info.getDefaultGateway();
            } else {
                initialAddresses = new ArrayList<>();
                initialDefaultGateway = "";
            }

            Config config = new Config();

            // Set IPv6 address
            log.info("Setting {} IPv6 configuration...", nic);
            config.setAutoconf(Boolean.TRUE.equals(autoconf));
            config.setDhcp(Boolean.TRUE.equals(dhcp));

            Map<String, String> addressPrefixMap = getAddressPrefixMap(addresses);
            List<Address> addressList = new ArrayList<>();
            for (Entry<String, String> addressEntry : addressPrefixMap.entrySet()) {
                Address addr = new Address();
                addr.setAddress(addressEntry.getKey());
                addr.setPrefix(Long.parseLong(addressEntry.getValue()));
                addressList.add(addr);
            }
            config.setAddresses(addressList);
            config.setDefaultGateway(requireNonNullElse(defaultGateway, ""));

            ipv6Service.set(nic, config);

            // Get and display IPv6 address
            log.info("----- IPv6 Information for nic : {}", nic);
            Info getIpv6Info = ipv6Service.get(nic);
            NetworkingHelper.printIPv6Info(getIpv6Info);

            // cleanup
            log.info("----- Cleaning up IPv6 Configuration...");
            Config cleanupCfg = new Config();
            cleanupCfg.setAutoconf(initialAutoconf);
            cleanupCfg.setDhcp(initialDhcp);
            cleanupCfg.setAddresses(initialAddresses);
            cleanupCfg.setDefaultGateway(initialDefaultGateway);
            ipv6Service.set(nic, cleanupCfg);
        }
    }

    private static Map<String, String> getAddressPrefixMap(String[] addresses) {
        Map<String, String> addressPrefixMap = new HashMap<>();
        if (addresses != null) {
            for (String address : addresses) {
                String[] addressPrefix = address.split("-");
                addressPrefixMap.put(addressPrefix[0], addressPrefix[1]);
            }
        }
        return addressPrefixMap;
    }
}
