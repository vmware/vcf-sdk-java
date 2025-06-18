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

import com.vmware.pbm.InvalidArgumentFaultMsg;
import com.vmware.pbm.PbmCapabilityProfile;
import com.vmware.pbm.PbmComplianceResult;
import com.vmware.pbm.PbmPortType;
import com.vmware.pbm.PbmProfile;
import com.vmware.pbm.PbmProfileId;
import com.vmware.pbm.PbmServerObjectRef;
import com.vmware.pbm.PbmServiceInstanceContent;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.pbm.PbmUtil;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;

/** This sample checks the compliance of the VM's associated with a storage profile. */
public class CheckCompliance {
    private static final Logger log = LoggerFactory.getLogger(CheckCompliance.class);

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
        SampleCommandLineParser.load(CheckCompliance.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(client.getVimPort());
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
                log.info("FOUND PROFILE: {}", pbmProfile.getName());
                if (pbmProfile.getName().equals(profileName)) {
                    profile = (PbmCapabilityProfile) pbmProfile;
                }
            }
            if (profile == null) {
                throw new InvalidArgumentFaultMsg("Specified storage profile name does not exist." + profileName);
            }

            // Step 3: Retrieve Associated Entities
            List<PbmServerObjectRef> entities = pbmPort.pbmQueryAssociatedEntity(
                    serviceInstanceContent.getProfileManager(), profile.getProfileId(), "virtualMachine");

            // Step 4: Check Compliance Results of associated entities
            if (entities.isEmpty()) {
                log.info("Storage Profile should have associated VM's.");
                return;
            }

            List<PbmComplianceResult> complianceResults = pbmPort.pbmCheckCompliance(
                    serviceInstanceContent.getComplianceManager(), entities, profile.getProfileId());

            for (PbmComplianceResult result : complianceResults) {
                ManagedObjectReference moRef = new ManagedObjectReference();
                moRef.setType("VirtualMachine");
                moRef.setValue(result.getEntity().getKey());
                String vmName = propertyCollectorHelper.fetch(moRef, "name");

                log.info(
                        "Compliance status of VM {}: {}",
                        vmName,
                        result.getComplianceStatus().toUpperCase());
            }
        }
    }
}
