/*
 * ******************************************************************
 * Copyright (c) 2025-2026 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcf.installer;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.sdk.samples.vcf.installer.helpers.SddcSpecHelper.AUTO_GENERATED_PASSWORD;
import static com.vmware.sdk.samples.vcf.installer.helpers.SddcSpecHelper.hostnameToFqdn;

import java.security.KeyStore;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcf.installer.helpers.SddcSpecHelper;
import com.vmware.sdk.vcf.installer.model.SddcSpec;
import com.vmware.sdk.vcf.installer.model.SddcTask;
import com.vmware.sdk.vcf.installer.model.Validation;
import com.vmware.sdk.vcf.installer.model.IPv4Pool;
import com.vmware.sdk.vcf.installer.model.IPv6Pool;
import com.vmware.sdk.vcf.installer.utils.MiscUtil;
import com.vmware.sdk.vcf.installer.utils.SddcTaskUtil;
import com.vmware.sdk.vcf.installer.utils.VcfInstallerClientFactory;
import com.vmware.sdk.vcf.installer.v1.Sddcs;
import com.vmware.sdk.vcf.installer.v1.sddcs.Validations;
import com.vmware.vapi.client.ApiClient;

/**
 * Demonstrates how to deploy new VCF Fleet with first VCF Instance in it, reusing existing Vcenter and NSX-T. <br>
 * In addition to that deploy VSP components.
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
 * Downloading respective bundles can be achieved by running the
 * {@link DownloadBundlesVcfFleetFirstVcfInstanceFromExistingComponents} sample.
 */
public class DeployVcfFleetFirstVcfInstanceFromExistingComponents {
    private static final Logger log =
            LoggerFactory.getLogger(DeployVcfFleetFirstVcfInstanceFromExistingComponents.class);

    /** REQUIRED: VCF Installer Appliance hostname or FQDN. */
    public static String vcfInstallerFqdn = "vcf-installer.mycompany.com";

    /** REQUIRED: VCF Installer Appliance password for admin@local user. */
    public static String vcfInstallerAdminPassword = "Passw0rd!ForAdmin@Local";

    /**
     * REQUIRED: Domain of existing and to-be-deployed appliances. Provided appliance hostnames will be expanded to FQDN
     * with the DNS domain.
     */
    public static String dnsDomain = "vcf.local";

    /** REQUIRED: Nameserver containing the domain's DNS records. */
    public static String dnsNameserver = "192.168.0.1";

    /** REQUIRED: Comma separated list of NTP servers used when deploying SDDC Manager appliance. */
    public static String[] ntpServers = {};

    /** REQUIRED: Hostname or FQDN of the VCF Operations that will be deployed. */
    public static String vcfOpsFqdn = "vcfops1";

    /**
     * OPTIONAL: Hostname or FQDN of the VCF Automation that will be deployed. If left null along with
     * {@link #vcfAutomationIpv4Addresses} and {@link #vcfAutomationPlatformFqdn} - VCF Automation deployment is
     * skipped.
     */
    public static String vcfAutomationFqdn = null;

    /**
     * OPTIONAL: VCF Automation IPv4 addresses. If left null along with {@link #vcfAutomationFqdn}
     * and {@link #vcfAutomationPlatformFqdn} VCF Automation deployment is skipped.
     */
    public static String[] vcfAutomationIpv4Addresses = null;

    /**
     * OPTIONAL: Hostname or FQDN of the VCF Automation platform that will be deployed.
     * If left null along with {@link #vcfAutomationFqdn} and {@link #vcfAutomationIpv4Addresses}
     * VCF Automation deployment is skipped.
     *
     */
    public static String vcfAutomationPlatformFqdn = null;

    /** REQUIRED: Hostname or FQDN of the VCF Operations Collector that will be deployed. */
    public static String vcfOpsCollectorFqdn = "vcfopscp";

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

    /** REQUIRED: Hostname or FQDN of the existing NSX-T deployment. */
    public static String nsxFqdn = "nsx1.vcf.local";

