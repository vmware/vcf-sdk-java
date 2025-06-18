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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.content.library.Item;
import com.vmware.content.library.item.DownloadSession;
import com.vmware.content.library.item.DownloadSessionModel;
import com.vmware.content.library.item.downloadsession.File;
import com.vmware.content.library.item.downloadsession.FileTypes;
import com.vmware.content.library.item.downloadsession.FileTypes.EndpointType;
import com.vmware.sdk.samples.helpers.HttpClient;

public class ItemDownloadHelper {
    private static final Logger log = LoggerFactory.getLogger(ItemDownloadHelper.class);

    /**
     * Performing the library item download.
     *
     * @param downloadService service to create the download session
     * @param downloadFileService service to download the files when the session is created
     * @param libraryItemService passed when the download session is created in order to download the file from the
     *     library item
     * @param libraryItemId ID of the library item
     * @param dir directory where the files will be downloaded
     */
    public static void performDownload(
            DownloadSession downloadService,
            File downloadFileService,
            Item libraryItemService,
            String libraryItemId,
            java.io.File dir) {
        log.info(
                "Download start for Library Item : {} Name : {}",
                libraryItemId,
                libraryItemService.get(libraryItemId).getName());
        String downloadSessionId = null;
        try {
            // create download session
            downloadSessionId = createDownloadSession(
                    downloadService, libraryItemId, UUID.randomUUID().toString());
            downloadFiles(downloadService, downloadFileService, downloadSessionId, dir);
            // delete the download session.
        } finally {
            downloadService.delete(downloadSessionId);
        }
    }

    /** Downloading files from library item using the download session. */
    private static void downloadFiles(
            DownloadSession downloadService, File downloadFileService, String sessionId, java.io.File dir) {
        HttpClient httpClient = new HttpClient(true);

        List<FileTypes.Info> downloadFileInfos = downloadFileService.list(sessionId);
        for (FileTypes.Info downloadFileInfo : downloadFileInfos) {
            prepareForDownload(downloadService, downloadFileService, sessionId, downloadFileInfo);

            // Do a get after file is prepared for download.
            downloadFileInfo = downloadFileService.get(sessionId, downloadFileInfo.getName());
            // Download the file
            log.info("Download File Info : {}", downloadFileInfo);

            try {
                URI downloadUri = downloadFileInfo.getDownloadEndpoint().getUri();
                String downloadUrl = downloadUri.toURL().toString();
                log.info("Download from URL : {}", downloadUrl);

                InputStream inputStream = httpClient.downloadFile(downloadUrl);
                String fileName = downloadFileInfo.getName();

                downloadFile(inputStream, dir.getAbsolutePath() + System.getProperty("file.separator") + fileName);
            } catch (MalformedURLException e) {
                log.error("Failed to download due to IOException!", e);
                throw new RuntimeException("Failed to download due to IOException!", e);
            } catch (IOException e) {
                log.error("IO exception during download", e);
                throw new RuntimeException("Failed to download due to IOException!", e);
            }
        }
    }

    /** Make sure the file to be downloaded is ready for download. */
    private static void prepareForDownload(
            DownloadSession downloadService,
            File downloadFileService,
            String sessionId,
            FileTypes.Info downloadFileInfo) {
        log.info("Download File name : {}", downloadFileInfo.getName());
        log.info("Download File Prepare Status : {}", downloadFileInfo.getStatus());

        downloadFileService.prepare(sessionId, downloadFileInfo.getName(), EndpointType.HTTPS);

        waitForDownloadFileReady(
                downloadService,
                downloadFileService,
                sessionId,
                downloadFileInfo.getName(),
                com.vmware.content.library.item.downloadsession.File.PrepareStatus.PREPARED,
                SESSION_FILE_TIMEOUT);
    }

    private static final long SESSION_FILE_TIMEOUT = 360;

    /** Wait for the download file status to be prepared. */
    private static void waitForDownloadFileReady(
            DownloadSession downloadService,
            File downloadFileService,
            String sessionId,
            String fileName,
            FileTypes.PrepareStatus expectedStatus,
            long timeOut) {
        long endTime = System.currentTimeMillis() + timeOut * 1000;

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            log.error("InterruptedException: ", e);
        }

        FileTypes.Info fileInfo = downloadFileService.get(sessionId, fileName);
        FileTypes.PrepareStatus currentStatus = fileInfo.getStatus();

        if (currentStatus == expectedStatus) {
            return;
        } else {
            while (endTime > System.currentTimeMillis()) {
                fileInfo = downloadFileService.get(sessionId, fileName);
                currentStatus = fileInfo.getStatus();

                log.info("Current Status : {}", currentStatus);

                if (currentStatus == expectedStatus) {
                    return;
                } else if (currentStatus == com.vmware.content.library.item.downloadsession.File.PrepareStatus.ERROR) {
                    log.info("DownloadSession Info : {}", downloadService.get(sessionId));
                    log.info("list on the downloadSessionFile : {}", downloadFileService.list(sessionId));

                    throw new RuntimeException("Error while waiting for download file status to " + "be PREPARED...");
                }
            }
        }
        throw new RuntimeException(
                "Timeout waiting for download file status to be PREPARED," + "  status : " + currentStatus.toString());
    }

    /** Create a new download session for downloading files from library item. */
    private static String createDownloadSession(
            DownloadSession downloadService, String libraryItemId, String clientToken) {
        DownloadSessionModel downloadSpec = new DownloadSessionModel();
        downloadSpec.setLibraryItemId(libraryItemId);

        return downloadService.create(clientToken, downloadSpec);
    }

    /** Download a specific file. */
    private static void downloadFile(InputStream inputStream, String fullPath) throws IOException {
        Files.copy(inputStream, Paths.get(fullPath), StandardCopyOption.REPLACE_EXISTING);
    }
}
