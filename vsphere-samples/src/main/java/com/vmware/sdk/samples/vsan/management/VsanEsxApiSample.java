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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.ESXiClient;
import com.vmware.sdk.vsphere.utils.ESXiClientFactory;
import com.vmware.sdk.vsphere.utils.vsan.management.VsanManagedObjectsCatalog;
import com.vmware.vim25.VsanPerfNodeInformation;

/**
 * This sample demonstrates access to the vSAN API on ESXi.
 *
 * <p>To provide an example of ESXi side vSAN API access, it shows how to get performance server related host
 * information by invoking the VsanPerfQueryNodeInformation API of the VsanPerformanceManager Managed Object.
 */
public class VsanEsxApiSample {
    private static final Logger log = LoggerFactory.getLogger(VsanEsxApiSample.class);

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

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VsanEsxApiSample.class, args);

        ESXiClientFactory clientFactory =
                new ESXiClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));
        try (ESXiClient client = clientFactory.createClient(username, password, null)) {
            var serviceContent = client.getVimServiceContent();

            var aboutInfo = serviceContent.getAbout();
            if (!aboutInfo.getApiType().equals("HostAgent")) {
                log.error("This sample can only be run against ESXi endpoint.");
                return;
            }

            // Here is an example of how to access ESXi side VSAN Performance
            // Service API.
            var vsanPort = client.getVsanPort();

            var vsanPerfMgrRef = VsanManagedObjectsCatalog.getVsanPerfMgrServiceInstanceReference();
            var nodeInfos = vsanPort.vsanPerfQueryNodeInformation(vsanPerfMgrRef, null);
            if (nodeInfos != null && !nodeInfos.isEmpty()) {
                VsanPerfNodeInformation nodeInfo = nodeInfos.get(0);
                log.info("Hostname: ");
                log.info("  version: {}", nodeInfo.getVersion());
                log.info("  isCmmdsMaster: {}", nodeInfo.isIsCmmdsMaster());
                log.info("  isStatsMaster: {}", nodeInfo.isIsStatsMaster());
                log.info("  vsanMasterUuid: {}", nodeInfo.getVsanMasterUuid());
                log.info("  vsanNodeUuid: {}", nodeInfo.getVsanNodeUuid());
            }
        }
    }
}
