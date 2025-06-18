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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcf.installer.utils.SddcSpecUtil;
import com.vmware.sdk.vcf.installer.model.DvsSpec;
import com.vmware.sdk.vcf.installer.model.SddcDatastoreSpec;
import com.vmware.sdk.vcf.installer.model.SddcHostSpec;
import com.vmware.sdk.vcf.installer.model.SddcNetworkConfigProfile;
import com.vmware.sdk.vcf.installer.model.SddcNetworkSpec;
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
 * Demonstrates how to deploy new VVF Instance.<br>
 * Prerequisites for successful deployment:
 *
 * <ol>
 *   <li>At least 4 prepared ESXi hosts
 *   <li>All provided hostnames must be resolvable from the VCF Installer appliance
 *   <li>The addresses of all components must be resolvable from the VCF Installer appliance (NTP, various VCF
 *       components, etc.)
 *   <li>The Depot configuration must be complete
 *   <li>The respective binary bundles need to be downloaded
 * </ol>
 *
 * Downloading respective bundles can be achieved by running the {@link DownloadBundlesVvfInstance} sample.
 */
public class DeployVvfInstance {
    private static final Logger log = LoggerFactory.getLogger(DeployVvfInstance.class);

    /** REQUIRED: VCF Installer Appliance name or FQDN. */
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

    /** REQUIRED: Hostname or FQDN of the vCenter that is to be deployed. */
    public static String vCenterFqdn = "vc1.vcf.local";

    /** OPTIONAL: SSO domain for the vCenter deployment. Defaults to vsphere.local. */
    public static String vCenterSsoDomain = null;

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
        SampleCommandLineParser.load(DeployVvfInstance.class, args);

        VcfInstallerClientFactory vcfInstallerClientFactory = new VcfInstallerClientFactory();

        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);

        String installerFqdn = hostnameToFqdn(vcfInstallerFqdn, dnsDomain);

        try (ApiClient client =
                vcfInstallerClientFactory.createClient(installerFqdn, vcfInstallerAdminPassword, keyStore)) {

            SddcSpec sddcSpec = createSddcSpecForNewVvfInstance(client);
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

    public static SddcSpec createSddcSpecForNewVvfInstance(ApiClient vcfClient) throws Exception {
        Map<String, String> hostFqdnToThumbprintMap = SddcSpecUtil.parseHostFqdns(esxHosts, dnsDomain, trustStorePath);
        List<SddcHostSpec> hostSpecs = SddcSpecUtil.createSddcHostSpecs(esxiHostRootPassword, hostFqdnToThumbprintMap);

        SddcNetworkConfigProfile defaultNetworkProfile =
                SddcSpecUtil.getDefaultNetworkProfile(vcfClient, dnsDomain, hostSpecs);
        List<DvsSpec> defaultDvsSpecs = defaultNetworkProfile.getDvsSpecs();
        List<SddcNetworkSpec> dvsPortgroups = defaultNetworkProfile
                .getDvsNameToPortgroupSpecs()
                .get(defaultDvsSpecs.get(0).getDvsName());

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
        // Deploy New vCenter
        builder.setVcenterSpec(
                SddcSpecUtil.createSddcVcenterSpec(hostnameToFqdn(vCenterFqdn, dnsDomain), vCenterSsoDomain));
        builder.setClusterSpec(SddcSpecUtil.createSddcClusterSpec(sddcId));

        // Host Storage
        SddcDatastoreSpec datastoreSpec = new SddcDatastoreSpec();
        datastoreSpec.setVsanSpec(SddcSpecUtil.createVsanSpec(sddcId));
        builder.setDatastoreSpec(datastoreSpec);

        // Hosts
        builder.setHostSpecs(hostSpecs);

        // Networking

        SddcSpecUtil.NetworkInput managementNetwork = new SddcSpecUtil.NetworkInput(
                managementNetworkGateway, managementNetworkSubnet, managementNetworkVlanId, null, null);
        SddcSpecUtil.NetworkInput vsanNetwork = new SddcSpecUtil.NetworkInput(
                vsanNetworkGateway,
                vsanNetworkSubnet,
                vsanNetworkVlanId,
                vsanNetworkIpRangeStart,
                vsanNetworkIpRangeEnd);
        SddcSpecUtil.NetworkInput vmotionNetwork = new SddcSpecUtil.NetworkInput(
                vmotionNetworkGateway,
                vmotionNetworkSubnet,
                vmotionNetworkVlanId,
                vmotionNetworkIpRangeStart,
                vmotionNetworkIpRangeEnd);

        builder.setDvsSpecs(defaultDvsSpecs);
        builder.setNetworkSpecs(
                SddcSpecUtil.createSddcNetworkSpecs(dvsPortgroups, managementNetwork, vsanNetwork, vmotionNetwork));

        // SDDC ID
        builder.setSddcId(sddcId);

        return builder.build();
    }
}
