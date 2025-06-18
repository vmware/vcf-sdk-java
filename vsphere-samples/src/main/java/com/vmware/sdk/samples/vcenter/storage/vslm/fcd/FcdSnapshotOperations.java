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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.storage.fcd.helpers.FcdHelper;
import com.vmware.sdk.samples.vcenter.storage.vslm.fcd.helpers.FcdVslmHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.DiskChangeInfo;
import com.vmware.vim25.ID;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VStorageObject;
import com.vmware.vim25.VStorageObjectSnapshotDetails;
import com.vmware.vim25.VStorageObjectSnapshotInfo;
import com.vmware.vim25.VStorageObjectSnapshotInfoVStorageObjectSnapshot;
import com.vmware.vim25.VirtualMachineProfileSpec;
import com.vmware.vslm.VslmPortType;
import com.vmware.vslm.VslmTaskInfo;

/**
 * This sample executes below snapshot related operation on a snapshot of a given VStorageObject from vslm:
 *
 * <ol>
 *   <li>Creates snapshot of a given VStorageObject.
 *   <li>Retrieves Snapshot Info of a given VStorageObject.
 *   <li>Retrieves Snapshot Details of a given VStorageObject.
 *   <li>Query disk changed areas of a given VStorageObject.
 *   <li>Creates vStorageObject from snapshot.
 *   <li>Reverts VStorageObject to a given snapshot.
 *   <li>Deletes snapshot of a given VStorageObject.
 * </ol>
 *
 * <p>Sample Prerequisites: Existing VStorageObject ID
 */
public class FcdSnapshotOperations {
    private static final Logger log = LoggerFactory.getLogger(FcdSnapshotOperations.class);

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
    /** REQUIRED: A short description to be associated with the snapshot. */
    public static String description = "description";
    /** REQUIRED: FCD name is required to create disk from snapshot. */
    public static String fcdName = "fcdName";
    /** OPTIONAL: SPBM Profile requirement on the new virtual storage object. */
    public static String pbmProfileId = null;
    /** OPTIONAL: ID of the replication device group. */
    public static String deviceGroupId = null;
    /** OPTIONAL: ID of the fault domain to which the group belongs. */
    public static String faultDomainId = null;
    /** OPTIONAL: Relative location in the specified datastore where disk needs to be created. */
    public static String datastorePath = null;

