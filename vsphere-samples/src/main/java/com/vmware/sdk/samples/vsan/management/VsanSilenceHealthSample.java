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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.vsan.management.VsanManagedObjectsCatalog;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vsan.sdk.NotFoundFaultMsg;
import com.vmware.vsan.sdk.NotSupportedFaultMsg;
import com.vmware.vsan.sdk.RuntimeFaultFaultMsg;
import com.vmware.vsan.sdk.VsanFaultFaultMsg;
import com.vmware.vsan.sdk.VsanhealthPortType;

/**
 * This sample demonstrates how to silence vSAN cluster health checks, query the health summary, and unsilence them.
 *
 * <ol>
 *   <li>Step 1: Query vSAN health summary
 *   <li>Step 2: Find out the non-green health
 *   <li>Step 3: Silence the health check and query the health summary
 *   <li>Step 4: Un-silence the health check and query the health summary again
 * </ol>
 *
 * <p>Sample Prerequisites: vCenter version &gt;= 9.0 and the cluster should be vSAN enabled.
 */
public class VsanSilenceHealthSample {
    private static final Logger log = LoggerFactory.getLogger(VsanSilenceHealthSample.class);

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
    /** REQUIRED: VC cluster name using in cluster health query API */
    public static String clusterName;

    private static PropertyCollectorHelper propertyCollectorHelper;

    private static void silenceClusterHealth(
            VsanhealthPortType vsanPort, ManagedObjectReference cluster, List<String> checks) {
        ManagedObjectReference vhs = VsanManagedObjectsCatalog.getVsanVcHealthServiceInstanceReference();
        try {
            vsanPort.vsanHealthSetVsanClusterSilentChecks(vhs, cluster, checks, List.of());
            log.info("Successfully silenced the {}", checks);
        } catch (NotSupportedFaultMsg | NotFoundFaultMsg | RuntimeFaultFaultMsg | VsanFaultFaultMsg e) {
            throw new RuntimeException(e);
        }
    }

    private static void unsilenceClusterHealth(
            VsanhealthPortType vsanPort, ManagedObjectReference cluster, List<String> checks) {
        ManagedObjectReference vhs = VsanManagedObjectsCatalog.getVsanVcHealthServiceInstanceReference();
        try {
            vsanPort.vsanHealthSetVsanClusterSilentChecks(vhs, cluster, List.of(), checks);
            log.info("Successfully un-silenced the {}", checks);
        } catch (NotSupportedFaultMsg | NotFoundFaultMsg | RuntimeFaultFaultMsg | VsanFaultFaultMsg e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VsanSilenceHealthSample.class, args);

        VcenterClientFactory clientFactory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = clientFactory.createClient(username, password, null)) {
            ServiceContent serviceContent = client.getVimServiceContent();

            VimPortType vimPort = client.getVimPort();
            propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            var aboutInfo = serviceContent.getAbout();
            if (!aboutInfo.getApiType().equals("VirtualCenter")) {
                log.error("Host {} is not a VC host. Please run on VC", serviceContent);
            }

            ManagedObjectReference cluster =
                    propertyCollectorHelper.getMoRefByName(clusterName, CLUSTER_COMPUTE_RESOURCE);
            if (cluster == null) {
                log.error("Cluster {} not found for {}", clusterName, serverAddress);
                return;
            }

            VsanhealthPortType vsanPort = client.getVsanPort();
            var vsanVcHealthRef = VsanManagedObjectsCatalog.getVsanVcHealthServiceInstanceReference();

            // Query vSAN health summary
            var healthSummary = vsanPort.vsanQueryVcClusterHealthSummary(
                    vsanVcHealthRef, cluster, null, null, true, null, false, null, null, null);
            log.info("Overall health for cluster {} : {}", clusterName, healthSummary.getOverallHealth());
            if (!"red".equals(healthSummary.getOverallHealth()) && !"yellow".equals(healthSummary.getOverallHealth())) {
                log.info("Cluster health not yellow or red, exiting..");
                return;
            }

            // Find out the non-green health
            String groupId = null;
            List<String> checks = new ArrayList<>();
            for (var group : healthSummary.getGroups()) {
                if (!"yellow".equals(group.getGroupHealth()) && !"red".equals(group.getGroupHealth())) {
                    continue;
                }
                groupId = group.getGroupId();
                for (var test : group.getGroupTests()) {
                    if ("yellow".equals(test.getTestHealth()) || "red".equals(test.getTestHealth())) {
                        String[] testIdArray = test.getTestId().split("\\.");
                        checks.add(testIdArray[testIdArray.length - 1]);
                    }
                }
            }

            if (groupId == null) {
                log.info("All health checks are green!");
                return;
            }

            // Silence the health check and query the health summary
            silenceClusterHealth(vsanPort, cluster, checks);
            healthSummary = vsanPort.vsanQueryVcClusterHealthSummary(
                    vsanVcHealthRef, cluster, null, null, true, null, false, null, null, null);
            log.info(
                    "Overall health for cluster {} is {} after silencing {}",
                    clusterName,
                    healthSummary.getOverallHealth(),
                    checks);

            // Unsilence the health check and query health summary again
            unsilenceClusterHealth(vsanPort, cluster, checks);
            healthSummary = vsanPort.vsanQueryVcClusterHealthSummary(
                    vsanVcHealthRef, cluster, null, null, true, null, false, null, null, null);
            log.info(
                    "Overall health for cluster {} is {} after un-silencing {}",
                    clusterName,
                    healthSummary.getOverallHealth(),
                    checks);
        }
    }
}