    /** REQUIRED: Hostname or FQDN of the cluster for the existing NSX-T deployment. */
    public static String nsxVipFqdn = "nsx.vcf.local";

    /** OPTIONAL: SSL Certificate SHA256 Thumbprint of the NSX-T deployment. */
    public static String nsxThumbprint = null;

    /** REQUIRED: Password for the root user of the existing NSX-T deployment. */
    public static String nsxRootPassword = "NsxPasswordForRootUser";

    /** REQUIRED: Password for the admin user of the existing NSX-T deployment. */
    public static String nsxAdminPassword = "NsxPasswordForAdminUser";

    /** REQUIRED: Password for the audit user of the existing NSX-T deployment. */
    public static String nsxAuditPassword = "NsxPasswordForAuditUser";

    /** REQUIRED: SDDC ID. */
    public static String sddcId = "sddc-01";

    /** REQUIRED: Hostname or FQDN of the SDDC Manager that will be deployed. */
    public static String sddcManagerFqdn = "sm";

    /** OPTIONAL: Only validate {@link SddcSpec} and skip VCF deployment. */
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

    /**
     * REQUIRED: VCF Identity Broker hostname.
     */
    public static String vidbHostname = "vidb.vcf.local";

    /**
     * OPTIONAL: VCF Identity Broker version.
     */
    public static String vidbVersion = null;

    /**
     * OPTIONAL: VCF Identity Broker size.
     */
    public static String vidbSize = null;

    /**
     * REQUIRED: VCF Services Platform fqdn.
     */
    public static String vspPlatformFqdn = "vsp1.vcf.local";

    /**
     * OPTIONAL: VCF Services Platform system user password. If blank the password will be auto-generated.
     */
    public static String vspSystemUserPassword = null;

    /**
     * OPTIONAL: VCF Services Platform IPv4 cidr. All IPv4 fields are optional,
     * however IPv4Pool is required for the VCF Services Platform cluster spec.
     * Either provide vspIPv4Addresses or provide vspIPv4Cidr, or
     * vspIPv4StartIpAddress and vspIPv4EndIpAddress.
     */
    public static String vspIPv4Cidr = null;

    /**
     * OPTIONAL: VCF Services Platform IPv4 start ip address. All IPv4 fields are optional,
     * however IPv4Pool is required for the VCF Services Platform cluster spec.
     * Either provide vspIPv4Addresses or provide vspIPv4Cidr, or
     * vspIPv4StartIpAddress and vspIPv4EndIpAddress.
     */
    public static String vspIPv4StartIpAddress = null;

    /**
     * OPTIONAL: VCF Services Platform IPv4 end ip address. All IPv4 fields are optional,
     * however IPv4Pool is required for the VCF Services Platform cluster spec.
     * Either provide vspIPv4Addresses or provide vspIPv4Cidr, or
     * vspIPv4StartIpAddress and vspIPv4EndIpAddress.
     */
    public static String vspIPv4EndIpAddress = null;

    /**
     * OPTIONAL: VCF Services Platform IPv4 addressses. All IPv4 fields are optional,
     * however IPv4Pool is required for the VCF Services Platform cluster spec.
     * Either provide vspIPv4Addresses or provide vspIPv4Cidr, or
     * vspIPv4StartIpAddress and vspIPv4EndIpAddress.
     */
    public static String[] vspIPv4Addresses = null;

    /**
     * OPTIONAL: VCF Services Platform IPv4 excluded addresses.
     */
    public static String[] vspIPv4ExcludedAddresses = null;

    /**
     * OPTIONAL: VCF Services Platform IPv6 cidr.
     */
    public static String vspIPv6Cidr = null;

    /**
     * OPTIONAL: VCF Services Platform IPv6 start ip address.
     */
    public static String vspIPv6StartIpAddress = null;

    /**
     * OPTIONAL: VCF Services Platform IPv6 end ip address.
     */
    public static String vspIPv6EndIpAddress = null;

