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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcf.installer.helpers.SddcSpecHelper;
import com.vmware.sdk.vcf.installer.model.DvsSpec;
import com.vmware.sdk.vcf.installer.model.IPv4Pool;
import com.vmware.sdk.vcf.installer.model.IPv6Pool;
import com.vmware.sdk.vcf.installer.model.SddcDatastoreSpec;
import com.vmware.sdk.vcf.installer.model.SddcHostSpec;
import com.vmware.sdk.vcf.installer.model.SddcNetworkConfigProfile;
import com.vmware.sdk.vcf.installer.model.SddcNetworkSpec;
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
 * Demonstrates how to deploy new VCF Instance within an existing VCF Fleet, reusing existing VCF Operations, VCF
 * Operations Fleet Management, VCF Automation, VIDB and VSP. <br>
 * Prerequisites for successful deployment:
 *
 * <ol>
 *   <li>At least 4 prepared ESXi hosts
 *   <li>An existing VCF Fleet, which is to be extended with a new VCF Instance
 *   <li>All provided hostnames must be resolvable from the VCF Installer appliance
 *   <li>The addresses of all components must be resolvable from the VCF Installer appliance (NTP, various VCF
 *       components, etc.)
 *   <li>The Depot configuration must be complete
 *   <li>The respective binary bundles need to be downloaded
 * </ol>
 *
 * Downloading respective bundles can be achieved by running the {@link DownloadBundlesExtendVcfFleetWithVcfInstance}
 * sample.<br>
 * Deploying a new VCF Fleet with a VCF Instance can be achieved by running either
 * {@link DeployVcfFleetFirstVcfInstance} or {@link DeployVcfFleetFirstVcfInstanceFromExistingComponents} sample.
 */
public class ExtendVcfFleetWithVcfInstanceHighAvailability {
    private static final Logger log = LoggerFactory.getLogger(ExtendVcfFleetWithVcfInstanceHighAvailability.class);

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

    /** REQUIRED: Hostname or FQDN of the vCenter that will be deployed. */
    public static String vCenterFqdn = "vc1.vcf.local";

    /** OPTIONAL: SSO domain for the vCenter deployment. Defaults to vsphere.local. */
    public static String vCenterSsoDomain = null;

    /** REQUIRED: Hostname or FQDN of the NSX-T that will be deployed. */
    public static String nsxFqdn1 = "nsx1.vcf.local";

    /** REQUIRED: Hostname or FQDN of the NSX-T that will be deployed. */
    public static String nsxFqdn2 = "nsx2.vcf.local";

    /** REQUIRED: Hostname or FQDN of the NSX-T that will be deployed. */
    public static String nsxFqdn3 = "nsx3.vcf.local";

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

    /** REQUIRED: Password for the hosts used. */
    public static String esxiHostRootPassword = "HostPasswordForRootUser";

    /**
     * REQUIRED: String of a list of comma separated ESXi hostnames or fqdns. Optionally - SSL thumbprints can be
     * provided for ESXi host. SSL Thumbprints will be acquired automatically for ESXi hosts without explicitly provided
     * thumbprints.
     */
    public static String esxHosts = "esx1.vcf.local, "
            + "esx2, "
            + "esx3=51:8D:84:62:AB:06:9E:BC:1D:2C:F5:72:FB:D2:C4:CA:D3:7D:BF:E1:19:98:D7:6D:A9:F4:9A:A4:03:E3:0B:38, "
            + "esx4.vcf.local=3F:AD:17:6C:80:29:10:B2:C6:BB:B9:41:18:CD:1C:3D:04:FF:F8:22:4E:58:F0:FD:D4:44:D2:B1:0A:9B:94:20";

    /** REQUIRED: Gateway of the Management network. */
    public static String managementNetworkGateway = "192.168.1.1";

    /** REQUIRED: Subnet of the Management network. */
    public static String managementNetworkSubnet = "192.168.1.0/24";

    /** REQUIRED: VLAN ID Of the management network. */
    public static Integer managementNetworkVlanId = 1;

    /** REQUIRED: Gateway of the vSAN network. */
    public static String vsanNetworkGateway = "192.168.2.1";

    /** REQUIRED: Subnet of the vSAN network. */
    public static String vsanNetworkSubnet = "192.168.2.0/24";

    /** REQUIRED: VLAN ID Of the vSAN network. */
    public static Integer vsanNetworkVlanId = 2;

    /** REQUIRED: Start of the IP pool range of the vSAN network. */
    public static String vsanNetworkIpRangeStart = "192.168.2.2";

    /** REQUIRED: End of the IP pool range of the vSAN network. */
    public static String vsanNetworkIpRangeEnd = "192.168.2.200";

