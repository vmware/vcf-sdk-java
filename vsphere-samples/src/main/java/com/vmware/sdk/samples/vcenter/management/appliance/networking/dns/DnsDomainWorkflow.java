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

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appliance.networking.dns.Domains;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;

/** Demonstrates getting and setting DNS domains. */
public class DnsDomainWorkflow {
    private static final Logger log = LoggerFactory.getLogger(DnsDomainWorkflow.class);
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

    /** REQUIRED: Specify the DNS domain names as comma separated values. */
    public static String[] dnsDomains = {};

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(DnsDomainWorkflow.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Domains domainService = client.createStub(Domains.class);

            List<String> initialDomains = domainService.list();
            log.info("Existing DNS domains : {}\n", initialDomains);

            // Adding new DNS domain
            log.info("Adding domain {} to the DNS domains...", dnsDomains[0]);
            domainService.add(dnsDomains[0]);
            log.info("New list of DNS domains : {}\n", domainService.list());

            // Setting new DNS domains
            log.info("Setting {} as DNS domains...", Arrays.asList(dnsDomains));
            domainService.set(Arrays.asList(dnsDomains));
            log.info("New DNS domains list : {}", domainService.list());

            // cleanup
            log.info("Cleaning up DNS domain settings...");
            domainService.set(initialDomains);
        }
    }
}
