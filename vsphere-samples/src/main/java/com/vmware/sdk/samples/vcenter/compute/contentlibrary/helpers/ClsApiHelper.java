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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.vmware.content.LibraryModel;
import com.vmware.content.SubscribedLibrary;
import com.vmware.content.library.Item;
import com.vmware.content.library.ItemModel;

/** Helper class to perform commonly used operations using Content Library API. */
public class ClsApiHelper {

    /**
     * Wait for the synchronization of the subscribed library to complete or until the timeout is reached. The
     * subscribed library is fully synchronized when it has the same library items and the same versions as the items in
     * the source published library.
     *
     * @param publishedLibraryId the identifier of the published library
     * @param subscribedLibraryService Stub for the Subscribe library service
     * @param subscribedLibraryId the identifier of the subscribed library
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout parameter
     * @param itemService Stub for the Item service
     * @return true if the subscribed library is synchronized with the published library, false otherwise
     * @throws InterruptedException if the current thread was interrupted
     */
    public boolean waitForLibrarySync(
            String publishedLibraryId,
            SubscribedLibrary subscribedLibraryService,
            String subscribedLibraryId,
            Item itemService,
            long timeout,
            TimeUnit unit)
            throws InterruptedException {
        return new SyncHelper(timeout, unit)
                .waitForLibrarySync(publishedLibraryId, subscribedLibraryService, itemService, subscribedLibraryId);
    }

    /**
     * Wait for the synchronization of the subscribed library item to complete or until the timeout is reached. The
     * subscribed item is fully synchronized when it has the same metadata and content version as the source published
     * item.
     *
     * @param subscribedItemId the identifier of the subscribed item
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout parameter
     * @param itemService Stub for the Item service
     * @return true if the subscribed item is synchronized with the published item, false otherwise
     * @throws InterruptedException if the current thread was interrupted
     */
    public boolean waitForItemSync(Item itemService, String subscribedItemId, long timeout, TimeUnit unit)
            throws InterruptedException {
        return new SyncHelper(timeout, unit).waitForItemSync(itemService, subscribedItemId);
    }

    /** Helper class to wait for the subscribed libraries and items to be synchronized completely with their source. */
    private static class SyncHelper {

        private static final long WAIT_INTERVAL_MS = 1000;

        private final long startTime;
        private final long timeoutNano;

        public SyncHelper(long timeout, TimeUnit unit) {
            this.startTime = System.nanoTime();
            this.timeoutNano = TimeUnit.NANOSECONDS.convert(timeout, unit);
        }

        /*
         * Wait until the subscribed library and its items are synchronized with the published library.
         */
        public boolean waitForLibrarySync(
                String publishedLibraryId,
                SubscribedLibrary subscribedLibraryService,
                Item itemService,
                String subscribedLibraryId)
                throws InterruptedException {

            if (!waitForSameItems(itemService, publishedLibraryId, subscribedLibraryId)) {
                return false;
            }

            List<String> subscribedItemIds = itemService.list(subscribedLibraryId);
            for (String subscribedItemId : subscribedItemIds) {
                if (!waitForItemSync(itemService, subscribedItemId)) {
                    return false;
                }
            }

            return waitForLibraryLastSyncTime(subscribedLibraryService, subscribedLibraryId);
        }

        /*
         * Wait until the subscribed item is synchronized with the published item.
         */
        public boolean waitForItemSync(Item itemService, String subscribedItemId) throws InterruptedException {
            boolean isSynced = false;
            String publishedItemId = itemService.get(subscribedItemId).getSourceId();
            ItemModel publishedItem = itemService.get(publishedItemId);

            while (notTimedOut()) {
                ItemModel subscribedItem = itemService.get(subscribedItemId);
                if (isSubscribedItemLatest(publishedItem, subscribedItem)) {
                    isSynced = true;
                    break;
                }

                Thread.sleep(WAIT_INTERVAL_MS);
            }
            return isSynced;
        }

        /*
         * Wait until the subscribed library has the same source item IDs as the published library.
         */
        private boolean waitForSameItems(Item itemService, String publishedLibraryId, String subscribedLibraryId)
                throws InterruptedException {
            boolean isSynced = false;
            List<String> publishedItemIds = itemService.list(publishedLibraryId);

            while (notTimedOut()) {
                List<String> subscribedItemIds = itemService.list(subscribedLibraryId);
                if (hasSameItems(publishedItemIds, itemService, subscribedItemIds)) {
                    isSynced = true;
                    break;
                }

                Thread.sleep(WAIT_INTERVAL_MS);
            }
            return isSynced;
        }

        /*
         * Check if the subscribed library contains the same items as the source published library.
         * The item versions are not checked.
         */
        private boolean hasSameItems(List<String> publishedItemIds, Item itemService, List<String> subscribedItemIds) {
            if (publishedItemIds.size() != subscribedItemIds.size()) {
                return false;
            }

            List<String> syncedIds = new ArrayList<>(publishedItemIds.size());
            for (String subscribedItemId : subscribedItemIds) {
                ItemModel subscribedItem = itemService.get(subscribedItemId);
                String sourceId = subscribedItem.getSourceId();

                if (!syncedIds.contains(subscribedItemId) && publishedItemIds.contains(sourceId)) {
                    syncedIds.add(subscribedItemId);
                }
            }

            return (publishedItemIds.size() == syncedIds.size());
        }

        /*
         * Wait until the subscribed library's last sync time is populated.
         */
        private boolean waitForLibraryLastSyncTime(
                SubscribedLibrary subscribedLibraryService, String subscribedLibraryId) throws InterruptedException {
            boolean isSynced = false;

            while (notTimedOut()) {
                LibraryModel library = subscribedLibraryService.get(subscribedLibraryId);
                if (library.getLastSyncTime() != null) {
                    isSynced = true;
                    break;
                }

                Thread.sleep(WAIT_INTERVAL_MS);
            }
            return isSynced;
        }

        /*
         * Check if the subscribed item has the same metadata and content version as the source published item.
         */
        private boolean isSubscribedItemLatest(ItemModel publishedItem, ItemModel subscribedItem) {
            String metadataVersion = publishedItem.getMetadataVersion();
            String contentVersion = publishedItem.getContentVersion();

            return subscribedItem.getMetadataVersion().equals(metadataVersion)
                    && subscribedItem.getContentVersion().equals(contentVersion);
        }

        /*
         * Check if we have not timed out yet.
         */
        private boolean notTimedOut() {
            long elapsedTime = System.nanoTime() - startTime;
            return elapsedTime < timeoutNano;
        }
    }
}
