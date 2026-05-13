/*
 * ******************************************************************
 * Copyright (c) 2025-2026 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcf.installer.helpers;

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
import com.vmware.sdk.vcf.installer.model.DvsSpec;
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
import com.vmware.sdk.vcf.installer.model.VcfOperationsNode;
import com.vmware.sdk.vcf.installer.model.VcfOperationsNodeInfo;
import com.vmware.sdk.vcf.installer.model.VcfOperationsSpec;
import com.vmware.sdk.vcf.installer.model.VsanEsaConfig;
import com.vmware.sdk.vcf.installer.model.VsanSpec;
import com.vmware.sdk.vcf.installer.model.FleetDepotServiceSpec;
import com.vmware.sdk.vcf.installer.model.VidbSpec;
import com.vmware.sdk.vcf.installer.model.SddcVspClusterSpec;
import com.vmware.sdk.vcf.installer.model.TelemetryAcceptorSpec;
import com.vmware.sdk.vcf.installer.model.SaltSpec;
import com.vmware.sdk.vcf.installer.model.FleetLcmServiceSpec;
import com.vmware.sdk.vcf.installer.model.LicenseServerSpec;
import com.vmware.sdk.vcf.installer.model.IpRangeV6;
import com.vmware.sdk.vcf.installer.model.IPv6Pool;
import com.vmware.sdk.vcf.installer.model.IPv4Pool;
import com.vmware.sdk.vcf.installer.model.IpRange;
import com.vmware.sdk.vcf.installer.model.SddcLcmServiceSpec;
import com.vmware.sdk.vcf.installer.v1.sddcs.NetworkConfigProfiles;
import com.vmware.sdk.vcf.installer.v1.sddcs.Spec;
import com.vmware.sdk.vcf.installer.v1.sddcs.VcfopsDiscovery;
import com.vmware.vapi.client.ApiClient;

/** Util class used for crafting default SDDC spec. */
public class SddcSpecHelper {
    private static final Logger log = LoggerFactory.getLogger(SddcSpecHelper.class);

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

    public static final String VCFA_NODE_PREFIX = "vcfa";
    public static final String VCFA_INTERNAL_CLUSTER_CIDR = "198.18.0.0/15";

    public static final String VCF_OPS_MASTER_NODE_TYPE = "master";
    public static final String VCF_OPS_REPLICA_NODE_TYPE = "replica";
    public static final String VCF_OPS_DATA_NODE_TYPE = "data";

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

    public static SddcNsxtSpec createSddcNsxtSpecHA(
            String nsxtHostname1,
            String nsxtHostname2,
            String nsxtHostname3,
            String nsxtVipHostname,
            NetworkInput nsxtNetwork) {
        // Deploy New NSX-T HA

        IpAddressPoolRangeSpec ipRange = new IpAddressPoolRangeSpec();
        ipRange.setStart(nsxtNetwork.ipRangeStart);
        ipRange.setEnd(nsxtNetwork.ipRangeEnd);

        IpAddressPoolSubnetSpec ipAddressPoolSubnetSpec = new IpAddressPoolSubnetSpec.Builder()
                .setGateway(nsxtNetwork.gateway)
                .setCidr(nsxtNetwork.subnet)
                .setIpAddressPoolRanges(Collections.singletonList(ipRange))
                .build();

        NsxtManagerSpec nsxtManagerSpec1 = new NsxtManagerSpec();
        nsxtManagerSpec1.setHostname(nsxtHostname1);

        NsxtManagerSpec nsxtManagerSpec2 = new NsxtManagerSpec();
        nsxtManagerSpec2.setHostname(nsxtHostname2);

        NsxtManagerSpec nsxtManagerSpec3 = new NsxtManagerSpec();
        nsxtManagerSpec3.setHostname(nsxtHostname3);

        List<NsxtManagerSpec> nsxtManagers = List.of(nsxtManagerSpec1, nsxtManagerSpec2, nsxtManagerSpec3);

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
                .setNsxtManagers(nsxtManagers)
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
        vcfAutomationSpec.setNodePrefix(VCFA_NODE_PREFIX);
        vcfAutomationSpec.setAdminUserPassword(AUTO_GENERATED_PASSWORD);
        vcfAutomationSpec.setInternalClusterCidr(VCFA_INTERNAL_CLUSTER_CIDR);
        vcfAutomationSpec.setIpPool(Arrays.asList(vcfAutomationIpPoolStart, vcfAutomationIpPoolEnd));
        vcfAutomationSpec.setUseExistingDeployment(false);

        return vcfAutomationSpec;
    }


