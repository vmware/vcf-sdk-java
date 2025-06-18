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

/**
 * This sample demonstrates access to the vSAN API on vCenter.
 *
 * <p>To provide an example of vCenter side vSAN API access, it shows how to get vSAN cluster health status by invoking
 * the QueryClusterHealthSummary API from vSAN health service against VC, how to track a task returned by vSAN API and
 * how to performance server related host information by invoking the VsanPerfQueryNodeInformation API of the Virtual
 * performance service against ESXi host.
 */
public class VsanVcApiSample {
    private static final Logger log = LoggerFactory.getLogger(VsanVcApiSample.class);

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
    public static String clusterName;

    private static PropertyCollectorHelper propertyCollectorHelper;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VsanVcApiSample.class, args);

        VcenterClientFactory clientFactory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = clientFactory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            var aboutInfo = serviceContent.getAbout();
            if (!aboutInfo.getApiType().equals("VirtualCenter")) {
                log.info("This sample can only be run against vCenter endpoint.");
            }

            ManagedObjectReference clusterMoRef = queryClusterByName(clusterName);
            if (clusterMoRef == null) {
                log.error("Cannot find cluster: {}", clusterName);
                return;
            }

            var vsanPort = client.getVsanPort();
            var fetchFromCache = false;
            var includeObjUuid = true;

            var vsanVcHealthRef = VsanManagedObjectsCatalog.getVsanVcHealthServiceInstanceReference();
            var healthSummary = vsanPort.vsanQueryVcClusterHealthSummary(
                    vsanVcHealthRef, clusterMoRef, null, null, includeObjUuid, null, fetchFromCache, null, null, null);
            log.info(
                    "Get vc health version {}",
                    healthSummary.getClusterVersions().getVcVersion());

            // Here is an example of how to track a task returned by the VSAN API.

            // Call Repair cluster objects
            var vsanTask = vsanPort.vsanHealthRepairClusterObjectsImmediate(vsanVcHealthRef, clusterMoRef, null);

            Boolean status = VsanUtil.waitForTasks(propertyCollectorHelper, vsanTask);
            if (status) {
                log.info("Repairing cluster objects task completed successfully!");
            } else {
                log.info("Repair cluster objects task failed!");
            }
        }
    }

    /**
     * Get the VC cluster instance from the cluster name. It will try to search the cluster under all of VC data centers
     * and return the first VC cluster matching the give name.
     *
     * @return The VC cluster instance. Return null if not found
     */
    private static ManagedObjectReference queryClusterByName(String clusterName) {
        try {
            return propertyCollectorHelper.getMoRefByName(clusterName, CLUSTER_COMPUTE_RESOURCE);
        } catch (Exception e) {
            log.error("Failed to get cluster with error.", e);
        }
        return null;
    }
}