    /** REQUIRED: Gateway of the vMotion network. */
    public static String vmotionNetworkGateway = "192.168.3.1";

    /** REQUIRED: Subnet of the vMotion network. */
    public static String vmotionNetworkSubnet = "192.168.3.0/24";

    /** REQUIRED: VLAN ID Of the vMotion network. */
    public static Integer vmotionNetworkVlanId = 3;

    /** REQUIRED: Start of the IP pool range of the vMotion network. */
    public static String vmotionNetworkIpRangeStart = "192.168.3.2";

    /** REQUIRED: End of the IP pool range of the vMotion network. */
    public static String vmotionNetworkIpRangeEnd = "192.168.3.200";

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
        SampleCommandLineParser.load(ExtendVcfFleetWithVcfInstanceHighAvailability.class, args);

        VcfInstallerClientFactory vcfInstallerClientFactory = new VcfInstallerClientFactory();

        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);

        String installerFqdn = hostnameToFqdn(vcfInstallerFqdn, dnsDomain);

        try (ApiClient client =
                vcfInstallerClientFactory.createClient(installerFqdn, vcfInstallerAdminPassword, keyStore)) {
            SddcSpec sddcSpec = createSddcSpecForExtensionOfExistingVcfFleet(client);
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

    public static SddcSpec createSddcSpecForExtensionOfExistingVcfFleet(ApiClient vcfClient) throws Exception {
        Map<String, String> hostFqdnToThumbprintMap = SddcSpecHelper.parseHostFqdns(esxHosts, dnsDomain, trustStorePath);
        List<SddcHostSpec> hostSpecs = SddcSpecHelper.createSddcHostSpecs(esxiHostRootPassword, hostFqdnToThumbprintMap);

        SddcNetworkConfigProfile defaultNetworkProfile =
                SddcSpecHelper.getDefaultNetworkProfile(vcfClient, dnsDomain, hostSpecs);
        List<DvsSpec> defaultDvsSpecs = defaultNetworkProfile.getDvsSpecs();
        List<SddcNetworkSpec> dvsPortgroups = defaultNetworkProfile
                .getDvsNameToPortgroupSpecs()
                .get(defaultDvsSpecs.get(0).getDvsName());

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
        // Deploy new vCenter
        builder.setVcenterSpec(
                SddcSpecHelper.createSddcVcenterSpec(hostnameToFqdn(vCenterFqdn, dnsDomain), vCenterSsoDomain));
        builder.setClusterSpec(SddcSpecHelper.createSddcClusterSpec(sddcId));

        // NSX-T
        // Deploy New NSX-T
        SddcSpecHelper.NetworkInput nsxNetwork =
                new SddcSpecHelper.NetworkInput(nsxGateway, nsxSubnet, nsxVlanId, nsxIpRangeStart, nsxIpRangeEnd);

        builder.setNsxtSpec(SddcSpecHelper.createSddcNsxtSpecHA(
                hostnameToFqdn(nsxFqdn1, dnsDomain),
                hostnameToFqdn(nsxFqdn2, dnsDomain),
                hostnameToFqdn(nsxFqdn3, dnsDomain),
                hostnameToFqdn(nsxVipFqdn, dnsDomain),
                nsxNetwork));

        // Host Storage
        SddcDatastoreSpec datastoreSpec = new SddcDatastoreSpec();
        datastoreSpec.setVsanSpec(SddcSpecHelper.createVsanSpec(sddcId));
        builder.setDatastoreSpec(datastoreSpec);

        // Hosts
        builder.setHostSpecs(hostSpecs);

        // Networking
        SddcSpecHelper.NetworkInput managementNetwork = new SddcSpecHelper.NetworkInput(
                managementNetworkGateway, managementNetworkSubnet, managementNetworkVlanId, null, null);
        SddcSpecHelper.NetworkInput vsanNetwork = new SddcSpecHelper.NetworkInput(
                vsanNetworkGateway,
                vsanNetworkSubnet,
                vsanNetworkVlanId,
                vsanNetworkIpRangeStart,
                vsanNetworkIpRangeEnd);
        SddcSpecHelper.NetworkInput vmotionNetwork = new SddcSpecHelper.NetworkInput(
                vmotionNetworkGateway,
                vmotionNetworkSubnet,
                vmotionNetworkVlanId,
                vmotionNetworkIpRangeStart,
                vmotionNetworkIpRangeEnd);

        builder.setDvsSpecs(defaultDvsSpecs);
        builder.setNetworkSpecs(
                SddcSpecHelper.createSddcNetworkSpecs(dvsPortgroups, managementNetwork, vsanNetwork, vmotionNetwork));

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