    public static VcfAutomationSpec createSddcVcfAutomationSpecOnVsp(
            String vcfAutomationFqdn, String[] vcfAutomationIpv4Addresses,
	    String vcfAutomationPlatformFqdn, Boolean vcfAutomationUseExisting) {

        // If no arguments are provided VCF Automation deployment will be skipped.
        // Users will be required to set up VCF Automation manually later.
        // If some but not all arguments are provided an exception is thrown.
        if (isBlank(vcfAutomationFqdn) && isBlank(vcfAutomationPlatformFqdn) &&
	    (vcfAutomationIpv4Addresses == null || vcfAutomationIpv4Addresses.length == 0)) {
            log.info("VCF Automation FQDN not found. Skipping creation of VCF Automation deployment.");
            return null;
        }
        if (isBlank(vcfAutomationFqdn) || isBlank(vcfAutomationPlatformFqdn) ||
	    vcfAutomationIpv4Addresses == null || vcfAutomationIpv4Addresses.length == 0) {
           throw new RuntimeException(
                    "Invalid arguments provided for VCF Automation deployment. Either all arguments "
                            + "(fqdn, ip range start and ip range end) need to be provided or all should be null to skip VCF Automation deployment.");
        }

        VcfAutomationSpec vcfAutomationSpec = new VcfAutomationSpec();
        vcfAutomationSpec.setHostname(vcfAutomationFqdn);
        vcfAutomationSpec.setNodePrefix(VCFA_NODE_PREFIX);
        vcfAutomationSpec.setAdminUserPassword(AUTO_GENERATED_PASSWORD);
        vcfAutomationSpec.setInternalClusterCidr(VCFA_INTERNAL_CLUSTER_CIDR);
        vcfAutomationSpec.setIpPool(Arrays.asList(vcfAutomationIpv4Addresses));
        vcfAutomationSpec.setPlatformFqdn(vcfAutomationPlatformFqdn);

        Boolean reuse = Boolean.valueOf(vcfAutomationUseExisting);
        vcfAutomationSpec.setUseExistingDeployment(reuse);

        return vcfAutomationSpec;
    }

    public static List<DvsSpec> updateMtu(List<DvsSpec> specs, Long mtu) {
       // If mtu is not passed just return the specs passed as argument.
       if (mtu != null) {
	  for (DvsSpec spec : specs) {
             spec.setMtu(mtu);
	  }
          return specs;
       }
       return specs;
    }

