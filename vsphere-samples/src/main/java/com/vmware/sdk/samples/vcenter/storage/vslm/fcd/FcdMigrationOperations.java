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
import com.vmware.vim25.HostScsiDisk;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VStorageObject;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDiskCompatibilityMode;
import com.vmware.vim25.VslmCloneSpec;
import com.vmware.vim25.VslmCreateSpecBackingSpec;
import com.vmware.vim25.VslmCreateSpecDiskFileBackingSpec;
import com.vmware.vim25.VslmCreateSpecRawDiskMappingBackingSpec;
import com.vmware.vim25.VslmRelocateSpec;
import com.vmware.vslm.VslmDatastoreSyncStatus;
import com.vmware.vslm.VslmPortType;
import com.vmware.vslm.VslmTaskInfo;

/**
 * This sample executes migration related operations on a given VStorageObject from vslm:
 *
 * <ol>
 *   <li>Clone a virtual storage object.
 *   <li>Relocate a virtual storage object.
 *   <li>Query synchronization status of the global catalog.
 *   <li>Query the synchronization state of the Global Catalog for a specified datastore.
 * </ol>
 *
 * <p>Sample Prerequisites: Existing VStorageObject ID
 */
public class FcdMigrationOperations {
    private static final Logger log = LoggerFactory.getLogger(FcdMigrationOperations.class);

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
    /** REQUIRED: Name of destination datastore. */
    public static String destDatastoreName = "destDatastoreName";
    /** OPTIONAL: Name of cloned vStorageObject. */
    public static String clonedDiskName = null;
    /**
     * OPTIONAL: Type of provisioning for the disk [thin | eagerZeroedThick | lazyZeroedThick | virtualMode |
     * physicalMode].
     */
    public static String provisioningType = null;
    /** OPTIONAL: Host Scsi disk to clone vStorageObject. */
    public static HostScsiDisk hostScsiDisk = null;

