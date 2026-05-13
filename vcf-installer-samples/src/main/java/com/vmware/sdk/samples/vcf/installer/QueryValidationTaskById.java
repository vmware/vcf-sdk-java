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
import com.vmware.sdk.vcf.installer.model.Validation;
import com.vmware.sdk.vcf.installer.utils.SddcTaskUtil;
import com.vmware.sdk.vcf.installer.utils.VcfInstallerClientFactory;
import com.vmware.sdk.vcf.installer.v1.sddcs.Validations;
import com.vmware.vapi.client.ApiClient;

/** Queries validation task by ID. */
public class QueryValidationTaskById {
    private static final Logger log = LoggerFactory.getLogger(QueryValidationTaskById.class);

    /** REQUIRED: VCF Installer Appliance FQDN. */
    public static String vcfInstallerFqdn = "vcf-installer.mycompany.com";

    /** REQUIRED: VCF Installer Appliance password for admin@local user. */
    public static String vcfInstallerAdminPassword = "Passw0rd!ForAdmin@Local";

    /** REQUIRED: Validation Task ID to query (UUID). */
    public static String validationTaskId = "123e4567-e89b-12d3-a456-426614174000";

    /** OPTIONAL: Boolean flag if waitng for task completion is needed. Default: True */
    public static Boolean waitForTaskCompletion = null;

    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     */
    public static String trustStorePath = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(QueryValidationTaskById.class, args);

        VcfInstallerClientFactory vcfInstallerClientFactory = new VcfInstallerClientFactory();
        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);

        try (ApiClient client =
                vcfInstallerClientFactory.createClient(vcfInstallerFqdn, vcfInstallerAdminPassword, keyStore)) {

            Validations validations = client.createStub(Validations.class);

            if (Boolean.FALSE.equals(waitForTaskCompletion)) {
                Validation validation = SddcTaskUtil.getValidation(validations, validationTaskId);
                SddcTaskUtil.logValidationResult(validation);
            } else {
                SddcTaskUtil.waitForValidationTaskAndFailOnError(validations, validationTaskId);
                log.info("Validation task {} completed.", validationTaskId);
            }

            log.info("Sample completed successfully");
        }
    }
}
