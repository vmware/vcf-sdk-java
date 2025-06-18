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
import static java.util.Objects.requireNonNullElse;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appliance.networking.Proxy;
import com.vmware.appliance.networking.ProxyTypes;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.management.appliance.helpers.NetworkingHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;

/**
 * Demonstrates setting of proxyServer details, getting proxy server details, test the proxy server whether we're able
 * to connect to test host through proxy and deleting the proxy server details for a specific protocol.
 */
public class ProxyWorkflow {
    private static final Logger log = LoggerFactory.getLogger(ProxyWorkflow.class);
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

    /** REQUIRED: Specify the protocol (HTTP/HTTPS/FTP) for which proxy has to be set. */
    public static String protocol = "HTTPS";
    /** OPTIONAL: Specify the proxy server name (Hostname/IP). */
    public static String proxyServer = null;
    /** OPTIONAL: Specify the proxy port number. Default value is 0. */
    public static Long port = null;
    /** REQUIRED: Specify whether the proxy has to be enabled or not. */
    public static boolean enabled = false;
    /** OPTIONAL: Specify the proxy username, if authentication is required to connect to the proxy. */
    public static String proxyUsername = null;
    /** OPTIONAL: Specify the proxy password. */
    public static String proxyPassword = null;
    /** OPTIONAL: Specify the test host to connect to test the proxy. Default value is www.google.com. */
    public static String testHost = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(ProxyWorkflow.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Proxy proxyService = client.createStub(Proxy.class);

            Map<ProxyTypes.Protocol, ProxyTypes.Config> proxyList = proxyService.list();
            NetworkingHelper.printProxyDetails(proxyList);

            // Set Proxy details
            ProxyTypes.Config cfg = new ProxyTypes.Config();
            cfg.setEnabled(enabled);
            cfg.setServer(proxyServer);
            cfg.setPort(requireNonNullElse(port, 0L));

            if (proxyUsername != null) {
                cfg.setUsername(proxyUsername);
                cfg.setPassword(proxyPassword.toCharArray());
            }
            log.info("Setting proxy server configuration...");

            proxyService.set(protocol, cfg);

            // Get Protocol detail
            NetworkingHelper.printProxyDetail(protocol, proxyService.get(protocol));

            log.info("Testing host : '{}' is reachable through proxy server : '{}'...", testHost, cfg.getServer());
            ProxyTypes.TestResult result =
                    proxyService.test(Objects.requireNonNullElse(testHost, "www.google.com"), protocol, cfg);
            log.info("Server status : {}", result.getStatus());
            log.info("Result Message : {}", result.getMessage().getDefaultMessage());

            // cleanup
            // Delete the proxy configuration
            log.info("Deleting proxy configuration for protocol : {}", protocol);
            proxyService.delete(protocol);
            NetworkingHelper.printProxyDetail(protocol, proxyService.get(protocol));
        }
    }
}
