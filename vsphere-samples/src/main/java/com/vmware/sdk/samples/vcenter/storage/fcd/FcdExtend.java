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

/** This sample extends a virtual storage object (FCD) capacity. */
public class FcdExtend {
    private static final Logger log = LoggerFactory.getLogger(FcdExtend.class);
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

    /** REQUIRED: UUID of the disk. */
    public static String vStorageObjectId = "vStorageObjectId";
    /** REQUIRED: Name of the datastore which contains the virtual storage object. */
    public static String datastoreName = "datastoreName";
    /** REQUIRED: The new capacity of the virtual disk in MB. */
    public static long newCapacityInMB = 0L;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FcdExtend.class, args);

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

            // Extend capacity of VStorageObject
            log.info("Operation: Extending capacity of vStorageObject from vc.");
            ManagedObjectReference extendTaskMoRef = vimPort.extendDiskTask(
                    vStrObjManagerMoRef, FcdHelper.makeId(vStorageObjectId), datastoreMoRef, newCapacityInMB);

            if (propertyCollectorHelper.awaitTaskCompletion(extendTaskMoRef)) {
                log.info(
                        "Success: Extended vStorageObject : \n [ Uuid = {} ]\n [ NewCapacity = {} ]\n",
                        vStorageObjectId,
                        newCapacityInMB);
            } else {
                String msg = "Error: Extending vStorageObject [ " + FcdHelper.makeId(vStorageObjectId) + "] from vc.";
                throw new RuntimeException(msg);
            }

            // Retrieve all the properties of a virtual storage objects based on the
            // Uuid of the vStorageObject and verify the new capacity
            log.info("Operation: Retrieve the extendedVStorageObject from datastore from vc.");
            VStorageObject retrievedVStrObj = vimPort.retrieveVStorageObject(
                    vStrObjManagerMoRef, FcdHelper.makeId(vStorageObjectId), datastoreMoRef, null);
            if (retrievedVStrObj != null) {
                log.info(
                        "Success: Retrieved vStorageObject :: [ {} ] from vc.\n",
                        retrievedVStrObj.getConfig().getId().getId());
            } else {
                String msg = "Error: Retrieving VStorageObject [ " + vStorageObjectId + " ] from vc.";
                throw new RuntimeException(msg);
            }

            // Verify capacity of vStorageObject
            if (retrievedVStrObj.getConfig().getCapacityInMB() == newCapacityInMB) {
                log.info(
                        "Success: Extend vStorageObject capacity :: [ {} ] from vc.",
                        retrievedVStrObj.getConfig().getId().getId());
            } else {
                String msg = "Error: VStorageObject [ " + vStorageObjectId + " ] capacity extend failed from vc.";
                throw new RuntimeException(msg);
            }
        }
    }
}
