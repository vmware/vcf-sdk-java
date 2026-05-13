/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vsan.management;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.CLUSTER_COMPUTE_RESOURCE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vmware.pbm.InvalidArgumentFaultMsg;
import com.vmware.pbm.PbmCapabilityProfile;
import com.vmware.pbm.PbmPortType;
import com.vmware.pbm.PbmProfile;
import com.vmware.pbm.PbmProfileId;
import com.vmware.pbm.PbmProfileResourceType;
import com.vmware.pbm.PbmProfileResourceTypeEnum;
import com.vmware.pbm.PbmServiceInstanceContent;
import com.vmware.pbm.RuntimeFaultFaultMsg;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.vsan.management.VsanManagedObjectsCatalog;
import com.vmware.sdk.vsphere.utils.vsan.management.VsanUtil;
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimVsanReconfigSpec;
import com.vmware.vim25.VirtualMachineDefinedProfileSpec;
import com.vmware.vsan.sdk.FileShareQueryResult;
import com.vmware.vsan.sdk.VsanFileServiceConfig;
import com.vmware.vsan.sdk.VsanFileServiceDomain;
import com.vmware.vsan.sdk.VsanFileServiceDomainConfig;
import com.vmware.vsan.sdk.VsanFileServiceDomainQuerySpec;
import com.vmware.vsan.sdk.VsanFileServiceIpConfig;
import com.vmware.vsan.sdk.VsanFileShare;
import com.vmware.vsan.sdk.VsanFileShareAccessType;
import com.vmware.vsan.sdk.VsanFileShareConfig;
import com.vmware.vsan.sdk.VsanFileShareNetPermission;
import com.vmware.vsan.sdk.VsanFileShareQuerySpec;
import com.vmware.vsan.sdk.VsanhealthPortType;

/**
 * This sample demonstrates the vCenter side vSAN file service API accessing. To provide an example of vSAN file service
 * API access, it shows how to download file service OVF, enable file service, create domain, create a file share,
 * remove a file share, remove domain, together with disable file service.
 *
 * <p>Sample Prerequisites: this sample only requires a vCenter server with a vSAN cluster.
 */
public class VsanFsSample {

    private static final Logger log = LoggerFactory.getLogger(VsanFsSample.class);

    // users can customize the parameters according to your own environment
    private static final String DOMAIN_NAME = "VSANFS-PA.PRV";
    private static final String SUBNET_MASK = "255.255.255.0";
    private static final String GATEWAY_ADDRESS = "192.168.111.1";
    private static final List<String> DNS_SUFFIXES = List.of("example.com");
    private static final List<String> DNS_ADDRESS = List.of("1.2.3.4");
    private static final Map<String, String> IP_FQDN_DIC = Map.of(
            "192.168.111.2", "h192-168-111-2.example.com",
            "192.168.111.3", "h192-168-111-3.example.com",
            "192.168.111.4", "h192-168-111-4.example.com",
            "192.168.111.5", "h192-168-111-5.example.com");

    /** REQUIRED: vCenter FQDN or IP address. */
    public static String serverAddress = "vcenter1.mycompany.com";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";
    /** REQUIRED: VC cluster name using in cluster health query API. */
    public static String clusterName;
    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (do not leave it empty on production environments).
     */
    public static String trustStorePath = null;

