/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.storage.policy;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.pbm.PbmCapabilityVendorNamespaceInfo;
import com.vmware.pbm.PbmCapabilityVendorResourceTypeInfo;
import com.vmware.pbm.PbmPortType;
import com.vmware.pbm.PbmServiceInstanceContent;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;

/**
 * This sample prints the server time, prints the endpoint information, prints the supported vendors information. The
 * sample exercise both the vim API and spbm API side by side Both these APIs can be executed independently, and no
 * particular order is implied.
 */
public class AboutInfo {
    private static final Logger log = LoggerFactory.getLogger(AboutInfo.class);

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
        SampleCommandLineParser.load(AboutInfo.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            PbmPortType pbmPort = client.getPbmPort();

            PbmServiceInstanceContent serviceInstanceContent = client.getPbmServiceInstanceContent();
            log.info(
                    "SPBM Endpoint Information: {} - {}",
                    serviceInstanceContent.getAboutInfo().getName(),
                    serviceInstanceContent.getAboutInfo().getVersion());

            List<PbmCapabilityVendorResourceTypeInfo> vendorInfo = pbmPort.pbmFetchVendorInfo(
                    client.getPbmServiceInstanceContent().getProfileManager(), null);

            log.info("The No. of supported vendors are {}", vendorInfo.size());
            for (PbmCapabilityVendorResourceTypeInfo vendor : vendorInfo) {
                log.info("====== Resource Type: {}", vendor.getResourceType());

                List<PbmCapabilityVendorNamespaceInfo> vNamespaceInfo = vendor.getVendorNamespaceInfo();
                for (PbmCapabilityVendorNamespaceInfo namespaceInfo : vNamespaceInfo) {
                    log.info("vendor UUID: {}", namespaceInfo.getVendorInfo().getVendorUuid());
                    log.info(
                            "vendor.info.key: {}",
                            namespaceInfo.getVendorInfo().getInfo().getKey());
                }
            }
        }
    }
}
