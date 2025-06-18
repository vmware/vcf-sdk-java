/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcf.installer.utils;

import static com.vmware.vapi.internal.util.StringUtils.isBlank;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.sdk.samples.utils.ssl.SecurityHelper;
import com.vmware.sdk.vcf.installer.model.DnsSpec;
import com.vmware.sdk.vcf.installer.model.IpAddressPoolRangeSpec;
import com.vmware.sdk.vcf.installer.model.IpAddressPoolSpec;
import com.vmware.sdk.vcf.installer.model.IpAddressPoolSubnetSpec;
import com.vmware.sdk.vcf.installer.model.IpRange;
import com.vmware.sdk.vcf.installer.model.NsxtManagerSpec;
import com.vmware.sdk.vcf.installer.model.SddcClusterSpec;
import com.vmware.sdk.vcf.installer.model.SddcCredentials;
import com.vmware.sdk.vcf.installer.model.SddcHostSpec;
import com.vmware.sdk.vcf.installer.model.SddcManagerSpec;
import com.vmware.sdk.vcf.installer.model.SddcNetworkConfigProfile;
import com.vmware.sdk.vcf.installer.model.SddcNetworkConfigProfileResponse;
import com.vmware.sdk.vcf.installer.model.SddcNetworkConfigProfileSpec;
import com.vmware.sdk.vcf.installer.model.SddcNetworkSpec;
import com.vmware.sdk.vcf.installer.model.SddcNsxtSpec;
import com.vmware.sdk.vcf.installer.model.SddcSpec;
import com.vmware.sdk.vcf.installer.model.SddcVcenterSpec;
import com.vmware.sdk.vcf.installer.model.VcfAutomationNodeInfo;
import com.vmware.sdk.vcf.installer.model.VcfAutomationSpec;
import com.vmware.sdk.vcf.installer.model.VcfOperationsCollectorSpec;
import com.vmware.sdk.vcf.installer.model.VcfOperationsDiscoveryResult;
import com.vmware.sdk.vcf.installer.model.VcfOperationsDiscoverySpec;
import com.vmware.sdk.vcf.installer.model.VcfOperationsFleetManagementSpec;
import com.vmware.sdk.vcf.installer.model.VcfOperationsManagementNodeInfo;
import com.vmware.sdk.vcf.installer.model.VcfOperationsNode;
import com.vmware.sdk.vcf.installer.model.VcfOperationsNodeInfo;
import com.vmware.sdk.vcf.installer.model.VcfOperationsSpec;
import com.vmware.sdk.vcf.installer.model.VsanEsaConfig;
import com.vmware.sdk.vcf.installer.model.VsanSpec;
import com.vmware.sdk.vcf.installer.v1.sddcs.NetworkConfigProfiles;
import com.vmware.sdk.vcf.installer.v1.sddcs.Spec;
import com.vmware.sdk.vcf.installer.v1.sddcs.VcfopsDiscovery;
import com.vmware.vapi.client.ApiClient;

/** Util class used for crafting default SDDC spec. */
public class SddcSpecUtil {
    private static final Logger log = LoggerFactory.getLogger(SddcSpecUtil.class);

    public enum WorkflowType {
        VCF,
        VVF
    }

    public static final String CLUSTER_NAME = "%s-cl-01";
    public static final String CLUSTER_DATACENTER_NAME = "%s-cl-vdc-01";

    public static final boolean VSAN_ESA_ENABLED = false;

    public static final String VSAN_DATASTORE_NAME = "%s-cl-ds-vsan-01";

    public static final String VSAN_STORAGE_TYPE = "VSAN";

    public static final String VSAN_NETWORK_TYPE = "VSAN";
    public static final String MANAGEMENT_NETWORK_TYPE = "MANAGEMENT";
    public static final String VMOTION_NETWORK_TYPE = "VMOTION";
    public static final String VM_MANAGEMENT_NETWORK_TYPE = "VM_MANAGEMENT";

