/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.storage.vslm.fcd;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.storage.fcd.helpers.FcdHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ID;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VslmTagEntry;
import com.vmware.vslm.VslmPortType;

/**
 * This sample executes below tag related operations on a given VStorageObject from vslm:
 *
 * <ol>
 *   <li>Attach a tag to a virtual storage object.
 *   <li>List all tags attached to virtual storage object.
 *   <li>List all virtual storage objects attached to the tag.
 *   <li>Detach a tag from a virtual storage object.
 * </ol>
 *
 * <p>Sample Prerequisites:
 *
 * <ul>
 *   <li>Existing VStorageObject name
 *   <li>Existing tag name
 *   <li>Existing category name
 * </ul>
 */
public class FcdTagOperations {
    private static final Logger log = LoggerFactory.getLogger(FcdTagOperations.class);

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

    /** REQUIRED: UUID of the vstorageobject. */
    public static String vStorageObjectId = "vStorageObjectId";
    /** REQUIRED: The category to which the tag belongs. */
    public static String category = "category";
    /** REQUIRED: The tag which has to be associated with the virtual storage object. */
    public static String tag = "tag";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FcdTagOperations.class, args);

        VcenterClientFactory clientFactory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = clientFactory.createClient(username, password, null)) {
            VslmPortType vslmPort = client.getVslmPort();

            ManagedObjectReference vStorageObjectManager =
                    client.getVslmServiceInstanceContent().getVStorageObjectManager();

            log.info("Invoking Tagging related APIs from VSLM ::");

            // Attach a tag to a virtual storage object.
            log.info("Operation: Attach a tag to a virtual storage object from vslm.");
            vslmPort.vslmAttachTagToVStorageObject(
                    vStorageObjectManager, FcdHelper.makeId(vStorageObjectId), category, tag);

            // Verify attachTagToVStorageObject operation by invoking listVStorageObjectsAttachedToTag
            log.info("Operation: List all vStorageObjects attached to a tag from vslm.");
            List<ID> retrievedVStrObjIdList =
                    vslmPort.vslmListVStorageObjectsAttachedToTag(vStorageObjectManager, category, tag);

            List<String> retrivedFcdIdList = new ArrayList<>();
            for (ID retrievedId : retrievedVStrObjIdList) {
                retrivedFcdIdList.add(retrievedId.getId());
            }

            if (!retrievedVStrObjIdList.isEmpty() && retrivedFcdIdList.contains(vStorageObjectId)) {
                log.info(
                        "vStorageObject list returned by listVStorageObjectsAttachedToTag contains vStorageObject :: [ {} ] from vslm. \nStorage id :: [ {} ]",
                        retrievedVStrObjIdList.size(),
                        vStorageObjectId);
            } else {
                String message =
                        "Error: VStorageObject [ " + vStorageObjectId + " ] is not present in the list returned by"
                                + " listVStorageObjectsAttachedToTag for category " + category + " and tag " + tag
                                + " from vslm.";
                throw new RuntimeException(message);
            }

            // Verify attachTagToVStorageObject by invoking listTagsAttachedToVStorageObject
            log.info("Operation: List all tags attached to a vStorageObject from vslm.");
            List<VslmTagEntry> retrievedTagList = vslmPort.vslmListTagsAttachedToVStorageObject(
                    vStorageObjectManager, FcdHelper.makeId(vStorageObjectId));

            if (!retrievedTagList.isEmpty()) {
                List<String> categoryList = new ArrayList<>();
                List<String> tagList = new ArrayList<>();
                for (VslmTagEntry retrievedTag : retrievedTagList) {
                    categoryList.add(retrievedTag.getParentCategoryName());
                    tagList.add(retrievedTag.getTagName());
                }
                if (categoryList.contains(category) && tagList.contains(tag)) {
                    log.info(
                            "Tag list of size [ {} ] returned by listTagsAttachedToVStorageObject for vStorageObject :: [ {} ] contains category :: [ {} ] and tag :: [ {} ]\n",
                            retrievedVStrObjIdList.size(),
                            vStorageObjectId,
                            category,
                            tag);
                }
            } else {
                String message = "Error: Category [ " + category + " ] and tag [ " + tag
                        + " ] is not present in the list returned by" + " listTagsAttachedToVStorageObject"
                        + " for vStorageObject [ " + vStorageObjectId + " ] from vslm.";
                throw new RuntimeException(message);
            }

            // Detaching a tag from a virtual storage object.
            log.info("Operation: Detach a tag from a virtual storage object.");
            vslmPort.vslmDetachTagFromVStorageObject(
                    vStorageObjectManager, FcdHelper.makeId(vStorageObjectId), category, tag);

            // Verify detachTagFromVStorageObject API by invoking listVStorageObjectsAttachedToTag
            log.info("Operation: List all vStorageObject attached to tag from vslm.");
            List<ID> retrievedVStrObjIdListAfterDeletion =
                    vslmPort.vslmListVStorageObjectsAttachedToTag(vStorageObjectManager, category, tag);
            if (!retrievedVStrObjIdListAfterDeletion.contains(FcdHelper.makeId(vStorageObjectId))) {
                log.info(
                        "vStorageObject list returned by listVStorageObjectsAttachedToTag doesn't contain vstorageObject :: [ {} ].\n",
                        vStorageObjectId);
            } else {
                String message = "Error: VStorageObject [ " + vStorageObjectId
                        + " ] is still present in the list returned by"
                        + " listVStorageObjectsAttachedToTag for category "
                        + category + " and tag " + tag + " from vslm.";
                throw new RuntimeException(message);
            }

            // Verify detachTagFromVStorageObject API by invoking listTagsAttachedToVStorageObject
            log.info("Operation: List all tags attached to vStorageObject from vslm.");
            List<VslmTagEntry> retrievedTagListAfterDeletion = vslmPort.vslmListTagsAttachedToVStorageObject(
                    vStorageObjectManager, FcdHelper.makeId(vStorageObjectId));

            List<String> categoryListAfterDeletion = new ArrayList<>();
            List<String> tagListAfterDeletion = new ArrayList<>();
            if (!retrievedTagListAfterDeletion.isEmpty()) {
                for (VslmTagEntry retrievedTag : retrievedTagListAfterDeletion) {
                    categoryListAfterDeletion.add(retrievedTag.getParentCategoryName());
                    tagListAfterDeletion.add(retrievedTag.getTagName());
                }
            }
            if (retrievedTagListAfterDeletion.isEmpty()
                    || !(categoryListAfterDeletion.contains(category) && tagListAfterDeletion.contains(tag))) {
                log.info(
                        "Tag list returned by listTagsAttachedToVStorageObject for vStorageObject :: [ {} ] doesn't contain category :: [ {} ] and tag :: [ {} ].\n",
                        vStorageObjectId,
                        category,
                        tag);
            }
        }
    }
}
