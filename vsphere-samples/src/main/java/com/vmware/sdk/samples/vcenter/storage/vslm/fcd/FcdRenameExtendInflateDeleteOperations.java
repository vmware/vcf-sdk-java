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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.storage.fcd.helpers.FcdHelper;
import com.vmware.sdk.samples.vcenter.storage.vslm.fcd.helpers.FcdVslmHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.BaseConfigInfoBackingInfo;
import com.vmware.vim25.BaseConfigInfoDiskFileBackingInfo;
import com.vmware.vim25.BaseConfigInfoRawDiskMappingBackingInfo;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VStorageObject;
import com.vmware.vslm.VslmPortType;

/**
 * This sample executes below operations on a given VStorageObject from vslm:
 *
 * <ol>
 *   <li>Rename a given virtual storage object.
 *   <li>Extend a virtual storage object capacity.
 *   <li>Inflate a sparse or thin-provisioned virtual disk up to the full size.
 *   <li>Delete a given virtual storage object.
 * </ol>
 *
 * <p>Sample Prerequisites: Existing VStorageObject ID
 */
public class FcdRenameExtendInflateDeleteOperations {
    private static final Logger log = LoggerFactory.getLogger(FcdRenameExtendInflateDeleteOperations.class);

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
    /** REQUIRED: The new name for the virtual storage object. */
    public static String newVStorageObjectName = "newVStorageObjectName";
    /** REQUIRED: The new capacity of the virtual disk in MB, which should be greater than the original disk size. */
    public static long newCapacityInMB = 0L;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FcdRenameExtendInflateDeleteOperations.class, args);

        VcenterClientFactory clientFactory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = clientFactory.createClient(username, password, null)) {
            VslmPortType vslmPort = client.getVslmPort();
            FcdVslmHelper vslmHelper = new FcdVslmHelper(vslmPort);

            ManagedObjectReference vStorageObjectManager =
                    client.getVslmServiceInstanceContent().getVStorageObjectManager();

            log.info("Invoking rename, extend, inflate and delete operations on VStorageObject from VSLM ::");

            // Retrieves vStorageObject before renaming and verify
            log.info(
                    "Operation: Retrieve vStorageObject after revertVStorageObject and before renaming from datastore from vslm.");
            VStorageObject retrievedVStrObjBeforeRename =
                    vslmPort.vslmRetrieveVStorageObject(vStorageObjectManager, FcdHelper.makeId(vStorageObjectId));
            if (retrievedVStrObjBeforeRename != null) {
                log.info(
                        "Success: Retrieved vStorageObject :: \n [ UUid = {} ] \n [ name = {} ]\n ",
                        retrievedVStrObjBeforeRename.getConfig().getId().getId(),
                        retrievedVStrObjBeforeRename.getConfig().getName());
            } else {
                String message = "Error: Retrieving VStorageObject [ " + FcdHelper.makeId(vStorageObjectId);
                throw new RuntimeException(message);
            }

            // Rename a vStorageObject
            log.info("Operation: Renaming the given vStorageObject from vslm.");
            vslmPort.vslmRenameVStorageObject(
                    vStorageObjectManager, FcdHelper.makeId(vStorageObjectId), newVStorageObjectName);

            // Retrieve vStorageObject after renaming
            log.info("Operation: Retrieve the vStorageObject after renaming from datastore from vslm.");
            VStorageObject retrievedVStrObjAfterRename =
                    vslmPort.vslmRetrieveVStorageObject(vStorageObjectManager, FcdHelper.makeId(vStorageObjectId));
            if (retrievedVStrObjAfterRename != null) {
                log.info(
                        "Success: Retrieved vStorageObject :: \n [ UUid = {} ]\n [ name = {} ] \n",
                        retrievedVStrObjAfterRename.getConfig().getId().getId(),
                        retrievedVStrObjAfterRename.getConfig().getName());
            } else {
                String message = "Error: Retrieving VStorageObject [ " + FcdHelper.makeId(vStorageObjectId);
                throw new RuntimeException(message);
            }

            // Verify rename vStorageObject
            if (retrievedVStrObjAfterRename.getConfig().getName().equals(newVStorageObjectName)) {
                log.info(
                        "Success: Renamed vStorageObject :: [ {} ] from vslm",
                        retrievedVStrObjAfterRename.getConfig().getId().getId());
            } else {
                String message = "Error: VStorageObject [ "
                        + retrievedVStrObjAfterRename.getConfig().getId().getId() + "] rename to [ "
                        + newVStorageObjectName
                        + " ] from vslm";
                throw new RuntimeException(message);
            }

            // Extend capacity of VStorageObject
            log.info("Operation: Extending capacity of vStorageObject from vslm.");
            ManagedObjectReference taskMoRef = vslmPort.vslmExtendDiskTask(
                    vStorageObjectManager, FcdHelper.makeId(vStorageObjectId), newCapacityInMB);

            boolean isExtendDiskSucceeded = vslmHelper.waitForTask(taskMoRef);
            if (isExtendDiskSucceeded) {
                log.info(
                        "Success: Extended vStorageObject : \n [ Uuid = {} ]\n [ NewCapacity = {} ]\n",
                        vStorageObjectId,
                        newCapacityInMB);
            } else {
                String message =
                        "Error: Extending vStorageObject [ " + FcdHelper.makeId(vStorageObjectId) + "] from vslm.";
                throw new RuntimeException(message);
            }

            // Retrieve all the properties of a virtual storage objects based on the
            // Uuid of the vStorageObject and verify the new capacity
            log.info("Operation: Retrieve the extendedVStorageObject from datastore from vslm.");
            VStorageObject retrievedVStrObjAfterExtend =
                    vslmPort.vslmRetrieveVStorageObject(vStorageObjectManager, FcdHelper.makeId(vStorageObjectId));
            if (retrievedVStrObjAfterExtend != null) {
                log.info(
                        "Success: Retrieved vStorageObject :: [ {} ] from vslm.\n",
                        retrievedVStrObjAfterExtend.getConfig().getId().getId());
            } else {
                String message = "Error: Retrieving VStorageObject [ " + vStorageObjectId + " ] from vslm.";
                throw new RuntimeException(message);
            }

            // Verify capacity of vStorageObject
            if (retrievedVStrObjAfterExtend.getConfig().getCapacityInMB() == newCapacityInMB) {
                log.info(
                        "Success: Extend vStorageObject capacity :: [ {} ] from vslm.",
                        retrievedVStrObjAfterExtend.getConfig().getId().getId());
            } else {
                String message = "Error: VStorageObject [ " + vStorageObjectId + " ] capacity extend failed from vslm.";
                throw new RuntimeException(message);
            }

            // Retrieve vStorageObject before inflate disk
            VStorageObject vStorageObjectBeforeInflate =
                    vslmPort.vslmRetrieveVStorageObject(vStorageObjectManager, FcdHelper.makeId(vStorageObjectId));

            if (vStorageObjectBeforeInflate != null) {
                log.info(
                        "Success: Retrieved vStorageObject before inflate :: \n [ {} ]\n from vslm.\n",
                        vStorageObjectId);
            } else {
                String message = "Error: Retrieving VStorageObject [ " + vStorageObjectId + " ] before inflate";
                throw new RuntimeException(message);
            }

            // Inflate a sparse or thin-provisioned virtual disk up to the full size.
            log.info("Operation: Inflate a sparse or thin-provisioned virtual disk up to the full size from vslm.");
            ManagedObjectReference inflateTaskMoRef =
                    vslmPort.vslmInflateDiskTask(vStorageObjectManager, FcdHelper.makeId(vStorageObjectId));

            boolean isInflateDiskSucceeded = vslmHelper.waitForTask(inflateTaskMoRef);
            if (isInflateDiskSucceeded) {
                log.info("Success: Inflated vStorageObject : [ Uuid = {} ] from vslm.\n", vStorageObjectId);
            } else {
                String message = "Error: Inflating vStorageObject [ " + vStorageObjectId + " ] from vslm.";
                throw new RuntimeException(message);
            }

            // Retrieve vStorageObject after inflate disk
            VStorageObject vStorageObjectAfterInflate =
                    vslmPort.vslmRetrieveVStorageObject(vStorageObjectManager, FcdHelper.makeId(vStorageObjectId));

            if (vStorageObjectAfterInflate != null) {
                log.info("Success: Retrieved vStorageObject after inflate :: [ {} ] from vslm.\n", vStorageObjectId);
            } else {
                String message =
                        "Error: Retrieving VStorageObject [ " + vStorageObjectId + " ] after inflate from vslm.";
                throw new RuntimeException(message);
            }

            // Print provisioning type before and after inflate
            String diskProvisioningTypeBefore = getProvisioningType(vStorageObjectBeforeInflate);
            String diskProvisioningTypeAfter = getProvisioningType(vStorageObjectAfterInflate);
            log.info("Provisioning type before inflate is : [ {} ] \n", diskProvisioningTypeBefore);
            log.info("Provisioning type after inflate is : [ {} ] \n", diskProvisioningTypeAfter);

            // Verify disk path after and before inflate
            String filePathBeforeInflate = FcdHelper.getFcdFilePath(vStorageObjectBeforeInflate);
            if (filePathBeforeInflate == null) {
                String message =
                        "Error: File backing doesn't present for source vStorageObject [ " + vStorageObjectId + " ]";
                throw new RuntimeException(message);
            }

            String filePathAfterInflate = FcdHelper.getFcdFilePath(vStorageObjectAfterInflate);
            if (filePathAfterInflate == null) {
                String message = "Error: File backing doesn't present for vStorageObject [ " + vStorageObjectId
                        + " ] after inflate";
                throw new RuntimeException(message);
            }

            if (!filePathAfterInflate.equals(filePathBeforeInflate)) {
                String message =
                        "Error: File path changed for vStorageObject [ " + vStorageObjectId + " ] after inflate";
                throw new RuntimeException(message);
            } else {
                log.info("File Path before and after inflate is same.");
            }

            // Verify capacity before and after inflate
            long capacityBeforeInflate = vStorageObjectBeforeInflate.getConfig().getCapacityInMB();
            long capacityAfterInflate = vStorageObjectAfterInflate.getConfig().getCapacityInMB();
            if (capacityBeforeInflate != capacityAfterInflate) {
                String message =
                        "Error: Disk size changed for vStorageObject [ " + vStorageObjectId + " ] after inflate";
                throw new RuntimeException(message);
            } else {
                log.info("Disk size before and after inflate is same.");
            }

            log.info("Operation: Deleting a vStorageObject");
            ManagedObjectReference deleteTaskMoRef =
                    vslmPort.vslmDeleteVStorageObjectTask(vStorageObjectManager, FcdHelper.makeId(vStorageObjectId));

            boolean isDeleteDiskSucceeded = vslmHelper.waitForTask(deleteTaskMoRef);
            if (isDeleteDiskSucceeded) {
                log.info("Delete disk task is succeeded");
                log.info("Success: Deleted vStorageObject : \n [ Uuid = {} ]\n", vStorageObjectId);
            } else {
                String message = "Error: Deleting [ " + vStorageObjectId + "] vStorageObject";
                throw new RuntimeException(message);
            }
        }
    }

    /** Method to return provisioning type of a VStorageObject. */
    private static String getProvisioningType(VStorageObject vStorageObject) {
        String provisionType = null;

        BaseConfigInfoBackingInfo backingInfo = vStorageObject.getConfig().getBacking();
        if (backingInfo instanceof BaseConfigInfoDiskFileBackingInfo) {
            BaseConfigInfoDiskFileBackingInfo diskFileBackingInfo = (BaseConfigInfoDiskFileBackingInfo) backingInfo;
            provisionType = diskFileBackingInfo.getProvisioningType().toString();
        } else if (backingInfo instanceof BaseConfigInfoRawDiskMappingBackingInfo) {
            BaseConfigInfoRawDiskMappingBackingInfo rdmBackingInfo =
                    (BaseConfigInfoRawDiskMappingBackingInfo) backingInfo;
            provisionType = rdmBackingInfo.getCompatibilityMode();
        }
        return provisionType;
    }
}
