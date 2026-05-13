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
import com.vmware.vim25.VsanIscsiLUNSpec;
import com.vmware.vim25.VsanIscsiTargetServiceDefaultConfigSpec;
import com.vmware.vim25.VsanIscsiTargetServiceSpec;
import com.vmware.vim25.VsanIscsiTargetSpec;
import com.vmware.vsan.sdk.VsanhealthPortType;

/**
 * This sample demonstrates accessing vCenter vSAN iSCSI API.
 *
 * <p>To provide an example of vSAN iSCSI API access, it shows how to enable vSAN iSCSI Target service, create targets
 * and the associated LUNs, together with disable iSCSI service.
 */
public class VsanIscsiSample {
    private static final Logger log = LoggerFactory.getLogger(VsanIscsiSample.class);

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

    /** REQUIRED: VC cluster name using in cluster health query API. */
    public static String clusterName = "Cluster_42";

    private static PropertyCollectorHelper propertyCollectorHelper;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VsanIscsiSample.class, args);

        VcenterClientFactory clientFactory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = clientFactory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            // Here is an example of how to access vCenter side vSAN Health Service API.
            ManagedObjectReference clusterMoRef =
                    propertyCollectorHelper.getMoRefByName(clusterName, CLUSTER_COMPUTE_RESOURCE);
            if (clusterMoRef == null) {
                log.error("Cannot find vCenter cluster {}", clusterName);
                return;
            }

            // Construct the VSAN iSCSI service spec and enable iSCSI service;
            var vsanConfigSpec = new VsanIscsiTargetServiceDefaultConfigSpec();
            vsanConfigSpec.setNetworkInterface("vmk0");
            vsanConfigSpec.setPort(2300);

            VsanIscsiTargetServiceSpec enableSpec = new VsanIscsiTargetServiceSpec();
            enableSpec.setDefaultConfig(vsanConfigSpec);
            enableSpec.setEnabled(true);

            var reconfigSpec = new VimVsanReconfigSpec();
            reconfigSpec.setIscsiSpec(enableSpec);
            reconfigSpec.setModify(true);

            VsanhealthPortType healthPort = client.getVsanPort();
            ManagedObjectReference vsanTask = healthPort.vsanClusterReconfig(
                    VsanManagedObjectsCatalog.getVsanVcClusterConfigServiceInstanceReference(),
                    clusterMoRef,
                    reconfigSpec);

            if (VsanUtil.waitForTasks(propertyCollectorHelper, vsanTask)) {
                log.info("Enable vSAN iSCSI target service task completed with status: success");
            } else {
                log.error("Enable vSAN iSCSI target service task completed with status: failure");
                return;
            }

            // Create a iSCSI target and the associated LUN
            String targetAlias = "sampleTarget2";
            VsanIscsiTargetSpec targetSpec = new VsanIscsiTargetSpec();
            targetSpec.setAlias(targetAlias);
            targetSpec.setIqn("iqn.2015-08.com.vmware:vit.target2");
            vsanTask = healthPort.vsanVitAddIscsiTarget(
                    VsanManagedObjectsCatalog.getVsanVcIscsiTargetServiceInstanceReference(), clusterMoRef, targetSpec);

            if (VsanUtil.waitForTasks(propertyCollectorHelper, vsanTask)) {
                log.info("Create vSAN iSCSI target service task completed with status: success");
            } else {
                log.error("Create vSAN iSCSI target service task completed with status: failure");
                return;
            }

            int lunSize = 1 * 1024 * 1024 * 1024;
            VsanIscsiLUNSpec lunSpec = new VsanIscsiLUNSpec();
            lunSpec.setLunId(0);
            lunSpec.setLunSize(lunSize);
            vsanTask = healthPort.vsanVitAddIscsiLUN(
                    VsanManagedObjectsCatalog.getVsanVcIscsiTargetServiceInstanceReference(),
                    clusterMoRef,
                    targetAlias,
                    lunSpec);

            if (VsanUtil.waitForTasks(propertyCollectorHelper, vsanTask)) {
                log.info("Create vSAN iSCSI LUN task completed with status: success");
            } else {
                log.error("Create vSAN iSCSI LUN task completed with status: failure");
                return;
            }

            // Remove iSCSI target and the associated LUN
            vsanTask = healthPort.vsanVitRemoveIscsiLUN(
                    VsanManagedObjectsCatalog.getVsanVcIscsiTargetServiceInstanceReference(),
                    clusterMoRef,
                    targetAlias,
                    0);
            if (VsanUtil.waitForTasks(propertyCollectorHelper, vsanTask)) {
                log.info("Remove vSAN iSCSI LUN task completed with status: success");
            } else {
                log.error("Remove vSAN iSCSI LUN task completed with status: failure");
                return;
            }

            vsanTask = healthPort.vsanVitRemoveIscsiTarget(
                    VsanManagedObjectsCatalog.getVsanVcIscsiTargetServiceInstanceReference(),
                    clusterMoRef,
                    targetAlias);
            if (VsanUtil.waitForTasks(propertyCollectorHelper, vsanTask)) {
                log.info("Remove vSAN iSCSI target task completed with status: success");
            } else {
                log.error("Remove vSAN iSCSI target task completed with status: failure");
                return;
            }

            // Construct the vSAN iSCSI service spec and disable iSCSI service;
            VsanIscsiTargetServiceSpec disableSpec = new VsanIscsiTargetServiceSpec();
            disableSpec.setEnabled(false);

            reconfigSpec = new VimVsanReconfigSpec();
            reconfigSpec.setIscsiSpec(disableSpec);
            reconfigSpec.setModify(true);
            vsanTask = healthPort.vsanClusterReconfig(
                    VsanManagedObjectsCatalog.getVsanVcClusterConfigServiceInstanceReference(),
                    clusterMoRef,
                    reconfigSpec);

            var status = VsanUtil.waitForTasks(propertyCollectorHelper, vsanTask);
            if (status) {
                log.info("Disable vSAN iSCSI target service task completed with status: success");
            } else {
                log.error("Disable vSAN iSCSI target service task completed with status: failure");
            }
        }
    }
}
