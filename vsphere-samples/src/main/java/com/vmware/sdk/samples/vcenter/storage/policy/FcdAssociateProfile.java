/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.storage.policy;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.sdk.samples.vcenter.storage.fcd.helpers.FcdHelper.makeId;
import static com.vmware.vim25.ManagedObjectType.DATASTORE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.pbm.PbmCapabilityProfile;
import com.vmware.pbm.PbmPortType;
import com.vmware.pbm.PbmProfileId;
import com.vmware.pbm.PbmServiceInstanceContent;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.pbm.PbmUtil;
import com.vmware.vim25.ArrayOfVirtualDevice;
import com.vmware.vim25.BaseConfigInfoBackingInfo;
import com.vmware.vim25.BaseConfigInfoDiskFileBackingInfo;
import com.vmware.vim25.BaseConfigInfoFileBackingInfo;
import com.vmware.vim25.ConcurrentAccessFaultMsg;
import com.vmware.vim25.DuplicateNameFaultMsg;
import com.vmware.vim25.FileFaultFaultMsg;
import com.vmware.vim25.InsufficientResourcesFaultFaultMsg;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidDatastoreFaultMsg;
import com.vmware.vim25.InvalidNameFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TaskInProgressFaultMsg;
import com.vmware.vim25.VStorageObject;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDeviceFileBackingInfo;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineDefinedProfileSpec;
import com.vmware.vim25.VirtualSCSIController;
import com.vmware.vim25.VmConfigFaultFaultMsg;

/**
 * This sample attaches a virtual storage object (FCD) to given virtual machine and associates a given storage profile
 * to the virtual storage object.
 */
public class FcdAssociateProfile {
    private static final Logger log = LoggerFactory.getLogger(FcdAssociateProfile.class);

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
    /** REQUIRED: Inventory path of the virtual machine. */
    public static String vmPath = "vmPath";
    /** REQUIRED: Name of the datastore which contains the virtual storage object. */
    public static String datastoreName = "datastoreName";
    /** REQUIRED: Name of the storage profile. */
    public static String profileName = "profileName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FcdAssociateProfile.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent vimServiceContent = client.getVimServiceContent();

