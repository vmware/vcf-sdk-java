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
import static com.vmware.vim25.ManagedObjectType.DATACENTER;
import static com.vmware.vim25.ManagedObjectType.HOST_SYSTEM;
import static java.util.Objects.requireNonNullElse;

import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import jakarta.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.vsan.management.VsanManagedObjectsCatalog;
import com.vmware.sdk.vsphere.utils.vsan.management.VsanUtil;
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.ClusterComputeResourceDvsProfile;
import com.vmware.vim25.ClusterComputeResourceDvsProfileDVPortgroupSpecToServiceMapping;
import com.vmware.vim25.ClusterComputeResourceHCIConfigSpec;
import com.vmware.vim25.ClusterComputeResourceHostConfigurationInput;
import com.vmware.vim25.ClusterComputeResourceHostConfigurationProfile;
import com.vmware.vim25.ClusterComputeResourceVCProfile;
import com.vmware.vim25.ClusterConfigSpecEx;
import com.vmware.vim25.ClusterDrsConfigInfo;
import com.vmware.vim25.DVPortgroupConfigSpec;
import com.vmware.vim25.DrsBehavior;
import com.vmware.vim25.FolderNewHostSpec;
import com.vmware.vim25.HostConfigInfo;
import com.vmware.vim25.HostConfigManager;
import com.vmware.vim25.HostConnectSpec;
import com.vmware.vim25.HostDateTimeConfig;
import com.vmware.vim25.HostLockdownMode;
import com.vmware.vim25.HostNetworkInfo;
import com.vmware.vim25.HostNtpConfig;
import com.vmware.vim25.HostProxySwitch;
import com.vmware.vim25.HostVirtualSwitch;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PhysicalNic;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.SDDCBase;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimVsanReconfigSpec;
import com.vmware.vim25.VsanClusterConfigInfo;
import com.vmware.vim25.VsanClusterConfigInfoHostDefaultInfo;
import com.vmware.vim25.VsanDataEfficiencyConfig;
import com.vmware.vsan.sdk.VsanDataEncryptionConfig;

/**
 * This sample demonstrates accessing both vSAN APIs and vSphere APIs by using different WSDL port types.
 *
 * <p>Firstly, it shows how to call the vSphere ConfigureHCI operation from the vSphere API by using the
 * {@link VimPortType} in conjunction with additional vSAN-related data types which are needed to fully configure the
 * HCI cluster.
 *
 * <p>Secondly, this sample demonstrates how to get vSAN cluster health status by invoking the QueryClusterHealthSummary
 * API from vSAN health service against VC, then call a vSphere API to configure HCI cluster from vSphere service.
 */
public class ConfigureHciSample {
    private static final Logger log = LoggerFactory.getLogger(ConfigureHciSample.class);

    /** REQUIRED: vCenter FQDN or IP address. */
    public static String serverAddress = "vcenter1.mycompany.com";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";
    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (do not leave it empty on production environments).
     */
    public static String trustStorePath = null;

    /** REQUIRED: IPs of the hosts to be added to the cluster. */
    public static String[] hostIps = {"192.168.10.11", "192.168.10.12"};
    /** REQUIRED: Username of the hosts. */
    public static String hostUsername = "username";
    /** REQUIRED: Password of the hosts. */
    public static String hostPassword = "password";
    /** OPTIONAL: Name of the Datacenter to be created for HCI, default is {@code "Datacenter"}. */
    public static String datacenterName = null;
    /** OPTIONAL: Name of the Cluster to be created for HCI, default is {@code "HCI-Cluster"}. */
    public static String clusterName = null;

    private static PropertyCollectorHelper propertyCollectorHelper;

    private static final String DEFAULT_DATACENTER_NAME = "Datacenter";
    private static final String DEFAULT_CLUSTER_NAME = "HCI-Cluster";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(ConfigureHciSample.class, args);