    private static PropertyCollectorHelper propertyCollectorHelper;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VsanFsSample.class, args);

        VcenterClientFactory clientFactory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = clientFactory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            var aboutInfo = serviceContent.getAbout();
            if (!aboutInfo.getApiType().equals("VirtualCenter")) {
                log.info("The vSAN file service APIs are only available on vCenter.");
                return;
            }

            ManagedObjectReference clusterMoRef =
                    propertyCollectorHelper.getMoRefByName(clusterName, CLUSTER_COMPUTE_RESOURCE);
            if (clusterMoRef == null) {
                log.error("Cluster {} is not found for {}", clusterName, serverAddress);
                return;
            }

            VsanhealthPortType vsanPort = client.getVsanPort();

            // Find OVF download url
            log.info("Finding OVF download url ...");
            String ovfUrl =
                    vsanPort.vsanFindOvfDownloadUrl(VsanManagedObjectsCatalog.getVsanFileServiceSystem(), clusterMoRef);
            if (ovfUrl == null) {
                log.error("Failed to find the OVF download url.");
                return;
            }
            log.info("Found OVF download url: {}", ovfUrl);

            // Download FSVM OVF files to vCenter
            log.info("Downloading ovf files from {} to vCenter ...", ovfUrl);
            try {
                ManagedObjectReference taskMo = vsanPort.vsanDownloadFileServiceOvf(
                        VsanManagedObjectsCatalog.getVsanFileServiceSystem(), ovfUrl);
                boolean state = VsanUtil.waitForTasks(propertyCollectorHelper, taskMo);
                if (!state) {
                    log.error("Failed to download ovf files");
                    return;
                }
            } catch (Exception e) {
                log.error("Failed to download ovf files with error: ", e);
                return;
            }
            log.info("Downloaded ovf files to vCenter successfully");

            // Enable file service
            log.info("Enabling the file service%n");
            VsanFileServiceConfig fileServiceConfig = getVsanFileServiceConfig(clusterMoRef);
            VimVsanReconfigSpec vimVsanReconfigSpec = new VimVsanReconfigSpec();
            vimVsanReconfigSpec.setFileServiceConfig(fileServiceConfig);
            vimVsanReconfigSpec.setModify(false);
            // call vsanClusterReconfig api
            try {
                ManagedObjectReference taskMo = vsanPort.vsanClusterReconfig(
                        VsanManagedObjectsCatalog.getVsanVcClusterConfigServiceInstanceReference(),
                        clusterMoRef,
                        vimVsanReconfigSpec);
                boolean state = VsanUtil.waitForTasks(propertyCollectorHelper, taskMo);
                if (!state) {
                    log.error("Failed to reconfig vsan cluster");
                    return;
                }
            } catch (Exception e) {
                log.error("Failed to enable file service with error:", e);
                return;
            }
            log.info("Enabled file service successfully");

            // Create file service domain
            VsanFileServiceDomainConfig fsDomainConfig = getFileServiceDomainConfig();
            String domainName = fsDomainConfig.getName();
            log.info("Creating file service domain%n");
            try {
                ManagedObjectReference taskMo = vsanPort.vsanClusterCreateFsDomain(
                        VsanManagedObjectsCatalog.getVsanFileServiceSystem(), fsDomainConfig, clusterMoRef);
                boolean state = VsanUtil.waitForTasks(propertyCollectorHelper, taskMo);
                if (!state) {
                    log.error("vsan cluster create file service domain failed");
                    return;
                }
            } catch (Exception e) {
                log.error("Failed to create file service domain with error:", e);
                return;
            }
            log.info("Created file service domain {} successfully", domainName);

            // Create a file share
            PbmPortType pbmPort = client.getPbmPort();
            var pbmServiceInstanceContent = client.getPbmServiceInstanceContent();
            VsanFileShareConfig fileShareConfig = getFileShareConfig(pbmPort, pbmServiceInstanceContent, domainName);
            if (fileShareConfig == null) {
                log.error("Failed to get file share config");
                return;
            }

            String fileShareName = fileShareConfig.getName();
            log.info("Creating a file share: {}", fileShareName);
            try {
                ManagedObjectReference taskMo = vsanPort.vsanCreateFileShare(
                        VsanManagedObjectsCatalog.getVsanFileServiceSystem(), fileShareConfig, clusterMoRef);
                boolean state = VsanUtil.waitForTasks(propertyCollectorHelper, taskMo);
                if (!state) {
                    log.error("vsan create file share failed");
                    return;
                }
            } catch (Exception e) {
                log.error("Failed to create a file share with error:", e);
                return;
            }
            log.info("Created file share {} successfully", fileShareName);

            // Remove a file share
            log.info("Removing file share: {}", fileShareName);

            var fileShareQuerySpec = new VsanFileShareQuerySpec();
            fileShareQuerySpec.setDomainName(domainName);
            fileShareQuerySpec.getNames().add(fileShareName);

            FileShareQueryResult queryResult = vsanPort.vsanClusterQueryFileShares(
                    VsanManagedObjectsCatalog.getVsanFileServiceSystem(), fileShareQuerySpec, clusterMoRef);
            List<VsanFileShare> fileShares = queryResult.getFileShares();
            try {
                ManagedObjectReference taskMo = vsanPort.vsanClusterRemoveShare(
                        VsanManagedObjectsCatalog.getVsanFileServiceSystem(),
                        fileShares.get(0).getUuid(),
                        clusterMoRef,
                        null);
                boolean state = VsanUtil.waitForTasks(propertyCollectorHelper, taskMo);
                if (!state) {
                    log.error("vsan cluster remove share failed");
                    return;
                }
            } catch (Exception e) {
                log.error("Failed to remove a file share with error: ", e);
                return;
            }
            log.info("Removed file share {} successfully", fileShareName);

            // Remove file service domain
            List<VsanFileServiceDomain> fsDomains = vsanPort.vsanClusterQueryFsDomains(
                    VsanManagedObjectsCatalog.getVsanFileServiceSystem(),
                    new VsanFileServiceDomainQuerySpec(),
                    clusterMoRef);
            log.info(
                    "Removing file service domain: {}",
                    fsDomains.get(0).getConfig().getName());
            try {
                ManagedObjectReference taskMo = vsanPort.vsanClusterRemoveFsDomain(
                        VsanManagedObjectsCatalog.getVsanFileServiceSystem(),
                        fsDomains.get(0).getUuid(),
                        clusterMoRef);
                boolean state = VsanUtil.waitForTasks(propertyCollectorHelper, taskMo);
                if (!state) {
                    log.error("vsan cluster remove file service domain failed");
                    return;
                }
            } catch (Exception e) {
                log.error("Failed to remove file service domain with error: ", e);
                return;
            }
            log.info("Removed file service domain {} successfully", fileShareName);

            // Disable file service
            log.info("Disabling file service");
            var disableFileServiceConfig = new VsanFileServiceConfig();
            disableFileServiceConfig.setEnabled(false);
            VimVsanReconfigSpec disableReconfigSpec = new VimVsanReconfigSpec();
            disableReconfigSpec.setFileServiceConfig(disableFileServiceConfig);
            disableReconfigSpec.setModify(false);

            // Call vsanClusterReconfig api
            try {
                ManagedObjectReference taskMo = vsanPort.vsanClusterReconfig(
                        VsanManagedObjectsCatalog.getVsanVcClusterConfigServiceInstanceReference(),
                        clusterMoRef,
                        disableReconfigSpec);
                boolean state = VsanUtil.waitForTasks(propertyCollectorHelper, taskMo);
                if (!state) {
                    log.error("vsan cluster reconfig failed");
                    return;
                }
            } catch (Exception e) {
                log.error("Failed to disable file service with error:", e);
                return;
            }
            log.info("Disabled file service successfully");
        }
    }

    private static VsanFileServiceConfig getVsanFileServiceConfig(ManagedObjectReference clusterMoRef) {
        List<ManagedObjectReference> hosts;
        try {
            hosts = ((ArrayOfManagedObjectReference) propertyCollectorHelper.fetch(clusterMoRef, "host"))
                    .getManagedObjectReference();
        } catch (InvalidPropertyFaultMsg | com.vmware.vim25.RuntimeFaultFaultMsg e) {
            throw new RuntimeException(e);
        }

        ManagedObjectReference firstHost = hosts.get(0);
        ManagedObjectReference network;
        try {
            network = ((ArrayOfManagedObjectReference) propertyCollectorHelper.fetch(firstHost, "network"))
                    .getManagedObjectReference()
                    .get(0);
        } catch (InvalidPropertyFaultMsg | com.vmware.vim25.RuntimeFaultFaultMsg e) {
            throw new RuntimeException(e);
        }
        var fileServiceConfig = new VsanFileServiceConfig();
        fileServiceConfig.setEnabled(true);
        fileServiceConfig.setNetwork(network);
        fileServiceConfig.getDomains().clear();
        return fileServiceConfig;
    }

    private static VsanFileServiceDomainConfig getFileServiceDomainConfig() {
        List<VsanFileServiceIpConfig> networkProfiles = new ArrayList<>();
        for (Map.Entry<String, String> entry : IP_FQDN_DIC.entrySet()) {
            var networkProfile = new VsanFileServiceIpConfig();
            networkProfile.setDhcp(false);
            networkProfile.setIpAddress(entry.getKey());
            networkProfile.setSubnetMask(SUBNET_MASK);
            networkProfile.setGateway(GATEWAY_ADDRESS);
            networkProfile.setFqdn(entry.getValue());
            networkProfiles.add(networkProfile);
        }
        networkProfiles.get(0).setIsPrimary(true);

        var fileServiceDomainConfig = new VsanFileServiceDomainConfig();
        fileServiceDomainConfig.setName(DOMAIN_NAME);
        fileServiceDomainConfig.getDnsServerAddresses().addAll(DNS_ADDRESS);
        fileServiceDomainConfig.getDnsSuffixes().addAll(DNS_SUFFIXES);
        fileServiceDomainConfig.getFileServerIpConfig().addAll(networkProfiles);
        return fileServiceDomainConfig;
    }

    private static VirtualMachineDefinedProfileSpec getVsanStoragePolicy(
            PbmPortType pbmPort, PbmServiceInstanceContent pbmServiceInstanceContent) {
        var resourceType = new PbmProfileResourceType();
        resourceType.setResourceType(PbmProfileResourceTypeEnum.STORAGE.value());

        // Query for profile ids
        List<PbmProfileId> profileIds;
        try {
            profileIds = pbmPort.pbmQueryProfile(pbmServiceInstanceContent.getProfileManager(), resourceType, null);
        } catch (RuntimeFaultFaultMsg | InvalidArgumentFaultMsg e) {
            throw new RuntimeException(e);
        }

        // Query for profile contents
        List<PbmProfile> profiles;
        try {
            profiles = pbmPort.pbmRetrieveContent(pbmServiceInstanceContent.getProfileManager(), profileIds);
        } catch (RuntimeFaultFaultMsg | InvalidArgumentFaultMsg e) {
            throw new RuntimeException(e);
        }

        for (var profile : profiles) {
            // vSAN default storage profile possesses a unique profile ID of
            // 'aa6d5a82-1c88-45da-85d3-3d74b91a5bad' across different releases.
            String profileId = profile.getProfileId().getUniqueId();
            if (profile instanceof PbmCapabilityProfile && "aa6d5a82-1c88-45da-85d3-3d74b91a5bad".equals((profileId))) {
                var spec = new VirtualMachineDefinedProfileSpec();
                spec.setProfileId(profileId);
                return spec;
            }
        }

        return null;
    }

    private static VsanFileShareConfig getFileShareConfig(
            PbmPortType pbmPort, PbmServiceInstanceContent pbmServiceInstanceContent, String domainName) {
        String shareName = "TestShare-1";
        String shareQuota = "10G";
        VirtualMachineDefinedProfileSpec vsanStoragePolicy = getVsanStoragePolicy(pbmPort, pbmServiceInstanceContent);
        if (vsanStoragePolicy == null) {
            System.err.println("Cannot find the vSAN Storage Policy from the Virtual Center server.");
            return null;
        }

        var netPermission = new VsanFileShareNetPermission();
        netPermission.setIps("*");
        netPermission.setPermissions(VsanFileShareAccessType.READ_WRITE.value());
        netPermission.setAllowRoot(true);
        VsanFileShareConfig fileShareConfig = new VsanFileShareConfig();
        fileShareConfig.setName(shareName);
        fileShareConfig.setDomainName(domainName);
        fileShareConfig.setQuota(shareQuota);
        fileShareConfig.setStoragePolicy(vsanStoragePolicy);
        fileShareConfig.getPermission().add(netPermission);
        return fileShareConfig;
    }
}
