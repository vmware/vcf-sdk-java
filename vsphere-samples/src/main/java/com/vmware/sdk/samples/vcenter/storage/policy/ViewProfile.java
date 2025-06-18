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

import com.vmware.pbm.InvalidArgumentFaultMsg;
import com.vmware.pbm.PbmCapabilityConstraintInstance;
import com.vmware.pbm.PbmCapabilityDiscreteSet;
import com.vmware.pbm.PbmCapabilityInstance;
import com.vmware.pbm.PbmCapabilityProfile;
import com.vmware.pbm.PbmCapabilityPropertyInstance;
import com.vmware.pbm.PbmCapabilityRange;
import com.vmware.pbm.PbmCapabilitySubProfile;
import com.vmware.pbm.PbmCapabilitySubProfileConstraints;
import com.vmware.pbm.PbmPlacementHub;
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

/** This sample prints the contents of a storage Profile. */
public class ViewProfile {
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
        SampleCommandLineParser.load(ViewProfile.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            PbmPortType pbmPort = client.getPbmPort();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(client.getVimPort());

            // Step 1: Get PBM Profile Manager & Associated Capability Metadata
            PbmServiceInstanceContent pbmServiceInstanceContent = client.getPbmServiceInstanceContent();
            ManagedObjectReference profileMgrMoRef = pbmServiceInstanceContent.getProfileManager();
            ManagedObjectReference placementSolverMoRef = pbmServiceInstanceContent.getPlacementSolver();

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

            // Step 3: Print the basic information of the Profile
            System.out.println("Profile Name: " + profile.getName());
            System.out.println("Profile Id: " + profile.getProfileId().getUniqueId());
            System.out.println("Description: " + profile.getDescription());

            // Step 4: Fetch Rulesets defined in the profile
            PbmCapabilitySubProfileConstraints constraints =
                    (PbmCapabilitySubProfileConstraints) profile.getConstraints();

            List<PbmCapabilitySubProfile> ruleSets = constraints.getSubProfiles();
            System.out.println("\nNo. of Rule-Sets: " + ruleSets.size());
            System.out.println("List of Rules");
            System.out.println("-------------");

            for (PbmCapabilitySubProfile ruleSet : ruleSets) {
                System.out.println("RuleSet Name: '" + ruleSet.getName() + "'");
                for (PbmCapabilityInstance capability : ruleSet.getCapability()) {
                    for (PbmCapabilityConstraintInstance rule : capability.getConstraint()) {
                        for (PbmCapabilityPropertyInstance prop : rule.getPropertyInstance()) {
                            if (capability.getId().getNamespace().contains("tag")) {
                                System.out.println(
                                        " Tag Category: " + capability.getId().getId());
                                System.out.println(" Selected Tags:");

                                PbmCapabilityDiscreteSet tagSet = (PbmCapabilityDiscreteSet) prop.getValue();
                                for (Object tag : tagSet.getValues()) {
                                    System.out.println(" " + tag);
                                }
                            }
                            if (capability.getId().getNamespace().contains("vSan")) {
                                System.out.println(
                                        " Capability: " + capability.getId().getId());
                                if (capability.getId().getId().equals("proportionalCapacity")) {
                                    PbmCapabilityRange range = (PbmCapabilityRange) prop.getValue();
                                    System.out.println(" Min: " + range.getMin() + ", Max: " + range.getMax());
                                } else {
                                    System.out.println(" Value: " + prop.getValue());
                                }
                            }
                            System.out.println(" ---");
                        }
                    }
                }
            }

            // Step 5: Print Associated VM's
            List<PbmServerObjectRef> entities =
                    pbmPort.pbmQueryAssociatedEntity(profileMgrMoRef, profile.getProfileId(), "virtualMachine");

            System.out.println("\nNo. of Associated VM's: " + entities.size());
            if (!entities.isEmpty()) {
                System.out.println("List of VM's");
                System.out.println("----------- ");
                for (PbmServerObjectRef entity : entities) {
                    ManagedObjectReference moRef = new ManagedObjectReference();
                    moRef.setType("VirtualMachine");
                    moRef.setValue(entity.getKey());

                    String name = propertyCollectorHelper.fetch(moRef, "name");
                    System.out.println(name);
                }
            }

            // Step 6: Print Matching Resources (e.g. Datastores)
            List<PbmPlacementHub> hubs =
                    pbmPort.pbmQueryMatchingHub(placementSolverMoRef, null, profile.getProfileId());
            System.out.println("\nNo. of Matching Resources: " + hubs.size());
            if (!hubs.isEmpty()) {
                System.out.println("List of Resources:");
                System.out.println("----------- ");

                for (PbmPlacementHub hub : hubs) {
                    ManagedObjectReference moRef = new ManagedObjectReference();
                    moRef.setType(hub.getHubType());
                    moRef.setValue(hub.getHubId());

                    String name = propertyCollectorHelper.fetch(moRef, "name");
                    System.out.println(name + " : " + hub.getHubType());
                }
            }
        }
    }
}