    private static final Map<String, String> provisioningTypeHashMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FcdMigrationOperations.class, args);

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

            FcdVslmHelper vslmHelper = new FcdVslmHelper(vslmPort);

            ManagedObjectReference vStorageObjectManager =
                    client.getVslmServiceInstanceContent().getVStorageObjectManager();

            log.info("Cloning and Relocating vStorage Object from VSLM ::");

            // Init provisioning types:
            String diskProvisioningType = provisioningTypeHashMap.get(
                    Objects.requireNonNullElse(provisioningType, "thin").trim().toLowerCase());

            if (diskProvisioningType == null) {
                throw new RuntimeException("The input provisioning Type is not valid.");
            }

            ManagedObjectReference destDatastoreMoRef =
                    propertyCollectorHelper.getMoRefByName(destDatastoreName, DATASTORE);

            if (destDatastoreMoRef == null) {
                throw new RuntimeException("The datastore name is not valid.");
            }

            // Create a cloneSpec for VStorageObject
            VslmCloneSpec vslmCloneSpec = generateVslmCloneSpec(destDatastoreMoRef, diskProvisioningType);

            log.info("Operation: Cloning a vStorageObject from vslm.");
            ManagedObjectReference cloneTaskMoRef = vslmPort.vslmCloneVStorageObjectTask(
                    vStorageObjectManager, FcdHelper.makeId(vStorageObjectId), vslmCloneSpec);

            boolean isCloneDiskSucceeded = vslmHelper.waitForTask(cloneTaskMoRef);
            VStorageObject clonedVStorageObject = null;

            if (isCloneDiskSucceeded) {
                VslmTaskInfo taskInfo = vslmPort.vslmQueryInfo(cloneTaskMoRef);
                clonedVStorageObject = (VStorageObject) taskInfo.getResult();
                log.info(
                        "Success: vStorageObject : [ Uuid = {} ] cloned : [ Uuid = {} ] to [ datastore = {} ] from vslm.\n",
                        vStorageObjectId,
                        clonedVStorageObject.getConfig().getId().getId(),
                        destDatastoreMoRef);
            } else {
                String message =
                        "Error: Cloning [ " + FcdHelper.makeId(vStorageObjectId) + "] vStorageObject from vslm.";
                throw new RuntimeException(message);
            }

            // Retrieve all the properties of a virtual storage objects based on the
            // Uuid of the vStorageObject obtained from the list call.
            log.info("Operation: Retrieve the clonedVStorageObject in datastore from vslm.");
            VStorageObject retrievedVStrObjAfterClone = vslmPort.vslmRetrieveVStorageObject(
                    vStorageObjectManager, clonedVStorageObject.getConfig().getId());
            if (retrievedVStrObjAfterClone
                    .getConfig()
                    .getId()
                    .getId()
                    .equals(clonedVStorageObject.getConfig().getId().getId())) {
                log.info(
                        "Success: Retrieved vStorageObject :: [ {} ] from vslm.",
                        retrievedVStrObjAfterClone.getConfig().getId().getId());
            } else {
                String message = "Error: Cloned VStorageObject [ "
                        + clonedVStorageObject.getConfig().getId().getId()
                        + " ] and retrieved VStorageObject are different from vslm.";
                throw new RuntimeException(message);
            }

            // Create a relocate spec for VStorageObject
            VslmRelocateSpec vslmRelocateSpec = generateVslmRelocateSpec(destDatastoreMoRef, diskProvisioningType);

            log.info("Operation: Relocating a vStorageObject from vslm.");
            ManagedObjectReference relocateTaskMoRef = vslmPort.vslmRelocateVStorageObjectTask(
                    vStorageObjectManager, FcdHelper.makeId(vStorageObjectId), vslmRelocateSpec);

            boolean isRelocateDiskSucceeded = vslmHelper.waitForTask(relocateTaskMoRef);
            VStorageObject relocatedVStorageObject = null;

            if (isRelocateDiskSucceeded) {
                VslmTaskInfo taskInfo = vslmPort.vslmQueryInfo(relocateTaskMoRef);
                relocatedVStorageObject = (VStorageObject) taskInfo.getResult();
                log.info(
                        "Success: Relocated vStorageObject : \n [ Name = {} ] \n [ Uuid = {} ] \n to destination datastore [ datastore = {} ].\n",
                        relocatedVStorageObject.getConfig().getName(),
                        relocatedVStorageObject.getConfig().getId().getId(),
                        destDatastoreMoRef.getValue());
            } else {
                String message = "Error: Relocating [ " + FcdHelper.makeId(vStorageObjectId)
                        + "] vStorageObject to [ datastore = %s ] from vslm." + destDatastoreMoRef;
                throw new RuntimeException(message);
            }

            // Retrieve all the properties of a virtual storage objects based on the
            // Uuid of the vStorageObject obtained from the list call.
            log.info("Operation: Retrieve the createdVStorageObjects in datastore from vslm.");
            VStorageObject retrievedVStrObjAfterRelocation = vslmPort.vslmRetrieveVStorageObject(
                    vStorageObjectManager, relocatedVStorageObject.getConfig().getId());
            if (retrievedVStrObjAfterRelocation
                    .getConfig()
                    .getId()
                    .getId()
                    .equals(relocatedVStorageObject.getConfig().getId().getId())) {
                log.info(
                        "Success: Retrieved vStorageObject :: [ {} ] from vslm.",
                        retrievedVStrObjAfterRelocation.getConfig().getId().getId());
            } else {
                String message = "Error: Created VStorageObject [ "
                        + relocatedVStorageObject.getConfig().getId().getId()
                        + " ] and retrieved VStorageObject are different from vslm.";
                throw new RuntimeException(message);
            }

            // query synchronization status of the global catalog.
            log.info("Operation: Query synchronization status of the global catalog from vslm.");
            List<VslmDatastoreSyncStatus> syncStatus = vslmPort.vslmQueryGlobalCatalogSyncStatus(vStorageObjectManager);
            if (!syncStatus.isEmpty()) {
                log.info("Success: Retrieved synchronization status for the below datastores :");
                for (VslmDatastoreSyncStatus status : syncStatus) {
                    log.info("Datastore URL : {}", status.getDatastoreURL());
                }
            } else {
                String message = "Error : Failed to retrieve synchronization status of the global catalog.";
                throw new RuntimeException(message);
            }

            // query the synchronization state of the Global Catalog for a specified datastore.
            log.info(
                    "Operation: Query synchronization status of the global catalog for a specified datastore from vslm.");
            VslmDatastoreSyncStatus syncStatusForDatastore = vslmPort.vslmQueryGlobalCatalogSyncStatusForDatastore(
                    vStorageObjectManager, syncStatus.get(0).getDatastoreURL());
            if (syncStatusForDatastore != null) {
                log.info("Success: Retrieved synchronization status for datastore :");
                log.info("Datastore URL : {}", syncStatus.get(0).getDatastoreURL());
            } else {
                String message = "Error : Failed to retrieve synchronization status of the"
                        + " global catalog for a specified datastore.";
                throw new RuntimeException(message);
            }
        }
    }

    /**
     * This method constructs a {@link VslmCloneSpec} for the vStorageObject.
     *
     * @param dsMoRef the {@link ManagedObjectReference} of the datastore
     * @param provisioningType the provisioningType of the disk
     * @return {@link VslmCloneSpec}
     */
    private static VslmCloneSpec generateVslmCloneSpec(ManagedObjectReference dsMoRef, String provisioningType)
            throws IllegalArgumentException {
        log.info("Creating VslmCloneSpec with dsMoRef: {} provisioningType: {}", dsMoRef.getValue(), provisioningType);

        VslmCreateSpecBackingSpec backingSpec;
        if (!provisioningType.equals(VirtualDiskCompatibilityMode.VIRTUAL_MODE.value())
                && !provisioningType.equals(VirtualDiskCompatibilityMode.PHYSICAL_MODE.value())) {
            VslmCreateSpecDiskFileBackingSpec diskFileBackingSpec = new VslmCreateSpecDiskFileBackingSpec();
            diskFileBackingSpec.setDatastore(dsMoRef);
            diskFileBackingSpec.setProvisioningType(
                    BaseConfigInfoDiskFileBackingInfoProvisioningType.fromValue(provisioningType)
                            .value());

            backingSpec = diskFileBackingSpec;
        } else {
            VslmCreateSpecRawDiskMappingBackingSpec rdmBackingSpec = new VslmCreateSpecRawDiskMappingBackingSpec();
            rdmBackingSpec.setDatastore(dsMoRef);
            rdmBackingSpec.setCompatibilityMode(
                    VirtualDiskCompatibilityMode.fromValue(provisioningType).value());
            rdmBackingSpec.setLunUuid(hostScsiDisk.getUuid());

            backingSpec = rdmBackingSpec;
        }
        VslmCloneSpec cloneSpec = new VslmCloneSpec();
        cloneSpec.setBackingSpec(backingSpec);
        cloneSpec.setName(clonedDiskName);

        return cloneSpec;
    }

    /**
     * This method constructs a {@link VslmRelocateSpec} for the vStorageObject.
     *
     * @param dsMoRef the {@link ManagedObjectReference} of the datastore
     * @param provisioningType the provisioningType of the disk
     * @return {@link VslmRelocateSpec}
     */
    private static VslmRelocateSpec generateVslmRelocateSpec(ManagedObjectReference dsMoRef, String provisioningType)
            throws IllegalArgumentException {
        log.info(
                "Creating VslmRelocateSpec with dsMoRef: {} provisioningType:{}", dsMoRef.getValue(), provisioningType);
        VslmCreateSpecBackingSpec backingSpec;

        if (!provisioningType.equals(VirtualDiskCompatibilityMode.VIRTUAL_MODE.value())
                && !provisioningType.equals(VirtualDiskCompatibilityMode.PHYSICAL_MODE.value())) {
            VslmCreateSpecDiskFileBackingSpec diskFileBackingSpec = new VslmCreateSpecDiskFileBackingSpec();
            diskFileBackingSpec.setDatastore(dsMoRef);
            diskFileBackingSpec.setProvisioningType(
                    BaseConfigInfoDiskFileBackingInfoProvisioningType.fromValue(provisioningType)
                            .value());

            backingSpec = diskFileBackingSpec;
        } else {
            VslmCreateSpecRawDiskMappingBackingSpec rdmBackingSpec = new VslmCreateSpecRawDiskMappingBackingSpec();
            rdmBackingSpec.setDatastore(dsMoRef);
            rdmBackingSpec.setCompatibilityMode(
                    VirtualDiskCompatibilityMode.fromValue(provisioningType).value());
            rdmBackingSpec.setLunUuid(hostScsiDisk.getUuid());

            backingSpec = rdmBackingSpec;
        }
        VslmRelocateSpec relocateSpec = new VslmRelocateSpec();
        relocateSpec.setBackingSpec(backingSpec);

        return relocateSpec;
    }
}
