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
import static com.vmware.vim25.ManagedObjectType.DATASTORE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.storage.fcd.helpers.FcdHelper;
import com.vmware.sdk.samples.vcenter.storage.vslm.fcd.helpers.FcdVslmHelper;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.BaseConfigInfoDiskFileBackingInfoProvisioningType;
import com.vmware.vim25.ID;
import com.vmware.vim25.KeyValue;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VStorageObject;
import com.vmware.vim25.VStorageObjectSnapshotInfo;
import com.vmware.vim25.VStorageObjectSnapshotInfoVStorageObjectSnapshot;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDiskCompatibilityMode;
import com.vmware.vim25.VslmCreateSpec;
import com.vmware.vim25.VslmCreateSpecBackingSpec;
import com.vmware.vim25.VslmCreateSpecDiskFileBackingSpec;
import com.vmware.vim25.VslmCreateSpecRawDiskMappingBackingSpec;
import com.vmware.vslm.VslmPortType;
import com.vmware.vslm.VslmTaskInfo;

/**
 * This sample executes metadata related operations on a given VStorageObject from vslm:
 *
 * <ol>
 *   <li>Create VStorageObject with metadata.
 *   <li>Retrieve metadata key-value pairs from a virtual storage object.
 *   <li>Retrieve the metadata value by key from a virtual storage object.
 *   <li>Updates metadata key-value pairs to a virtual storage object.
 * </ol>
 *
 * <p>Sample Prerequisites: Existing VStorageObject name
 */
public class FcdMetadataOperations {
    private static final Logger log = LoggerFactory.getLogger(FcdMetadataOperations.class);

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

    /** REQUIRED: Name of the vstorageobject to be created. */
    public static String vStorageObjectName = "vStorageObject";
    /** REQUIRED: Name of the datastore. */
    public static String datastoreName = "datastoreName";
    /** REQUIRED: Size of the disk (in MB). */
    public static long vStorageObjectSizeInMB = 0L;
    /**
     * OPTIONAL: Type of provisioning for the disk [thin | eagerZeroedThick | lazyZeroedThick | virtualMode |
     * physicalMode].
     */
    public static String provisioningType = null;
    /** OPTIONAL: Canonical name of the LUN to use for disk types. */
    public static String deviceName = null;

