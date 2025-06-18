/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.hci;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.CLUSTER_COMPUTE_RESOURCE;
import static com.vmware.vim25.ManagedObjectType.DATACENTER;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.AboutInfo;
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.ClusterComputeResourceHCIConfigSpec;
import com.vmware.vim25.ClusterComputeResourceHostConfigurationInput;
import com.vmware.vim25.ClusterComputeResourceHostConfigurationProfile;
import com.vmware.vim25.ClusterConfigSpecEx;
import com.vmware.vim25.DesiredSoftwareSpec;
import com.vmware.vim25.DesiredSoftwareSpecBaseImageSpec;
import com.vmware.vim25.DesiredSoftwareSpecComponentSpec;
import com.vmware.vim25.DesiredSoftwareSpecVendorAddOnSpec;
import com.vmware.vim25.FolderNewHostSpec;
import com.vmware.vim25.HostConnectSpec;
import com.vmware.vim25.HostDateTimeConfig;
import com.vmware.vim25.HostLockdownMode;
import com.vmware.vim25.HostNtpConfig;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VsanClusterConfigInfo;

/**
 * This sample demonstrates calling batchAddHost HCI API to add new hosts to cluster. At least four hosts are required
 * for this sample. It demonstrates adding hosts to cluster and extend the cluster.
 */
public class HCIBatchAddHostAndExtendCluster {
    private static final Logger log = LoggerFactory.getLogger(HCIBatchAddHostAndExtendCluster.class);
    private static final String DATACENTER_NAME = "Datacenter";
    private static final String CLUSTER_NAME = "HCI-Cluster";

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

