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

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appliance.networking.dns.Hostname;
import com.vmware.appliance.networking.dns.HostnameTypes.Message;
import com.vmware.appliance.networking.dns.HostnameTypes.TestStatus;
import com.vmware.appliance.networking.dns.HostnameTypes.TestStatusInfo;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;

/**
 *
 *
 * <ol>
 *   <li>Demonstrates getting Hostname
 *   <li>Demonstrates setting Hostname
 *   <li>Demonstrates testing Hostname
 * </ol>
 */
public class HostNameWorkflow {
    private static final Logger log = LoggerFactory.getLogger(HostNameWorkflow.class);
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

    /** OPTIONAL: Specify the hostname to be set for the server. Default value is "testhost.com". */
    public static String dnsHostname = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(HostNameWorkflow.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Hostname dnsHostnameService = client.createStub(Hostname.class);

            // Set specified hostname - Set operation not allowed as of now
            // dnsHostnameService.set(dnsHostname);

            // Get hostname
            log.info("DNS Hostname : {}\n", dnsHostnameService.get());

            // Test hostname
            log.info("Testing the new hostname entry...");
            TestStatusInfo testInfo = dnsHostnameService.test(Objects.requireNonNullElse(dnsHostname, "testhost.com"));

            TestStatus testStatus = testInfo.getStatus();
            log.info("Hostname Test status : {}", testStatus);

            for (Message testMsg : testInfo.getMessages()) {
                log.info("Test Result : {}", testMsg.getResult());
                log.info("Test Message : {}", testMsg.getMessage());
            }
        }
    }
}
