/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.appliance.networking;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appliance.Networking;
import com.vmware.appliance.NetworkingTypes.Info;
import com.vmware.appliance.NetworkingTypes.UpdateSpec;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.management.appliance.helpers.NetworkingHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;

/**
 *
 *
 * <ol>
 *   <li>Demonstrates getting network information for all interfaces
 *   <li>Demonstrates Enabling/Disabling IPv6 on all interfaces
 *   <li>Demonstrates resetting and refreshing network
 * </ol>
 */
public class NetworkingWorkflow {
    private static final Logger log = LoggerFactory.getLogger(NetworkingWorkflow.class);
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

    /** OPTIONAL: Specify this option if you want to enable IPv6. Default is false. */
    public static Boolean enableIPv6 = null;
    /** OPTIONAL: Specify this option if you want to reset and refresh the network. Default is false. */
    public static Boolean reset = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(NetworkingWorkflow.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Networking networkingService = client.createStub(Networking.class);

            Info networkInfo = networkingService.get();
            NetworkingHelper.printNetworkInfo(networkInfo);

            boolean initialIPv6Config = networkInfo
                            .getInterfaces()
                            .entrySet()
                            .iterator()
                            .next()
                            .getValue()
                            .getIpv6()
                    != null;

            UpdateSpec updateSpec = new UpdateSpec();

            updateSpec.setIpv6Enabled(Boolean.TRUE.equals(enableIPv6));
            if (Boolean.TRUE.equals(enableIPv6)) {
                log.info("Enabling IPv6...");
            } else {
                log.info("Disabling IPv6...");
            }

            networkingService.update(updateSpec);

            // Display the network information
            networkInfo = networkingService.get();
            NetworkingHelper.printNetworkInfo(networkInfo);

            if (Boolean.TRUE.equals(reset)) {
                log.info("Refreshing network configuration...");

                networkingService.reset();

                networkInfo = networkingService.get();
                NetworkingHelper.printNetworkInfo(networkInfo);
            }

            // cleanup

            log.info("Cleaning up IPv6 configuration...");

            UpdateSpec cleanupSpec = new UpdateSpec();
            updateSpec.setIpv6Enabled(initialIPv6Config);
            networkingService.update(cleanupSpec);

            Info cleanupNetworkInfo = networkingService.get();
            NetworkingHelper.printNetworkInfo(cleanupNetworkInfo);
        }
    }
}
