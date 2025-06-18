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
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

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

/** This sample attaches a given virtual storage object (FCD) to the given virtual machine. */
public class FcdAttachToVM {
    private static final Logger log = LoggerFactory.getLogger(FcdAttachToVM.class);
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
    /**
     * REQUIRED: Name of the virtual machine. A minimum virtual machine version of 'vmx-13' is required for the attach
     * operation to succeed.
     */
    public static String vmName = "vmName";
    /**
     * OPTIONAL: Device Key of the controller the disk will connect to. It can be unset if there is only one controller
     * (SCSI or SATA) with the available slot in the virtual machine. If there are multiple SCSI or SATA controllers
     * available, user must specify the controller.
     */
    public static Integer controllerKey = null;
    /** OPTIONAL: Unit number of the virtual machine. */
    public static Integer unitNumber = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FcdAttachToVM.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference vStrObjManagerMoRef = serviceContent.getVStorageObjectManager();

            ManagedObjectReference vmMoRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);

            // We assume that the host managing the VM has access to datastore
            // containing the FCD.

            // Get all the input Mor's required for retrieving VStorageObject.
            ManagedObjectReference datastoreMoRef = propertyCollectorHelper.getMoRefByName(datastoreName, DATASTORE);

            // Retrieve a vStorageObject:
            log.info("Operation: Retrieving a vStorageObject");
            VStorageObject retrievedVStrObj = vimPort.retrieveVStorageObject(
                    vStrObjManagerMoRef, FcdHelper.makeId(vStorageObjectId), datastoreMoRef, null);

            log.info("Success: Retrieved vStorageObject ::{}", vStorageObjectId);
            log.info("Operation: Attaching a given vStorageObject to the given virtualMachine.");

            ManagedObjectReference attachDiskTaskMoRef = vimPort.attachDiskTask(
                    vmMoRef, FcdHelper.makeId(vStorageObjectId), datastoreMoRef, controllerKey, unitNumber);

            if (propertyCollectorHelper.awaitTaskCompletion(attachDiskTaskMoRef)) {
                log.info(
                        "Success: Attached vStorageObjectId : [ Name = {}, Id = {} ] to VM [ Name = {} ]",
                        retrievedVStrObj.getConfig().getName(),
                        retrievedVStrObj.getConfig().getId().getId(),
                        vmName);
            } else {
                String msg = "Error: Attaching [ " + vStorageObjectId + " ] to the VM [ " + vmName + " ] failed.";
                throw new RuntimeException(msg);
            }

            // Retrieve a vStorageObject based on the given vStorageObjectId and
            // verify virtualMachine is reflected as a new consumer in the
            // retrievedVStorageObject when a virtualMachine is reconfigured to ADD a FCD.
            log.info("Operation: Retrieve the vStorageObjects in datastore.");
            VStorageObject retrievedVStrObjWithConsumer = vimPort.retrieveVStorageObject(
                    vStrObjManagerMoRef, FcdHelper.makeId(vStorageObjectId), datastoreMoRef, null);
            if (retrievedVStrObjWithConsumer.getConfig().getId().getId().equals(vStorageObjectId)) {
                if (retrievedVStrObjWithConsumer.getConfig().getConsumerId().get(0) != null) {
                    log.info(
                            "Success: Retrieved vStorageObject :: \n [ Uuid = {} ] is associated with consumer [ ConsumerId = {} ]\n",
                            retrievedVStrObjWithConsumer.getConfig().getId().getId(),
                            retrievedVStrObjWithConsumer
                                    .getConfig()
                                    .getConsumerId()
                                    .get(0)
                                    .getId());
                } else {
                    String msg = "Error: Given vStorageObject [ "
                            + vStorageObjectId
                            + "] does not have a consumer attached to it.";
                    throw new RuntimeException(msg);
                }
            } else {
                String msg = "Error: Given vStorageObject [ " + vStorageObjectId
                        + "] and retrieved VStorageObject are different.";
                throw new RuntimeException(msg);
            }
        }
    }
}