    public static final String VM_APPLIANCE_SIZE_SMALL = "small";
    public static final String NSX_APPLIANCE_SIZE_MEDIUM = "medium";
    public static final String VCENTER_STORAGE_LARGE = "lstorage";

    public static final String VCFA_INTERNAL_CLUSTER_CIDR = "198.18.0.0/15";

    public static final String VCF_OPS_MASTER_NODE_TYPE = "master";

    public static final String ROOT_USER = "root";
    public static final String ADMIN_USER = "admin";

    public static final String VCENTER_SSO_DOMAIN = "vsphere.local";

    public static final String AUTO_GENERATED_PASSWORD = null; // Password is to be auto-generated.

    public static final String FQDN_NODE_ADDRESS_TYPE = "fqdn";

    public static SddcClusterSpec createSddcClusterSpec(String sddcId) {
        return new SddcClusterSpec.Builder()
                .setClusterName(String.format(CLUSTER_NAME, sddcId))
                .setDatacenterName(String.format(CLUSTER_DATACENTER_NAME, sddcId))
                .build();
    }

    public static VsanSpec createVsanSpec(String sddcId) {
        VsanEsaConfig esaConfig = new VsanEsaConfig();
        esaConfig.setEnabled(VSAN_ESA_ENABLED);

        return new VsanSpec.Builder()
                .setDatastoreName(String.format(VSAN_DATASTORE_NAME, sddcId))
                .setVsanDedup(false)
                .setFailuresToTolerate(1L)
                .setEsaConfig(esaConfig)
                .build();
    }

    public static DnsSpec createDnsSpec(String dnsDomain, String dnsNameserver) {
        return new DnsSpec.Builder()
                .setNameservers(Collections.singletonList(dnsNameserver))
                .setSubdomain(dnsDomain)
                .build();
    }

    public static SddcNetworkConfigProfile getDefaultNetworkProfile(
            ApiClient vcfClient, String dnsDomain, List<SddcHostSpec> hostSpecs) {
        SddcNetworkConfigProfileSpec sddcNetworkConfigProfileSpec = new SddcNetworkConfigProfileSpec.Builder()
                .setHostSpecs(hostSpecs)
                .setSubdomain(dnsDomain)
                .setStorageType(VSAN_STORAGE_TYPE)
                .build();
        SddcNetworkConfigProfileResponse networkConfigProfileResponse;

        try {
            networkConfigProfileResponse = vcfClient
                    .createStub(NetworkConfigProfiles.class)
                    .getNetworkConfigProfiles(sddcNetworkConfigProfileSpec)
                    .invoke()
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to acquire network config profiles from VCF.", e);
            throw new RuntimeException(e);
        }
        if (networkConfigProfileResponse != null
                && networkConfigProfileResponse.getProfiles() != null
                && !networkConfigProfileResponse.getProfiles().isEmpty()) {

            log.info("Successfully acquired network config profile from VCF.");
            return networkConfigProfileResponse.getProfiles().get(0);
        }
        throw new RuntimeException("Failed to acquire default network config profile from VCF.");
    }

