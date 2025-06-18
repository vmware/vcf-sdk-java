/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.compute.contentlibrary.contentupdate;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.content.Library;
import com.vmware.content.LibraryModel;
import com.vmware.content.LibraryTypes;
import com.vmware.content.LocalLibrary;
import com.vmware.content.library.Item;
import com.vmware.content.library.ItemModel;
import com.vmware.content.library.StorageBacking;
import com.vmware.content.library.item.UpdateSession;
import com.vmware.content.library.item.UpdateSessionModel;
import com.vmware.content.library.item.updatesession.FileTypes;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.compute.contentlibrary.helpers.ItemUploadHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;

/**
 * Demonstrates content library item content updates using UpdateSession API.
 *
 * <p>Sample Prerequisites: This sample needs an existing content library to create and update library item.
 */
public class ContentUpdate {
    private static final Logger log = LoggerFactory.getLogger(ContentUpdate.class);
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

    /** REQUIRED: The name of the content library where the library item will be created. */
    public static String libName = "libName";

    private static final String OVF_ITEM_TYPE = "ovf";
    private static final String ISO_ITEM_TYPE = "iso";

    private static final String OVF_ITEM_ONE_FOLDER_NAME = "simpleVmTemplate";
    private static final String OVF_ITEM_ONE_OVF_FILE_NAME = "descriptor.ovf";
    private static final String OVF_ITEM_ONE_VMDK_FILE_NAME = "disk-0.vmdk";
    private static final String OVF_ITEM_TWO_FOLDER_NAME = "plainVmTemplate";
    private static final String OVF_ITEM_TWO_OVF_FILE_NAME = "plain-vm.ovf";
    private static final String OVF_ITEM_TWO_VMDK_FILE_NAME = "plain-vm.vmdk";
    private static final String ISO_ITEM_FOLDER_NAME = "isoImages";
    private static final String ISO_ITEM_ONE_ISO_FILE_NAME = "test.iso";
    private static final String ISO_ITEM_TWO_ISO_FILE_NAME = "small.iso";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(ContentUpdate.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Library libraryService = client.createStub(Library.class);
            LocalLibrary localLibraryService = client.createStub(LocalLibrary.class);
            Item itemService = client.createStub(Item.class);
            UpdateSession updateSessionService = client.createStub(UpdateSession.class);

            var updateSessionFileService = client.createStub(com.vmware.content.library.item.updatesession.File.class);

            // Find the content library id by name
            if (libName == null || libName.trim().isEmpty()) {
                throw new RuntimeException("Local library name must be provided");
            }

            LibraryTypes.FindSpec findSpec = new LibraryTypes.FindSpec();
            findSpec.setName(libName);

            List<String> libraryIds = libraryService.find(findSpec);
            if (libraryIds.isEmpty()) {
                throw new RuntimeException("Unable to find a library with name: " + libName);
            }
            String libraryId = libraryIds.get(0);
            log.info("Found library : {}", libraryId);

            LibraryModel localLibrary = localLibraryService.get(libraryId);
            for (StorageBacking storageBacking : localLibrary.getStorageBackings()) {
                log.info("The DataStore ID of the Content Library::{}", storageBacking.getDatastoreId());
            }
            // Content update scenario 1:
            // Update OVF library item by creating an update session for the
            // OVF item, removing all existing files in the session, then
            // adding all new files into the same update session, and completing
            // the session to finish the content update.

            // Create an OVF item and upload initial content.
            String ovfItemId = createOvfItem(itemService, updateSessionService, updateSessionFileService, libraryId);
            ItemModel ovfItem = itemService.get(ovfItemId);
            String contentVersionBeforeUpdate = ovfItem.getContentVersion();
            log.info("OVF Library Item Created : {}, content version: {}", ovfItemId, contentVersionBeforeUpdate);

            // Update the OVF item with new OVF template via UpdateSession API.
            UpdateSessionModel updateSessionModel = new UpdateSessionModel();
            updateSessionModel.setLibraryItemId(ovfItemId);
            String sessionId = updateSessionService.create(null /* clientToken */, updateSessionModel);

            // Delete all existing files
            List<FileTypes.Info> existingFiles = updateSessionFileService.list(sessionId);
            for (FileTypes.Info fileInfo : existingFiles) {
                updateSessionFileService.remove(sessionId, fileInfo.getName());
            }

            // Upload new files and complete the update session.
            Map<String, String> filePathMap = getVmTemplateFiles(
                    OVF_ITEM_TWO_FOLDER_NAME, OVF_ITEM_TWO_OVF_FILE_NAME, OVF_ITEM_TWO_VMDK_FILE_NAME);
            ItemUploadHelper.uploadFile(
                    updateSessionFileService,
                    sessionId,
                    OVF_ITEM_TWO_OVF_FILE_NAME,
                    filePathMap.get(OVF_ITEM_TWO_OVF_FILE_NAME));
            ItemUploadHelper.uploadFile(
                    updateSessionFileService,
                    sessionId,
                    OVF_ITEM_TWO_VMDK_FILE_NAME,
                    filePathMap.get(OVF_ITEM_TWO_VMDK_FILE_NAME));

            updateSessionService.complete(sessionId);

            // Verify that the item content version increases by one.
            ovfItem = itemService.get(ovfItemId);
            String contentVersionAfterUpdate = ovfItem.getContentVersion();
            log.info("OVF Library Item Updated : {}, content version: {}", ovfItemId, contentVersionAfterUpdate);
            if (Integer.parseInt(contentVersionBeforeUpdate) + 1 != Integer.parseInt(contentVersionAfterUpdate)) {
                throw new RuntimeException("Assertion: The content item version is not incremented by 1");
            }

            // Content update scenario 2:
            // Update ISO library item by creating an update session for the
            // item, then adding the new ISO file using the same session file
            // name into the update session, which will update the existing
            // ISO file upon session complete.

            // Create a new ISO item in the content library and upload the initial ISO file.
            String isoItemId = createIsoItem(itemService, updateSessionService, updateSessionFileService, libraryId);
            ItemModel isoItem = itemService.get(isoItemId);
            contentVersionBeforeUpdate = isoItem.getContentVersion();
            log.info("ISO Library Item Created : {}, content version: {}", isoItemId, contentVersionBeforeUpdate);

            // Replace the existing ISO file in the ISO item with a new ISO
            // file with the same session file name via UpdateSession API.
            updateSessionModel = new UpdateSessionModel();
            updateSessionModel.setLibraryItemId(isoItemId);
            sessionId = updateSessionService.create(null /* clientToken */, updateSessionModel);

            String isoFilePath = getIsoFile(ISO_ITEM_FOLDER_NAME, ISO_ITEM_TWO_ISO_FILE_NAME);
            ItemUploadHelper.uploadFile(updateSessionFileService, sessionId, ISO_ITEM_ONE_ISO_FILE_NAME, isoFilePath);

            updateSessionService.complete(sessionId);

            // Verify that item content version increases by one.
            isoItem = itemService.get(isoItemId);
            contentVersionAfterUpdate = isoItem.getContentVersion();
            log.info("ISO Library Item Updated : {}, content version: {}", isoItemId, contentVersionAfterUpdate);

            if (Integer.parseInt(contentVersionBeforeUpdate) + 1 != Integer.parseInt(contentVersionAfterUpdate)) {
                throw new RuntimeException("Assertion: The content item version is not incremented by 1");
            }

            // cleanup
            if (ovfItemId != null) {
                log.info("Deleting Library Item : {}", ovfItemId);
                itemService.delete(ovfItemId);
            }

            if (isoItemId != null) {
                log.info("Deleting Library Item : {}", isoItemId);
                itemService.delete(isoItemId);
            }
        }
    }

    /**
     * Create an OVF item with OVF files uploaded.
     *
     * @param libraryId ID of the library
     * @return OVF item ID
     * @throws IOException when an I/O error occurs
     */
    private static String createOvfItem(
            Item itemService,
            UpdateSession updateSessionService,
            com.vmware.content.library.item.updatesession.File updateSessionFileService,
            String libraryId)
            throws IOException {

        ItemModel ovfLibItem = createLibraryItem(itemService, libraryId, "descriptorovf", OVF_ITEM_TYPE);

        Map<String, String> filePathMap =
                getVmTemplateFiles(OVF_ITEM_ONE_FOLDER_NAME, OVF_ITEM_ONE_OVF_FILE_NAME, OVF_ITEM_ONE_VMDK_FILE_NAME);

        List<String> fileLocations = Arrays.asList(
                filePathMap.get(OVF_ITEM_ONE_OVF_FILE_NAME), filePathMap.get(OVF_ITEM_ONE_VMDK_FILE_NAME));

        ItemUploadHelper.performUpload(
                updateSessionService, updateSessionFileService, itemService, ovfLibItem.getId(), fileLocations);

        if (itemService.list(libraryId).isEmpty()) {
            throw new RuntimeException("Assertion: OVF item not found after creation");
        }
        return ovfLibItem.getId();
    }

    /**
     * Create an ISO item with ISO file uploaded.
     *
     * @param libraryId ID of the library
     * @return ISO item ID
     * @throws IOException when an I/O error occurs
     */
    private static String createIsoItem(
            Item itemService,
            UpdateSession updateSessionService,
            com.vmware.content.library.item.updatesession.File updateSessionFileService,
            String libraryId)
            throws IOException {

        String isoFilePath = getIsoFile(ISO_ITEM_FOLDER_NAME, ISO_ITEM_ONE_ISO_FILE_NAME);
        ItemModel isoLibItem = createLibraryItem(itemService, libraryId, "smalliso", ISO_ITEM_TYPE);

        ItemUploadHelper.performUpload(
                updateSessionService, updateSessionFileService, itemService, isoLibItem.getId(), List.of(isoFilePath));

        if (itemService.list(libraryId).isEmpty()) {
            throw new RuntimeException("Assertion: ISO item not found after creation");
        }
        return isoLibItem.getId();
    }

    /**
     * Generate and return OVF and VMDK files from class resources with the given OVF and VMDK file names.
     *
     * @param folderName the name of the folder that contains both files
     * @param ovfFileName the name of the OVF file
     * @param diskFileName the name of the VMDK file
     * @return map of OVF file and VMDK file absolute path as below: { ovf-file-name: ovf-file-path, vmdk-file-name:
     *     vmdk-file-path}
     * @throws IOException when an I/O error occurs
     */
    private static Map<String, String> getVmTemplateFiles(String folderName, String ovfFileName, String diskFileName)
            throws IOException {
        Map<String, String> filePathMap = new HashMap<>();

        java.io.File tempDir = ItemUploadHelper.createTempDir(folderName);
        String ovfFile = ItemUploadHelper.copyResourceToFile(folderName + "/" + ovfFileName, tempDir, ovfFileName);
        filePathMap.put(ovfFileName, ovfFile);

        String vmdkFile = ItemUploadHelper.copyResourceToFile(folderName + "/" + diskFileName, tempDir, diskFileName);
        filePathMap.put(diskFileName, vmdkFile);

        log.info("OVF Path : {}", ovfFile);
        log.info("VMDK Path : {}", vmdkFile);

        return filePathMap;
    }

    /**
     * Generate and return the ISO file from the class resources with the given ISO file name.
     *
     * @param folderName the name of the folder that contains the ISO file
     * @param isoFileName the name of the ISO file
     * @return the absolute path to the ISO file
     * @throws IOException when an I/O error occurs
     */
    private static String getIsoFile(String folderName, String isoFileName) throws IOException {
        File tempDir = ItemUploadHelper.createTempDir(folderName);
        String isoFile = ItemUploadHelper.copyResourceToFile(folderName + "/" + isoFileName, tempDir, isoFileName);

        log.info("Iso Image Path : {}", isoFile);
        return isoFile;
    }

    /**
     * Create a library item in the specified library.
     *
     * @param libraryId ID of the library
     * @param libraryItemName name of the library item
     * @param itemType type of the library item
     * @return {@link ItemModel}
     */
    private static ItemModel createLibraryItem(
            Item itemService, String libraryId, String libraryItemName, String itemType) {
        // get the library item spec
        ItemModel libItemSpec = getLibraryItemSpec(libraryId, libraryItemName, "item update", itemType);
        // create a library item
        String libItemId = itemService.create(UUID.randomUUID().toString(), libItemSpec);

        return itemService.get(libItemId);
    }

    /**
     * Construct a library item spec.
     *
     * @param libraryId ID of the library
     * @param name the name of the library item
     * @param description the description of the library item
     * @param type type of the library item
     * @return {@link ItemModel}
     */
    private static ItemModel getLibraryItemSpec(String libraryId, String name, String description, String type) {
        ItemModel libItemSpec = new ItemModel();
        libItemSpec.setName(name);
        libItemSpec.setDescription(description);
        libItemSpec.setLibraryId(libraryId);
        libItemSpec.setType(type);

        return libItemSpec;
    }
}
