/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.appliance.networking.proxy;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appliance.networking.NoProxy;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;

/** Demonstrates setting and getting servers that has to be excluded from Proxy. */
public class NoProxyWorkflow {
    private static final Logger log = LoggerFactory.getLogger(NoProxyWorkflow.class);
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

    /** REQUIRED: Specify the servers to be excluded from proxy (Hostname/IP). */
    public static String[] noProxyServers = new String[] {};

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(NoProxyWorkflow.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            NoProxy noProxyService = client.createStub(NoProxy.class);

            log.info("Existing list of servers that should not be accessed via the proxy server...");
            List<String> initialServerList = noProxyService.get();
            log.info("initialServerList {}", initialServerList);

            List<String> noProxyServerList = Arrays.asList(noProxyServers);
            log.info("Excluding {} from proxy...", noProxyServerList);
            noProxyService.set(noProxyServerList);

            log.info("New list of servers that should not be accessed via the proxy server");
            log.info("noProxyService.get() {}", noProxyService.get());

            // cleanup
            log.info("Cleaning up no proxy configuration...");
            if (initialServerList.isEmpty()) {
                noProxyService.set(new ArrayList<>());
            } else {
                noProxyService.set(initialServerList);
            }
            log.info("noProxyService.get() {}", noProxyService.get());
        }
    }
}
