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

/** This sample inflates a sparse or thin-provisioned virtual disk up to the full size. */
public class FcdInflateDisk {
    private static final Logger log = LoggerFactory.getLogger(FcdInflateDisk.class);
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

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FcdInflateDisk.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference vStrObjManagerMoRef = serviceContent.getVStorageObjectManager();

            // Get all the input moRefs required for retrieving VStorageObject.
            ManagedObjectReference datastoreMoRef = propertyCollectorHelper.getMoRefByName(datastoreName, DATASTORE);

            if (datastoreMoRef == null) {
                throw new RuntimeException("The datastore name is not valid.");
            }

            // Retrieve vStorageObject before inflate disk
            VStorageObject vStorageObjectBeforeInflate = vimPort.retrieveVStorageObject(
                    vStrObjManagerMoRef, FcdHelper.makeId(vStorageObjectId), datastoreMoRef, null);

            if (vStorageObjectBeforeInflate != null) {
                log.info("Success: Retrieved vStorageObject before inflate:: [ {} ] from vc.\n", vStorageObjectId);
            } else {
                String msg = "Error: Retrieving VStorageObject [ " + vStorageObjectId + " ] before inflate";
                throw new RuntimeException(msg);
            }

            // Inflate a sparse or thin-provisioned virtual disk up to the full size.
            log.info("Operation: Inflate a sparse or thin-provisioned virtual disk up to the full size from vc.");
            ManagedObjectReference inflateDiskTaskMoRef =
                    vimPort.inflateDiskTask(vStrObjManagerMoRef, FcdHelper.makeId(vStorageObjectId), datastoreMoRef);
            if (propertyCollectorHelper.awaitTaskCompletion(inflateDiskTaskMoRef)) {
                log.info("Success: Inflated vStorageObject : [ Uuid = {} ] from vc.\n", vStorageObjectId);
            } else {
                String msg = "Error: Inflating vStorageObject [ " + vStorageObjectId + " ] from vc.";
                throw new RuntimeException(msg);
            }

            // Retrieve vStorageObject after inflate disk
            VStorageObject inflatedDisk = vimPort.retrieveVStorageObject(
                    vStrObjManagerMoRef, FcdHelper.makeId(vStorageObjectId), datastoreMoRef, null);

            if (inflatedDisk != null) {
                log.info("Success: Retrieved vStorageObject after inflate:: [ {} ] from vc.\n", vStorageObjectId);
            } else {
                String msg = "Error: Retrieving VStorageObject [ " + vStorageObjectId + " ] after inflate from vc.";
                throw new RuntimeException(msg);
            }
        }
    }
}
