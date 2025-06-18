/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.compute.contentlibrary.helpers;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.content.library.Item;
import com.vmware.content.library.item.UpdateSession;
import com.vmware.content.library.item.UpdateSessionModel;
import com.vmware.content.library.item.updatesession.File;
import com.vmware.content.library.item.updatesession.FileTypes.AddSpec;
import com.vmware.content.library.item.updatesession.FileTypes.Info;
import com.vmware.content.library.item.updatesession.FileTypes.SourceType;
import com.vmware.content.library.item.updatesession.FileTypes.ValidationError;
import com.vmware.sdk.samples.helpers.HttpClient;

public class ItemUploadHelper {
    private static final Logger log = LoggerFactory.getLogger(ItemUploadHelper.class);

    /**
     * Perform the upload.
     *
     * @param uploadService service to create the upload session
     * @param uploadFileService service to upload the files when the session is created
     * @param libraryItemService passed when the upload session is created in order to upload the file to the library
     *     item
     * @param libraryItemId ID of the library item
     * @param fileLocations list of files to be uploaded
     */
    public static void performUpload(
            UpdateSession uploadService,
            File uploadFileService,
            Item libraryItemService,
            String libraryItemId,
            List<String> fileLocations) {

        // get the file names from the local file locations.
        List<String> fileNames = new ArrayList<>();
        for (String location : fileLocations) {
            java.io.File file = new java.io.File(location);
            fileNames.add(file.getName());
        }

        // create a new upload session for uploading the files
        String sessionId = createUploadSession(uploadService, libraryItemService, libraryItemId);

        // add the files to the item and PUT the file to the transfer URL
        uploadFiles(uploadFileService, sessionId, fileNames, fileLocations);

        // check if there were any invalid or missing files
        List<ValidationError> invalidFiles =
                uploadFileService.validate(sessionId).getInvalidFiles();

        Set<String> missingFiles = uploadFileService.validate(sessionId).getMissingFiles();

        log.info("UploadSession Info : {}", uploadService.get(sessionId));
        log.info("Invalid Files : {}", invalidFiles);
        log.info("Missing Files : {}", missingFiles);

        if (missingFiles.isEmpty() && invalidFiles.isEmpty()) {
            uploadService.complete(sessionId);
            // Delete the update session once the upload is done to free up the session.
            uploadService.delete(sessionId);
        }

        if (!invalidFiles.isEmpty()) {
            uploadService.fail(sessionId, invalidFiles.get(0).getErrorMessage().toString());
            uploadService.delete(sessionId);

            log.info("Invalid files : {}", invalidFiles);
            throw new RuntimeException(invalidFiles.toString());
        }
        if (!missingFiles.isEmpty()) {
            uploadService.cancel(sessionId);

            log.info("Following files are missing : {}", missingFiles);
            throw new RuntimeException("Missing the required files : " + missingFiles);
        }

        // verify that the content version number has incremented after the commit.
        log.info(
                "The Library Item version after the upload commit : {}",
                libraryItemService.get(libraryItemId).getContentVersion());
    }

    /** Creating a new upload session. */
    private static String createUploadSession(
            UpdateSession uploadService, Item libraryItemService, String libraryItemId) {
        // Create a session for upload.
        String currentVersion = libraryItemService.get(libraryItemId).getContentVersion();
        UpdateSessionModel createSpec = new UpdateSessionModel();
        createSpec.setLibraryItemId(libraryItemId);
        createSpec.setLibraryItemContentVersion(currentVersion);

        return uploadService.create(UUID.randomUUID().toString(), createSpec);
    }

    /** Upload files using upload session. */
    private static void uploadFiles(
            File uploadFileService, String sessionId, List<String> fileNames, List<String> fileLocations) {
        if (fileNames.size() != fileLocations.size()) {
            throw new IllegalArgumentException("fileNames and fileLocation have different dimensions");
        }
        for (int i = 0; i < fileNames.size(); i++) {
            uploadFile(uploadFileService, sessionId, fileNames.get(i), fileLocations.get(i));
        }
    }

    /**
     * Upload a file using upload session.
     *
     * @param uploadFileService service to upload the files when the session is created
     * @param sessionId ID of the upload session
     * @param fileName destination name of the file
     * @param fileLocation local location of the file to upload
     * @return info of the update session file
     */
    public static Info uploadFile(File uploadFileService, String sessionId, String fileName, String fileLocation) {
        HttpClient httpClient = new HttpClient(true);
        // add the file spec to the upload file service
        AddSpec addSpec = new AddSpec();
        addSpec.setName(fileName);
        addSpec.setSourceType(SourceType.PUSH);
        uploadFileService.add(sessionId, addSpec);

        // Do a get on the file, verify the information is the same
        com.vmware.content.library.item.updatesession.FileTypes.Info fileInfo =
                uploadFileService.get(sessionId, fileName);

        // Get the transfer uri.
        URI transferUri = fileInfo.getUploadEndpoint().getUri();

        log.info("File Location : {}", fileLocation);
        java.io.File file = new java.io.File(fileLocation);
        log.info("File Name {}", file.getName());

        try {
            String transferUrl = transferUri.toURL().toString();
            log.info("Upload/Transfer URL : {}", transferUrl);
            httpClient.upload(file, transferUrl);

        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to upload due to IOException!", e);
        }

        // Verify that the file has been received
        fileInfo = uploadFileService.get(sessionId, fileName);
        return fileInfo;
    }

    /**
     * Creating a local temporary dir with the given prefix.
     *
     * @param prefix the prefix for the temp directory
     * @return the File handle for the created directory
     * @throws IOException when an I/O error occurs
     */
    public static java.io.File createTempDir(String prefix) throws IOException {
        java.io.File temp = java.io.File.createTempFile(prefix, "");

        boolean deleted = temp.delete();
        boolean created = temp.mkdir();

        log.debug("Temp folder {} deleted={}, created={}", temp, deleted, created);
        temp.deleteOnExit();

        return temp;
    }

    /**
     * Copies the resource into a temporary file.
     *
     * @param resourceName the resource name to copy
     * @param dir the directory where the file will be created
     * @param filename the name of the file to create
     * @return the absolute path to the file
     * @throws IOException when an I/O error occurs
     */
    public static String copyResourceToFile(String resourceName, java.io.File dir, String filename) throws IOException {
        // Create a temporary file in the directory
        java.io.File tempFile = new java.io.File(dir, filename);
        tempFile.deleteOnExit();

        // Copy the resource to the temporary file
        ClassLoader classLoader = ItemUploadHelper.class.getClassLoader();
        try (InputStream in = classLoader.getResourceAsStream(resourceName);
                OutputStream out = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
        }

        return tempFile.getAbsolutePath();
    }
}
