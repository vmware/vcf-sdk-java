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
import static com.vmware.vapi.internal.util.StringUtils.isBlank;

import java.security.KeyStore;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcf.installer.helpers.SddcSpecHelper;
import com.vmware.sdk.vcf.installer.model.IPv4Pool;
import com.vmware.sdk.vcf.installer.model.IPv6Pool;
import com.vmware.sdk.vcf.installer.model.SddcSpec;
import com.vmware.sdk.vcf.installer.model.SddcTask;
import com.vmware.sdk.vcf.installer.model.Validation;
import com.vmware.sdk.vcf.installer.model.VcfOperationsDiscoveryResult;
import com.vmware.sdk.vcf.installer.utils.MiscUtil;
import com.vmware.sdk.vcf.installer.utils.SddcTaskUtil;
import com.vmware.sdk.vcf.installer.utils.VcfInstallerClientFactory;
import com.vmware.sdk.vcf.installer.v1.Sddcs;
import com.vmware.sdk.vcf.installer.v1.sddcs.Validations;
import com.vmware.vapi.client.ApiClient;

/**
 * Demonstrates how to deploy new VCF Instance within an existing VCF Fleet, reusing existing Vcenter, using a new
 * NSX, VIDB and VSP. <br>
 * Prerequisites for successful deployment:
 *
 * <ol>
 *   <li>An existing VCF Fleet, which is to be extended with a new VCF Instance
 *   <li>Existing components need to be configured and reachable by the VCF Installer appliance
 *   <li>All provided hostnames must be resolvable from the VCF Installer appliance
 *   <li>The addresses of all components must be resolvable the VCF Installer appliance (NTP, various VCF components,
 *       etc.)
 *   <li>The Depot configuration must be complete
 *   <li>The respective binary bundles need to be downloaded
 * </ol>
 *
 * Downloading respective bundles can be achieved by running the
 * {@link DownloadBundlesVcfFleetFirstVcfInstanceFromExistingComponents} sample. <br>
 * Deploying a new VCF Fleet with a VCF Instance can by achieved by running either
 * {@link DeployVcfFleetFirstVcfInstance} or {@link DeployVcfFleetFirstVcfInstanceFromExistingComponents} sample.
 */
public class ExtendVcfFleetWithVcfInstanceFromExistingVcNewNsx {
    private static final Logger log = LoggerFactory.getLogger(ExtendVcfFleetWithVcfInstanceFromExistingVcNewNsx.class);

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

    /** OPTIONAL: SSL Certificate SHA256 Thumbprint of the existing VCF Operations Fleet Management. */
    public static String vcfOpsFleetManagementThumbprint = null;

    /** REQUIRED: Admin user password of existing the VCF Operations Fleet Management. */
    public static String vcfOpsFleetManagementAdminPassword = "VcfOpsFleetManagementAdminPassword";

    /** REQUIRED: Hostname or FQDN of the existing VCF Operations. */
    public static String vcfOpsFqdn = "vcfops1";

    /** REQUIRED: Admin user password of existing the VCF Operations. */
    public static String vcfOpsAdminPassword = "VcfOpsAdminPassword";

    /** OPTIONAL: SSL Certificate SHA256 Thumbprint of the existing VCF Operations. */
    public static String vcfOpsThumbprint = null;

    /**
     * OPTIONAL: Hostname or FQDN of the existing VCF Automation. If passed discovery will be skipped for VCF
     * Automation.
     */
    public static String vcfAutomationFqdn = null;

    /** OPTIONAL: SSL Certificate SHA256 Thumbprint of the existing the VCF Automation deployment. */
    public static String vcfAutomationThumbprint = null;

    /** OPTIONAL: Admin user password of existing the VCF Automation. */
    public static String vcfAutomationAdminPassword = null;

    /** REQUIRED: Hostname or FQDN of the existing VCF Operations Collector. */
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

    /** REQUIRED: Hostname or FQDN of the NSX-T that will be deployed. */
    public static String nsxFqdn = "nsx1.vcf.local";

    /** REQUIRED: Hostname or FQDN of the cluster for the NSX-T deployment. */
    public static String nsxVipFqdn = "nsx.vcf.local";

    /** REQUIRED: Gateway for the NSX-T deployment. */
    public static String nsxGateway = "192.168.11.1";

    /** REQUIRED: Subnet of the NSX-T deployment. */
    public static String nsxSubnet = "192.168.11.0/24";

    /** REQUIRED: Transport VLAN ID of the NSX-T deployment. */
    public static Integer nsxVlanId = 4;