    private static final String CONTROL_FLAG = "enableChangedBlockTracking";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FcdSnapshotOperations.class, args);

        VcenterClientFactory clientFactory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = clientFactory.createClient(username, password, null)) {
            VslmPortType vslmPort = client.getVslmPort();
            FcdVslmHelper vslmHelper = new FcdVslmHelper(vslmPort);

            ManagedObjectReference vStorageObjectManager =
                    client.getVslmServiceInstanceContent().getVStorageObjectManager();

            log.info("Invoking Snapshot related APIs from VSLM ::");

            // set control flag on vStorage object to enable change block tracking.
            vslmPort.vslmSetVStorageObjectControlFlags(
                    vStorageObjectManager, FcdHelper.makeId(vStorageObjectId), Collections.singletonList(CONTROL_FLAG));

            VStorageObject vStrObj =
                    vslmPort.vslmRetrieveVStorageObject(vStorageObjectManager, FcdHelper.makeId(vStorageObjectId));

            // Create snapshot of vStorageObject
            log.info("Operation: Creating snapshot of given vStorageObject from vslm.");
            ManagedObjectReference taskMoRef = vslmPort.vslmCreateSnapshotTask(
                    vStorageObjectManager, FcdHelper.makeId(vStorageObjectId), description);

            ID snapshotId = null;

            boolean isSnapshotDiskSucceeded = vslmHelper.waitForTask(taskMoRef);
            if (isSnapshotDiskSucceeded) {
                log.info("snapshot disk task is succeeded");
                VslmTaskInfo taskInfo = vslmPort.vslmQueryInfo(taskMoRef);
                snapshotId = (ID) taskInfo.getResult();
                log.info(
                        "Success: Created snapshot : [ Id = {} ] of vStorageObject : [ UUID = {} ] from vslm.",
                        snapshotId.getId(),
                        vStorageObjectId);
            } else {
                String message = "Error: Creating vStorageObject [ " + vStorageObjectId + "] snapshot from vslm.";
                throw new RuntimeException(message);
            }

            // Retrieve all the snapshots of vStorageObject and
            // verify if created snapshot is present in returned list
            log.info("Operation: Retrieve VStorageObject snapshot info from vslm.");
            VStorageObjectSnapshotInfo retrievedSnapshotInfo =
                    vslmPort.vslmRetrieveSnapshotInfo(vStorageObjectManager, FcdHelper.makeId(vStorageObjectId));
            if (!retrievedSnapshotInfo.getSnapshots().isEmpty()
                    && isSnapshotIdInSnapshotList(retrievedSnapshotInfo, snapshotId)) {
                log.info("Success: Retrieved vStorageObject Snapshot :: [ {} ] from vslm.", snapshotId.getId());
            } else {
                String message = "Error: Retrieving VStorageObject Snapshot [ " + snapshotId.getId() + " ] from vslm.";
                throw new RuntimeException(message);
            }

            // Retrieve snapshot details
            log.info("Operation: Retrieve VStorageObject snapshot details from vslm.");
            VStorageObjectSnapshotDetails snapshotDetails = vslmPort.vslmRetrieveSnapshotDetails(
                    vStorageObjectManager, FcdHelper.makeId(vStorageObjectId), snapshotId);
            if (snapshotDetails != null) {
                String expectedSnapshotPath = FcdHelper.getFcdFilePath(vStrObj);
                String actualSnapshotPath = snapshotDetails.getPath();

                if (expectedSnapshotPath.equals(actualSnapshotPath)) {
                    log.info(
                            "Success: Retrieved vStorageObject Snapshot details{} of snapshot id :: [ {} ], snapshot path :: [ %s ] from vslm.",
                            snapshotId.getId(), actualSnapshotPath);
                } else {
                    log.error(
                            "Retrieved vStorageObject Snapshot details of snapshot id :: [ {} ], actual snapshot path :: [ {} ] doesn't match with expected snapshot path :: [ {} ] from vslm.",
                            snapshotId.getId(),
                            actualSnapshotPath,
                            expectedSnapshotPath);
                }
            } else {
                String message =
                        "Error: Retrieving VStorageObject Snapshot Details [ " + snapshotId.getId() + " ] from vslm.";
                throw new RuntimeException(message);
            }

            // query a list of areas of a virtual disk that have been modified since a well-defined point in the past.
            log.info("Operation: query disk changed areas from vslm.");
            String changeId = snapshotDetails.getChangedBlockTrackingId();

            long startOffset = 0L;
            log.info("Retrieved change id :{}", changeId);
            DiskChangeInfo diskChangeInfo = vslmPort.vslmQueryChangedDiskAreas(
                    vStorageObjectManager, FcdHelper.makeId(vStorageObjectId), snapshotId, startOffset, changeId);
            if (diskChangeInfo != null) {
                log.info(
                        "Success: Query disk change area of vStorageObject :: vStorageObjectId \n [ {} ]\n",
                        vStorageObjectId);
            } else {
                String message = "Error: Query disk change area of " + "vStorageObject [ "
                        + FcdHelper.makeId(vStorageObjectId) + "] from vslm.";
                throw new RuntimeException(message);
            }

            List<VirtualMachineProfileSpec> profileSpec = null;
            if (pbmProfileId != null) {
                profileSpec = FcdHelper.generateVirtualMachineProfileSpec(pbmProfileId, deviceGroupId, faultDomainId);
            }

            // Create vStorageObject from snapshot
            log.info("Operation: Creating snapshot of given vStorageObject from vslm.");
            ManagedObjectReference createDiskFromSnapTaskMoRef = vslmPort.vslmCreateDiskFromSnapshotTask(
                    vStorageObjectManager,
                    FcdHelper.makeId(vStorageObjectId),
                    snapshotId,
                    fcdName,
                    profileSpec,
                    null,
                    datastorePath);

            VStorageObject vStorageObjectCreatedFromSnapshot = null;
            boolean isCreateDiskFromSnapshotSucceeded = vslmHelper.waitForTask(createDiskFromSnapTaskMoRef);
            if (isCreateDiskFromSnapshotSucceeded) {
                VslmTaskInfo taskInfo = vslmPort.vslmQueryInfo(createDiskFromSnapTaskMoRef);

                vStorageObjectCreatedFromSnapshot = (VStorageObject) taskInfo.getResult();
                log.info(
                        "Success: Created vStorageObject from snapshot : \n [ SnapshotId = {} ] of vStorageObject : \n [ UUid = {} ] with name : \n [ fcd name = {} ] \n [ Uuid = {} ] \n from vslm.",
                        snapshotId,
                        vStorageObjectId,
                        vStorageObjectCreatedFromSnapshot.getConfig().getName(),
                        vStorageObjectCreatedFromSnapshot.getConfig().getId().getId());
            } else {
                String message = "Error: Creating vStorageObject from snapshot : [ " + snapshotId.getId()
                        + "] of vStorageObject : [ " + vStorageObjectId + " ] from vslm.";
                throw new RuntimeException(message);
            }

            // Retrieve all the properties of a newly created virtual storage objects
            // based on the Uuid of the vStorageObject obtained from the list call.
            log.info("Operation: Retrieve the vStorageObject created from snapshot in datastore from vslm.");
            VStorageObject retrievedVStrObj = vslmPort.vslmRetrieveVStorageObject(
                    vStorageObjectManager,
                    vStorageObjectCreatedFromSnapshot.getConfig().getId());
            if (retrievedVStrObj
                    .getConfig()
                    .getId()
                    .getId()
                    .equals(vStorageObjectCreatedFromSnapshot
                            .getConfig()
                            .getId()
                            .getId())) {
                log.info(
                        "Success: Retrieved vStorageObject :: \n [ {} ]\n from vslm.",
                        retrievedVStrObj.getConfig().getId().getId());
            } else {
                String message = "Error: Created VStorageObject [ "
                        + vStorageObjectCreatedFromSnapshot.getConfig().getId().getId()
                        + "] and retrieved VStorageObject are different from vslm";
                throw new RuntimeException(message);
            }

            // Reverts to a given snapshot of a VStorageObject
            log.info("Operation: Reverting to a given snapshot of a VStorageObject from vslm.");
            ManagedObjectReference revertTaskMoRef = vslmPort.vslmRevertVStorageObjectTask(
                    vStorageObjectManager, FcdHelper.makeId(vStorageObjectId), snapshotId);

            boolean isRevertDiskSucceeded = vslmHelper.waitForTask(revertTaskMoRef);
            if (isRevertDiskSucceeded) {
                log.info(
                        "Success: Reverted vStorageObject : [ UUID = {} ] to snapshot : [ SnapshotId = {} ] from vslm.\n",
                        vStorageObjectId,
                        snapshotId.getId());
            } else {
                String message = "Error: Reverting vStorageObject [ " + vStorageObjectId + "] to snapshot [ "
                        + snapshotId.getId() + " ] from vslm.";
                throw new RuntimeException(message);
            }

            // Delete snapshot
            log.info("Operation: Deleting snapshot of given vStorageObject from vslm.");
            ManagedObjectReference deleteSnapTaskMoRef = vslmPort.vslmDeleteSnapshotTask(
                    vStorageObjectManager, FcdHelper.makeId(vStorageObjectId), snapshotId);

            boolean isDeleteSnapshotSucceeded = vslmHelper.waitForTask(deleteSnapTaskMoRef);
            if (isDeleteSnapshotSucceeded) {
                log.info(
                        "Success: Deleted snapshot : [ SnapshotId = {} ]of vStorageObject : [ UUID = {} ] from vslm.",
                        snapshotId.getId(),
                        vStorageObjectId);
            } else {
                String message = "Error: Deleting [ " + vStorageObjectId + "] vStorageObject snapshot [ "
                        + snapshotId.getId() + " ] from vslm.%n";
                throw new RuntimeException(message);
            }

            // Retrieve all the properties of a virtual storage objects based on the
            // Uuid of the vStorageObject obtained from the list call.
            log.info("Operation: Retrieve VStorageObject snapshot from vslm.");
            VStorageObjectSnapshotInfo retrievedSnapshotInfoAfterDelete =
                    vslmPort.vslmRetrieveSnapshotInfo(vStorageObjectManager, FcdHelper.makeId(vStorageObjectId));
            if (!isSnapshotIdInSnapshotList(retrievedSnapshotInfoAfterDelete, snapshotId)) {
                log.info(
                        "Success: Deleted vStorageObject Snapshot :: [ {} ] is not included in retrieved snapshot list from vslm.",
                        snapshotId.getId());
            } else {
                String message = "Error: Deleted vStorageObject Snapshot [ " + snapshotId.getId() + " ] is included in"
                        + "retrieved snapshot list from vslm.";
                throw new RuntimeException(message);
            }
        }
    }

    /**
     * Verifies if Snapshot ID is included in retrievedSnapshotInfo.
     *
     * @param retrievedSnapshotInfo {@link VStorageObjectSnapshotInfo} containing snapshot list of VStorageObject
     * @param snapshotId snapshot ID of VStorageObject
     * @return true if retrievedSnapshotInfo contains snapshotId details
     */
    private static boolean isSnapshotIdInSnapshotList(VStorageObjectSnapshotInfo retrievedSnapshotInfo, ID snapshotId) {
        List<String> snapshotIdList = new ArrayList<>();
        for (VStorageObjectSnapshotInfoVStorageObjectSnapshot snapshotDetail : retrievedSnapshotInfo.getSnapshots()) {
            snapshotIdList.add(snapshotDetail.getId().getId());
        }
        return snapshotIdList.contains(snapshotId.getId());
    }
}
