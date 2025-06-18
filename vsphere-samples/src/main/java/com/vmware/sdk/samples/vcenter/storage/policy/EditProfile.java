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
import com.vmware.pbm.PbmCapabilityConstraintInstance;
import com.vmware.pbm.PbmCapabilityDescription;
import com.vmware.pbm.PbmCapabilityDiscreteSet;
import com.vmware.pbm.PbmCapabilityInstance;
import com.vmware.pbm.PbmCapabilityMetadata;
import com.vmware.pbm.PbmCapabilityMetadataPerCategory;
import com.vmware.pbm.PbmCapabilityProfile;
import com.vmware.pbm.PbmCapabilityProfileUpdateSpec;
import com.vmware.pbm.PbmCapabilityPropertyInstance;
import com.vmware.pbm.PbmCapabilityPropertyMetadata;
import com.vmware.pbm.PbmCapabilitySubProfile;
import com.vmware.pbm.PbmCapabilitySubProfileConstraints;
import com.vmware.pbm.PbmPortType;
import com.vmware.pbm.PbmProfile;
import com.vmware.pbm.PbmProfileId;
import com.vmware.pbm.PbmServiceInstanceContent;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.pbm.PbmUtil;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;

/**
 * This sample updates a Tag-Based Storage Profile. Adds or deletes rulesets.
 *
 * <p>Adds a ruleset based on tags from a tag category.
 *
 * <p>Deletes a ruleset from a storage profile.
 */
public class EditProfile {
    private static final Logger log = LoggerFactory.getLogger(EditProfile.class);

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

    /**
     * OPTIONAL: Flag to denote addition of a rule set. Requires tag_category property to be specified. Default value
     * assumed: false.
     */
    public static Boolean add = null;
    /**
     * OPTIONAL: Flag to denote deletion of a rule set. Requires ruleset_name property to be specified. Default value
     * assumed: false.
     */
    public static Boolean delete = null;
    /** OPTIONAL: Category Name of the tags. All tags in this category are added to the rule. */
    public static String tagCategory = null;
    /** REQUIRED: Name of an existing storage profile. */
    public static String profileName = "profileName";
    /** OPTIONAL: Name of the rule-set to be deleted. */
    public static String ruleSetName = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(EditProfile.class, args);

        // Validate Arguments
        if (!Boolean.TRUE.equals(add) && !Boolean.TRUE.equals(delete)) {
            throw new InvalidArgumentFaultMsg("Either of add or delete argument is required.");
        }

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            PbmPortType pbmPort = client.getPbmPort();

            // Step 1: Get PBM Profile Manager & Associated Capability Metadata
            PbmServiceInstanceContent serviceInstanceContent = client.getPbmServiceInstanceContent();
            ManagedObjectReference profileMgrMoRef = serviceInstanceContent.getProfileManager();

            // Get PBM Supported Capability Metadata
            List<PbmCapabilityMetadataPerCategory> metadata =
                    pbmPort.pbmFetchCapabilityMetadata(profileMgrMoRef, PbmUtil.getStorageResourceType(), null);

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

            // Step 3: Retrieve Existing RuleSets
            PbmCapabilitySubProfileConstraints constraints =
                    (PbmCapabilitySubProfileConstraints) profile.getConstraints();
            List<PbmCapabilitySubProfile> ruleSets = constraints.getSubProfiles();

            // Step 4: Add a Rule-Set based on the tag Category
            if (Boolean.TRUE.equals(add)) {
                if (tagCategory == null) {
                    throw new InvalidArgumentFaultMsg("Missing tag_category option with add");
                }

                // Create Tag Instances based on Category
                PbmCapabilityMetadata tagCategoryInfo = PbmUtil.getTagCategoryMeta(tagCategory, metadata);
                if (tagCategoryInfo == null) {
                    throw new InvalidArgumentFaultMsg("Specified Tag Category '" + tagCategory + "' does not exist");
                }

                // Fetch Metadata for Tag Category
                List<PbmCapabilityPropertyMetadata> propMetaList = tagCategoryInfo.getPropertyMetadata();
                PbmCapabilityPropertyMetadata propMeta = propMetaList.get(0);

                // Create a New Property Instance based on the Tag Category ID
                PbmCapabilityPropertyInstance prop = new PbmCapabilityPropertyInstance();
                prop.setId(propMeta.getId());

                // Fetch Allowed Tag Values Metadata
                PbmCapabilityDiscreteSet tagSetMeta = (PbmCapabilityDiscreteSet) propMeta.getAllowedValue();
                if (tagSetMeta == null || tagSetMeta.getValues().isEmpty()) {
                    throw new RuntimeFaultFaultMsg("Specified Tag Category does not have any associated tags");
                }

                // Create a New Discrete Set for holding Tag Values
                PbmCapabilityDiscreteSet tagSet = new PbmCapabilityDiscreteSet();
                for (Object obj : tagSetMeta.getValues()) {
                    tagSet.getValues().add(((PbmCapabilityDescription) obj).getValue());
                }
                prop.setValue(tagSet);

                // Associate Tag Instance with a Rule
                PbmCapabilityConstraintInstance rule = new PbmCapabilityConstraintInstance();
                rule.getPropertyInstance().add(prop);

                // Associate Rule with a Capability Instance
                PbmCapabilityInstance capability = new PbmCapabilityInstance();
                capability.setId(tagCategoryInfo.getId());
                capability.getConstraint().add(rule);

                // Add Rule to a RuleSet
                PbmCapabilitySubProfile ruleSet = new PbmCapabilitySubProfile();
                ruleSet.setName("Rule-Set " + (ruleSets.size() + 1));
                ruleSet.getCapability().add(capability);

                // Add Rule-Set to Existing Rule-Sets
                ruleSets.add(ruleSet);
            }

            // Step 5: Delete a specified Rule-Set
            if (Boolean.TRUE.equals(delete)) {
                if (ruleSetName == null) {
                    throw new InvalidArgumentFaultMsg("Missing ruleset_name option with delete");
                }

                PbmCapabilitySubProfile deleteRuleSet = null;
                for (PbmCapabilitySubProfile ruleSet : ruleSets) {
                    if (ruleSet.getName().equals(ruleSetName)) {
                        deleteRuleSet = ruleSet;
                    }
                }

                if (deleteRuleSet == null) {
                    throw new RuntimeFaultFaultMsg("Specified Rule-Set name " + ruleSetName + " does not exist");
                } else if (ruleSets.size() == 1) {
                    throw new RuntimeFaultFaultMsg(
                            "Cannot delete the ruleset. At least one ruleset is required for a profile.");
                } else {
                    ruleSets.remove(deleteRuleSet);
                }
            }

            // Step 6: Build Capability-Based Profile Update Spec
            PbmCapabilityProfileUpdateSpec spec = new PbmCapabilityProfileUpdateSpec();
            spec.setName(profileName);
            spec.setDescription("Tag Based Storage Profile Created by SDK Samples. Rule based on tags from Category "
                    + tagCategory);
            spec.setConstraints(constraints);

            // Step 7: Update Storage Profile
            pbmPort.pbmUpdate(profileMgrMoRef, profile.getProfileId(), spec);
            log.info("Profile {} Updated.", profileName);
        }
    }
}