    /** REQUIRED: Start of the IP Pool Range of the NSX-T deployment. */
    public static String nsxIpRangeStart = "192.168.11.2";

    /** REQUIRED: End of the IP Pool Range of the NSX-T deployment. */
    public static String nsxIpRangeEnd = "192.168.11.2";

    /** REQUIRED: SDDC ID. */
    public static String sddcId = "sddc-01";

    /** REQUIRED: Hostname or FQDN of the SDDC Manager that will be deployed. */
    public static String sddcManagerFqdn = "sm.vcf.local";

    /** OPTIONAL: VIDB hostname. */
    public static String vidbHostname = null;

    /** OPTIONAL: VIDB size. */
    public static String vidbSize = null;

    /** REQUIRED: VSP platform fqdn. */
    public static String vspPlatformFqdn = "vsp-platform.vrack.vsphere.local";

    /** OPTIONAL: VSP IPv4 CIDR. */
    public static String vspIpv4Cidr = null;

    /** OPTIONAL: VSP IPv4 start IP address. */
    public static String vspIpv4StartIpAddress = null;

    /** OPTIONAL: VSP IPv4 end IP address. */
    public static String vspIpv4EndIpAddress = null;

    /** OPTIONAL: VSP IPv4 IP pool addresses. */
    public static String[] vspIpv4Addresses = null;

    /** OPTIONAL: VSP IPv4 excluded addresses. */
    public static String[] vspIpv4ExcludedAddresses = null;

    /** OPTIONAL: VSP IPv6 CIDR. */
    public static String vspIpv6Cidr = null;

    /** OPTIONAL: VSP IPv6 start IP address. */
    public static String vspIpv6StartIpAddress = null;

    /** OPTIONAL: VSP IPv6 end IP address. */
    public static String vspIpv6EndIpAddress = null;

    /** OPTIONAL: VSP IPv6 pool addresses. */
    public static String[] vspIpv6Addresses = null;

    /** OPTIONAL: VSP IPv6 excluded addresses. */
    public static String[] vspIpv6ExcludedAddresses = null;

    /** OPTIONAL: VSP size. */
    public static String vspSize = null;

    /** OPTIONAL: VSP internal cluster CIDR for IPv4. */
    public static String vspInternalClusterCidrIpv4 = null;

    /** OPTIONAL: VSP internal cluster CIDR for IPv6. */
    public static String vspInternalClusterCidrIpv6 = null;

    /** REQUIRED: VSP instance FQDN. */
    public static String vspInstanceFqdn = "vsp-instance.vrack.vsphere.local";

    /** OPTIONAL: VSP fleet FQDN. */
    public static String vspFleetFqdn = null;

    /** OPTIONAL: VSP use existing deployment. */
    public static Boolean vspUseExistingDeployment = null;

    /** OPTIONAL: VSP SSL thumbprint. */
    public static String vspSslThumbprint = null;

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

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(ExtendVcfFleetWithVcfInstanceFromExistingVcNewNsx.class, args);

