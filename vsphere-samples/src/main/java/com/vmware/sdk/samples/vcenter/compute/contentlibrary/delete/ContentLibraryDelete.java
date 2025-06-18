/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.compute.contentlibrary.delete;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.content.Library;
import com.vmware.content.LibraryModel;
import com.vmware.content.LibraryTypes;
import com.vmware.content.LocalLibrary;
import com.vmware.content.library.StorageBacking;
import com.vmware.sdk.samples.helpers.DatastoreHelper;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.Datastore;

/** Demonstrates the deletion of all content libraries in a datastore or deletion of a specific content library */
public class ContentLibraryDelete {
    private static final Logger log = LoggerFactory.getLogger(ContentLibraryDelete.class);
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

    /** REQUIRED: Content Librarie(s) under this VC datastore will be deleted. */
    public static String dsName = "dsName";
    /** OPTIONAL: If provided Content Library with this name will be deleted. */
    public static String libName = null;

    private static StorageBacking storageBacking;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(ContentLibraryDelete.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Datastore datastoreService = client.createStub(Datastore.class);
            Library libraryService = client.createStub(Library.class);
            LocalLibrary localLibraryService = client.createStub(LocalLibrary.class);

            // Get the Storage Backing on the datastore
            // Compare it with the Content Library's to identify if it that can be deleted
            storageBacking = DatastoreHelper.createStorageBacking(datastoreService, dsName);

            if (!(libName == null || libName.trim().isEmpty())) {
                LibraryTypes.FindSpec findSpec = new LibraryTypes.FindSpec();
                findSpec.setName(libName);

                List<String> libraryIds = libraryService.find(findSpec);
                if (libraryIds.isEmpty()) {
                    throw new RuntimeException("Unable to find a library with name: " + libName);
                }

                String libraryId = libraryIds.get(0);
                log.info("Found library : {}", libraryId);

                deleteContentLibrary(localLibraryService, libraryId);
            } else {
                // Delete all the content libraries in the datastore managed by this VC
                List<String> visibleCls = localLibraryService.list();
                log.info("All libraries : {}", visibleCls);

                for (String libId : visibleCls) {
                    deleteContentLibrary(localLibraryService, libId);
                }
            }
        }
    }

    /** Deletes the Content Library matching the library ID and within the DataStore storage backing. */
    private static void deleteContentLibrary(LocalLibrary localLibraryService, String libraryId) {
        boolean canDelete = false;
        // Retrieve the local content library
        LibraryModel localLibrary = localLibraryService.get(libraryId);
        for (StorageBacking storageBackingTmp : localLibrary.getStorageBackings()) {
            if (storageBacking.equals(storageBackingTmp)) {
                canDelete = true;
                break;
            }
        }
        if (canDelete) {
            localLibraryService.delete(libraryId);
            log.info("Deleted Content Library : {}", libraryId);
        } else {
            log.info("Can not delete Content Library : {}", libraryId);
        }
    }
}
