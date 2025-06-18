/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.storage.vslm.fcd;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.storage.fcd.helpers.FcdHelper;
import com.vmware.sdk.samples.vcenter.storage.vslm.fcd.helpers.FcdVslmHelper;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VStorageObject;
import com.vmware.vim25.VimPortType;
import com.vmware.vslm.VslmPortType;
import com.vmware.vslm.VslmVsoVStorageObjectAssociations;
import com.vmware.vslm.VslmVsoVStorageObjectAssociationsVmDiskAssociation;

/**
 * This sample executes below operations on a given VStorageObject from vslm:
 *
 * <ol>
 *   <li>Register a given legacy disk as FCD.
 *   <li>Attach a given virtual storage object to the given virtual machine.
 *   <li>Retrieve vm associations for each virtual storage object in the query.
 * </ol>
 *
 * <p>Sample Prerequisites:
 *
 * <ul>
 *   <li>Existing VStorageObject ID
 *   <li>Existing vm name
 * </ul>
 */
public class FcdLegacyDiskAndAssociationOperations {
    private static final Logger log = LoggerFactory.getLogger(FcdLegacyDiskAndAssociationOperations.class);

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
    /** REQUIRED: File name of the legacy disk. Ex: [sharedVmfs-0] VM_NAME/VM_NAME.vmdk. */
    public static String legacyDiskFileName = "legacyDiskFileName";
    /** REQUIRED: Name of the datacenter. */
    public static String dataCenterName = "dataCenterName";
    /**
     * REQUIRED: Name of the virtual machine. A minimum virtual machine version of 'vmx-13' is required for the attach
     * operation to succeed. Vm can be present in any folder, host or datastore.
     */
    public static String vmName = "vmName";
    /** OPTIONAL: Name of the newly created first class disk object. */
    public static String fcdName = null;
    /**
     * OPTIONAL: Device Key of the controller the disk will connect to. It can be unset if there is only one controller
     * (SCSI or SATA) with the available slot in the virtual machine. If there are multiple SCSI or SATA controllers
     * available, user must specify the controller.
     */
    public static Integer controllerKey = null;
    /** OPTIONAL: Unit number of the virtual machine. */
    public static Integer unitNumber = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FcdLegacyDiskAndAssociationOperations.class, args);

        VcenterClientFactory clientFactory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = clientFactory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);
            VslmPortType vslmPort = client.getVslmPort();

            ManagedObjectReference vStorageObjectManager =
                    client.getVslmServiceInstanceContent().getVStorageObjectManager();

            FcdVslmHelper vslmHelper = new FcdVslmHelper(vslmPort);

            log.info(
                    "Registering Legacy disk as FCD, attaching FCD to VM and retrieving FCD associations from VSLM ::");

            // Generate the httpUrl from diskPath which the vc recognizes.
            String legacyDiskPathForVc = getDiskPathForVc(legacyDiskFileName);

            // Register the disk as FirstClassDisk.
            log.info("Operation: Register a legacy disk as FCD with disk Path :: {}", legacyDiskPathForVc);
            VStorageObject registeredVStrObj =
                    vslmPort.vslmRegisterDisk(vStorageObjectManager, legacyDiskPathForVc, fcdName);

            log.info(
                    "Success: Registered Disk(now a vStorageObject) : [Uuid = {} ] with Name [ {} ]\n",
                    registeredVStrObj.getConfig().getId().getId(),
                    registeredVStrObj.getConfig().getName());

            // Get Virtual Machine moRef.
            ManagedObjectReference vmMoRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);
            log.info("VM Managed Object Reference : {}", vmMoRef.getValue());

            // Retrieve a vStorageObject.
            log.info("Operation: Retrieving a vStorageObject");
            VStorageObject retrievedVStrObj =
                    vslmPort.vslmRetrieveVStorageObject(vStorageObjectManager, FcdHelper.makeId(vStorageObjectId));
            log.info(
                    "Success: Retrieved vStorageObject ::{}",
                    retrievedVStrObj.getConfig().getId().getId());

            log.info("Operation: Attaching a given vStorageObject to the given virtualMachine.");
            ManagedObjectReference taskMoRef = vslmPort.vslmAttachDiskTask(
                    vStorageObjectManager, retrievedVStrObj.getConfig().getId(), vmMoRef, controllerKey, unitNumber);

            boolean isAttachDiskSucceeded = vslmHelper.waitForTask(taskMoRef);
            if (isAttachDiskSucceeded) {
                log.info(
                        "Success: Attached vStorageObjectId : [ Name = {},  Id = {}] to VM [ Name = {}]\n",
                        retrievedVStrObj.getConfig().getName(),
                        retrievedVStrObj.getConfig().getId().getId(),
                        vmName);
            } else {
                String message = "Error: Attaching [ "
                        + retrievedVStrObj.getConfig().getId().getId() + "] vStorageObject";
                throw new RuntimeException(message);
            }

            // Retrieve a vStorageObject based on the given vStorageObjectId and
            // verify virtualMachine is reflected as a new consumer in the
            // retrievedVStorageObject when a virtualMachine is reconfigured to ADD a FCD.
            log.info("Operation: Retrieve the vStorageObjects in datastore.");
            VStorageObject retrievedVStrObjWithConsumer = vslmPort.vslmRetrieveVStorageObject(
                    vStorageObjectManager, retrievedVStrObj.getConfig().getId());
            if (retrievedVStrObjWithConsumer
                    .getConfig()
                    .getId()
                    .getId()
                    .equals(retrievedVStrObj.getConfig().getId().getId())) {
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
                    String message = "Error: Given vStorageObject [ "
                            + retrievedVStrObj.getConfig().getId().getId()
                            + "] does not have a consumer attached to it.";
                    throw new RuntimeException(message);
                }
            } else {
                String message = "Error: Given vStorageObject [ "
                        + retrievedVStrObj.getConfig().getId().getId()
                        + "] and retrieved VStorageObject are different.";
                throw new RuntimeException(message);
            }

            log.info("Operation : Retrieve vStorage object association ::");
            List<VslmVsoVStorageObjectAssociations> vStorageObjectAssociations =
                    vslmPort.vslmRetrieveVStorageObjectAssociations(
                            vStorageObjectManager,
                            Collections.singletonList(
                                    retrievedVStrObj.getConfig().getId()));

            log.info(
                    "RetrieveVStorageObjectAssociations returned association list of size : [ {} ] for vStorageObject : [ {} ] from vslm.\n",
                    vStorageObjectAssociations.size(),
                    retrievedVStrObj.getConfig().getId());

            for (VslmVsoVStorageObjectAssociations vStorageObjectAssociation : vStorageObjectAssociations) {
                for (VslmVsoVStorageObjectAssociationsVmDiskAssociation vmDiskAssociation :
                        vStorageObjectAssociation.getVmDiskAssociation()) {
                    log.info(
                            "VirtualMachine Key :: [ {} ]and Disk Key :: [ {} ]",
                            vmDiskAssociation.getVmId(),
                            vmDiskAssociation.getDiskKey());
                }
            }
        }
    }

    /**
     * Util method to get the diskPath recognized by vc for a given disk.
     *
     * @return filePath of vStorageObject
     */
    private static String getDiskPathForVc(String fileNameOfDisk) {
        // Ex: vmdkLocation is :: [sharedVmfs-0] TestVm_3PYN/TestVm_3PYN.vmdk.
        String regex1 = "\\[(.*)\\]\\s(.*)/(.*\\.vmdk)";
        String ds = null;
        String vmFolder = null;
        String vmdk = null;
        if (Pattern.matches(regex1, fileNameOfDisk)) {
            log.info("FileName Pattern matches required pattern.");
            Pattern diskNamePattern = Pattern.compile(regex1);
            Matcher fileNameMatcher = diskNamePattern.matcher(fileNameOfDisk);
            if (fileNameMatcher.find()) {
                ds = fileNameMatcher.group(1);
                vmFolder = fileNameMatcher.group(2);
                vmdk = fileNameMatcher.group(3);
            }
        }
        /*
         * diskPath format as recognized by VC:
         * https://VCIP/folder/PathToVmdkInsideDatastore
         * ?dcPath=<DataCenterName>&dsName=DatastoreName
         *
         * Ex: diskpath = https://VC_NAME/folder/VM_NAME/VM_NAME.vmdk
         * ?dcPath=vcqaDC&dsName=sharedVmfs-0
         */

        return "https://" + serverAddress + "/" + "folder/" + vmFolder + "/" + vmdk + "?dcPath=" + dataCenterName
                + "&dsName=" + ds;
    }
}