        VcfInstallerClientFactory vcfInstallerClientFactory = new VcfInstallerClientFactory();

        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);

        String installerFqdn = hostnameToFqdn(vcfInstallerFqdn, dnsDomain);

        try (ApiClient client =
                vcfInstallerClientFactory.createClient(installerFqdn, vcfInstallerAdminPassword, keyStore)) {
            SddcSpec sddcSpec = createSddcSpecForExtensionWithExistingVcNewNsx(client);
            log.info("Crafted Deployment Spec is: {}", SddcSpecHelper.sddcSpecToJson(sddcSpec));

            Validations validations = client.createStub(Validations.class);
            Validation validationResult =
                    validations.validateSddcSpec(sddcSpec).invoke().get();
            String validationId = validationResult.getId();
            log.info("Started Sddc Spec validation task with id: {}", validationId);

            SddcTaskUtil.waitForValidationTaskAndFailOnError(validations, validationId);
            log.info("Finished Sddc Spec validation task with id: {}", validationId);

            if (!Boolean.TRUE.equals(validateOnly)) {
                log.info("Starting VCF Instance deployment into existing VCF Fleet");

                Sddcs sddcs = client.createStub(Sddcs.class);
                SddcTask sddcTask = sddcs.deploySddc(sddcSpec).invoke().get();
                String sddcTaskId = sddcTask.getId();
                log.info("Started VCF Instance deployment task with id: {}", sddcTaskId);

                SddcTaskUtil.waitForSddcDeploymentTaskAndFailOnError(sddcs, sddcTaskId);
                log.info("Finished VCF Instance deployment task with id: {}", sddcTaskId);

                SddcSpecHelper.saveSddcSpecToFile(client, sddcTaskId, deploymentSpecSaveFilePath);
            }

            log.info("Sample completed successfully");
        }
    }

    public static SddcSpec createSddcSpecForExtensionWithExistingVcNewNsx(ApiClient vcfClient) throws Exception {
        String vcfOperationsFqdn = hostnameToFqdn(vcfOpsFqdn, dnsDomain);
        String vcfOperationsThumbprint = vcfOpsThumbprint;
        if (isBlank(vcfOperationsThumbprint)) {
            vcfOperationsThumbprint = SddcSpecHelper.getSslThumbprint(vcfOperationsFqdn, trustStorePath);
        }

        VcfOperationsDiscoveryResult vcfOpsDiscoveryResult =
                SddcSpecHelper.discoverVcfOps(vcfClient, vcfOperationsFqdn, vcfOpsAdminPassword, vcfOperationsThumbprint);

        SddcSpec.Builder builder = new SddcSpec.Builder();
        builder.setWorkflowType(SddcSpecHelper.WorkflowType.VCF.toString());
        builder.setCeipEnabled(true);
        builder.setVersion(MiscUtil.getVersionWithoutBuildNumber(vcfClient));
        builder.setNtpServers(List.of(ntpServers));
        builder.setDnsSpec(SddcSpecHelper.createDnsSpec(dnsDomain, dnsNameserver));

        // Operations stack
        // Use Existing VCF Operations
        builder.setVcfOperationsSpec(SddcSpecHelper.createVcfOperationsSpec(
                vcfOperationsFqdn,
                vcfOpsAdminPassword,
                vcfOperationsThumbprint,
                vcfOpsDiscoveryResult.getVcfOperationsNodes()));
        // Use Existing VCF Automation
        builder.setVcfAutomationSpec(SddcSpecHelper.createSddcVcfAutomationSpec(
                hostnameToFqdn(vcfAutomationFqdn, dnsDomain),
                vcfAutomationAdminPassword,
                vcfAutomationThumbprint,
                vcfOpsDiscoveryResult.getVcfAutomationNodes(),
                trustStorePath));
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
        // Deploy New NSX-T
        SddcSpecHelper.NetworkInput nsxNetwork =
                new SddcSpecHelper.NetworkInput(nsxGateway, nsxSubnet, nsxVlanId, nsxIpRangeStart, nsxIpRangeEnd);

        builder.setNsxtSpec(SddcSpecHelper.createSddcNsxtSpec(
                hostnameToFqdn(nsxFqdn, dnsDomain), hostnameToFqdn(nsxVipFqdn, dnsDomain), nsxNetwork));

        // SDDC Manager
        builder.setSddcId(sddcId);
        builder.setSddcManagerSpec(SddcSpecHelper.createSddcManagerSpec(
                hostnameToFqdn(sddcManagerFqdn, dnsDomain),
                AUTO_GENERATED_PASSWORD, // SDDC Manager Root Password
                AUTO_GENERATED_PASSWORD, // SDDC Manager Local User Password
                AUTO_GENERATED_PASSWORD, // SDDC Manager VCF User Password
                false)); // Use Existing SDDC Manager

        // VIDB spec
        builder.setVidbSpec(SddcSpecHelper.createVidbSpec(vidbHostname, null, vidbSize));

        // IPv4 pool
        IPv4Pool ipv4Pool = SddcSpecHelper.createVspIPv4Pool(vspIpv4Cidr, 
                                                          vspIpv4StartIpAddress, 
                                                          vspIpv4EndIpAddress, 
                                                          vspIpv4Addresses, 
                                                          vspIpv4ExcludedAddresses);

        // IPv6 pool
        IPv6Pool ipv6Pool = SddcSpecHelper.createVspIPv6Pool(vspIpv6Cidr, 
                                                          vspIpv6StartIpAddress, 
                                                          vspIpv6EndIpAddress, 
                                                          vspIpv6Addresses, 
                                                          vspIpv6ExcludedAddresses);

        // VSP cluster spec
        builder.setVspClusterSpec(SddcSpecHelper.createVspClusterSpec(vspPlatformFqdn,
                                                                      AUTO_GENERATED_PASSWORD,
                                                                      ipv4Pool, 
                                                                      ipv6Pool, 
                                                                      vspSize,
                                                                      vspInternalClusterCidrIpv4, 
                                                                      vspInternalClusterCidrIpv6,
                                                                      vspInstanceFqdn, 
                                                                      vspFleetFqdn,
                                                                      null));

        return builder.build();
    }
}
