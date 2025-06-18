/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.compute.contentlibrary.publishsubscribe;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.content.LibraryModel;
import com.vmware.content.LocalLibrary;
import com.vmware.content.SubscribedLibrary;
import com.vmware.content.library.Item;
import com.vmware.content.library.ItemModel;
import com.vmware.content.library.PublishInfo;
import com.vmware.content.library.StorageBacking;
import com.vmware.content.library.SubscribedItem;
import com.vmware.content.library.SubscriptionInfo;
import com.vmware.content.library.item.UpdateSession;
import com.vmware.content.library.item.updatesession.File;
import com.vmware.sdk.samples.helpers.DatastoreHelper;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.compute.contentlibrary.helpers.ClsApiHelper;
import com.vmware.sdk.samples.vcenter.compute.contentlibrary.helpers.ItemUploadHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.Datastore;

/**
 * Demonstrates the workflow to publish and subscribe content libraries.
 *
 * <p>Sample Prerequisites: The sample needs an existing VC datastore with available storage.
 */
public class LibraryPublishSubscribe {
    private static final Logger log = LoggerFactory.getLogger(LibraryPublishSubscribe.class);
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

    /** REQUIRED: The name of the VC datastore to be used for the published and subscribed libraries. */
    public static String dsName = "dsName";

