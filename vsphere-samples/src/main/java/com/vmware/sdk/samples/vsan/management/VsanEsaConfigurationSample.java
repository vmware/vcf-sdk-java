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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.vsan.management.VsanManagedObjectsCatalog;
import com.vmware.sdk.vsphere.utils.vsan.management.VsanUtil;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimVsanReconfigSpec;
import com.vmware.vim25.VsanClusterConfigInfo;

/**
 * This file includes sample code for vCenter to configure vSAN ESA cluster using the ReconfigureEx API.
 *
 * <p>Sample Prerequisites: This sample assumes a vSphere cluster with vCenter version 8.0 and above.
 */
public class VsanEsaConfigurationSample {

    private static final Logger log = LoggerFactory.getLogger(VsanEsaConfigurationSample.class);

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
        SampleCommandLineParser.load(VsanEsaConfigurationSample.class, args);

        VcenterClientFactory clientFactory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = clientFactory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            var aboutInfo = serviceContent.getAbout();
            if (!aboutInfo.getApiType().equals("VirtualCenter")) {
                log.info("This sample can only be run against vCenter endpoint.");
                return;
            }

            ManagedObjectReference clusterMoRef =
                    propertyCollectorHelper.getMoRefByName(clusterName, CLUSTER_COMPUTE_RESOURCE);
            if (clusterMoRef == null) {
                log.error("Cannot find cluster: {}", clusterName);
                return;
            }

            // Step 1) Get the cluster current configuration
            var vsanPort = client.getVsanPort();
            var vsanConfigInfoEx = vsanPort.vsanClusterGetConfig(
                    VsanManagedObjectsCatalog.getVsanVcClusterConfigServiceInstanceReference(), clusterMoRef);
            log.info("Is vSAN ESA enabled: {}", vsanConfigInfoEx.isEnabled());

            // Step 2) Enable vSAN ESA on the cluster
            var vsanClusterConfig = new VsanClusterConfigInfo();
            vsanClusterConfig.setEnabled(true);
            vsanClusterConfig.setVsanEsaEnabled(true);
            var rs = new VimVsanReconfigSpec();
            rs.setVsanClusterConfig(vsanClusterConfig);
            var task = vsanPort.vsanClusterReconfig(
                    VsanManagedObjectsCatalog.getVsanVcClusterConfigServiceInstanceReference(), clusterMoRef, rs);
            boolean status = VsanUtil.waitForTasks(propertyCollectorHelper, task);
            if (status) {
                log.info("{} task completed with status: success", "Enable vSAN ESA");
            } else {
                log.error("{} task completed with status: failure", "Enable vSAN ESA");
            }

            // Step 3) Get the updated cluster configuration and notice the vSAN ESA flag enabled.
            var config = vsanPort.vsanClusterGetConfig(
                    VsanManagedObjectsCatalog.getVsanVcClusterConfigServiceInstanceReference(), clusterMoRef);
            log.info("Is vSAN ESA enabled: {}", config.isEnabled());
        }
    }
}
