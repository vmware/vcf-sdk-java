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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.pbm.InvalidArgumentFaultMsg;
import com.vmware.pbm.PbmCapabilityProfile;
import com.vmware.pbm.PbmPortType;
import com.vmware.pbm.PbmProfile;
import com.vmware.pbm.PbmProfileId;
import com.vmware.pbm.PbmProfileOperationOutcome;
import com.vmware.pbm.PbmServiceInstanceContent;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.pbm.PbmUtil;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;

/** This sample deletes a Storage Profile. */
public class DeleteProfile {
    private static final Logger log = LoggerFactory.getLogger(DeleteProfile.class);

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

    /** REQUIRED: Name of the storage profile. */
    public static String profileName = "profileName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(DeleteProfile.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            PbmPortType pbmPort = client.getPbmPort();

            // Step 1: Get PBM Profile Manager & Associated Capability Metadata
            PbmServiceInstanceContent serviceInstanceContent = client.getPbmServiceInstanceContent();
            ManagedObjectReference profileMgrMoRef = serviceInstanceContent.getProfileManager();

            // Step 2: Search for the given Profile Name
            List<PbmProfileId> profileIds =
                    pbmPort.pbmQueryProfile(profileMgrMoRef, PbmUtil.getStorageResourceType(), null);
            if (profileIds == null || profileIds.isEmpty()) {
                throw new RuntimeFaultFaultMsg("No storage Profiles exist.");
            }

            List<PbmProfile> pbmProfiles = pbmPort.pbmRetrieveContent(profileMgrMoRef, profileIds);
            PbmCapabilityProfile profile = null;
            for (PbmProfile pbmProfile : pbmProfiles) {
                if (pbmProfile.getName().equals(profileName)) {
                    profile = (PbmCapabilityProfile) pbmProfile;
                }
            }
            if (profile == null) {
                throw new InvalidArgumentFaultMsg("Specified storage profile name does not exist.");
            }

            // Step 3: Delete the profile
            List<PbmProfileId> deleteProfiles = new ArrayList<>();
            deleteProfiles.add(profile.getProfileId());
            List<PbmProfileOperationOutcome> results = pbmPort.pbmDelete(profileMgrMoRef, deleteProfiles);

            // Step 4: Check the Outcome
            for (PbmProfileOperationOutcome result : results) {
                if (result.getFault() != null) {
                    throw new RuntimeFaultFaultMsg(
                            "Unable to delete the Profile." + result.getFault().getFault(), (Throwable) null);
                }
            }
            // Step 5: Print the contents of the Profile
            log.info("Profile {} deleted. ", profileName);
        }
    }
}
