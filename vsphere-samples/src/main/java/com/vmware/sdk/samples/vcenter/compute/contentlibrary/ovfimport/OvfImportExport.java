/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.compute.contentlibrary.ovfimport;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.content.Library;
import com.vmware.content.LibraryTypes;
import com.vmware.content.library.Item;
import com.vmware.content.library.ItemModel;
import com.vmware.content.library.item.DownloadSession;
import com.vmware.content.library.item.Storage;
import com.vmware.content.library.item.UpdateSession;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.compute.contentlibrary.helpers.ItemDownloadHelper;
import com.vmware.sdk.samples.vcenter.compute.contentlibrary.helpers.ItemUploadHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;

/**
 * Demonstrates the workflow to import an OVF package into the content library.
 *
 * <p>Sample Prerequisites: The sample needs an existing content library to place the library item.
 */
public class OvfImportExport {
    private static final Logger log = LoggerFactory.getLogger(OvfImportExport.class);
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

    /** REQUIRED: The name of the content library where the library item will be created. Defaults to demo-local-lib. */
    public static String libName = "libName";

    private static final String LIB_FOLDER_NAME = "simpleVmTemplate";
    private static final String LIB_ITEM_NAME = "descriptor.ovf";
    private static final String LIB_VMDK_NAME = "disk-0.vmdk";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(OvfImportExport.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Library libraryService = client.createStub(Library.class);
            Storage storageService = client.createStub(Storage.class);
            UpdateSession updateSessionService = client.createStub(UpdateSession.class);
            DownloadSession downloadSessionService = client.createStub(DownloadSession.class);
            Item itemService = client.createStub(Item.class);

            var downloadSessionFileService =
                    client.createStub(com.vmware.content.library.item.downloadsession.File.class);
            var uploadSessionFileService = client.createStub(com.vmware.content.library.item.updatesession.File.class);

            // Get the template's OVF and VMDK files
            /*
            Editing files located in src/main/resources requires rebuilding the JAR before running the sample.
            Alternatively one can change the code below to read directly from the file system.
            - File ovfFile = new File("path-to-file.ovf");
            */
            var tempDir = ItemUploadHelper.createTempDir(LIB_FOLDER_NAME);

            String ovfFile =
                    ItemUploadHelper.copyResourceToFile(LIB_FOLDER_NAME + "/" + LIB_ITEM_NAME, tempDir, LIB_ITEM_NAME);
            String vmdkFile =
                    ItemUploadHelper.copyResourceToFile(LIB_FOLDER_NAME + "/" + LIB_VMDK_NAME, tempDir, LIB_VMDK_NAME);
            log.info("OVF Path : {}", ovfFile);
            log.info("VMDK Path : {}", vmdkFile);

            // Find the content library id by name
            LibraryTypes.FindSpec findSpec = new LibraryTypes.FindSpec();
            findSpec.setName(libName);

            List<String> libraryIds = libraryService.find(findSpec);
            if (libraryIds.isEmpty()) {
                throw new RuntimeException("Unable to find a library with name: " + libName);
            }

            String libraryId = libraryIds.get(0);
            log.info("Found library : {}", libraryId);

            // Build the specification for the library item to be created
            ItemModel createSpec = new ItemModel();
            createSpec.setName(LIB_FOLDER_NAME);
            createSpec.setLibraryId(libraryId);
            createSpec.setType("ovf");

            // Create a new library item in the content library for uploading the files
            String clientToken = UUID.randomUUID().toString();
            String libItemId = itemService.create(clientToken, createSpec);
            ItemModel libItem = itemService.get(libItemId);
            log.info("Library item created : {}", libItem.getId());

            // Upload the files in the OVF package into the library item
            ItemUploadHelper.performUpload(
                    updateSessionService,
                    uploadSessionFileService,
                    itemService,
                    libItem.getId(),
                    Arrays.asList(ovfFile, vmdkFile));
            log.info("Uploaded files : {}", storageService.list(libItem.getId()));

            // Download the template files from the library item into a folder
            var downloadDir = ItemUploadHelper.createTempDir(LIB_FOLDER_NAME);
            ItemDownloadHelper.performDownload(
                    downloadSessionService, downloadSessionFileService, itemService, libItem.getId(), downloadDir);
            log.info("Downloaded files to directory : {}", downloadDir);

            // Delete the library item
            itemService.delete(libItem.getId());
            log.info("Deleted library item : {}", libItem.getId());
        }
    }
}