    /**
     * OPTIONAL: VCF Services Platform IPv6 addresses.
     */
    public static String[] vspIPv6Addresses = null;

    /**
     * OPTIONAL: VCF Services Platform IPv6 excluded addresses.
     */
    public static String[] vspIPv6ExcludedAddresses = null;

    /**
     * OPTIONAL: VCF Services Platform size.
     */
    public static String vspSize = null;

    /**
     * OPTIONAL: VCF Services Platform internal cluster CIDR IPv4.
     */
    public static String vspInternalClusterCidrIPv4 = null;

    /**
     * OPTIONAL: VCF Services Platform internal cluster CIDR IPv6.
     */
    public static String vspInternalClusterCidrIPv6 = null;

    /**
     * REQUIRED: VCF Services Platform instance fqdn.
     */
    public static String vspInstanceFqdn = "vsp2.vcf.local";

    /**
     * OPTIONAL: VCF Services Platform fleet fqdn. VCF Services Platform cluster fleet FQDN.
     * This should be provided in VVF and primary VCF instance.
     * If building a secondary VCF instance, do not provide this field.
     */
    public static String vspFleetFqdn = null;

    /**
     * OPTIONAL: VCF Services Platform version.
     */
    public static String vspVersion = null;

    /**
     * REQUIRED: License server hostname.
     */
    public static String licenseServerHostname = "ls.vcf.local";

    /**
     * OPTIONAL: License server version.
     */
    public static String licenseServerVersion = null;

    /**
     * OPTIONAL: License server ssl thumbprint.
     */
    public static String licenseServerSslThumbprint = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(DeployVcfFleetFirstVcfInstanceFromExistingComponents.class, args);

