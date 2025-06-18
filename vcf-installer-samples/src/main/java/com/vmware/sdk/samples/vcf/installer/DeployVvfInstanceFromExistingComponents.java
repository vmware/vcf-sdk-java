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
import static com.vmware.sdk.samples.vcf.installer.utils.SddcSpecUtil.hostnameToFqdn;

import java.security.KeyStore;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcf.installer.utils.SddcSpecUtil;
import com.vmware.sdk.vcf.installer.model.SddcSpec;
import com.vmware.sdk.vcf.installer.model.SddcTask;
import com.vmware.sdk.vcf.installer.model.Validation;
import com.vmware.sdk.vcf.installer.utils.MiscUtil;
import com.vmware.sdk.vcf.installer.utils.SddcTaskUtil;
import com.vmware.sdk.vcf.installer.utils.VcfInstallerClientFactory;
import com.vmware.sdk.vcf.installer.v1.Sddcs;
import com.vmware.sdk.vcf.installer.v1.sddcs.Validations;
import com.vmware.vapi.client.ApiClient;

/**
 * Demonstrates how to deploy new VVF Instance, reusing existing Vcenter.<br>
 * Prerequisites for successful deployment:
 *
 * <ol>
 *   <li>Existing components need to be configured and reachable by the VCF Installer appliance
 *   <li>All provided hostnames must be resolvable from the VCF Installer appliance
 *   <li>The addresses of all components must be resolvable from the VCF Installer appliance (NTP, various VCF
 *       components, etc.)
 *   <li>The Depot configuration must be complete
 *   <li>The respective binary bundles need to be downloaded
 * </ol>
 *
 * Downloading respective bundles can be achieved by running the {@link DownloadBundlesVvfInstanceExistingVcenter}
 * sample.
 */
public class DeployVvfInstanceFromExistingComponents {
    private static final Logger log = LoggerFactory.getLogger(DeployVvfInstanceFromExistingComponents.class);

    /** REQUIRED: VCF Installer Appliance hostname or FQDN. */
    public static String vcfInstallerFqdn = "vcf-installer.mycompany.com";

    /** REQUIRED: VCF Installer Appliance password for admin@local user. */
    public static String vcfInstallerAdminPassword = "Passw0rd!ForAdmin@Local";

    /** REQUIRED: Domain of existing and to-be-deployed appliances. */
    public static String dnsDomain = "vcf.local";

    /** REQUIRED: Nameserver containing the domain's DNS records. */
    public static String dnsNameserver = "192.168.0.1";

    /** REQUIRED: Comma separated list of NTP servers used when deploying SDDC Manager appliance. */
    public static String[] ntpServers = {};

    /** REQUIRED: Hostname or FQDN of the VCF Operations that will be deployed. */
    public static String vcfOpsFqdn = "vcfops";

    /** REQUIRED: Hostname or FQDN of the existing vCenter deployment. */
    public static String vCenterFqdn = "vc1.vcf.local";

    /** OPTIONAL: SSL Certificate SHA256 Thumbprint of the vCenter deployment. */
    public static String vCenterThumbprint = null;

    /** REQUIRED: Password for the root user of the existing vCenter deployment. */
    public static String vCenterRootPassword = "VcenterPasswordForRootUser";

    /** REQUIRED: Admin SSO Username for the existing vCenter deployment. */
    public static String vCenterAdminSsoUsername = "Administrator@VSPHERE.LOCAL";

    /** REQUIRED: Admin SSO Password for the existing vCenter deployment. */
    public static String vCenterAdminSsoPassword = "VcenterPasswordForAdminSSO";

    /** REQUIRED: SDDC ID. */
    public static String sddcId = "sddc-01";

    /** OPTIONAL: Only validate {@link SddcSpec} and skip VVF deployment. */
    public static Boolean validateOnly = null;

    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (do not leave it empty on production environments).
     */
    public static String trustStorePath = null;

    /**
     * OPTIONAL: Path to file or directory where to save the actual deployment specification in JSON format used by the
     * VCF Installer during deployment.
     */
    public static String deploymentSpecSaveFilePath = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(DeployVvfInstanceFromExistingComponents.class, args);

        VcfInstallerClientFactory vcfInstallerClientFactory = new VcfInstallerClientFactory();

        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);

        String installerFqdn = hostnameToFqdn(vcfInstallerFqdn, dnsDomain);

        try (ApiClient client =
                vcfInstallerClientFactory.createClient(installerFqdn, vcfInstallerAdminPassword, keyStore)) {

            SddcSpec sddcSpec = createSddcSpecForNewVvfInstanceWithExistingVc(client);
            log.info("Crafted Deployment Spec is: {}", SddcSpecUtil.sddcSpecToJson(sddcSpec));

            Validations validations = client.createStub(Validations.class);
            Validation validationResult =
                    validations.validateSddcSpec(sddcSpec).invoke().get();
            String validationId = validationResult.getId();
            log.info("Started VVF Instance Deployment Spec validation task with id: {}", validationId);

            SddcTaskUtil.waitForValidationTaskAndFailOnError(validations, validationId);
            log.info("Finished VVF Instance Deployment Spec validation task with id: {}", validationId);

            if (!Boolean.TRUE.equals(validateOnly)) {
                log.info("Starting VVF Instance deployment");

                Sddcs sddcs = client.createStub(Sddcs.class);
                SddcTask sddcTask = sddcs.deploySddc(sddcSpec).invoke().get();
                String sddcTaskId = sddcTask.getId();
                log.info("Started VVF Instance deployment task with id: {}", sddcTaskId);

                SddcTaskUtil.waitForSddcDeploymentTaskAndFailOnError(sddcs, sddcTaskId);
                log.info("Finished VVF Instance deployment task with id: {}", sddcTaskId);

                SddcSpecUtil.saveSddcSpecToFile(client, sddcTaskId, deploymentSpecSaveFilePath);
            }

            log.info("Sample completed successfully");
        }
    }

    public static SddcSpec createSddcSpecForNewVvfInstanceWithExistingVc(ApiClient vcfClient) throws Exception {
        SddcSpec.Builder builder = new SddcSpec.Builder();
        builder.setWorkflowType(SddcSpecUtil.WorkflowType.VVF.toString());
        builder.setCeipEnabled(true);
        builder.setVersion(MiscUtil.getVersionWithoutBuildNumber(vcfClient));
        builder.setNtpServers(List.of(ntpServers));
        builder.setDnsSpec(SddcSpecUtil.createDnsSpec(dnsDomain, dnsNameserver));

        // Operations stack
        // Deploy New VCF Operations
        builder.setVcfOperationsSpec(SddcSpecUtil.createVcfOperationsSpec(hostnameToFqdn(vcfOpsFqdn, dnsDomain)));

        // vCenter
        // Use Existing vCenter
        builder.setVcenterSpec(SddcSpecUtil.createSddcVcenterSpec(
                hostnameToFqdn(vCenterFqdn, dnsDomain),
                vCenterThumbprint,
                vCenterRootPassword,
                vCenterAdminSsoUsername,
                vCenterAdminSsoPassword,
                trustStorePath));
        builder.setClusterSpec(SddcSpecUtil.createSddcClusterSpec(sddcId));

        // SDDC Manager
        builder.setSddcId(sddcId);

        return builder.build();
    }
}
