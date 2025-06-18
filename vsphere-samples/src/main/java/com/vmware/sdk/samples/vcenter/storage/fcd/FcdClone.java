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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.storage.fcd.helpers.FcdHelper;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.BaseConfigInfoDiskFileBackingInfoProvisioningType;
import com.vmware.vim25.HostScsiDisk;
import com.vmware.vim25.ID;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VStorageObject;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDiskCompatibilityMode;
import com.vmware.vim25.VslmCloneSpec;
import com.vmware.vim25.VslmCreateSpecBackingSpec;
import com.vmware.vim25.VslmCreateSpecDiskFileBackingSpec;
import com.vmware.vim25.VslmCreateSpecRawDiskMappingBackingSpec;

/** This sample clones a virtual storage object. */
public class FcdClone {
    private static final Logger log = LoggerFactory.getLogger(FcdClone.class);
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
    /** REQUIRED: Name of destination datastore. */
    public static String destDatastoreName = "destDatastoreName";
    /** REQUIRED: Name of cloned vStorageObject. */
    public static String clonedDiskName = "clonedDiskName";
    /**
     * OPTIONAL: Type of provisioning for the disk [thin | eagerZeroedThick | lazyZeroedThick | virtualMode |
     * physicalMode].
     */
    public static String provisioningType = null;
    /** OPTIONAL: Host Scsi disk to clone vStorageObject. */
    public static HostScsiDisk hostScsiDisk = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FcdClone.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            Map<String, String> provisioningTypeHashMap = new HashMap<>();
            provisioningTypeHashMap.put("thin", BaseConfigInfoDiskFileBackingInfoProvisioningType.THIN.value());
            provisioningTypeHashMap.put(
                    "eagerzeroedthick", BaseConfigInfoDiskFileBackingInfoProvisioningType.EAGER_ZEROED_THICK.value());
            provisioningTypeHashMap.put(
                    "lazyzeroedthick", BaseConfigInfoDiskFileBackingInfoProvisioningType.LAZY_ZEROED_THICK.value());
            provisioningTypeHashMap.put("virtualmode", VirtualDiskCompatibilityMode.VIRTUAL_MODE.value());
            provisioningTypeHashMap.put("physicalmode", VirtualDiskCompatibilityMode.PHYSICAL_MODE.value());

            ManagedObjectReference vStrObjManagerMoRef =
                    client.getVimServiceContent().getVStorageObjectManager();

            // Init provisioning types:
            String diskProvisioningType = provisioningTypeHashMap.get(
                    Objects.requireNonNullElse(provisioningType, "thin").trim().toLowerCase());

            if (diskProvisioningType == null) {
                throw new RuntimeException("The input provisioning Type is not valid.");
            }
            // Get all the input moRefs required for creating VStorageObject.
            ManagedObjectReference datastoreMoRef = propertyCollectorHelper.getMoRefByName(datastoreName, DATASTORE);

            if (datastoreMoRef == null) {
                throw new RuntimeException("The datastore name is not valid.");
            }

            ManagedObjectReference destDatastoreMoRef =
                    propertyCollectorHelper.getMoRefByName(destDatastoreName, DATASTORE);

            if (destDatastoreMoRef == null) {
                throw new RuntimeException("The datastore name is not valid.");
            }

            // Create a cloneSpec for VStorageObject
            VslmCloneSpec vslmCloneSpec = generateVslmCloneSpec(destDatastoreMoRef, diskProvisioningType);

            log.info("Operation: Cloning a vStorageObject from vc.");
            ManagedObjectReference cloneDiskTaskMoRef = vimPort.cloneVStorageObjectTask(
                    vStrObjManagerMoRef, FcdHelper.makeId(vStorageObjectId), datastoreMoRef, vslmCloneSpec);

            VStorageObject clonedVStorageObject;
            if (propertyCollectorHelper.awaitTaskCompletion(cloneDiskTaskMoRef)) {
                clonedVStorageObject = propertyCollectorHelper.fetch(cloneDiskTaskMoRef, "info.result");
                log.info(
                        "Success: vStorageObject : [ Uuid = {} ] from datastore [ datastore = {} ]  cloned : [ Uuid = {} ] to [ datastore = {} ] from vc.\n",
                        vStorageObjectId,
                        datastoreMoRef,
                        clonedVStorageObject.getConfig().getId().getId(),
                        destDatastoreMoRef);
            } else {
                String msg = "Error: Cloning [ " + FcdHelper.makeId(vStorageObjectId) + "] vStorageObject from vc.";
                throw new RuntimeException(msg);
            }

            // Retrieve a list of all the virtual storage objects in given datastore
            // and verify if the created vStorageObject is present in the returned list.
            log.info("Operation: List all vStorageObjects in datastore from vc.");
            List<ID> listOfVStrObj = vimPort.listVStorageObject(vStrObjManagerMoRef, destDatastoreMoRef);

            if (FcdHelper.isFcdIdInFcdList(
                    Collections.singletonList(
                            clonedVStorageObject.getConfig().getId().getId()),
                    listOfVStrObj)) {
                log.info(
                        "Success: listVStorageObject contains the cloned vStorageObjectId : [ {} ]\n",
                        clonedVStorageObject.getConfig().getId().getId());
            } else {
                String msg = "Error: Cloned VStorageObject [ "
                        + clonedVStorageObject.getConfig().getId().getId()
                        + " ] is not present in the returned list from vc.";
                throw new RuntimeException(msg);
            }

            // Retrieve all the properties of a virtual storage objects based on the
            // Uuid of the vStorageObject obtained from the list call.
            log.info("Operation: Retrieve the clonedVStorageObject in datastore from vc.");
            VStorageObject retrievedVStrObj = vimPort.retrieveVStorageObject(
                    vStrObjManagerMoRef, clonedVStorageObject.getConfig().getId(), destDatastoreMoRef, null);
            if (retrievedVStrObj
                    .getConfig()
                    .getId()
                    .getId()
                    .equals(clonedVStorageObject.getConfig().getId().getId())) {
                log.info(
                        "Success: Retrieved vStorageObject :: [ {} ] from vc.",
                        retrievedVStrObj.getConfig().getId().getId());
            } else {
                String msg = "Error: Cloned VStorageObject [ "
                        + clonedVStorageObject.getConfig().getId().getId()
                        + " ] and retrieved VStorageObject are different from vc.";
                throw new RuntimeException(msg);
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
}
