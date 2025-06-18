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

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appliance.networking.Interfaces;
import com.vmware.appliance.networking.InterfacesTypes.InterfaceInfo;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.management.appliance.helpers.NetworkingHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;

/**
 *
 *
 * <ol>
 *   <li>Demonstrates getting interface information for all interfaces
 *   <li>Demonstrates getting interface information for specific interface
 * </ol>
 */
public class InterfacesWorkflow {
    private static final Logger log = LoggerFactory.getLogger(InterfacesWorkflow.class);
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

    /** OPTIONAL: Specify the interface name. Default value is "nic0" */
    public static String nic = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(InterfacesWorkflow.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Interfaces interfacesService = client.createStub(Interfaces.class);

            // List all interfaces information
            log.info("Getting all interfaces info...");

            List<InterfaceInfo> interfaceInfo = interfacesService.list();
            for (InterfaceInfo intInfo : interfaceInfo) {
                log.info("Interface name : {}", intInfo.getName());
                log.info("MAC : {}", intInfo.getMac());
                log.info("Status : {}", intInfo.getStatus());

                log.info("----- IPv4 Configuration: -----");
                NetworkingHelper.printIPv4Info(intInfo.getIpv4());

                log.info("------ IPv6 Configuration: -----");
                NetworkingHelper.printIPv6Info(intInfo.getIpv6());
            }

            // Get info of specified interface
            log.info("Getting information for Nic : {}", nic);
            InterfaceInfo intInfo = interfacesService.get(Objects.requireNonNullElse(nic, "nic0"));
            log.info("Interface name : {}", intInfo.getName());
            log.info("MAC : {}", intInfo.getMac());
            log.info("Status : {}", intInfo.getStatus());

            log.info("----- IPv4 Configuration: -----");
            NetworkingHelper.printIPv4Info(intInfo.getIpv4());

            log.info("----- IPv6 Configuration: -----");
            NetworkingHelper.printIPv6Info(intInfo.getIpv6());
        }
    }
}