    private static final String VCSP_USERNAME = "vcsp";
    private static final char[] DEMO_PASSWORD = "Password!23".toCharArray();
    private static final long SYNC_TIMEOUT_SEC = 60;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(LibraryPublishSubscribe.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Datastore datastoreService = client.createStub(Datastore.class);
            SubscribedItem subscribedItemService = client.createStub(SubscribedItem.class);
            UpdateSession updateSessionService = client.createStub(UpdateSession.class);
            File updateSessionFileService = client.createStub(File.class);
            SubscribedLibrary subscribedLibraryService = client.createStub(SubscribedLibrary.class);
            LocalLibrary localLibraryService = client.createStub(LocalLibrary.class);
            Item itemService = client.createStub(Item.class);

            // Create the Content Library services with authenticated session
            ClsApiHelper clsHelper = new ClsApiHelper();

            // Build the storage backing for the libraries to be created
            StorageBacking storageBacking = DatastoreHelper.createStorageBacking(datastoreService, dsName);

            // Build the authenticated publish information. The username defaults to "vcsp".
            PublishInfo pubInfo = new PublishInfo();
            pubInfo.setPublished(true);
            pubInfo.setAuthenticationMethod(PublishInfo.AuthenticationMethod.BASIC);
            pubInfo.setPassword(DEMO_PASSWORD);

            // Build the specification for the published library
            LibraryModel pubSpec = new LibraryModel();
            pubSpec.setName("demo-publib");
            pubSpec.setType(LibraryModel.LibraryType.LOCAL);
            pubSpec.setPublishInfo(pubInfo);
            pubSpec.setStorageBackings(Collections.singletonList(storageBacking));

            // Create the published library and add a library item
            String pubToken = UUID.randomUUID().toString();
            String pubLibId = localLibraryService.create(pubToken, pubSpec);
            log.info("Published library created : {}", pubLibId);

            LibraryModel pubLib = localLibraryService.get(pubLibId);
            log.info("Publish URL : {}", pubLib.getPublishInfo().getPublishUrl());

            createLibraryItem(itemService, updateSessionFileService, updateSessionService, pubLibId, "item 1");

            // Build the subscription information using the publish URL of the
            // published library. The username must be "vcsp".
            SubscriptionInfo subInfo = new SubscriptionInfo();
            subInfo.setAuthenticationMethod(SubscriptionInfo.AuthenticationMethod.BASIC);
            subInfo.setUserName(VCSP_USERNAME);
            subInfo.setPassword(DEMO_PASSWORD);
            subInfo.setOnDemand(false);
            subInfo.setAutomaticSyncEnabled(true);
            subInfo.setSubscriptionUrl(pubLib.getPublishInfo().getPublishUrl());

            // Build the specification for the subscribed library
            LibraryModel subSpec = new LibraryModel();
            subSpec.setName("demo-sublib");
            subSpec.setType(LibraryModel.LibraryType.SUBSCRIBED);
            subSpec.setSubscriptionInfo(subInfo);
            subSpec.setStorageBackings(Collections.singletonList(storageBacking));

            // Create the subscribed library
            String subToken = UUID.randomUUID().toString();
            String subLibId = subscribedLibraryService.create(subToken, subSpec);

            LibraryModel subLib = subscribedLibraryService.get(subLibId);
            log.info("Subscribed library created : {}", subLibId);

            boolean syncSuccess;
            // Wait for the initial synchronization to finish
            syncSuccess = clsHelper.waitForLibrarySync(
                    pubLibId, subscribedLibraryService, subLibId, itemService, SYNC_TIMEOUT_SEC, TimeUnit.SECONDS);

            if (!syncSuccess) {
                throw new RuntimeException("Timed out while waiting for sync success");
            }

            subLib = subscribedLibraryService.get(subLibId);
            log.info("Subscribed library synced : {}", subLib.getLastSyncTime().getTime());

            List<String> subItemIds = itemService.list(subLibId);
            if (!(subItemIds.size() == 1)) {
                throw new RuntimeException("Subscribed library has one item");
            }

            // Add another item to the publish library
            createLibraryItem(itemService, updateSessionFileService, updateSessionService, pubLib.getId(), "item 2");

            // Manually synchronize the subscribed library to get the latest changes immediately.
            subscribedLibraryService.sync(subLibId);
            syncSuccess = clsHelper.waitForLibrarySync(
                    pubLibId, subscribedLibraryService, subLibId, itemService, SYNC_TIMEOUT_SEC, TimeUnit.SECONDS);

            if (!syncSuccess) {
                throw new RuntimeException("Timed out while waiting for sync success");
            }

            subLib = subscribedLibraryService.get(subLibId);
            log.info("Subscribed library synced : {}", subLib.getLastSyncTime().getTime());

            // List the subscribed items
            subItemIds = itemService.list(subLibId);

            if (subItemIds.size() != 2) {
                throw new RuntimeException("Subscribed library has two items");
            }

            for (String subItemId : subItemIds) {
                ItemModel subItem = itemService.get(subItemId);
                log.info("Subscribed item : {}", subItem);
            }

            // Change the subscribed library to be on-demand
            subInfo.setOnDemand(true);
            subscribedLibraryService.update(subLibId, subSpec);

            // Evict the cached content of the first subscribed library item
            String subItemId = subItemIds.get(0);
            subscribedItemService.evict(subItemId);

            ItemModel subItem = itemService.get(subItemId);
            log.info("Subscribed item evicted : {}", subItem);

            if (subItem.getCached()) {
                throw new RuntimeException("Subscribed item is not cached");
            }

            // Force synchronize the subscribed library item to fetch and cache the content
            subscribedItemService.sync(subItemId, true, false);
            syncSuccess = clsHelper.waitForItemSync(itemService, subItemId, SYNC_TIMEOUT_SEC, TimeUnit.SECONDS);

            if (!syncSuccess) {
                throw new RuntimeException("Timed out while waiting for sync success");
            }

            subItem = itemService.get(subItemId);
            log.info("Subscribed item force synced : {}", subItem);

            if (!(subItem.getCached())) {
                throw new RuntimeException("Subscribed item is cached");
            }

            // cleanup
            if (subLibId != null) {
                // Delete the subscribed content library
                subscribedLibraryService.delete(subLibId);
                log.info("Deleted subscribed library : {}", subLibId);
            }

            if (pubLibId != null) {
                // Delete the published content library
                localLibraryService.delete(pubLibId);
                log.info("Deleted published library : {}", pubLibId);
            }
        }
    }

    /**
     * Creates a library item with mock content, for demonstration purposes.
     *
     * @param localLibraryId identifier of the local library where a new item will be created
     * @param itemName name of the item to create
     * @return identifier of the created item
     * @throws IOException when an I/O error occurs
     */
    private static String createLibraryItem(
            Item itemService,
            File updateSessionFileService,
            UpdateSession updateSessionService,
            String localLibraryId,
            String itemName)
            throws IOException {
        // Build the specification for the library item to be created
        ItemModel createSpec = new ItemModel();
        createSpec.setLibraryId(localLibraryId);
        createSpec.setName(itemName);

        // Create the library item
        String clientToken = UUID.randomUUID().toString();
        String libItemId = itemService.create(clientToken, createSpec);

        // Create a temporary file
        Path path = Files.createTempFile(itemName, ".txt");
        path.toFile().deleteOnExit();

        // Write default content to the file
        String content = "Contents of " + itemName;
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));

        // Upload file to the library item
        ItemUploadHelper.performUpload(
                updateSessionService, updateSessionFileService, itemService, libItemId, List.of(path.toString()));

        log.info("Library item created : {}", libItemId);

        return libItemId;
    }
}
