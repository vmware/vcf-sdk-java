/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.appliance.networking.dns;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appliance.networking.dns.Servers;
import com.vmware.appliance.networking.dns.ServersTypes.DNSServerConfig;
import com.vmware.appliance.networking.dns.ServersTypes.DNSServerMode;
import com.vmware.appliance.networking.dns.ServersTypes.Message;
import com.vmware.appliance.networking.dns.ServersTypes.TestStatus;
import com.vmware.appliance.networking.dns.ServersTypes.TestStatusInfo;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;

/**
 *
 *
 * <ol>
 *   <li>Demonstrates getting DNS Servers
 *   <li>Demonstrates adding a DNS Server
 *   <li>Demonstrates setting DNS Servers with Static Mode
 *   <li>Demonstrates setting DNS Servers with DHCP Mode
 *   <li>Demonstrates testing the DNS Servers
 * </ol>
 */
public class DnsServersWorkflow {
    private static final Logger log = LoggerFactory.getLogger(DnsServersWorkflow.class);
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

    /** REQUIRED: Specify the DNS mode. */
    public static String dnsMode = "dhcp";
    /** REQUIRED: Specify the DNS servers as comma separated values. */
    public static String[] dnsServers = {};

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(DnsServersWorkflow.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Servers dnsServerService = client.createStub(Servers.class);

            DNSServerConfig dnsServerConf = dnsServerService.get();
            DNSServerMode initialDnsServerMode = dnsServerConf.getMode();

            List<String> initialDnsServers = new ArrayList<>();
            if (initialDnsServerMode == DNSServerMode.is_static) {
                initialDnsServers = dnsServerConf.getServers();
            }

            log.info("DNS Mode : {}", initialDnsServerMode);
            log.info("Existing DNS Servers : {}\n", initialDnsServers);

            DNSServerMode mode = getDnsServerMode(dnsMode);

            // Set the list of DNS servers
            if (mode == DNSServerMode.is_static) {
                DNSServerConfig dnsServerConfig = new DNSServerConfig();
                dnsServerConfig.setMode(mode);
                dnsServerConfig.setServers(Arrays.asList(dnsServers));
                dnsServerService.set(dnsServerConfig);
                dnsServerConf = dnsServerService.get();

                log.info("New DNS Mode : {}", dnsServerConf.getMode());
                log.info("New Modified DNS Servers : {}\n", dnsServerConf.getServers());

                // Add the first DNS server to the list of DNS servers
                dnsServerConfig = new DNSServerConfig();
                dnsServerConfig.setMode(DNSServerMode.is_static);
                dnsServerConfig.setServers(new ArrayList<>());
                dnsServerService.set(dnsServerConfig);
                dnsServerService.add(dnsServers[0]);
                dnsServerConf = dnsServerService.get();

                log.info("DNS Mode : {}", dnsServerConf.getMode());
                log.info("Modified DNS Servers : {}\n", dnsServerConf.getServers());

                // Test the list of servers
                TestStatusInfo testInfo = dnsServerService.test(Arrays.asList(dnsServers));
                TestStatus status = testInfo.getStatus();
                log.info("Test status for the provided DNS servers : {}", status);
                for (Message testMsg : testInfo.getMessages()) {
                    log.info("Test Result : {}", testMsg.getResult());
                    log.info("Test Message : {}", testMsg.getMessage());
                }
            } else {
                // Change the DNS Server config to DHCP mode
                DNSServerConfig dnsServerConfig = new DNSServerConfig();
                dnsServerConfig.setMode(mode);
                dnsServerConfig.setServers(new ArrayList<>());
                dnsServerService.set(dnsServerConfig);
                dnsServerConf = dnsServerService.get();

                log.info("DNS Mode : {}", dnsServerConf.getMode());
                log.info("DHCP DNS Servers : {}\n", dnsServerConf.getServers());
            }

            // cleanup
            log.info("Cleaning up DNS server configurations...");

            if (initialDnsServerMode == DNSServerMode.dhcp) {
                initialDnsServers = new ArrayList<>();
            }

            DNSServerConfig cleanupDnsServerConf = new DNSServerConfig();
            cleanupDnsServerConf.setMode(initialDnsServerMode);
            cleanupDnsServerConf.setServers(initialDnsServers);

            dnsServerService.set(cleanupDnsServerConf);
        }
    }

    public static DNSServerMode getDnsServerMode(String dnsMode) {
        if ("dhcp".equalsIgnoreCase(dnsMode)) {
            return DNSServerMode.dhcp;
        } else if ("static".equalsIgnoreCase(dnsMode)) {
            return DNSServerMode.is_static;
        } else {
            log.info("Unsupported DNS mode : {}", dnsMode);
            return null;
        }
    }
}
