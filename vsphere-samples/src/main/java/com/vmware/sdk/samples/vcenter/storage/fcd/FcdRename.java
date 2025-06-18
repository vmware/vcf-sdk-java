/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.storage.fcd;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.DATASTORE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.storage.fcd.helpers.FcdHelper;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VStorageObject;
import com.vmware.vim25.VimPortType;

/** This sample renames a given virtual storage object. */
public class FcdRename {
    private static final Logger log = LoggerFactory.getLogger(FcdRename.class);
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
    /** REQUIRED: Name of the datastore which contains the virtual storage object. */
    public static String datastoreName = "datastoreName";
    /** REQUIRED: The new name for the virtual storage object. */
    public static String newVStorageObjectName = "newVStorageObjectName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FcdRename.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            // Get vStorageObjectManager moRef.
            ManagedObjectReference vStrObjManagerMoRef = serviceContent.getVStorageObjectManager();

            // Get all the input moRefs required for retrieving VStorageObject.
            ManagedObjectReference datastoreMoRef = propertyCollectorHelper.getMoRefByName(datastoreName, DATASTORE);

            if (datastoreMoRef == null) {
                throw new RuntimeException("The datastore name is not valid.");
            }

            // Retrieve vStorageObject before renaming
            log.info("Operation: Retrieve vStorageObject before renaming from datastore from vc.");
            VStorageObject retrievedVStrObjBeforeRename = vimPort.retrieveVStorageObject(
                    vStrObjManagerMoRef, FcdHelper.makeId(vStorageObjectId), datastoreMoRef, null);
            if (retrievedVStrObjBeforeRename != null) {
                log.info(
                        "Success: Retrieved vStorageObject :: \n [ UUid = {} ] \n [ name = {} ]\n ",
                        retrievedVStrObjBeforeRename.getConfig().getId().getId(),
                        retrievedVStrObjBeforeRename.getConfig().getName());
            } else {
                String msg = "Error: Retrieving VStorageObject [ " + FcdHelper.makeId(vStorageObjectId);
                throw new RuntimeException(msg);
            }

            // Rename a vStorageObject:
            log.info("Operation: Renaming the given vStorageObject from vc.");
            vimPort.renameVStorageObject(
                    vStrObjManagerMoRef, FcdHelper.makeId(vStorageObjectId), datastoreMoRef, newVStorageObjectName);

            // Retrieve vStorageObject after renaming
            log.info("Operation: Retrieve the vStorageObject after renaming from datastore from vc.");
            VStorageObject retrievedVStrObjAfterRename = vimPort.retrieveVStorageObject(
                    vStrObjManagerMoRef, FcdHelper.makeId(vStorageObjectId), datastoreMoRef, null);
            if (retrievedVStrObjAfterRename != null) {
                log.info(
                        "Success: Retrieved vStorageObject :: \n [ UUid = {} ]\n [ name = {} ] \n",
                        retrievedVStrObjAfterRename.getConfig().getId().getId(),
                        retrievedVStrObjAfterRename.getConfig().getName());
            } else {
                String msg = "Error: Retrieving VStorageObject [ " + FcdHelper.makeId(vStorageObjectId);
                throw new RuntimeException(msg);
            }

            // Verify rename vStorageObject
            if (retrievedVStrObjAfterRename.getConfig().getName().equals(newVStorageObjectName)) {
                log.info(
                        "Success: Renamed vStorageObject :: [ {} ] from vc",
                        retrievedVStrObjAfterRename.getConfig().getId().getId());
            } else {
                String msg = "Error: VStorageObject [ "
                        + retrievedVStrObjAfterRename.getConfig().getId().getId()
                        + "] rename to [ " + newVStorageObjectName + " ] from vc";
                throw new RuntimeException(msg);
            }
        }
    }
}
