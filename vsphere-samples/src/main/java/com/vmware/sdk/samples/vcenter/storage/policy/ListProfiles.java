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

import com.vmware.pbm.PbmPortType;
import com.vmware.pbm.PbmProfile;
import com.vmware.pbm.PbmProfileId;
import com.vmware.pbm.PbmServiceInstanceContent;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.pbm.PbmUtil;

/** This sample lists the storage profiles and their basic information. */
public class ListProfiles {
    private static final Logger log = LoggerFactory.getLogger(ListProfiles.class);

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
        SampleCommandLineParser.load(ListProfiles.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            PbmPortType pbmPort = client.getPbmPort();
            PbmServiceInstanceContent pbmServiceInstanceContent = client.getPbmServiceInstanceContent();

            // Retrieve the list of storage profile ID's
            List<PbmProfileId> profileIds = pbmPort.pbmQueryProfile(
                    pbmServiceInstanceContent.getProfileManager(), PbmUtil.getStorageResourceType(), null);
            log.info("No. of storage profiles are {}", profileIds.size());

            if (!profileIds.isEmpty()) {
                // Fetch more details about the retrieved profiles
                List<PbmProfile> profiles =
                        pbmPort.pbmRetrieveContent(pbmServiceInstanceContent.getProfileManager(), profileIds);
                for (PbmProfile profile : profiles) {
                    System.out.println("------------");
                    System.out.println("Profile Name: " + profile.getName());
                    System.out.println("Profile Id: " + profile.getProfileId().getUniqueId());
                    System.out.println("Description: " + profile.getDescription());
                    System.out.println("Created by: " + profile.getCreatedBy());
                    System.out.println("Created at: " + profile.getCreationTime());
                }
            }
        }
    }
}
