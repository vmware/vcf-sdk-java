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
import static com.vmware.sdk.samples.vcenter.storage.fcd.helpers.FcdHelper.isFcdIdInFcdList;
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
import com.vmware.vim25.ID;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VStorageObject;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDiskCompatibilityMode;
import com.vmware.vim25.VslmCreateSpec;
import com.vmware.vim25.VslmCreateSpecBackingSpec;
import com.vmware.vim25.VslmCreateSpecDiskFileBackingSpec;
import com.vmware.vim25.VslmCreateSpecRawDiskMappingBackingSpec;

/** This sample creates a virtual storage object (FCD). */
public class FcdCreate {
    private static final Logger log = LoggerFactory.getLogger(FcdCreate.class);
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

    /** REQUIRED: Name of the disk. */
    public static String vStorageObjectName = "vStorageObjectName";
    /** REQUIRED: Name of the datastore which contains the virtual storage object. */
    public static String datastoreName = "datastoreName";
    /** REQUIRED: Size of the disk (in MB). */
    public static long vStorageObjectSizeInMB = 0L;
    /**
     * OPTIONAL: Type of provisioning for the disk [thin | eagerZeroedThick | lazyZeroedThick | virtualMode |
     * physicalMode].
     */
    public static String provisioningType = null;
    /** OPTIONAL: Canonical name of the LUN to use for RDM provisioning type. Ex: vmhba0:0:0:0 */
    public static String deviceName = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FcdCreate.class, args);

        Map<String, String> provisioningTypeHashMap = new HashMap<>();
        provisioningTypeHashMap.put("thin", BaseConfigInfoDiskFileBackingInfoProvisioningType.THIN.value());
        provisioningTypeHashMap.put(
                "eagerzeroedthick", BaseConfigInfoDiskFileBackingInfoProvisioningType.EAGER_ZEROED_THICK.value());
        provisioningTypeHashMap.put(
                "lazyzeroedthick", BaseConfigInfoDiskFileBackingInfoProvisioningType.LAZY_ZEROED_THICK.value());
        provisioningTypeHashMap.put("virtualmode", VirtualDiskCompatibilityMode.VIRTUAL_MODE.value());
        provisioningTypeHashMap.put("physicalmode", VirtualDiskCompatibilityMode.PHYSICAL_MODE.value());

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference vStrObjManagerMoRef = serviceContent.getVStorageObjectManager();

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

            // Create a create spec for VStorageObject
            VslmCreateSpec vslmCreateSpec = generateVslmCreateSpec(datastoreMoRef, diskProvisioningType);
            log.info("Operation: Creating a vStorageObject");

            ManagedObjectReference createDiskTaskMoRef = vimPort.createDiskTask(vStrObjManagerMoRef, vslmCreateSpec);

            VStorageObject createdVStorageObject;
            if (propertyCollectorHelper.awaitTaskCompletion(createDiskTaskMoRef)) {
                createdVStorageObject = propertyCollectorHelper.fetch(createDiskTaskMoRef, "info.result");
                log.info(
                        "Success: Created vStorageObject : \n [ Name = {} ] \n [ Uuid = {} ] \n [ DatastorePath = {} ]\n",
                        createdVStorageObject.getConfig().getName(),
                        createdVStorageObject.getConfig().getId().getId(),
                        FcdHelper.getFcdFilePath(createdVStorageObject));
            } else {
                String msg = "Error: Creating [ " + vStorageObjectName + "] vStorageObject";
                throw new RuntimeException(msg);
            }

            // Retrieve a list of all the virtual storage objects in given datastore
            // and verify if the created vStorageObject is present in the returned list.
            log.info("Operation: List all vStorageObjects in datastore.");
            List<ID> listOfVStrObj = vimPort.listVStorageObject(vStrObjManagerMoRef, datastoreMoRef);

            if (isFcdIdInFcdList(
                    Collections.singletonList(
                            createdVStorageObject.getConfig().getId().getId()),
                    listOfVStrObj)) {
                log.info(
                        "Success: listVStorageObject contains the created vStorageObjectId : \n [ {} ]\n",
                        createdVStorageObject.getConfig().getId().getId());
            } else {
                String msg = "Error: Created VStorageObject [ "
                        + createdVStorageObject.getConfig().getId().getId()
                        + "] is not present in the returned list from vc.";
                throw new RuntimeException(msg);
            }

            // Retrieve all the properties of a virtual storage objects based on the
            // Uuid of the vStorageObject obtained from the list call.
            log.info("Operation: Retrieve the createdVStorageObjects in datastore.");
            VStorageObject retrievedVStrObj = vimPort.retrieveVStorageObject(
                    vStrObjManagerMoRef, createdVStorageObject.getConfig().getId(), datastoreMoRef, null);

            if (retrievedVStrObj
                    .getConfig()
                    .getId()
                    .getId()
                    .equals(createdVStorageObject.getConfig().getId().getId())) {
                log.info(
                        "Success: Retrieved vStorageObject :: \n [ {} ]\n",
                        retrievedVStrObj.getConfig().getId().getId());
            } else {
                String msg = "Error: Created VStorageObject [ "
                        + createdVStorageObject.getConfig().getId().getId()
                        + "] and retrieved VStorageObject are different.";
                throw new RuntimeException(msg);
            }
        }
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

        return createSpec;
    }
}