    private static final Map<String, String> provisioningTypeHashMap = new HashMap<>();
    private static final String DESCRIPTION = "snapshot description";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FcdMetadataOperations.class, args);

        provisioningTypeHashMap.put("thin", BaseConfigInfoDiskFileBackingInfoProvisioningType.THIN.value());
        provisioningTypeHashMap.put(
                "eagerzeroedthick", BaseConfigInfoDiskFileBackingInfoProvisioningType.EAGER_ZEROED_THICK.value());
        provisioningTypeHashMap.put(
                "lazyzeroedthick", BaseConfigInfoDiskFileBackingInfoProvisioningType.LAZY_ZEROED_THICK.value());
        provisioningTypeHashMap.put("virtualmode", VirtualDiskCompatibilityMode.VIRTUAL_MODE.value());
        provisioningTypeHashMap.put("physicalmode", VirtualDiskCompatibilityMode.PHYSICAL_MODE.value());

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

            log.info("Invoking metadata operations from VSLM ::");

            // Init provisioning types
            String diskProvisioningType = provisioningTypeHashMap.get(
                    Objects.requireNonNullElse(provisioningType, "thin").trim().toLowerCase());

            if (diskProvisioningType == null) {
                throw new RuntimeException("The input provisioning Type is not valid.");
            }

            // Get all the input moRefs required for creating VStorageObject.
            ManagedObjectReference datastoreMoRef = propertyCollectorHelper.getMoRefByName(datastoreName, DATASTORE);
            if (datastoreMoRef == null) {
                throw new RuntimeException("Datastore " + datastoreName + " was not found.");
            }

            // Create a create spec for VStorageObject
            VslmCreateSpec vslmCreateSpec = generateVslmCreateSpec(datastoreMoRef, diskProvisioningType);
            log.info("Operation: Creating a vStorageObject with metadata");

            ManagedObjectReference createTaskMoRef = vslmPort.vslmCreateDiskTask(vStorageObjectManager, vslmCreateSpec);
            VStorageObject createdVStorageObject = null;

            boolean isCreateDiskWithMetadataSucceeded = vslmHelper.waitForTask(createTaskMoRef);
            if (isCreateDiskWithMetadataSucceeded) {
                log.info("Create disk with metadata task has succeeded");
                VslmTaskInfo taskInfo = vslmPort.vslmQueryInfo(createTaskMoRef);

                createdVStorageObject = (VStorageObject) taskInfo.getResult();
                log.info(
                        "Success: Created vStorageObject : \n [ Name = {} ] \n [ Uuid = {} ] \n [ DatastorePath = {} ]\n",
                        createdVStorageObject.getConfig().getName(),
                        createdVStorageObject.getConfig().getId().getId(),
                        FcdHelper.getFcdFilePath(createdVStorageObject));
            } else {
                String message = "Error: Creating [ " + vStorageObjectName + "] vStorageObject";
                throw new RuntimeException(message);
            }

            // Retrieve all the properties of a virtual storage objects based on the
            // Uuid of the vStorageObject obtained from the list call.
            log.info("Operation: Retrieve the createdVStorageObjects in datastore.");
            VStorageObject retrievedVStrObj = vslmPort.vslmRetrieveVStorageObject(
                    vStorageObjectManager, createdVStorageObject.getConfig().getId());
            if (retrievedVStrObj
                    .getConfig()
                    .getId()
                    .getId()
                    .equals(createdVStorageObject.getConfig().getId().getId())) {
                log.info(
                        "Success: Retrieved vStorageObject :: \n [ {} ]\n",
                        retrievedVStrObj.getConfig().getId().getId());
            } else {
                String message = "Error: Created VStorageObject [ "
                        + createdVStorageObject.getConfig().getId().getId()
                        + "] and retrieved VStorageObject are different.";
                throw new RuntimeException(message);
            }

            // Create snapshot of vStorageObject. This snapshot is required to retrieve fcd
            // metadata.
            log.info("Operation: Creating snapshot of given vStorageObject from vslm.");
            ManagedObjectReference createSnapTaskMoRef = vslmPort.vslmCreateSnapshotTask(
                    vStorageObjectManager, createdVStorageObject.getConfig().getId(), DESCRIPTION);

            ID snapshotId = null;

            boolean isSnapshotDiskSucceeded = vslmHelper.waitForTask(createSnapTaskMoRef);
            if (isSnapshotDiskSucceeded) {
                log.info("snapshot disk task is succeeded");
                VslmTaskInfo taskInfo = vslmPort.vslmQueryInfo(createSnapTaskMoRef);

                snapshotId = (ID) taskInfo.getResult();
                log.info(
                        "Success: Created snapshot : [ Id = {} ] of vStorageObject : [ UUID = {} ] from vslm.",
                        snapshotId.getId(),
                        createdVStorageObject.getConfig().getId().getId());
            } else {
                String message = "Error: Creating vStorageObject [ "
                        + createdVStorageObject.getConfig().getId() + "] snapshot from vslm.";
                throw new RuntimeException(message);
            }

            // Retrieve all the snapshots of vStorageObject and
            // verify if created snapshot is present in returned list
            log.info("Operation: Retrieve VStorageObject snapshots from vslm.");
            VStorageObjectSnapshotInfo retrievedSnapshotInfo = vslmPort.vslmRetrieveSnapshotInfo(
                    vStorageObjectManager, createdVStorageObject.getConfig().getId());

            if (!retrievedSnapshotInfo.getSnapshots().isEmpty()
                    && isSnapshotIdInSnapshotList(retrievedSnapshotInfo, snapshotId)) {
                log.info("Success: Retrieved vStorageObject Snapshot :: [ {} ] from vslm.", snapshotId.getId());
            } else {
                String message = "Error: Retrieving VStorageObject Snapshot [ " + snapshotId.getId() + " ] from vslm.";
                throw new RuntimeException(message);
            }

            // Retrieving metadata to verify create fcd metadata
            log.info("Operation: Retrieving metadata of a given VStorageObject from vslm.");
            List<KeyValue> metadataList = vslmPort.vslmRetrieveVStorageObjectMetadata(
                    vStorageObjectManager, createdVStorageObject.getConfig().getId(), snapshotId, null);
            if (metadataList != null && !metadataList.isEmpty()) {
                log.info(
                        "Success: Retrieved metadata of vStorageObject :: vStorageObjectId \n [ {} ]\n",
                        createdVStorageObject.getConfig().getId().getId());
            } else {
                String message = "Error: Retrieved metadata of" + "vStorageObject [ "
                        + createdVStorageObject.getConfig().getId().getId() + "] from vslm.";
                throw new RuntimeException(message);
            }

            // Print metadata key value returned by retrieveVStorageObjectMetadata
            log.info("Metadata key value returned by retrieveVStorageObjectMetadata: \n");
            for (KeyValue metadata : metadataList) {
                log.info("Metadata key [ {} ] \n ", metadata.getKey());
                log.info("Metadata value [ {} ] \n ", metadata.getValue());
            }

            // Retrieving metadata value for a metadata key
            log.info("Operation: Retrieving metadata value of a given VStorageObject from vslm.");
            String metaDataValue = vslmPort.vslmRetrieveVStorageObjectMetadataValue(
                    vStorageObjectManager,
                    createdVStorageObject.getConfig().getId(),
                    snapshotId,
                    metadataList.get(0).getKey());
            if (metaDataValue != null) {
                log.info(
                        "Success: Retrieved metadata value of vStorageObject :: vStorageObjectId \n [ {} ]\n",
                        createdVStorageObject.getConfig().getId().getId());
            } else {
                String message = "Error: Retrieved metadata value of" + "vStorageObject [ "
                        + createdVStorageObject.getConfig().getId().getId() + "] from vslm.";
                throw new RuntimeException(message);
            }

            // Print metadata key value returned by retrieveVStorageObjectMetadata
            log.info("metadata value returned by retrieveVStorageObjectMetadataValue: \n");
            log.info("Metadata value [ {} ] \n ", metaDataValue);

            // Updating metadata key-value
            String updatedMetadataValue = metadataList.get(0).getValue() + "-updated";

            KeyValue metadataKeyValue = new KeyValue();
            metadataKeyValue.setKey(metadataList.get(0).getKey());
            metadataKeyValue.setValue(updatedMetadataValue);

            List<KeyValue> metadataListForUpdate = new ArrayList<>();
            metadataListForUpdate.add(metadataKeyValue);

            log.info("Operation: Updating metadata value of a given VStorageObject from vslm.");
            ManagedObjectReference updateTaskMoRef = vslmPort.vslmUpdateVStorageObjectMetadataTask(
                    vStorageObjectManager, createdVStorageObject.getConfig().getId(), metadataListForUpdate, null);

            boolean isUpdateMetadataSucceeded = vslmHelper.waitForTask(updateTaskMoRef);
            if (isUpdateMetadataSucceeded) {
                log.info(
                        "Success: Updating metadata of vStorageObject :: vStorageObjectId \n [ {} ]\n",
                        createdVStorageObject.getConfig().getId().getId());
            } else {
                String message = "Error: Updating metadata of " + "vStorageObject [ "
                        + createdVStorageObject.getConfig().getId().getId() + "] from vslm.";
                throw new RuntimeException(message);
            }

            // Retrieving metadata to verify update fcd metadata
            log.info("Operation: Retrieving metadata of a given VStorageObject from vslm.");
            List<KeyValue> retrievedMetadataList = vslmPort.vslmRetrieveVStorageObjectMetadata(
                    vStorageObjectManager, createdVStorageObject.getConfig().getId(), null, null);
            if (retrievedMetadataList != null) {
                log.info("retrievedMetadataList : {}", retrievedMetadataList);
                log.info(
                        "Success: Retrieved metadata of vStorageObject :: vStorageObjectId \n [ {} ]\n",
                        createdVStorageObject.getConfig().getId().getId());
            } else {
                String message = "Error: Retrieved metadata of vStorageObject [ "
                        + createdVStorageObject.getConfig().getId().getId() + "] from vslm.";
                throw new RuntimeException(message);
            }
            log.info("Verify list of metadata key-values updated properly from vslm ::");
            for (KeyValue kv : retrievedMetadataList) {
                if (kv.getKey().equals(metadataList.get(0).getKey())
                        && kv.getValue().equals(updatedMetadataValue)) {
                    log.info("VStorage object metadata updated successfully from vslm :");
                    log.info("updated metadata key : {}", kv.getKey());
                    log.info("updated metadata value : {}", kv.getValue());
                    break;
                }
            }
        }
    }

    /**
     * Verify if Snapshot ID is included in retrievedSnapshotInfo.
     *
     * @param retrievedSnapshotInfo VStorageObjectSnapshotInfo containing snapshot list of VStorageObject
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

    /**
     * This method constructs a {@link VslmCreateSpec} for the vStorageObject.
     *
     * @param dsMoRef the {@link ManagedObjectReference} of the datastore
     * @param provisioningType the provisioningType of the disk
     * @return {@link VslmCreateSpec}
     */
    private static VslmCreateSpec generateVslmCreateSpec(ManagedObjectReference dsMoRef, String provisioningType)
            throws IllegalArgumentException {
        log.info("Creating VslmCreateSpec with dsMoRef: {} provisioningType:{}", dsMoRef.getValue(), provisioningType);
        VslmCreateSpecBackingSpec vslmCreateSpecBackingSpec;

        if (!provisioningType.equals(VirtualDiskCompatibilityMode.VIRTUAL_MODE.value())
                && !provisioningType.equals(VirtualDiskCompatibilityMode.PHYSICAL_MODE.value())) {
            VslmCreateSpecDiskFileBackingSpec diskFileBackingSpec = new VslmCreateSpecDiskFileBackingSpec();
            diskFileBackingSpec.setDatastore(dsMoRef);
            diskFileBackingSpec.setProvisioningType(
                    BaseConfigInfoDiskFileBackingInfoProvisioningType.fromValue(provisioningType)
                            .value());

            vslmCreateSpecBackingSpec = diskFileBackingSpec;
        } else {
            if (deviceName == null || deviceName.isEmpty()) {
                throw new IllegalArgumentException(
                        "The devicename is mandatory for specified disktype [ " + provisioningType + " ]");
            }
            VslmCreateSpecRawDiskMappingBackingSpec rdmBackingSpec = new VslmCreateSpecRawDiskMappingBackingSpec();
            rdmBackingSpec.setDatastore(dsMoRef);
            rdmBackingSpec.setCompatibilityMode(
                    VirtualDiskCompatibilityMode.fromValue(provisioningType).value());
            rdmBackingSpec.setLunUuid(deviceName);

            vslmCreateSpecBackingSpec = rdmBackingSpec;
        }
        VslmCreateSpec createSpec = new VslmCreateSpec();
        createSpec.setBackingSpec(vslmCreateSpecBackingSpec);
        createSpec.setName(vStorageObjectName);
        createSpec.setCapacityInMB(vStorageObjectSizeInMB);
        createSpec.getMetadata().add(generateMetadataPair(1).get(0));

        return createSpec;
    }

    /** This method creates metadata key-value pairs. */
    private static List<KeyValue> generateMetadataPair(int numberOfMetadataToCreate) {
        List<KeyValue> metadataList = new ArrayList<>();

        for (int i = 0; i < numberOfMetadataToCreate; i++) {
            KeyValue keyValue = new KeyValue();
            keyValue.setKey("SampleKey-" + i);
            keyValue.setValue("SampleValue-" + i);
            metadataList.add(keyValue);
        }
        return metadataList;
    }
}
