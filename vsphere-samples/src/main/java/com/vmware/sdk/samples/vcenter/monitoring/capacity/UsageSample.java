/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.monitoring.capacity;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.capacity.Usage;
import com.vmware.vcenter.capacity.UsageTypes.Info;

/**
 * Demonstrates getting vCenter Capacity Usage.
 *
 * <p>Sample Prerequisites:
 *
 * <ol>
 *   <li>Vcenter version &gt;= 9.1
 *   <li>vCenter deployment size should at least be medium
 * </ol>
 */
public class UsageSample {
    private static final Logger log = LoggerFactory.getLogger(UsageSample.class);
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

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(UsageSample.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Usage usage = client.createStub(Usage.class);
            log.info("Getting vCenter Capacity Usage");

            Info usageInfo = usage.get();
            log.info("vCenter Capacity Usage Report is: {}", usageInfo);
            log.info("vCenter deployment size is : {}", usageInfo.getDeploymentSize());
            log.info("vCenter version is : {}", usageInfo.getVersion());
            log.info("vCenter Capacity Usage configurations are : {}", usageInfo.getConfigurations());
            log.info("vCenter Capacity Usage report location is : {}", usageInfo.getCsvReportLocation());
        }
    }
}