    /** REQUIRED: IPs of the hosts (comma separated) to be added to the cluster. */
    public static String[] hostIps = new String[] {"firstHostIP", "secondHostIP"};
    /** REQUIRED: Username of the hosts to be added. */
    public static String hostUsername = "hostUsername";
    /** REQUIRED: Password of the hosts to be added. */
    public static String hostPassword = "hostPassword";
    /** REQUIRED: Create a lifecycle managed cluster. */
    public static Boolean createVlcmCluster = Boolean.FALSE;
    /** OPTIONAL: Base image version in the desired software spec for the lifecycle managed cluster. */
    public static String baseImageVersion = null;
    /** OPTIONAL: Name of the addon to include in the desired software spec for the lifecycle managed cluster. */
    public static String addOnName = null;
    /** OPTIONAL: Version of the addon to include in the desired software spec for the lifecycle managed cluster. */
    public static String addOnVersion = null;
    /** OPTIONAL: Name of the component to include in the desired software spec for the lifecycle managed cluster. */
    public static String componentName = null;
    /** OPTIONAL: Version of the component to include in the desired software spec for the lifecycle managed cluster. */
    public static String componentVersion = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(HCIBatchAddHostAndExtendCluster.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            // Firstly, using batchAddHostsToCluster to add hosts into a cluster. Then we call configHCI to config the
            // HCI cluster. Finally, we call extendHCI to extend the existing cluster with a couple more hosts.

            AboutInfo aboutInfo = serviceContent.getAbout();

            if (!aboutInfo.getApiType().equals("VirtualCenter")) {
                log.info("Configure HCI API is only supported on vCenter");
                System.exit(1);
            }

            String apiVersion = aboutInfo.getApiVersion();
            if (apiVersion.compareTo("6.7") < 0) {
                log.error(
                        "The Virtual Center with version {} (lower than 6.7) is not supported.",
                        aboutInfo.getApiVersion());
                return;
            }

            log.info("Creating a datacenter.");
            vimPort.createDatacenter(serviceContent.getRootFolder(), DATACENTER_NAME);

            ManagedObjectReference datacenterMoRef =
                    propertyCollectorHelper.getMoRefByName(DATACENTER_NAME, DATACENTER);
            ManagedObjectReference hostFolderMoRef = propertyCollectorHelper.fetch(datacenterMoRef, "hostFolder");

            log.info("Creating a cluster.");
            ClusterConfigSpecEx clusterConfigSpec = new ClusterConfigSpecEx();
            clusterConfigSpec.setInHciWorkflow(true);

            VsanClusterConfigInfo vsanConfig = new VsanClusterConfigInfo();
            vsanConfig.setEnabled(true);

            clusterConfigSpec.setVsanConfig(vsanConfig);

            if (createVlcmCluster) {
                DesiredSoftwareSpec desiredSoftwareSpec = new DesiredSoftwareSpec();

                DesiredSoftwareSpecBaseImageSpec baseImageSpec = new DesiredSoftwareSpecBaseImageSpec();
                baseImageSpec.setVersion(baseImageVersion);

                desiredSoftwareSpec.setBaseImageSpec(baseImageSpec);

                if (addOnName != null && addOnVersion != null) {
                    DesiredSoftwareSpecVendorAddOnSpec addOnSpec = new DesiredSoftwareSpecVendorAddOnSpec();
                    addOnSpec.setName(addOnName);
                    addOnSpec.setVersion(addOnVersion);

                    desiredSoftwareSpec.setVendorAddOnSpec(addOnSpec);
                }

                if (componentName != null && componentVersion != null) {
                    DesiredSoftwareSpecComponentSpec componentSpec = new DesiredSoftwareSpecComponentSpec();
                    componentSpec.setName(componentName);
                    componentSpec.setVersion(componentVersion);

                    List<DesiredSoftwareSpecComponentSpec> components = new ArrayList<>();
                    components.add(componentSpec);

                    desiredSoftwareSpec.getComponents().addAll(components);
                }
                clusterConfigSpec.setDesiredSoftwareSpec(desiredSoftwareSpec);
            }

            vimPort.createClusterEx(hostFolderMoRef, CLUSTER_NAME, clusterConfigSpec);

            ManagedObjectReference clusterMoRef =
                    propertyCollectorHelper.getMoRefByName(CLUSTER_NAME, CLUSTER_COMPUTE_RESOURCE);

            List<FolderNewHostSpec> folderSpecs = new ArrayList<>();
            int half = hostIps.length / 2;

            for (int i = 0; i < half; i++) {
                String hostIp = hostIps[i];

                HostConnectSpec hostSpec = new HostConnectSpec();
                hostSpec.setHostName(hostIp);
                hostSpec.setUserName(hostUsername);
                hostSpec.setPassword(hostPassword);

                FolderNewHostSpec folderSpec = new FolderNewHostSpec();
                folderSpec.setHostCnxSpec(hostSpec);
                folderSpecs.add(folderSpec);
            }

            log.info("Batch add first half hosts.");
            // Batch all first half of hosts to cluster.
            ManagedObjectReference hciBatchAddHostTaskMoRef = vimPort.batchAddHostsToClusterTask(
                    hostFolderMoRef,
                    clusterMoRef,
                    folderSpecs, // new hosts
                    null, // existing hosts
                    null, // config spec
                    null); // desired state

            propertyCollectorHelper.awaitTaskCompletion(hciBatchAddHostTaskMoRef);

            log.info("Config the cluster.");
            // Config the HCI cluster
            HostLockdownMode lockdownMode = HostLockdownMode.LOCKDOWN_DISABLED;

            HostDateTimeConfig dateTimeConfig = new HostDateTimeConfig();
            String NTP_SERVER = "time-c-c.nist.gov";

            HostNtpConfig hostNtpConfig = new HostNtpConfig();
            List<String> serversFile = hostNtpConfig.getServer();
            serversFile.add(NTP_SERVER);
            dateTimeConfig.setNtpConfig(hostNtpConfig);

            ClusterComputeResourceHostConfigurationProfile hostConfigProfile =
                    new ClusterComputeResourceHostConfigurationProfile();
            hostConfigProfile.setLockdownMode(lockdownMode);
            hostConfigProfile.setDateTimeConfig(dateTimeConfig);

            ClusterComputeResourceHCIConfigSpec hciSpec = new ClusterComputeResourceHCIConfigSpec();
            hciSpec.setHostConfigProfile(hostConfigProfile);

            ManagedObjectReference hciTaskMoRef = vimPort.configureHCITask(clusterMoRef, hciSpec, null);

            propertyCollectorHelper.awaitTaskCompletion(hciTaskMoRef);

            // Extend the cluster with the second half hosts.
            List<ClusterComputeResourceHostConfigurationInput> hostInputs = new ArrayList<>();

            // Get all Mo-IDs of firs half hosts
            ArrayOfManagedObjectReference existingHostMoRefs = propertyCollectorHelper.fetch(clusterMoRef, "host");

            List<ManagedObjectReference> existingHostList = existingHostMoRefs.getManagedObjectReference();

            log.info("Adding second half hosts to the cluster.");
            // Add the rest of the hosts to data center
            for (int i = half; i < hostIps.length; i++) {
                String hostIp = hostIps[i];

                HostConnectSpec hostSpec = new HostConnectSpec();
                hostSpec.setHostName(hostIp);
                hostSpec.setUserName(hostUsername);
                hostSpec.setPassword(hostPassword);

                FolderNewHostSpec folderSpec = new FolderNewHostSpec();
                folderSpec.setHostCnxSpec(hostSpec);
                folderSpecs.add(folderSpec);

                ManagedObjectReference taskMoRef = vimPort.addHostTask(clusterMoRef, hostSpec, true, null, null);
                propertyCollectorHelper.awaitTaskCompletion(taskMoRef);
            }

            // Get all Mo-IDs of all hosts
            ArrayOfManagedObjectReference allHostMoRefs = propertyCollectorHelper.fetch(clusterMoRef, "host");

            List<ManagedObjectReference> allHostList = allHostMoRefs.getManagedObjectReference();

            // Leave the second half host Mo-IDs
            Set<String> removeMoIDSet = new HashSet<>();
            for (ManagedObjectReference moRef : existingHostList) {
                removeMoIDSet.add(moRef.getValue());
            }

            Set<ManagedObjectReference> removeSet = new HashSet<>();
            for (ManagedObjectReference moRef : allHostList) {
                if (removeMoIDSet.contains(moRef.getValue())) {
                    removeSet.add(moRef);
                }
            }

            allHostList.removeAll(removeSet);

            for (ManagedObjectReference hostMoRef : allHostList) {
                ClusterComputeResourceHostConfigurationInput hostInput =
                        new ClusterComputeResourceHostConfigurationInput();
                hostInput.setHost(hostMoRef);
                hostInputs.add(hostInput);
            }

            log.info("Extend the config of second half hosts.");
            ManagedObjectReference hciExtendClusterTaskMoRef = vimPort.extendHCITask(
                    clusterMoRef,
                    hostInputs, // host inputs
                    null); // vsan spec

            propertyCollectorHelper.awaitTaskCompletion(hciExtendClusterTaskMoRef);
        }
    }
}
