/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.compute.contentlibrary.crud;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static java.util.Objects.requireNonNullElse;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.content.LibraryModel;
import com.vmware.content.LocalLibrary;
import com.vmware.content.library.StorageBacking;
import com.vmware.sdk.samples.helpers.DatastoreHelper;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.Datastore;

/**
 * Demonstrates the basic operations of a content library. The sample also demonstrates the interoperability of the VIM
 * and vAPI.
 *
 * <p>Sample Prerequisites: The sample needs an existing VC datastore with available storage.
 */
public class LibraryCrud {
    private static final Logger log = LoggerFactory.getLogger(LibraryCrud.class);
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

    /** REQUIRED: The name of the VC datastore to be used for the local library. */
    public static String dsName = "dsName";
    /** OPTIONAL: The name of the local content library to be created. Default value is "demo-local-lib". */
    public static String libName = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(LibraryCrud.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Datastore datastoreService = client.createStub(Datastore.class);
            LocalLibrary localLibraryService = client.createStub(LocalLibrary.class);

            // List of visible content libraries
            List<String> visibleCls = localLibraryService.list();
            log.info("All libraries : {}", visibleCls);
            // Build the storage backing for the libraries to be created
            StorageBacking storageBacking = DatastoreHelper.createStorageBacking(datastoreService, dsName);

            // Build the specification for the library to be created
            LibraryModel createSpec = new LibraryModel();
            createSpec.setName(requireNonNullElse(libName, "demo-local-lib"));
            createSpec.setDescription("Local library backed by VC datastore");
            createSpec.setType(LibraryModel.LibraryType.LOCAL);
            createSpec.setStorageBackings(Collections.singletonList(storageBacking));

            // Create a local content library backed the VC datastore using vAPIs
            String clientToken = UUID.randomUUID().toString();
            String libraryId = localLibraryService.create(clientToken, createSpec);
            log.info("Local library created : {}", libraryId);

            // Retrieve the local content library
            LibraryModel localLibrary = localLibraryService.get(libraryId);
            log.info("Retrieved library : {}", localLibrary);

            // Update the local content library
            LibraryModel updateSpec = new LibraryModel();
            updateSpec.setDescription("new description");
            localLibraryService.update(libraryId, updateSpec);
            log.info("Updated library description");

            // cleanup
            if (localLibrary != null) {
                // Delete the content library
                localLibraryService.delete(localLibrary.getId());
                log.info("Deleted Local Content Library : {}", localLibrary.getId());
            }
        }
    }
}
