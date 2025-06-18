/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcf.installer.system;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.security.KeyStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vcf.installer.utils.VcfInstallerClientFactory;
import com.vmware.sdk.vcf.installer.v1.system.ApplianceInfo;
import com.vmware.vapi.client.ApiClient;

/**
 * A minimalistic sample that logs into the VCF Installer and demonstrates how to retrieve the appliance info - role and
 * version.
 */
public class GetApplianceInfo {
    private static final Logger log = LoggerFactory.getLogger(GetApplianceInfo.class);

    /** REQUIRED: VCF Installer Appliance password for admin@local user. */
    public static String vcfInstallerAdminPassword = "Passw0rd!ForAdmin@Local";

    /** REQUIRED: VCF Installer Appliance name or FQDN. */
    public static String vcfInstallerServerAddress = "vcf-installer.mycompany.com";

    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (do not leave it empty on production environments).
     */
    public static String trustStorePath = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(GetApplianceInfo.class, args);

        VcfInstallerClientFactory vcfInstallerClientFactory = new VcfInstallerClientFactory();

        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);

        try (ApiClient client = vcfInstallerClientFactory.createClient(
                vcfInstallerServerAddress, vcfInstallerAdminPassword, keyStore)) {
            var applianceInfo = client.createStub(ApplianceInfo.class)
                    .getApplianceInfo()
                    .invoke()
                    .get();

            log.info("Result: {}", applianceInfo);
            log.info("Sample completed successfully");
        }
    }
}
