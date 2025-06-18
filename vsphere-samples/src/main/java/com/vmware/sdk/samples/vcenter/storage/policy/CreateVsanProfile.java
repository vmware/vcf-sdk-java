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
import com.vmware.pbm.PbmCapabilityConstraintInstance;
import com.vmware.pbm.PbmCapabilityInstance;
import com.vmware.pbm.PbmCapabilityMetadata;
import com.vmware.pbm.PbmCapabilityMetadataPerCategory;
import com.vmware.pbm.PbmCapabilityProfileCreateSpec;
import com.vmware.pbm.PbmCapabilityPropertyInstance;
import com.vmware.pbm.PbmCapabilitySubProfile;
import com.vmware.pbm.PbmCapabilitySubProfileConstraints;
import com.vmware.pbm.PbmCapabilityVendorNamespaceInfo;
import com.vmware.pbm.PbmCapabilityVendorResourceTypeInfo;
import com.vmware.pbm.PbmPortType;
import com.vmware.pbm.PbmProfileId;
import com.vmware.pbm.PbmServiceInstanceContent;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.pbm.PbmUtil;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;

/** This sample creates a new Storage Profile with one rule-set based on vSAN Capabilities. */
public class CreateVsanProfile {
    private static final Logger log = LoggerFactory.getLogger(CreateVsanProfile.class);

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

    /** REQUIRED: Minimum stripe width of each mirror. */
    public static Integer stripeWidth = 0;
    /** REQUIRED: Name of the storage profile. */
    public static String profileName = "profileName";
    /** OPTIONAL: If set, the object will be provisioned even if the policy is not satisfiable. Default value false. */
    public static Boolean forceProvision = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(CreateVsanProfile.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            PbmPortType pbmPort = client.getPbmPort();
            PbmServiceInstanceContent serviceInstanceContent = client.getPbmServiceInstanceContent();
            ManagedObjectReference profileMgrMoRef = serviceInstanceContent.getProfileManager();

            // Step 1: Check if there is a vSAN Provider
            boolean vSanCapabale = false;
            List<PbmCapabilityVendorResourceTypeInfo> vendorInfo = pbmPort.pbmFetchVendorInfo(profileMgrMoRef, null);
            for (PbmCapabilityVendorResourceTypeInfo vendor : vendorInfo) {
                for (PbmCapabilityVendorNamespaceInfo namespaceInfo : vendor.getVendorNamespaceInfo()) {
                    if (namespaceInfo.getNamespaceInfo().getNamespace().equalsIgnoreCase("vSan")) {
                        vSanCapabale = true;
                        break;
                    }
                }
            }

            if (!vSanCapabale) {
                throw new RuntimeFaultFaultMsg("Cannot create storage profile. vSAN Provider not found.");
            }

            // Step 2: Get PBM Supported Capability Metadata
            List<PbmCapabilityMetadataPerCategory> metadata =
                    pbmPort.pbmFetchCapabilityMetadata(profileMgrMoRef, PbmUtil.getStorageResourceType(), null);

            // Step 3: Add Provider Specific Capabilities
            List<PbmCapabilityInstance> capabilities = new ArrayList<>();
            capabilities.add(buildCapability("stripeWidth", stripeWidth, metadata));
            if (Boolean.TRUE.equals(forceProvision)) {
                capabilities.add(buildCapability("forceProvisioning", true, metadata));
            }

            // Step 4: Add Capabilities to a RuleSet
            PbmCapabilitySubProfile ruleSet = new PbmCapabilitySubProfile();
            ruleSet.getCapability().addAll(capabilities);

            // Step 5: Add Rule-Set to Capability Constraints
            PbmCapabilitySubProfileConstraints constraints = new PbmCapabilitySubProfileConstraints();
            ruleSet.setName("Rule-Set " + (constraints.getSubProfiles().size() + 1));
            constraints.getSubProfiles().add(ruleSet);

            // Step 6: Build Capability-Based Profile
            PbmCapabilityProfileCreateSpec spec = new PbmCapabilityProfileCreateSpec();
            spec.setName(profileName);
            spec.setDescription("Storage Profile Created by SDK Samples. Rule based on vSAN capability");
            spec.setResourceType(PbmUtil.getStorageResourceType());
            spec.setConstraints(constraints);

            // Step 7: Create Storage Profile
            PbmProfileId profile = pbmPort.pbmCreate(profileMgrMoRef, spec);
            log.info("Profile {} created with ID: {}", profileName, profile.getUniqueId());
        }
    }

    /** This method builds a capability instance based on the capability name associated with a vSAN provider. */
    private static PbmCapabilityInstance buildCapability(
            String capabilityName, Object value, List<PbmCapabilityMetadataPerCategory> metadata)
            throws InvalidArgumentFaultMsg {

        // Create Property Instance with capability stripeWidth
        PbmCapabilityMetadata capabilityMeta = getCapabilityMeta(capabilityName, metadata);
        if (capabilityMeta == null) {
            throw new InvalidArgumentFaultMsg("Specified Capability does not exist");
        }

        // Create a New Property Instance based on the Stripe Width Capability
        PbmCapabilityPropertyInstance propertyInstance = new PbmCapabilityPropertyInstance();
        propertyInstance.setId(capabilityName);
        propertyInstance.setValue(value);

        // Associate Property Instance with a Rule
        PbmCapabilityConstraintInstance rule = new PbmCapabilityConstraintInstance();
        rule.getPropertyInstance().add(propertyInstance);

        // Associate Rule with a Capability Instance
        PbmCapabilityInstance capability = new PbmCapabilityInstance();
        capability.setId(capabilityMeta.getId());
        capability.getConstraint().add(rule);

        return capability;
    }

    /**
     * Returns the Capability Metadata for the specified capability name.
     *
     * @param capabilityName name of the capability
     * @param schema list of capability metadata to search in (including the name)
     * @return the {@link PbmCapabilityMetadata} instance if found, otherwise null
     */
    public static PbmCapabilityMetadata getCapabilityMeta(
            String capabilityName, List<PbmCapabilityMetadataPerCategory> schema) {
        for (PbmCapabilityMetadataPerCategory cat : schema) {
            for (PbmCapabilityMetadata cap : cat.getCapabilityMetadata()) {
                if (cap.getId().getId().equals(capabilityName)) {
                    return cap;
                }
            }
        }
        return null;
    }
}
