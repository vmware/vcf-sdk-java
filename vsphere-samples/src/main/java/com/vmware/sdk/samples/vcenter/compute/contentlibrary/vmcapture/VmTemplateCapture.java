/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.compute.contentlibrary.vmcapture;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.content.LibraryModel;
import com.vmware.content.LocalLibrary;
import com.vmware.content.library.Item;
import com.vmware.content.library.ItemTypes.FindSpec;
import com.vmware.content.library.StorageBacking;
import com.vmware.sdk.samples.helpers.DatastoreHelper;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.Datastore;
import com.vmware.vcenter.ovf.LibraryItem;
import com.vmware.vcenter.ovf.LibraryItemTypes.CreateResult;
import com.vmware.vcenter.ovf.LibraryItemTypes.CreateSpec;
import com.vmware.vcenter.ovf.LibraryItemTypes.CreateTarget;
import com.vmware.vcenter.ovf.LibraryItemTypes.DeployableIdentity;
import com.vmware.vim25.ManagedObjectReference;

/**
 * Demonstrates the workflow to capture vm to content library as vm template.
 *
 * <p>Sample Prerequisites: The sample needs an existing VM to capture and a datastore to create the content library.
 */
public class VmTemplateCapture {
    private static final Logger log = LoggerFactory.getLogger(VmTemplateCapture.class);
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

    /** REQUIRED: The name of the content library. */
    public static String contentLibraryName = "LocalLibraryToCapture";
    /** REQUIRED: The name of the content library item. */
    public static String libItemName = "capturedItem";
    /** REQUIRED: The name of the datastore for content library backing (of type vmfs). */
    public static String dataStoreName = "dataStoreName";
    /** REQUIRED: The Name of the vm to be captured. */
    public static String vmName = "vmName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VmTemplateCapture.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Datastore datastoreService = client.createStub(Datastore.class);
            LibraryItem ovfLibraryItemService = client.createStub(LibraryItem.class);
            LocalLibrary localLibraryService = client.createStub(LocalLibrary.class);
            Item itemService = client.createStub(Item.class);
            PropertyCollectorHelper propertyCollectorHelper =
                    new PropertyCollectorHelper(client.getVimPort(), client.getVimServiceContent());

            // Create content library.
            // Build the storage backing for the library to be created
            StorageBacking storage = DatastoreHelper.createStorageBacking(datastoreService, dataStoreName);

            // Build the specification for the library to be created
            LibraryModel createSpec = new LibraryModel();
            createSpec.setName(contentLibraryName);
            createSpec.setDescription("Local library backed by VC datastore");
            createSpec.setType(LibraryModel.LibraryType.LOCAL);
            createSpec.setStorageBackings(Collections.singletonList(storage));

            // Create a local content library backed the VC datastore using vAPIs
            String clientToken = UUID.randomUUID().toString();
            String libraryId = localLibraryService.create(clientToken, createSpec);
            log.info("Local library created : {}", libraryId);

            // Capture the vm to content library.
            captureVM(ovfLibraryItemService, libraryId, propertyCollectorHelper);

            // Find the library item by name and verify capture vm created new vm template.
            FindSpec findSpec = new FindSpec();
            findSpec.setName(libItemName);

            List<String> itemIds = itemService.find(findSpec);
            if (itemIds.isEmpty()) {
                throw new RuntimeException("Unable to find captured library item with name: " + libItemName);
            }

            String itemId = itemIds.get(0);
            log.info("The VM : {} is captured as library item : {} of type vm-template.", vmName, itemId);

            // cleanup
            if (libraryId != null) {
                // Delete the content library
                localLibraryService.delete(libraryId);
                log.info("Deleted library : {}", libraryId);
            }
        }
    }

    /**
     * Capture the VM to the local library provided.
     *
     * @param libraryId identifier of the library on which vm will be captured
     */
    private static void captureVM(
            LibraryItem ovfLibraryItemService, String libraryId, PropertyCollectorHelper propertyCollectorHelper)
            throws Exception {
        String entityType = "VirtualMachine"; // Substitute 'VirtualApp' for vApp
        ManagedObjectReference entityId = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);

        DeployableIdentity deployable = new DeployableIdentity();
        deployable.setType(entityType);
        deployable.setId(entityId.getValue());

        CreateTarget target = new CreateTarget();
        target.setLibraryId(libraryId);

        CreateSpec spec = new CreateSpec();
        spec.setName(libItemName);
        spec.setDescription("VM template created from a VM capture");

        // Create OVF library item
        CreateResult result = ovfLibraryItemService.create(null, deployable, target, spec);
        if (result.getSucceeded()) {
            log.info("The vm capture to content library succeeded");
        } else {
            log.error("The vm capture to content library failed");
        }
    }
}