    public static VcfOperationsDiscoveryResult discoverVcfOps(
            ApiClient vcfClient, String vcfOpsFqdn, String vcfOpsAdminPassword, String vcfOpsSslThumbprint) {
        VcfOperationsDiscoverySpec vcfOperationsDiscoverySpec = new VcfOperationsDiscoverySpec();
        vcfOperationsDiscoverySpec.setAddress(vcfOpsFqdn);
        vcfOperationsDiscoverySpec.setAdminUsername(ADMIN_USER);
        vcfOperationsDiscoverySpec.setAdminPassword(vcfOpsAdminPassword);
        vcfOperationsDiscoverySpec.setSslThumbprint(vcfOpsSslThumbprint);

        try {
            VcfOperationsDiscoveryResult vcfOperationsDiscoveryResult = vcfClient
                    .createStub(VcfopsDiscovery.class)
                    .discoverVcfOps(vcfOperationsDiscoverySpec)
                    .invoke()
                    .get();

            return vcfOperationsDiscoveryResult;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<SddcHostSpec> createSddcHostSpecs(
            String hostPassword, Map<String, String> hostFqdnToThumbprintMap) {
        List<SddcHostSpec> sddcHostSpecs = new ArrayList<>();
        hostFqdnToThumbprintMap.forEach(
                (fqdn, thumbprint) -> sddcHostSpecs.add(createSddcHostSpec(fqdn, hostPassword, thumbprint)));
        return sddcHostSpecs;
    }

    public static SddcHostSpec createSddcHostSpec(String hostname, String hostPassword, String hostThumbprint) {
        SddcHostSpec sddcHostSpec = new SddcHostSpec();

        SddcCredentials sddcCredentials = new SddcCredentials();
        sddcCredentials.setUsername(ROOT_USER);
        sddcCredentials.setPassword(hostPassword);

        sddcHostSpec.setCredentials(sddcCredentials);
        sddcHostSpec.setHostname(hostname);
        sddcHostSpec.setSslThumbprint(hostThumbprint);
        return sddcHostSpec;
    }

    public static List<SddcNetworkSpec> createSddcNetworkSpecs(
            List<SddcNetworkSpec> defaultPortgroupList,
            NetworkInput managementNetwork,
            NetworkInput vsanNetwork,
            NetworkInput vmotionNetwork) {
        if (defaultPortgroupList == null || defaultPortgroupList.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, SddcNetworkSpec> networkTypeToPortgroupMap = defaultPortgroupList.stream()
                .collect(Collectors.toMap(networkSpec -> networkSpec.getNetworkType(), networkSpec -> networkSpec));

        List<SddcNetworkSpec> sddcNetworkSpecs = new ArrayList<>();
        // VM_MANAGEMENT Network Spec
        sddcNetworkSpecs.add(createSddcNetworkSpec(
                networkTypeToPortgroupMap.get(VM_MANAGEMENT_NETWORK_TYPE),
                managementNetwork.gateway,
                managementNetwork.subnet,
                managementNetwork.vlanId,
                null));

        // MANAGEMENT Network Spec
        sddcNetworkSpecs.add(createSddcNetworkSpec(
                networkTypeToPortgroupMap.get(MANAGEMENT_NETWORK_TYPE),
                managementNetwork.gateway,
                managementNetwork.subnet,
                managementNetwork.vlanId,
                null));

        // VSAN Network Spec
        IpRange ipRange1 = new IpRange();
        ipRange1.setStartIpAddress(vsanNetwork.ipRangeStart);
        ipRange1.setEndIpAddress(vsanNetwork.ipRangeEnd);

        sddcNetworkSpecs.add(createSddcNetworkSpec(
                networkTypeToPortgroupMap.get(VSAN_NETWORK_TYPE),
                vsanNetwork.gateway,
                vsanNetwork.subnet,
                vsanNetwork.vlanId,
                Collections.singletonList(ipRange1)));

        // VMOTION Network Spec
        IpRange ipRange2 = new IpRange();
        ipRange2.setStartIpAddress(vmotionNetwork.ipRangeStart);
        ipRange2.setEndIpAddress(vmotionNetwork.ipRangeEnd);

        sddcNetworkSpecs.add(createSddcNetworkSpec(
                networkTypeToPortgroupMap.get(VMOTION_NETWORK_TYPE),
                vmotionNetwork.gateway,
                vmotionNetwork.subnet,
                vmotionNetwork.vlanId,
                Collections.singletonList(ipRange2)));

        return sddcNetworkSpecs;
    }

    public static SddcNetworkSpec createSddcNetworkSpec(
            SddcNetworkSpec portGroup, String gateway, String subnet, long vlanId, List<IpRange> ipRanges) {
        return new SddcNetworkSpec.Builder()
                .setNetworkType(portGroup.getNetworkType())
                .setMtu(portGroup.getMtu())
                .setPortGroupKey(portGroup.getPortGroupKey())
                .setGateway(gateway)
                .setSubnet(subnet)
                .setTeamingPolicy(portGroup.getTeamingPolicy())
                .setVlanId(vlanId)
                .setIncludeIpAddressRanges(ipRanges)
                .setActiveUplinks(portGroup.getActiveUplinks())
                .build();
    }

    public static SddcNsxtSpec createSddcNsxtSpec(
            String nsxtHostname, String nsxtVipHostname, NetworkInput nsxtNetwork) {
        // Deploy New NSX-T

        IpAddressPoolRangeSpec ipRange = new IpAddressPoolRangeSpec();
        ipRange.setStart(nsxtNetwork.ipRangeStart);
        ipRange.setEnd(nsxtNetwork.ipRangeEnd);

        IpAddressPoolSubnetSpec ipAddressPoolSubnetSpec = new IpAddressPoolSubnetSpec.Builder()
                .setGateway(nsxtNetwork.gateway)
                .setCidr(nsxtNetwork.subnet)
                .setIpAddressPoolRanges(Collections.singletonList(ipRange))
                .build();

        NsxtManagerSpec nsxtManagerSpec = new NsxtManagerSpec();
        nsxtManagerSpec.setHostname(nsxtHostname);

        IpAddressPoolSpec ipAddressPoolSpec = new IpAddressPoolSpec.Builder()
                .setDescription("ESXi Host Overlay TEP IP Pool")
                .setName("tep01")
                .setSubnets(Collections.singletonList(ipAddressPoolSubnetSpec))
                .build();

        SddcNsxtSpec.Builder builder = new SddcNsxtSpec.Builder()
                .setIpAddressPoolSpec(ipAddressPoolSpec)
                .setRootNsxtManagerPassword(AUTO_GENERATED_PASSWORD)
                .setNsxtAdminPassword(AUTO_GENERATED_PASSWORD)
                .setNsxtAuditPassword(AUTO_GENERATED_PASSWORD)
                .setNsxtManagers(Collections.singletonList(nsxtManagerSpec))
                .setNsxtManagerSize(NSX_APPLIANCE_SIZE_MEDIUM)
                .setSkipNsxOverlayOverManagementNetwork(true)
                .setTransportVlanId((long) nsxtNetwork.vlanId)
                .setUseExistingDeployment(false)
                .setVipFqdn(nsxtVipHostname)
                .setEnableEdgeClusterSync(false);

        return builder.build();
    }

    public static SddcNsxtSpec createSddcNsxtSpec(
            String nsxtHostname,
            String nsxtVipHostname,
            String nsxtSslThumbprint,
            String nsxtRootPassword,
            String nsxtAdminPassword,
            String nsxtAuditPassword,
            String trustStorePath) {
        // Use Existing NSX-T
        if (isBlank(nsxtSslThumbprint)) {
            nsxtSslThumbprint = getSslThumbprint(nsxtVipHostname, trustStorePath);
        }

        NsxtManagerSpec nsxtManagerSpec = new NsxtManagerSpec();
        nsxtManagerSpec.setHostname(nsxtHostname);

        SddcNsxtSpec.Builder builder = new SddcNsxtSpec.Builder()
                .setSslThumbprint(nsxtSslThumbprint)
                .setRootNsxtManagerPassword(nsxtRootPassword)
                .setNsxtAdminPassword(nsxtAdminPassword)
                .setNsxtAuditPassword(nsxtAuditPassword)
                .setNsxtManagers(Collections.singletonList(nsxtManagerSpec))
                .setUseExistingDeployment(true)
                .setVipFqdn(nsxtVipHostname)
                .setEnableEdgeClusterSync(false);

        return builder.build();
    }

    public static SddcManagerSpec createSddcManagerSpec(
            String sddcManagerFqdn,
            String rootUserPassword,
            String localUserPassword,
            String vcfUserPassword,
            boolean useExistingDeployment) {
        SddcManagerSpec sddcManagerSpec = new SddcManagerSpec();
        sddcManagerSpec.setHostname(sddcManagerFqdn);
        sddcManagerSpec.setRootPassword(rootUserPassword);
        sddcManagerSpec.setSshPassword(vcfUserPassword);
        sddcManagerSpec.setLocalUserPassword(localUserPassword);
        sddcManagerSpec.setUseExistingDeployment(useExistingDeployment);
        return sddcManagerSpec;
    }

    public static SddcVcenterSpec createSddcVcenterSpec(String vCenterHostname, String vCenterSsoDomain) {
        // Deploy New vCenter
        if (isBlank(vCenterSsoDomain)) {
            vCenterSsoDomain = VCENTER_SSO_DOMAIN;
        }
        SddcVcenterSpec vcenterSpec = new SddcVcenterSpec();
        vcenterSpec.setVcenterHostname(vCenterHostname);
        vcenterSpec.setRootVcenterPassword(AUTO_GENERATED_PASSWORD);
        vcenterSpec.setStorageSize(VCENTER_STORAGE_LARGE);
        vcenterSpec.setVmSize(VM_APPLIANCE_SIZE_SMALL);
        vcenterSpec.setSsoDomain(vCenterSsoDomain);
        vcenterSpec.setUseExistingDeployment(false);

        return vcenterSpec;
    }

    public static SddcVcenterSpec createSddcVcenterSpec(
            String vCenterHostname,
            String vCenterThumbprint,
            String vCenterRootPassword,
            String adminSsoUsername,
            String adminSsoPassword,
            String trustStorePath) {
        // Use Existing vCenter
        if (isBlank(vCenterThumbprint)) {
            vCenterThumbprint = getSslThumbprint(vCenterHostname, trustStorePath);
        }

        SddcVcenterSpec vcenterSpec = new SddcVcenterSpec();
        vcenterSpec.setVcenterHostname(vCenterHostname);
        vcenterSpec.setRootVcenterPassword(vCenterRootPassword);
        vcenterSpec.setUseExistingDeployment(true);
        vcenterSpec.setAdminUserSsoUsername(adminSsoUsername);
        vcenterSpec.setAdminUserSsoPassword(adminSsoPassword);
        vcenterSpec.setSslThumbprint(vCenterThumbprint);

        return vcenterSpec;
    }

    public static VcfAutomationSpec createSddcVcfAutomationSpec(
            String vcfAutomationFqdn, String vcfAutomationIpPoolStart, String vcfAutomationIpPoolEnd) {
        // Deploying new VCF Automation

        // If no arguments are provided VCF Automation deployment will be skipped.
        // Users will be required to set up VCF Automation manually at a later date.
        // If not all arguments are provided an exception is thrown.
        if (isBlank(vcfAutomationFqdn) && isBlank(vcfAutomationIpPoolStart) && isBlank(vcfAutomationIpPoolEnd)) {
            // None the required arguments for VCF Automation deployment are provided. Skipping VCF Automation
            // deployment.
            log.info("VCF Automation FQDN not found. Skipping creation of VCF Automation deployment.");
            return null;
        }
        if (isBlank(vcfAutomationFqdn) || isBlank(vcfAutomationIpPoolStart) || isBlank(vcfAutomationIpPoolEnd)) {
            // Some of the required arguments for VCF Automation are missing. Throwing an exception, because either
            // all arguments are expected or none of them.
            throw new RuntimeException(
                    "Invalid arguments provided for VCF Automation deployment. Either all arguments "
                            + "(fqdn, ip range start and ip range end) need to be provided or all should be null to skip VCF Automation deployment.");
        }

        VcfAutomationSpec vcfAutomationSpec = new VcfAutomationSpec();
        vcfAutomationSpec.setHostname(vcfAutomationFqdn);
        vcfAutomationSpec.setAdminUserPassword(AUTO_GENERATED_PASSWORD);
        vcfAutomationSpec.setInternalClusterCidr(VCFA_INTERNAL_CLUSTER_CIDR);
        vcfAutomationSpec.setIpPool(Arrays.asList(vcfAutomationIpPoolStart, vcfAutomationIpPoolEnd));
        vcfAutomationSpec.setUseExistingDeployment(false);

        return vcfAutomationSpec;
    }

    public static VcfAutomationSpec createSddcVcfAutomationSpec(
            String vcfAutomationFqdn,
            String vcfAutomationAdminPassword,
            String vcfAutomationSslThumbprint,
            List<VcfAutomationNodeInfo> vcfAutomationNodeList,
            String trustStorePath) {
        // Using existing VCF Automation
        if (isBlank(vcfAutomationAdminPassword)) {
            log.info("VCF Automation Password not provided. Skipping VCF Automation setup in the SDDC spec.");
            return null;
        }
        if (isBlank(vcfAutomationFqdn)) {
            if (vcfAutomationNodeList != null && !vcfAutomationNodeList.isEmpty()) {
                VcfAutomationNodeInfo nodeInfo = vcfAutomationNodeList.get(0);
                vcfAutomationFqdn = nodeInfo.getAddresses().stream()
                        .filter(a -> a.getType().equalsIgnoreCase(FQDN_NODE_ADDRESS_TYPE))
                        .findFirst()
                        .get()
                        .getValue();
                vcfAutomationSslThumbprint =
                        nodeInfo.getCertificateThumbprints().get(0);
            }
        } else if (isBlank(vcfAutomationSslThumbprint)) {
            vcfAutomationSslThumbprint = getSslThumbprint(vcfAutomationFqdn, trustStorePath);
        }
        if (isBlank(vcfAutomationFqdn)) { // VCF Automation FQDN was not provided and was not found during VCF Ops
            // discovery.
            log.info("VCF Automation FQDN not found. Skipping VCF Automation setup in the SDDC spec.");
            return null;
        }

        VcfAutomationSpec vcfAutomationSpec = new VcfAutomationSpec();
        vcfAutomationSpec.setHostname(vcfAutomationFqdn);
        vcfAutomationSpec.setAdminUserPassword(vcfAutomationAdminPassword);
        vcfAutomationSpec.setSslThumbprint(vcfAutomationSslThumbprint);
        vcfAutomationSpec.setUseExistingDeployment(true);

        return vcfAutomationSpec;
    }

    public static VcfOperationsCollectorSpec createVcfCollectorSpec(String vcfOpsCollectorFqdn) {
        VcfOperationsCollectorSpec vcfCollector = new VcfOperationsCollectorSpec();
        vcfCollector.setHostname(vcfOpsCollectorFqdn);
        vcfCollector.setRootUserPassword(AUTO_GENERATED_PASSWORD);
        vcfCollector.setUseExistingDeployment(false);
        vcfCollector.setApplianceSize(VM_APPLIANCE_SIZE_SMALL);
        return vcfCollector;
    }

    public static VcfOperationsFleetManagementSpec createVcfOperationsFleetManagementSpec(
            String vcfOpsFleetManagementFqdn) {
        // Deploying new VCF Operations Fleet Management
        VcfOperationsFleetManagementSpec vcfOperationsFleetManagementSpec = new VcfOperationsFleetManagementSpec();
        vcfOperationsFleetManagementSpec.setHostname(vcfOpsFleetManagementFqdn);
        vcfOperationsFleetManagementSpec.setRootUserPassword(AUTO_GENERATED_PASSWORD);
        vcfOperationsFleetManagementSpec.setAdminUserPassword(AUTO_GENERATED_PASSWORD);
        vcfOperationsFleetManagementSpec.setUseExistingDeployment(false);
        return vcfOperationsFleetManagementSpec;
    }

    public static VcfOperationsFleetManagementSpec createVcfOperationsFleetManagementSpec(
            String vcfOpsFleetManagementFqdn,
            String vcfOpsFleetManagementAdminPassword,
            String vcfOpsFleetManagementSslThumbprint,
            VcfOperationsManagementNodeInfo vcfOperationsManagementNodeInfo,
            String trustStorePath) {
        // Using existing VCF Operations Fleet Management
        if (isBlank(vcfOpsFleetManagementFqdn)) {
            if (vcfOperationsManagementNodeInfo != null) {
                vcfOpsFleetManagementFqdn = vcfOperationsManagementNodeInfo.getAddresses().stream()
                        .filter(addr -> addr.getType().equalsIgnoreCase(FQDN_NODE_ADDRESS_TYPE))
                        .findFirst()
                        .get()
                        .getValue();
                vcfOpsFleetManagementSslThumbprint =
                        vcfOperationsManagementNodeInfo.getCertificateThumbprints().stream()
                                .findFirst()
                                .orElseThrow();
            }
        } else if (isBlank(vcfOpsFleetManagementSslThumbprint)) {
            vcfOpsFleetManagementSslThumbprint = getSslThumbprint(vcfOpsFleetManagementFqdn, trustStorePath);
        }

        VcfOperationsFleetManagementSpec vcfOperationsFleetManagementSpec = new VcfOperationsFleetManagementSpec();
        vcfOperationsFleetManagementSpec.setHostname(vcfOpsFleetManagementFqdn);
        vcfOperationsFleetManagementSpec.setAdminUserPassword(vcfOpsFleetManagementAdminPassword);
        vcfOperationsFleetManagementSpec.setSslThumbprint(vcfOpsFleetManagementSslThumbprint);
        vcfOperationsFleetManagementSpec.setUseExistingDeployment(true);
        return vcfOperationsFleetManagementSpec;
    }

    public static VcfOperationsSpec createVcfOperationsSpec(String vcfOpsFqdn) {
        // Deploying new VCF Operations
        VcfOperationsNode vcfOperationsNode = new VcfOperationsNode();
        vcfOperationsNode.setHostname(vcfOpsFqdn);
        vcfOperationsNode.setType(VCF_OPS_MASTER_NODE_TYPE);

        VcfOperationsSpec vcfOperationsSpec = new VcfOperationsSpec();
        vcfOperationsSpec.setAdminUserPassword(AUTO_GENERATED_PASSWORD);
        vcfOperationsSpec.setUseExistingDeployment(false);
        vcfOperationsSpec.setApplianceSize(VM_APPLIANCE_SIZE_SMALL);
        vcfOperationsSpec.setNodes(Collections.singletonList(vcfOperationsNode));
        return vcfOperationsSpec;
    }

    public static VcfOperationsSpec createVcfOperationsSpec(
            String vcfOpsFqdn,
            String vcfOpsAdminPassword,
            String vcfOpsSslThumbprint,
            List<VcfOperationsNodeInfo> vcfOperationsNodeInfos) {
        // Using existing VCF Operations Fleet Management
        List<VcfOperationsNode> vcfOperationsNodes = new ArrayList<>();
        if (vcfOperationsNodeInfos != null && !vcfOperationsNodeInfos.isEmpty()) {
            for (VcfOperationsNodeInfo nodeInfo : vcfOperationsNodeInfos) {
                VcfOperationsNode node = new VcfOperationsNode();
                node.setHostname(nodeInfo.getAddress());
                node.setType(nodeInfo.getType());
                if (vcfOpsFqdn.equals(nodeInfo.getAddress())) {
                    node.setSslThumbprint(vcfOpsSslThumbprint);
                }
                vcfOperationsNodes.add(node);
            }
        }

        VcfOperationsSpec vcfOperationsSpec = new VcfOperationsSpec();
        vcfOperationsSpec.setAdminUserPassword(vcfOpsAdminPassword);
        vcfOperationsSpec.setUseExistingDeployment(true);
        vcfOperationsSpec.setApplianceSize(VM_APPLIANCE_SIZE_SMALL);
        vcfOperationsSpec.setNodes(vcfOperationsNodes);
        return vcfOperationsSpec;
    }

    public static String getSslThumbprint(String fqdn, String trustStorePath) {
        Certificate[] certificate = getCertificate(fqdn, trustStorePath);

        // Extract thumbprint from cert
        String thumbprint;
        try {
            thumbprint = DigestUtils.sha256Hex(certificate[0].getEncoded()).toUpperCase();
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }

        // Format hash
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < thumbprint.length(); ++i) {
            buffer.append(thumbprint.charAt(i));
            if ((i + 1) % 2 == 0 && i + 1 < thumbprint.length()) {
                buffer.append(":");
            }
        }
        return buffer.toString();
    }

    public static Certificate[] getCertificate(String hostAddress, String trustStorePath) {
        SSLSocketFactory sslSocketFactory = SecurityHelper.createSocketFactory(trustStorePath);

        try (SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(hostAddress, 443)) {
            return sslSocket.getSession().getPeerCertificates();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> parseHostFqdns(String hostInput, String dnsDomain, String trustStorePath) {
        Map<String, String> hostFqdnToThumbprintMap = new HashMap<>();
        String[] hosts = hostInput.split(",");
        for (String host : hosts) {
            String[] hostnameSslThumbprintPair = parseHostnameSslThumbprintPair(host, dnsDomain, trustStorePath);
            hostFqdnToThumbprintMap.put(hostnameSslThumbprintPair[0], hostnameSslThumbprintPair[1]);
        }
        return hostFqdnToThumbprintMap;
    }

    public static String[] parseHostnameSslThumbprintPair(
            String hostnameSslPair, String dnsDomain, String trustStorePath) {
        String[] hostnameThumbprintPair = hostnameSslPair.split("=");
        String hostFqdn = hostnameToFqdn(hostnameThumbprintPair[0], dnsDomain);
        String hostThumbprint;
        if (hostnameThumbprintPair.length < 2) {
            hostThumbprint = getSslThumbprint(hostFqdn, trustStorePath);
        } else {
            hostThumbprint = hostnameThumbprintPair[1].trim();
        }
        return new String[] {hostFqdn, hostThumbprint};
    }

    public static String hostnameToFqdn(String hostname, String dnsDomain) {
        if (!isBlank(hostname)) {
            hostname = hostname.trim();
            if (!hostname.contains(".")) {
                return hostname + "." + dnsDomain.trim();
            }
            return hostname;
        }
        return null;
    }

    public static String sddcSpecToJson(SddcSpec sddcSpec) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(sddcSpec);
    }

    public static void saveSddcSpecToFile(ApiClient client, String sddcTaskId, String deploymentSpecSaveFilePath)
            throws Exception {
        if (!isBlank(deploymentSpecSaveFilePath)) {
            File saveFile = new File(deploymentSpecSaveFilePath);
            String saveFileAbsolutePath = saveFile.getAbsolutePath();

            if (saveFile.isDirectory()) {
                saveFileAbsolutePath = Paths.get(
                                deploymentSpecSaveFilePath, String.format("sddc_spec-%s.json", sddcTaskId))
                        .toString();
                saveFile = new File(String.format(saveFileAbsolutePath));
            }

            Spec spec = client.createStub(Spec.class);
            SddcSpec deploymentSpec = spec.getSddcSpecByID(sddcTaskId).invoke().get();

            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(saveFile, deploymentSpec);
            log.info(
                    "Sddc Spec, provided by {} Instance deployment task '{}', was successfully saved in '{}'.",
                    deploymentSpec.getWorkflowType(),
                    sddcTaskId,
                    saveFileAbsolutePath);
        }
    }

    public static class NetworkInput {
        public String gateway;
        public String subnet;
        public long vlanId;
        public String ipRangeStart;
        public String ipRangeEnd;

        public NetworkInput(String gateway, String subnet, long vlanId, String ipRangeStart, String ipRangeEnd) {
            this.gateway = gateway;
            this.subnet = subnet;
            this.vlanId = vlanId;
            this.ipRangeStart = ipRangeStart;
            this.ipRangeEnd = ipRangeEnd;
        }
    }
}
