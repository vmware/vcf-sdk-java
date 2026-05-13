/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcf.installer;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.security.KeyStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcf.installer.helpers.SddcSpecHelper;
import com.vmware.sdk.vcf.installer.utils.SddcTaskUtil;
import com.vmware.sdk.vcf.installer.utils.VcfInstallerClientFactory;
import com.vmware.sdk.vcf.installer.v1.Sddcs;
import com.vmware.vapi.client.ApiClient;

/** This sample demonstrates querying deployment task by ID. */
public class QueryDeploymentTaskById {
    private static final Logger log = LoggerFactory.getLogger(QueryDeploymentTaskById.class);

    /** REQUIRED: VCF Installer Appliance FQDN. */
    public static String vcfInstallerFqdn = "vcf-installer.mycompany.com";

    /** REQUIRED: VCF Installer Appliance password for admin@local user. */
    public static String vcfInstallerAdminPassword = "Passw0rd!ForAdmin@Local";

    /** REQUIRED: Deployment Task ID to query (UUID). */
    public static String deploymentTaskId = "123e4567-e89b-12d3-a456-426614174000";

    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     */
    public static String trustStorePath = null;

    /**
     * OPTIONAL: Path to file or directory where to save the spec in JSON format used by the VCF Installer during
     * deployment.
     */
    public static String deploymentSpecSaveFilePath = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(QueryDeploymentTaskById.class, args);

        VcfInstallerClientFactory vcfInstallerClientFactory = new VcfInstallerClientFactory();

        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);

        try (ApiClient client =
                vcfInstallerClientFactory.createClient(vcfInstallerFqdn, vcfInstallerAdminPassword, keyStore)) {

            Sddcs sddcs = client.createStub(Sddcs.class);

            SddcTaskUtil.waitForSddcDeploymentTaskAndFailOnError(sddcs, deploymentTaskId);
            log.info("Finished deployment task with id: {}", deploymentTaskId);

            SddcSpecHelper.saveSddcSpecToFile(client, deploymentTaskId, deploymentSpecSaveFilePath);

            log.info("Sample completed successfully");
        }
    }
}