        VcenterClientFactory clientFactory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = clientFactory.createClient(username, password, null)) {
            var serviceContent = client.getVimServiceContent();
            var aboutInfo = serviceContent.getAbout();
            if (!aboutInfo.getApiType().equals("VirtualCenter")) {
                log.error("Configure HCI API is only supported on vCenter");
                System.exit(1);
            }

            VimPortType vimPort = client.getVimPort();
            propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            String actualDatacenterName = requireNonNullElse(datacenterName, DEFAULT_DATACENTER_NAME);
            String actualClusterName = requireNonNullElse(clusterName, DEFAULT_CLUSTER_NAME);

            vimPort.createDatacenter(serviceContent.getRootFolder(), actualDatacenterName);

            var dcMoRef = propertyCollectorHelper.getMoRefByName(actualDatacenterName, DATACENTER);
            var hostFolderMoRef = (ManagedObjectReference) propertyCollectorHelper.fetch(dcMoRef, "hostFolder");

            var clusterConfigSpec = new ClusterConfigSpecEx();
            clusterConfigSpec.setInHciWorkflow(true);
            var vsanConfig = new VsanClusterConfigInfo();
            vsanConfig.setEnabled(true);
            clusterConfigSpec.setVsanConfig(vsanConfig);

            vimPort.createClusterEx(hostFolderMoRef, actualClusterName, clusterConfigSpec);

            var cluster = propertyCollectorHelper.getMoRefByName(actualClusterName, CLUSTER_COMPUTE_RESOURCE);

            var hostSpecList = new ArrayList<FolderNewHostSpec>();
            for (String hostIp : hostIps) {
                var hostSpec = new HostConnectSpec();
                hostSpec.setHostName(hostIp);
                hostSpec.setUserName(hostUsername);
                hostSpec.setPassword(hostPassword);
                hostSpec.setSslThumbprint(getSslThumbprint(hostIp));

                var folderHostSpec = new FolderNewHostSpec();
                folderHostSpec.setHostCnxSpec(hostSpec);
                hostSpecList.add(folderHostSpec);

                var task = vimPort.batchAddHostsToClusterTask(
                        hostFolderMoRef, cluster, hostSpecList, null, null, "maintenance");
                String taskName = "Add host " + hostIp;

                monitorTask(task, taskName);
            }

            // Configure HCI Spec
            var hciSpec = new ClusterComputeResourceHCIConfigSpec();

            var NTP_SERVER = "time-c-b.nist.gov";
            var hostConfigProfile = createHostConfigProfile(NTP_SERVER, HostLockdownMode.LOCKDOWN_DISABLED);
            hciSpec.setHostConfigProfile(hostConfigProfile);

            var vcProfile = getVcProf();
            hciSpec.setVcProf(vcProfile);

            SDDCBase vsanConfigSpecBase = createDefaultVsanSpec();
            hciSpec.setVSanConfigSpec(vsanConfigSpecBase);

            var hostMoRefs = (ArrayOfManagedObjectReference) propertyCollectorHelper.fetch(cluster, "host");
            var dvsProfile = getDvsProfile(hostFolderMoRef, hostIps[0]);
            hciSpec.getDvsProf().add(dvsProfile);

            // Do the same configuration on all the hosts inside this cluster.
            var hostCfgs = new ArrayList<ClusterComputeResourceHostConfigurationInput>();
            for (var host : hostMoRefs.getManagedObjectReference()) {
                var hostCfg = new ClusterComputeResourceHostConfigurationInput();
                hostCfg.setHost(host);
                hostCfgs.add(hostCfg);
            }

            var hciTask = vimPort.configureHCITask(cluster, hciSpec, hostCfgs);

            // Need covert to vcTask to bind the managed object with VC session.
            monitorTask(hciTask, "Configure HCI");

            log.info("Starting health check... ");
            var vsanPort = client.getVsanPort();
            Boolean fetchFromCache = false;
            Boolean includeObjUuid = true;
            var healthSummary = vsanPort.vsanQueryVcClusterHealthSummary(
                    VsanManagedObjectsCatalog.getVsanVcHealthServiceInstanceReference(),
                    cluster,
                    null,
                    null,
                    includeObjUuid,
                    null,
                    fetchFromCache,
                    null,
                    null,
                    null);
            var clusterStatus = healthSummary.getClusterStatus();
            log.info("Cluster {} status: {}", actualClusterName, clusterStatus.getStatus());
            for (var hostStatus : clusterStatus.getTrackedHostsStatus()) {
                log.info("Host {} status: {}", hostStatus.getHostname(), hostStatus.getStatus());
            }
        }
    }

    private static void monitorTask(ManagedObjectReference task, String taskName) {
        // Here is an example of how to track a task returned by the vSphere API.
        Boolean status = VsanUtil.waitForTasks(propertyCollectorHelper, task);
        if (status) {
            log.info("{} completed successfully!", taskName);
        } else {
            log.info("{} failed!", taskName);
        }
    }

    private static String getSslThumbprint(String host) {
        String sslThumbPrint = "";
        try {
            X509TrustManager tm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {}

                @Override
                public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[] {tm}, null);
            SSLSocketFactory factory = sc.getSocketFactory();
            SSLSocket socket = (SSLSocket) factory.createSocket(host, 443);
            SSLSession session = socket.getSession();
            java.security.cert.Certificate[] servercerts = session.getPeerCertificates();
            String digest = DatatypeConverter.printHexBinary(
                            MessageDigest.getInstance("SHA-1").digest(servercerts[0].getEncoded()))
                    .toUpperCase();
            sslThumbPrint = colonify(digest.toCharArray());
        } catch (Exception e) {
            log.error("exception in get ssl", e);
        }
        return sslThumbPrint;
    }

    private static String colonify(char[] bytes) {
        Objects.requireNonNull(bytes, "byte array is null");
        if (bytes.length == 0) {
            return "";
        }

        final char[] chars = new char[bytes.length / 2 * 3 - 1];

        int charPos = 0;
        for (int pos = 0; pos < chars.length; pos++) {
            if (pos % 3 != 2) {
                chars[pos] = bytes[charPos];
                charPos++;
            } else {
                chars[pos] = ':';
            }
        }
        return String.valueOf(chars);
    }

    private static ClusterComputeResourceHostConfigurationProfile createHostConfigProfile(
            String ntpServer, HostLockdownMode lockdownMode) {
        HostNtpConfig hostNtpConfig = new HostNtpConfig();
        hostNtpConfig.getServer().add(ntpServer);

        HostDateTimeConfig dateTimeConfig = new HostDateTimeConfig();
        dateTimeConfig.setNtpConfig(hostNtpConfig);

        var hostConfigurationProfile = new ClusterComputeResourceHostConfigurationProfile();
        hostConfigurationProfile.setDateTimeConfig(dateTimeConfig);
        hostConfigurationProfile.setLockdownMode(lockdownMode);
        return hostConfigurationProfile;
    }

    private static ClusterComputeResourceVCProfile getVcProf() {
        var drsConfigInfo = new ClusterDrsConfigInfo();
        drsConfigInfo.setEnabled(true);
        drsConfigInfo.setVmotionRate(2);
        drsConfigInfo.setDefaultVmBehavior(DrsBehavior.FULLY_AUTOMATED);

        var vcProfile = new ClusterComputeResourceVCProfile();
        var configSpecEx = new ClusterConfigSpecEx();

        configSpecEx.setDrsConfig(drsConfigInfo);
        vcProfile.setClusterSpec(configSpecEx);
        vcProfile.setEvcModeKey("intel-merom");
        return vcProfile;
    }

    private static SDDCBase createDefaultVsanSpec() {
        var dedupConfig = new VsanDataEfficiencyConfig();
        dedupConfig.setDedupEnabled(false);
        var encryptionConfig = new VsanDataEncryptionConfig();
        encryptionConfig.setEncryptionEnabled(false);

        var vsanClusterConfigInfo = new VsanClusterConfigInfo();
        vsanClusterConfigInfo.setEnabled(true);
        var hostDefaultInfo = new VsanClusterConfigInfoHostDefaultInfo();

        var vsanSpec = new VimVsanReconfigSpec();
        hostDefaultInfo.setAutoClaimStorage(false);

        vsanClusterConfigInfo.setDefaultConfig(hostDefaultInfo);
        vsanSpec.setDataEfficiencyConfig(dedupConfig);
        vsanSpec.setDataEncryptionConfig(encryptionConfig);
        vsanSpec.setAllowReducedRedundancy(false);
        vsanSpec.setModify(true);
        vsanSpec.setVsanClusterConfig(vsanClusterConfigInfo);

        return vsanSpec;
    }

    private static ClusterComputeResourceDvsProfile getDvsProfile(
            ManagedObjectReference hostFolderMoRef, String hostname)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        String freePnic = getFreePnicList(hostFolderMoRef, hostname);

        var dvpgNameAndService = new HashMap<String, String>();
        dvpgNameAndService.put("vmotion-dvpg", "vmotion");
        dvpgNameAndService.put("vsan-dvpg", "vsan");
        String dvsName = "hci-dvs-new";

        var dvsProfile = createDvsProfile(dvpgNameAndService, freePnic, dvsName);
        return dvsProfile;
    }

    private static String getFreePnicList(ManagedObjectReference hostFolderMoRef, String hostname)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        ManagedObjectReference hostMoRef = null;
        if (hostFolderMoRef != null && hostname != null) {
            hostMoRef = propertyCollectorHelper.getMoRefByName(hostFolderMoRef, hostname, HOST_SYSTEM);
        }

        if (hostMoRef != null) {
            var configMgr = (HostConfigManager) propertyCollectorHelper.fetch(hostMoRef, "configManager");
            var nwSystemMoRef = configMgr.getNetworkSystem();

            HostNetworkInfo netInfo = propertyCollectorHelper.fetch(nwSystemMoRef, "networkInfo");

            List<String> allUpPnicsList = new ArrayList<String>();
            for (PhysicalNic pnic : netInfo.getPnic()) {
                if (pnic.getSpec().getLinkSpeed() != null) {
                    allUpPnicsList.add(pnic.getDevice());
                }
            }

            List<String> usedNicsOnVss = new ArrayList<String>();
            for (HostVirtualSwitch vSwitch : netInfo.getVswitch()) {
                if (vSwitch.getSpec().getBridge() != null && vSwitch.getPnic().size() != 0) {
                    usedNicsOnVss.addAll(vSwitch.getPnic());
                }
            }

            var hostConfig = (HostConfigInfo) propertyCollectorHelper.fetch(hostMoRef, "config");

            String usedNicsOnProxy = null;
            var proxySwitches = hostConfig.getNetwork().getProxySwitch();
            for (HostProxySwitch proxySwitch : proxySwitches) {
                if (proxySwitch.getPnic() != null && proxySwitch.getPnic().size() > 0) {
                    usedNicsOnProxy = proxySwitch.getPnic().get(0);
                    break;
                }
            }

            List<String> usedVssPnics = new ArrayList<String>();
            if (usedNicsOnVss.size() >= 1) {
                //          In this case, usedVnicsOnVss returns an array of type:
                //          [(str) [ 'vmnic0' ], (str) [ 'vmnic5' ]]
                //          To obtain the entire list of vmnics, we need to read the first
                //          element saved in pyVmomi.VmomiSupport.str[].
                usedVssPnics.add(usedNicsOnVss.get(0));
            }

            if (usedNicsOnProxy != null) {
                String[] pnicsOnProxyList = usedNicsOnProxy.split("-", -1);
                String pnicsOnProxy = pnicsOnProxyList[pnicsOnProxyList.length - 1];
                usedVssPnics.add(pnicsOnProxy);
            }

            var freePnics = allUpPnicsList.stream()
                    .filter(e -> !usedVssPnics.contains(e))
                    .collect(Collectors.toList());

            String freePnic = "";
            if (freePnics != null && !freePnics.isEmpty()) {
                freePnic = freePnics.get(0);
            }
            return freePnic;
        } else {
            return null;
        }
    }

    private static ClusterComputeResourceDvsProfile createDvsProfile(
            Map<String, String> dvpgNameAndServices, String freePnic, String dvsName) {
        var dvsProfile = new ClusterComputeResourceDvsProfile();
        dvsProfile.setDvsName(dvsName);
        dvsProfile.setDvSwitch(null);
        dvsProfile.getPnicDevices().add(freePnic);

        var dvpgToServiceMappings = new ArrayList<ClusterComputeResourceDvsProfileDVPortgroupSpecToServiceMapping>();

        for (Map.Entry<String, String> nameAndService : dvpgNameAndServices.entrySet()) {
            var dvpgToServiceMapping = new ClusterComputeResourceDvsProfileDVPortgroupSpecToServiceMapping();

            var dvpgConfigSpec = new DVPortgroupConfigSpec();
            dvpgConfigSpec.setNumPorts(128);
            dvpgConfigSpec.setName(nameAndService.getKey());
            dvpgConfigSpec.setType("earlyBinding");
            dvpgToServiceMapping.setDvPortgroupSpec(dvpgConfigSpec);
            dvpgToServiceMapping.setService(nameAndService.getValue());
            dvpgToServiceMappings.add(dvpgToServiceMapping);
        }
        dvsProfile.getDvPortgroupMapping().addAll(dvpgToServiceMappings);
        return dvsProfile;
    }
}
