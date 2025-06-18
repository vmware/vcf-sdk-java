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
import com.vmware.pbm.PbmCapabilityProfileCreateSpec;
import com.vmware.pbm.PbmCapabilityPropertyInstance;
import com.vmware.pbm.PbmCapabilityPropertyMetadata;
import com.vmware.pbm.PbmCapabilitySubProfile;
import com.vmware.pbm.PbmCapabilitySubProfileConstraints;
import com.vmware.pbm.PbmPortType;
import com.vmware.pbm.PbmProfileId;
import com.vmware.pbm.PbmServiceInstanceContent;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.pbm.PbmUtil;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;

/**
 * This sample creates a new Tag-Based Storage Profile with one rule-set. The rule-set contains a rule based on tags
 * from a tag-category.
 */
public class CreateProfile {
    private static final Logger log = LoggerFactory.getLogger(CreateProfile.class);

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

    /** REQUIRED: Category Name of the tags. All tags in this category are added to the rule. */
    public static String tagCategory = "tagCategory";
    /** REQUIRED: Name of the storage profile. */
    public static String profileName = "profileName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(CreateProfile.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            PbmPortType pbmPort = client.getPbmPort();
            PbmServiceInstanceContent serviceInstanceContent = client.getPbmServiceInstanceContent();
            ManagedObjectReference profileMgr = serviceInstanceContent.getProfileManager();

            List<PbmCapabilityMetadataPerCategory> metadata =
                    pbmPort.pbmFetchCapabilityMetadata(profileMgr, PbmUtil.getStorageResourceType(), null);

            // Step 1: Create Property Instance with tags from the specified Category
            PbmCapabilityMetadata tagCategoryInfo = PbmUtil.getTagCategoryMeta(tagCategory, metadata);
            if (tagCategoryInfo == null) {
                throw new InvalidArgumentFaultMsg("Specified Tag Category does not exist");
            }

            // Fetch Property Metadata of the Tag Category
            List<PbmCapabilityPropertyMetadata> propMetaList = tagCategoryInfo.getPropertyMetadata();
            PbmCapabilityPropertyMetadata propMeta = propMetaList.get(0);

            // Create a New Property Instance based on the Tag Category ID
            PbmCapabilityPropertyInstance prop = new PbmCapabilityPropertyInstance();
            prop.setId(propMeta.getId());

            // Fetch Allowed Tag Values Metadata
            PbmCapabilityDiscreteSet tagSetMeta = (PbmCapabilityDiscreteSet) propMeta.getAllowedValue();
            if (tagSetMeta == null || tagSetMeta.getValues().isEmpty()) {
                throw new RuntimeFaultFaultMsg(
                        "Specified Tag Category '" + tagCategory + "' does not have any associated tags");
            }

            // Create a New Discrete Set for holding Tag Values
            PbmCapabilityDiscreteSet tagSet = new PbmCapabilityDiscreteSet();
            for (Object obj : tagSetMeta.getValues()) {
                tagSet.getValues().add(((PbmCapabilityDescription) obj).getValue());
            }
            prop.setValue(tagSet);

            // Step 2: Associate Property Instance with a Rule
            PbmCapabilityConstraintInstance rule = new PbmCapabilityConstraintInstance();
            rule.getPropertyInstance().add(prop);

            // Step 3: Associate Rule with a Capability Instance
            PbmCapabilityInstance capability = new PbmCapabilityInstance();
            capability.setId(tagCategoryInfo.getId());
            capability.getConstraint().add(rule);

            // Step 4: Add Rule to a RuleSet
            PbmCapabilitySubProfile ruleSet = new PbmCapabilitySubProfile();
            ruleSet.getCapability().add(capability);

            // Step 5: Add Rule-Set to Capability Constraints
            PbmCapabilitySubProfileConstraints constraints = new PbmCapabilitySubProfileConstraints();
            ruleSet.setName("Rule-Set" + (constraints.getSubProfiles().size() + 1));
            constraints.getSubProfiles().add(ruleSet);

            // Step 6: Build Capability-Based Profile
            PbmCapabilityProfileCreateSpec spec = new PbmCapabilityProfileCreateSpec();
            spec.setName(profileName);
            spec.setDescription("Tag Based Storage Profile Created by SDK Samples. Rule based on tags from Category "
                    + tagCategory);
            spec.setResourceType(PbmUtil.getStorageResourceType());
            spec.setConstraints(constraints);

            // Step 7: Create Storage Profile
            PbmProfileId profile = pbmPort.pbmCreate(profileMgr, spec);
            log.info("Profile {} created with ID: {}", profileName, profile.getUniqueId());
        }
    }
}