        VcfInstallerClientFactory vcfInstallerClientFactory = new VcfInstallerClientFactory();

        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);

        String installerFqdn = hostnameToFqdn(vcfInstallerFqdn, dnsDomain);

        try (ApiClient client =
                vcfInstallerClientFactory.createClient(installerFqdn, vcfInstallerAdminPassword, keyStore)) {

            SddcSpec sddcSpec = createSddcSpecForNewVcfFleetWithExistingVcenterNsx(client);
            log.info("Crafted Deployment Spec is: {}", SddcSpecHelper.sddcSpecToJson(sddcSpec));

            Validations validations = client.createStub(Validations.class);
            Validation validationResult =
                    validations.validateSddcSpec(sddcSpec).invoke().get();
            String validationId = validationResult.getId();
            log.info("Started Sddc Spec validation task with id: {}", validationId);

            SddcTaskUtil.waitForValidationTaskAndFailOnError(validations, validationId);
            log.info("Finished Sddc Spec validation task with id: {}", validationId);

            if (!Boolean.TRUE.equals(validateOnly)) {
                log.info("Starting VCF Fleet deployment");

                Sddcs sddcs = client.createStub(Sddcs.class);
                SddcTask sddcTask = sddcs.deploySddc(sddcSpec).invoke().get();
                String sddcTaskId = sddcTask.getId();
                log.info("Started VCF Fleet deployment task with id: {}", sddcTaskId);

                SddcTaskUtil.waitForSddcDeploymentTaskAndFailOnError(sddcs, sddcTaskId);
                log.info("Finished VCF Fleet deployment task with id: {}", sddcTaskId);

                SddcSpecHelper.saveSddcSpecToFile(client, sddcTaskId, deploymentSpecSaveFilePath);
            }

            log.info("Sample completed successfully");
        }
    }

    public static SddcSpec createSddcSpecForNewVcfFleetWithExistingVcenterNsx(ApiClient vcfClient) throws Exception {
        SddcSpec.Builder builder = new SddcSpec.Builder();
        builder.setWorkflowType(SddcSpecHelper.WorkflowType.VCF.toString());
        builder.setCeipEnabled(true);
        builder.setVersion(MiscUtil.getVersionWithoutBuildNumber(vcfClient));
        builder.setNtpServers(List.of(ntpServers));
        builder.setDnsSpec(SddcSpecHelper.createDnsSpec(dnsDomain, dnsNameserver));

        // Operations stack
        // Deploy New VCF Operations
        builder.setVcfOperationsSpec(SddcSpecHelper.createVcfOperationsSpec(hostnameToFqdn(vcfOpsFqdn, dnsDomain)));
        // Deploy New VCF Automation
        builder.setVcfAutomationSpec(SddcSpecHelper.createSddcVcfAutomationSpecOnVsp(
                hostnameToFqdn(vcfAutomationFqdn, dnsDomain),
                vcfAutomationIpv4Addresses,
                vcfAutomationPlatformFqdn,
                false)); // VCF Automation use existing deployment.
        builder.setVcfOperationsCollectorSpec(
                SddcSpecHelper.createVcfCollectorSpec(hostnameToFqdn(vcfOpsCollectorFqdn, dnsDomain)));

        // vCenter
        // Use Existing vCenter
        builder.setVcenterSpec(SddcSpecHelper.createSddcVcenterSpec(
                hostnameToFqdn(vCenterFqdn, dnsDomain),
                vCenterThumbprint,
                vCenterRootPassword,
                vCenterAdminSsoUsername,
                vCenterAdminSsoPassword,
                trustStorePath));
        builder.setClusterSpec(SddcSpecHelper.createSddcClusterSpec(sddcId));

        // NSX-T
        // Use Existing New NSX-T
        builder.setNsxtSpec(SddcSpecHelper.createSddcNsxtSpec(
                hostnameToFqdn(nsxFqdn, dnsDomain),
                hostnameToFqdn(nsxVipFqdn, dnsDomain),
                nsxThumbprint,
                nsxRootPassword,
                nsxAdminPassword,
                nsxAuditPassword,
                trustStorePath));

        // Hosts
        // Skipping ESXi Thumbprint validation for deployments with existing vCenter.
        builder.setSkipEsxThumbprintValidation(true);

        builder.setVidbSpec(SddcSpecHelper.createVidbSpec(vidbHostname, vidbVersion, vidbSize));

        IPv4Pool IPv4Pool = SddcSpecHelper.createVspIPv4Pool(vspIPv4Cidr,
                                                             vspIPv4StartIpAddress,
                                                             vspIPv4EndIpAddress,
                                                             vspIPv4Addresses,
                                                             vspIPv4ExcludedAddresses);

        IPv6Pool IPv6Pool = SddcSpecHelper.createVspIPv6Pool(vspIPv6Cidr,
                                                             vspIPv6StartIpAddress,
                                                             vspIPv6EndIpAddress,
                                                             vspIPv6Addresses,
                                                             vspIPv6ExcludedAddresses);

        builder.setVspClusterSpec(SddcSpecHelper.createVspClusterSpec(vspPlatformFqdn,
                                                                      vspSystemUserPassword,
                                                                      IPv4Pool,
                                                                      IPv6Pool,
                                                                      vspSize,
                                                                      vspInternalClusterCidrIPv4,
                                                                      vspInternalClusterCidrIPv6,
                                                                      vspInstanceFqdn,
                                                                      vspFleetFqdn,
                                                                      vspVersion));

        builder.setLicenseServerSpec(SddcSpecHelper.createLicenseServerSpec(licenseServerHostname,
                                                                            licenseServerVersion,
                                                                            false, // use existing deployment
                                                                            licenseServerSslThumbprint));

        // SDDC Manager
        builder.setSddcId(sddcId);
        builder.setSddcManagerSpec(SddcSpecHelper.createSddcManagerSpec(
                hostnameToFqdn(sddcManagerFqdn, dnsDomain),
                AUTO_GENERATED_PASSWORD, // SDDC Manager Root Password
                AUTO_GENERATED_PASSWORD, // SDDC Manager Local User Password
                AUTO_GENERATED_PASSWORD, // SDDC Manager VCF User Password
                false)); // Use Existing SDDC Manager

        return builder.build();
    }
}
