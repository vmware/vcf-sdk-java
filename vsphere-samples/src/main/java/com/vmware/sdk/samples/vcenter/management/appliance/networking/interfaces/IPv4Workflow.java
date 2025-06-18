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

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appliance.networking.interfaces.Ipv4;
import com.vmware.appliance.networking.interfaces.Ipv4Types.Config;
import com.vmware.appliance.networking.interfaces.Ipv4Types.Info;
import com.vmware.appliance.networking.interfaces.Ipv4Types.Mode;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.management.appliance.helpers.NetworkingHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;

/**
 *
 *
 * <ol>
 *   <li>Demonstrates getting IPv4 information for specific nic
 *   <li>Demonstrates setting DHCP/STATIC IPv4 for specific nic
 * </ol>
 */
public class IPv4Workflow {
    private static final Logger log = LoggerFactory.getLogger(IPv4Workflow.class);
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
    /** OPTIONAL: Specify the IPv4 mode (DHCP, STATIC, UNCONFIGURED). Default value is "DHCP". */
    public static String ipMode = null;
    /** OPTIONAL: Specify the IP address to set if mode is STATIC. */
    public static String address = null;
    /** OPTIONAL: Specify the Default gateway to set if mode is STATIC. */
    public static String defaultGateway = null;
    /** OPTIONAL: Specify the prefix to set if mode is STATIC. Default value is 0. */
    public static Long prefix = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(IPv4Workflow.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Ipv4 ipv4Service = client.createStub(Ipv4.class);
            Info ipv4Info = ipv4Service.get(nic);
            Mode initialMode = ipv4Info.getMode();

            String initialAddress = null;
            long initialPrefix = 0L;
            String initialDefaultGateway = null;

            if (initialMode == Mode.STATIC) {
                initialAddress = ipv4Info.getAddress();
                initialDefaultGateway = ipv4Info.getDefaultGateway();
                initialPrefix = ipv4Info.getPrefix();
            }

            Mode mode = Mode.valueOf(Objects.requireNonNullElse(ipMode, "DHCP").toUpperCase());

            Config cfg = new Config();
            // Set IP address
            cfg.setMode(mode);
            if (mode.equals(Mode.STATIC)) {
                cfg.setAddress(address);
                cfg.setDefaultGateway(defaultGateway);
                cfg.setPrefix(Objects.requireNonNullElse(prefix, 0L));
            }
            log.info("Setting IPv4 address for nic : {} with mode : {}", nic, mode);
            ipv4Service.set(nic, cfg);

            // Get IP address
            log.info("----- IPv4 Address information for nic : {}", nic);
            Info getIpv4Info = ipv4Service.get(nic);
            NetworkingHelper.printIPv4Info(getIpv4Info);

            // cleanup
            log.info("----- Cleaning up IPv4 Configuration...");

            Config cleanupCfg = new Config();
            cleanupCfg.setMode(initialMode);
            if (initialMode == Mode.STATIC) {
                cleanupCfg.setAddress(initialAddress);
                cleanupCfg.setPrefix(initialPrefix);
                cleanupCfg.setDefaultGateway(initialDefaultGateway);
            }
            ipv4Service.set(nic, cleanupCfg);
        }
    }
}