    public static VcfAutomationSpec createSddcVcfAutomationSpecHA(
            String vcfAutomationFqdn,
            String vcfAutomationPlatformFqdn,
            String vcfAutomationIpPool1,
            String vcfAutomationIpPool2,
            String vcfAutomationIpPool3,
            String vcfAutomationIpPool4) {
        // Deploying new VCF Automation

        // If no arguments are provided VCF Automation deployment will be skipped.
        // Users will be required to set up VCF Automation manually at a later date.
        // If not all arguments are provided an exception is thrown.
        if (isBlank(vcfAutomationFqdn)
                && isBlank(vcfAutomationPlatformFqdn)
                && isBlank(vcfAutomationIpPool1)
                && isBlank(vcfAutomationIpPool2)
                && isBlank(vcfAutomationIpPool3)
                && isBlank(vcfAutomationIpPool4)) {
            // None the required arguments for VCF Automation deployment are provided. Skipping VCF Automation
            // deployment.
            log.info("VCF Automation FQDN not found. Skipping creation of VCF Automation deployment.");
            return null;
        }
        if (isBlank(vcfAutomationFqdn)
                || isBlank(vcfAutomationPlatformFqdn)
                || isBlank(vcfAutomationIpPool1)
                || isBlank(vcfAutomationIpPool2)
                || isBlank(vcfAutomationIpPool3)
                || isBlank(vcfAutomationIpPool4)) {
            // Some of the required arguments for VCF Automation are missing. Throwing an exception, because either
            // all arguments are expected or none of them.
            throw new RuntimeException(
                    "Invalid arguments provided for VCF Automation deployment. Either all arguments "
                            + "(fqdn and ip addresses for all 4 nodes) need to be provided or all should be null to skip VCF Automation deployment.");
        }

        VcfAutomationSpec vcfAutomationSpec = new VcfAutomationSpec();
        vcfAutomationSpec.setHostname(vcfAutomationFqdn);
        vcfAutomationSpec.setPlatformFqdn(vcfAutomationPlatformFqdn);
        vcfAutomationSpec.setAdminUserPassword(AUTO_GENERATED_PASSWORD);
        vcfAutomationSpec.setInternalClusterCidr(VCFA_INTERNAL_CLUSTER_CIDR);
        vcfAutomationSpec.setIpPool(
                Arrays.asList(vcfAutomationIpPool1, vcfAutomationIpPool2, vcfAutomationIpPool3, vcfAutomationIpPool4));
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

    public static VcfOperationsSpec createVcfOperationsSpecHA(
            String vcfOpsFqdn1, String vcfOpsFqdn2, String vcfOpsFqdn3, String loadBalancerFqdn) {
        // Deploying new VCF Operations
        VcfOperationsNode vcfOperationsNode1 = new VcfOperationsNode();
        vcfOperationsNode1.setHostname(vcfOpsFqdn1);
        vcfOperationsNode1.setType(VCF_OPS_MASTER_NODE_TYPE);

        VcfOperationsNode vcfOperationsNode2 = new VcfOperationsNode();
        vcfOperationsNode2.setHostname(vcfOpsFqdn2);
        vcfOperationsNode2.setType(VCF_OPS_REPLICA_NODE_TYPE);

        VcfOperationsNode vcfOperationsNode3 = new VcfOperationsNode();
        vcfOperationsNode3.setHostname(vcfOpsFqdn3);
        vcfOperationsNode3.setType(VCF_OPS_DATA_NODE_TYPE);

        VcfOperationsSpec vcfOperationsSpec = new VcfOperationsSpec();
        vcfOperationsSpec.setAdminUserPassword(AUTO_GENERATED_PASSWORD);
        vcfOperationsSpec.setUseExistingDeployment(false);
        vcfOperationsSpec.setApplianceSize(VM_APPLIANCE_SIZE_SMALL);
        vcfOperationsSpec.setNodes(Arrays.asList(vcfOperationsNode1, vcfOperationsNode2, vcfOperationsNode3));
        vcfOperationsSpec.setLoadBalancerFqdn(loadBalancerFqdn);
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

    public static VidbSpec createVidbSpec(String hostname, String version, String size) {
        VidbSpec vidbSpec = new VidbSpec();

        vidbSpec.setHostname(hostname);
        vidbSpec.setVersion(version);
        vidbSpec.setSize(size);

        return vidbSpec;
    }

    public static FleetDepotServiceSpec createFleetDepotSpec(String version, String size) {
        FleetDepotServiceSpec fdsSpec = new FleetDepotServiceSpec();

        fdsSpec.setVersion(version);
        fdsSpec.setSize(size);

        return fdsSpec;
    }

    public static TelemetryAcceptorSpec createTelemetryAcceptorSpec(String version, String size) {
        TelemetryAcceptorSpec telemetryAcceptorSpec = new TelemetryAcceptorSpec();

        telemetryAcceptorSpec.setSize(size);
        telemetryAcceptorSpec.setVersion(version);

        return telemetryAcceptorSpec;
    }

    public static SaltSpec createSaltSpec(String version, String size) {
        SaltSpec saltSpec = new SaltSpec();

        saltSpec.setSize(size);
        saltSpec.setVersion(version);

        return saltSpec;
    }

    public static FleetLcmServiceSpec createFleetLcmSpec(String version, String size) {
        FleetLcmServiceSpec fleetLcmServiceSpec = new FleetLcmServiceSpec();

        fleetLcmServiceSpec.setVersion(version);
        fleetLcmServiceSpec.setSize(size);

        return fleetLcmServiceSpec;
    }

    public static SddcLcmServiceSpec createSddcLcmSpec(String version, String size) {
        SddcLcmServiceSpec sddcLcmServiceSpec = new SddcLcmServiceSpec();

        sddcLcmServiceSpec.setVersion(version);
        sddcLcmServiceSpec.setSize(size);

        return sddcLcmServiceSpec;
    }

    public static IPv4Pool createVspIPv4Pool(String ipv4Cidr,
                                             String ipv4StartIpAddress,
                                             String ipv4EndIpAddress,
                                             String[] ipv4Addresses,
                                             String[] ipv4ExcludedAddresses) {

	if (isBlank(ipv4Cidr) &&
            (isBlank(ipv4StartIpAddress) || isBlank(ipv4EndIpAddress)) &&
	    (ipv4Addresses == null || ipv4Addresses.length == 0)) {

	   throw new RuntimeException("Please provide ipv4 settings.");
	}

        IPv4Pool vspIPv4Pool = new IPv4Pool();

	if (!isBlank(ipv4Cidr)) {
           vspIPv4Pool.setCidr(ipv4Cidr);
        }

	if (!isBlank(ipv4StartIpAddress) && !isBlank(ipv4EndIpAddress)) {
           IpRange ipRange = new IpRange();
           ipRange.setStartIpAddress(ipv4StartIpAddress);
           ipRange.setEndIpAddress(ipv4EndIpAddress);
           vspIPv4Pool.setIpRange(ipRange);
	}

	if (ipv4Addresses != null && ipv4Addresses.length > 0) {
           vspIPv4Pool.setAddresses(Arrays.asList(ipv4Addresses));
	}

	if (ipv4ExcludedAddresses != null && ipv4ExcludedAddresses.length > 0) {
	   vspIPv4Pool.setExcludedAddresses(Arrays.asList(ipv4ExcludedAddresses));
	}

	return vspIPv4Pool;
    }

    public static IPv6Pool createVspIPv6Pool(String ipv6Cidr,
                                             String ipv6StartIpAddress,
                                             String ipv6EndIpAddress,
                                             String[] ipv6Addresses,
                                             String[] ipv6ExcludedAddresses) {

	if (isBlank(ipv6Cidr) &&
            (isBlank(ipv6StartIpAddress) || isBlank(ipv6EndIpAddress)) &&
	    (ipv6Addresses == null || ipv6Addresses.length == 0)) {

	   return null;
	}

        IPv6Pool vspIPv6Pool = new IPv6Pool();

	if (!isBlank(ipv6Cidr)) {
           vspIPv6Pool.setCidr(ipv6Cidr);
        }

	if (!isBlank(ipv6StartIpAddress) && !isBlank(ipv6EndIpAddress)) {
           IpRangeV6 ipRange = new IpRangeV6();
           ipRange.setStartIpAddress(ipv6StartIpAddress);
           ipRange.setEndIpAddress(ipv6EndIpAddress);
           vspIPv6Pool.setIpRange(ipRange);
        }

	if (ipv6Addresses != null && ipv6Addresses.length > 0) {
           vspIPv6Pool.setAddresses(Arrays.asList(ipv6Addresses));
	}

	if (ipv6ExcludedAddresses != null && ipv6ExcludedAddresses.length > 0) {
           vspIPv6Pool.setExcludedAddresses(Arrays.asList(ipv6ExcludedAddresses));
        }

        return vspIPv6Pool;
    }

    public static SddcVspClusterSpec createVspClusterSpec(String platformFqdn,
                                                          String systemUserPassword,
                                                          IPv4Pool ipv4Pool,
                                                          IPv6Pool ipv6Pool,
                                                          String size,
                                                          String vspInternalClusterCidrIpv4,
                                                          String vspInternalClusterCidrIpv6,
                                                          String vspInstanceFqdn,
                                                          String vspFleetFqdn,
                                                          String vspVersion) {

        SddcVspClusterSpec vspClusterSpec = new SddcVspClusterSpec();

        vspClusterSpec.setPlatformFqdn(platformFqdn);
        vspClusterSpec.setSystemUserPassword(systemUserPassword);
        vspClusterSpec.setIpv4Pool(ipv4Pool);
        vspClusterSpec.setIpv6Pool(ipv6Pool);
        vspClusterSpec.setSize(size);

        vspClusterSpec.setInternalClusterCidrIpv4(vspInternalClusterCidrIpv4);
        vspClusterSpec.setInternalClusterCidrIpv6(vspInternalClusterCidrIpv6);
        vspClusterSpec.setInstanceFqdn(vspInstanceFqdn);
        vspClusterSpec.setFleetFqdn(vspFleetFqdn);
        vspClusterSpec.setVersion(vspVersion);

        return vspClusterSpec;
    }


    public static SddcVspClusterSpec createVspClusterSpec(String platformFqdn,
                                                          String systemUserPassword,
                                                          IPv4Pool ipv4Pool,
                                                          IPv6Pool ipv6Pool,
                                                          String size,
                                                          String vspInternalClusterCidrIpv4,
                                                          String vspInternalClusterCidrIpv6,
                                                          String vspInstanceFqdn,
                                                          String vspFleetFqdn,
                                                          String vspVersion,
                                                          String sslThumbprint,
                                                          Boolean useExistingDeployment) {

        SddcVspClusterSpec vspClusterSpec = createVspClusterSpec(platformFqdn,
                                                                 systemUserPassword,
                                                                 ipv4Pool,
                                                                 ipv6Pool,
                                                                 size,
                                                                 vspInternalClusterCidrIpv4,
                                                                 vspInternalClusterCidrIpv6,
                                                                 vspInstanceFqdn,
                                                                 vspFleetFqdn,
                                                                 vspVersion);

        vspClusterSpec.setSslThumbprint(sslThumbprint);
        vspClusterSpec.setUseExistingDeployment(useExistingDeployment);

        return vspClusterSpec;
    }

    public static LicenseServerSpec createLicenseServerSpec(String hostname,
                                                            String version,
                                                            Boolean useExistingDeployment,
                                                            String sslThumbprint) {
        LicenseServerSpec licenseServerSpec = new LicenseServerSpec();

        licenseServerSpec.setHostname(hostname);
        licenseServerSpec.setVersion(version);
        licenseServerSpec.setUseExistingDeployment(useExistingDeployment);
        licenseServerSpec.setSslThumbprint(sslThumbprint);

        return licenseServerSpec;
    }
}