            PbmPortType pbmPort = client.getPbmPort();
            PbmServiceInstanceContent pbmServiceInstanceContent = client.getPbmServiceInstanceContent();

            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, vimServiceContent);

            ManagedObjectReference vStrObjManagerMoRef =
                    client.getVimServiceContent().getVStorageObjectManager();

            log.info("vmPath is :: {}", vmPath);
            // Get Virtual Machine Mor.
            ManagedObjectReference vmMoRef =
                    vimPort.findByInventoryPath(client.getVimServiceContent().getSearchIndex(), vmPath);

            if (vmMoRef == null) {
                throw new RuntimeException("Error obtaining virtual machine managed object reference");
            }

            // Get all the input moRefs required.
            ManagedObjectReference datastoreMoRef =
                    propertyCollectorHelper.getMoRefByName(vimServiceContent.getRootFolder(), datastoreName, DATASTORE);

            PbmCapabilityProfile profile = PbmUtil.getPbmProfile(pbmPort, pbmServiceInstanceContent, profileName);
            PbmProfileId profileId = profile.getProfileId();

            // Retrieve a vStorageObject based on the given vStorageObjectId.
            log.info("Operation: Retrieve the vStorageObjects in datastore.");
            VStorageObject retrievedVStrObj =
                    vimPort.retrieveVStorageObject(vStrObjManagerMoRef, makeId(vStorageObjectId), datastoreMoRef, null);
            if (retrievedVStrObj.getConfig().getId().getId().equals(vStorageObjectId)) {
                log.info(
                        "Success: Retrieved vStorageObject :: \n [ {} ]\n",
                        retrievedVStrObj.getConfig().getId().getId());
            } else {
                String msg = "Error: Given vStorageObject [ " + vStorageObjectId
                        + "] and retrieved VStorageObject are different.";
                throw new RuntimeException(msg);
            }

            if (reconfigVMToAddFcdAndUpdateProfile(
                    vimPort, propertyCollectorHelper, vmMoRef, retrievedVStrObj, profileId)) {
                log.info(
                        "Success: Associated profile :: [ Name = {} ] to vStorageObject [ Name = {}, Uuid = {} ]\n",
                        profileName,
                        retrievedVStrObj.getConfig().getName(),
                        retrievedVStrObj.getConfig().getId().getId());
            } else {
                String msg = "Error: Associating profile [ " + profileName + " ] to the vStorageObject [ "
                        + vStorageObjectId + " ].";
                throw new RuntimeException(msg);
            }

            // Retrieve a vStorageObject based on the given vStorageObjectId and
            // verify virtualMachine is reflected as a new consumer in the
            // retrievedVStorageObject when a virtualMachine is reconfigured to ADD a FCD.
            log.info("Operation: Retrieve the vStorageObjects in datastore.");
            VStorageObject retrievedVStrObjWithConsumer =
                    vimPort.retrieveVStorageObject(vStrObjManagerMoRef, makeId(vStorageObjectId), datastoreMoRef, null);
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

    /**
     * Reconfigures a VM to add a FCD to the VM and update the disk profile. If input param profileId is null, an empty
     * profile is attempted to be set.
     *
     * @return isReconfigSuccessfull
     */
    private static boolean reconfigVMToAddFcdAndUpdateProfile(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            ManagedObjectReference vmMoRef,
            VStorageObject vStorageObj,
            PbmProfileId profileId)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, ConcurrentAccessFaultMsg, DuplicateNameFaultMsg,
                    FileFaultFaultMsg, InsufficientResourcesFaultFaultMsg, InvalidDatastoreFaultMsg,
                    InvalidNameFaultMsg, InvalidStateFaultMsg, TaskInProgressFaultMsg, VmConfigFaultFaultMsg,
                    InvalidCollectorVersionFaultMsg {
        log.info(
                "Info: Reconfig VM To Add Fcd [ {} ]\n",
                vStorageObj.getConfig().getId().getId());
        if (profileId != null) {
            log.info(
                    "Info: Associating profile with Profile Id [ {} ] to vStorageObject [ Uuid = {} ].",
                    profileId.getUniqueId(),
                    vStorageObj.getConfig().getId().getId());
        }

        VirtualDeviceConfigSpec vDiskSpec =
                getDiskDeviceConfigSpec(vmMoRef, vStorageObj, profileId, propertyCollectorHelper);
        List<VirtualDeviceConfigSpec> virtualDeviceConfigSpecs = new ArrayList<>();
        virtualDeviceConfigSpecs.add(vDiskSpec);

        VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
        vmConfigSpec.getDeviceChange().addAll(virtualDeviceConfigSpecs);

        ManagedObjectReference reconfigVmTaskMoRef = vimPort.reconfigVMTask(vmMoRef, vmConfigSpec);

        return propertyCollectorHelper.awaitTaskCompletion(reconfigVmTaskMoRef);
    }

    /**
     * Get the {@link VirtualDeviceConfigSpec} of the new disk to be added to the VM.
     *
     * @return VirtualDeviceConfigSpec of the disk
     */
    private static VirtualDeviceConfigSpec getDiskDeviceConfigSpec(
            ManagedObjectReference vmMoRef,
            VStorageObject vStorageObj,
            PbmProfileId profileId,
            PropertyCollectorHelper propertyCollectorHelper)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        VirtualDisk virtualDisk = new VirtualDisk();
        String fileName = null;
        BaseConfigInfoBackingInfo vStorageObjBackingInfo =
                vStorageObj.getConfig().getBacking();
        if (vStorageObjBackingInfo instanceof BaseConfigInfoFileBackingInfo) {
            BaseConfigInfoFileBackingInfo vStorageObjFileBackingInfo =
                    (BaseConfigInfoDiskFileBackingInfo) vStorageObjBackingInfo;
            fileName = vStorageObjFileBackingInfo.getFilePath();
        }

        if (fileName == null) {
            String message = "Error: No FileName/FilePath obtained for the"
                    + " vStorageObject [ "
                    + vStorageObj.getConfig().getId().getId() + " ].";
            throw new RuntimeException(message);
        }

        // Set the fileName / backing for the VirtualDisk =
        // fileName/filePath/backing of vStorageObject.
        VirtualDeviceFileBackingInfo virtualDiskDeviceFileBackingInfo = new VirtualDeviceFileBackingInfo();
        virtualDiskDeviceFileBackingInfo.setFileName(fileName);
        virtualDisk.setBacking(virtualDiskDeviceFileBackingInfo);

        // Set the controller Key and unit number obtained from virtual machine.
        int ckey = 0;
        int unitNumber = 0;
        List<Integer> getControllerKeyReturnArr = getControllerKey(vmMoRef, propertyCollectorHelper);
        if (!getControllerKeyReturnArr.isEmpty()) {
            ckey = getControllerKeyReturnArr.get(0);
            unitNumber = getControllerKeyReturnArr.get(1);
        }
        virtualDisk.setControllerKey(ckey);
        virtualDisk.setUnitNumber(unitNumber);

        // Set the size of the virtualDisk = size of vStorageObject.
        long virtualDiskSizeInKB = 1024 * vStorageObj.getConfig().getCapacityInMB();
        virtualDisk.setCapacityInKB(virtualDiskSizeInKB);
        virtualDisk.setKey(-1);

        // Set the profile to be associated with the disk.
        VirtualMachineDefinedProfileSpec diskProfileSpec = new VirtualMachineDefinedProfileSpec();
        diskProfileSpec.setProfileId(profileId.getUniqueId());
        log.info("Associating disk with profileId : {}\n", profileId.getUniqueId());

        VirtualDeviceConfigSpec virtualDiskDeviceConfigSpec = new VirtualDeviceConfigSpec();
        virtualDiskDeviceConfigSpec.getProfile().add(diskProfileSpec);

        // Set the operation, file operation and device details.
        virtualDiskDeviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
        virtualDiskDeviceConfigSpec.setFileOperation(null);
        virtualDiskDeviceConfigSpec.setDevice(virtualDisk);

        return virtualDiskDeviceConfigSpec;
    }

    /**
     * Gets the controller key and the next available free unit number on the SCSI controller.
     *
     * @return a list containing controller key followed by next free unit number
     */
    private static List<Integer> getControllerKey(
            ManagedObjectReference vmMoRef, PropertyCollectorHelper propertyCollectorHelper)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        List<Integer> result = new ArrayList<>();

        ArrayOfVirtualDevice arrayOfVirtualDevice = propertyCollectorHelper.fetch(vmMoRef, "config.hardware.device");
        List<VirtualDevice> virtualDevices = arrayOfVirtualDevice.getVirtualDevice();

        Map<Integer, VirtualDevice> deviceMap = new HashMap<>();
        for (VirtualDevice virtualDevice : virtualDevices) {
            deviceMap.put(virtualDevice.getKey(), virtualDevice);
        }

        boolean found = false;
        for (VirtualDevice virtualDevice : virtualDevices) {
            if (virtualDevice instanceof VirtualSCSIController) {
                VirtualSCSIController scsiController = (VirtualSCSIController) virtualDevice;
                int[] slots = new int[16];
                slots[7] = 1;

                List<Integer> devicelist = scsiController.getDevice();
                for (Integer deviceKey : devicelist) {
                    if (deviceMap.get(deviceKey).getUnitNumber() != null) {
                        slots[deviceMap.get(deviceKey).getUnitNumber()] = 1;
                    }
                }
                for (int i = 0; i < slots.length; i++) {
                    if (slots[i] != 1) {
                        result.add(scsiController.getKey());
                        result.add(i);
                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
        }

        if (!found) {
            throw new RuntimeException("The SCSI controller on the vm has maxed out its "
                    + "capacity. Please add an additional SCSI"
                    + " controller");
        }
        return result;
    }
}
