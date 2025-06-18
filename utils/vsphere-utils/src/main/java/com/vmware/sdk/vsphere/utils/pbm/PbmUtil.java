/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.vsphere.utils.pbm;

import java.util.List;

import com.vmware.pbm.InvalidArgumentFaultMsg;
import com.vmware.pbm.PbmCapabilityMetadata;
import com.vmware.pbm.PbmCapabilityMetadataPerCategory;
import com.vmware.pbm.PbmCapabilityProfile;
import com.vmware.pbm.PbmPortType;
import com.vmware.pbm.PbmProfile;
import com.vmware.pbm.PbmProfileId;
import com.vmware.pbm.PbmProfileResourceType;
import com.vmware.pbm.PbmProfileResourceTypeEnum;
import com.vmware.pbm.PbmServiceInstanceContent;
import com.vmware.pbm.RuntimeFaultFaultMsg;

/** Utility class for PBM Samples */
public class PbmUtil {

    /**
     * This method returns the Profile Spec for the given Storage Profile name
     *
     * @param pbmPort PBM Port for the call
     * @param pbmServiceInstanceContent PBM Service Instance Content
     * @param name name of the policy based management profile
     * @return VirtualMachineDefinedProfileSpec for this name
     * @throws InvalidArgumentFaultMsg if the name has unacceptable format and a call won't be made
     * @throws RuntimeFaultFaultMsg if the call was unsuccessful (for ex. if the profile does not exist)
     */
    public static PbmCapabilityProfile getPbmProfile(
            PbmPortType pbmPort, PbmServiceInstanceContent pbmServiceInstanceContent, String name)
            throws InvalidArgumentFaultMsg, RuntimeFaultFaultMsg {
        List<PbmProfileId> profileIds = pbmPort.pbmQueryProfile(
                pbmServiceInstanceContent.getProfileManager(), PbmUtil.getStorageResourceType(), null);

        if (profileIds == null || profileIds.isEmpty()) {
            throw new RuntimeFaultFaultMsg("No storage Profiles exist.");
        }
        List<PbmProfile> pbmProfiles =
                pbmPort.pbmRetrieveContent(pbmServiceInstanceContent.getProfileManager(), profileIds);
        for (PbmProfile pbmProfile : pbmProfiles) {
            if (pbmProfile.getName().equals(name)) {
                return (PbmCapabilityProfile) pbmProfile;
            }
        }
        throw new RuntimeFaultFaultMsg("Profile with the given name does not exist");
    }

    /** @return the Storage Resource Type Object */
    public static PbmProfileResourceType getStorageResourceType() {
        PbmProfileResourceType resourceType = new PbmProfileResourceType();
        resourceType.setResourceType(PbmProfileResourceTypeEnum.STORAGE.value());
        return resourceType;
    }

    /**
     * Finds the Capability Metadata associated to a Tag Category
     *
     * @param tagCategoryName name of the tag category
     * @param schema list of capability metadata to search in
     * @return the {@link PbmCapabilityMetadata} instance if found, otherwise null
     */
    public static PbmCapabilityMetadata getTagCategoryMeta(
            String tagCategoryName, List<PbmCapabilityMetadataPerCategory> schema) {
        for (PbmCapabilityMetadataPerCategory cat : schema) {
            if (cat.getSubCategory().equals("tag")) {
                for (PbmCapabilityMetadata cap : cat.getCapabilityMetadata()) {
                    if (cap.getId().getId().equals(tagCategoryName)) {
                        return cap;
                    }
                }
            }
        }
        return null;
    }
}
