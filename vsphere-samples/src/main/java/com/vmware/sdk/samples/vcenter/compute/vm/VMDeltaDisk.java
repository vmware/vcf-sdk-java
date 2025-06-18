/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.compute.vm;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecFileOperation;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDeviceFileBackingInfo;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer1BackingInfo;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualDiskRawDiskMappingVer1BackingInfo;
import com.vmware.vim25.VirtualDiskSparseVer1BackingInfo;
import com.vmware.vim25.VirtualDiskSparseVer2BackingInfo;
import com.vmware.vim25.VirtualHardware;
import com.vmware.vim25.VirtualMachineConfigSpec;

/**
 * This sample creates a delta disk on top of an existing virtual disk in a VM, and simultaneously removes the original
 * disk using the reconfigure API.
 */
public class VMDeltaDisk {
    private static final Logger log = LoggerFactory.getLogger(VMDeltaDisk.class);
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

    /** REQUIRED: Name of the virtual machine. */
    public static String vmName = "vmName";
    /** REQUIRED: Name of the new delta disk. */
    public static String deviceName = "deviceName";
    /** REQUIRED: Name of the disk. */
    public static String diskName = "diskName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VMDeltaDisk.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference vmMoRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);

            VirtualHardware virtualHardware = propertyCollectorHelper.fetch(vmMoRef, "config.hardware");

            VirtualDisk virtualDisk = findVirtualDisk(diskName, virtualHardware);

            if (virtualDisk != null) {
                VirtualDeviceConfigSpec deviceSpec = new VirtualDeviceConfigSpec();

                deviceSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
                deviceSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.CREATE);

                VirtualDisk newDisk = new VirtualDisk();

                newDisk.setCapacityInKB(virtualDisk.getCapacityInKB());

                if (virtualDisk.getShares() != null) {
                    newDisk.setShares(virtualDisk.getShares());
                }
                if (virtualDisk.getConnectable() != null) {
                    newDisk.setConnectable(virtualDisk.getConnectable());
                }
                if (virtualDisk.getControllerKey() != null) {
                    newDisk.setControllerKey(virtualDisk.getControllerKey());
                }

                VirtualDeviceFileBackingInfo fileBacking = (VirtualDeviceFileBackingInfo) virtualDisk.getBacking();

                String dsName = propertyCollectorHelper.fetch(fileBacking.getDatastore(), "summary.name");

                newDisk.setUnitNumber(virtualDisk.getUnitNumber());
                newDisk.setKey(virtualDisk.getKey());

                if (virtualDisk.getBacking() instanceof VirtualDiskFlatVer1BackingInfo) {
                    VirtualDiskFlatVer1BackingInfo virtualDiskBackingInfo = new VirtualDiskFlatVer1BackingInfo();
                    virtualDiskBackingInfo.setDiskMode(
                            ((VirtualDiskFlatVer1BackingInfo) virtualDisk.getBacking()).getDiskMode());
                    virtualDiskBackingInfo.setFileName("[" + dsName + "] " + vmName + "/" + deviceName + ".vmdk");
                    virtualDiskBackingInfo.setParent((VirtualDiskFlatVer1BackingInfo) virtualDisk.getBacking());

                    newDisk.setBacking(virtualDiskBackingInfo);
                } else if (virtualDisk.getBacking() instanceof VirtualDiskFlatVer2BackingInfo) {
                    VirtualDiskFlatVer2BackingInfo virtualDiskBackingInfo = new VirtualDiskFlatVer2BackingInfo();
                    virtualDiskBackingInfo.setDiskMode(
                            ((VirtualDiskFlatVer2BackingInfo) virtualDisk.getBacking()).getDiskMode());
                    virtualDiskBackingInfo.setFileName("[" + dsName + "] " + vmName + "/" + deviceName + ".vmdk");
                    virtualDiskBackingInfo.setParent((VirtualDiskFlatVer2BackingInfo) virtualDisk.getBacking());

                    newDisk.setBacking(virtualDiskBackingInfo);
                } else if (virtualDisk.getBacking() instanceof VirtualDiskRawDiskMappingVer1BackingInfo) {
                    VirtualDiskRawDiskMappingVer1BackingInfo virtualDiskBackingInfo =
                            new VirtualDiskRawDiskMappingVer1BackingInfo();
                    virtualDiskBackingInfo.setDiskMode(
                            ((VirtualDiskRawDiskMappingVer1BackingInfo) virtualDisk.getBacking()).getDiskMode());
                    virtualDiskBackingInfo.setFileName("[" + dsName + "] " + vmName + "/" + deviceName + ".vmdk");
                    virtualDiskBackingInfo.setParent(
                            (VirtualDiskRawDiskMappingVer1BackingInfo) virtualDisk.getBacking());

                    newDisk.setBacking(virtualDiskBackingInfo);
                } else if (virtualDisk.getBacking() instanceof VirtualDiskSparseVer1BackingInfo) {
                    VirtualDiskSparseVer1BackingInfo virtualDiskBackingInfo = new VirtualDiskSparseVer1BackingInfo();
                    virtualDiskBackingInfo.setDiskMode(
                            ((VirtualDiskSparseVer1BackingInfo) virtualDisk.getBacking()).getDiskMode());
                    virtualDiskBackingInfo.setFileName("[" + dsName + "] " + vmName + "/" + deviceName + ".vmdk");
                    virtualDiskBackingInfo.setParent((VirtualDiskSparseVer1BackingInfo) virtualDisk.getBacking());

                    newDisk.setBacking(virtualDiskBackingInfo);
                } else if (virtualDisk.getBacking() instanceof VirtualDiskSparseVer2BackingInfo) {
                    VirtualDiskSparseVer2BackingInfo virtualDiskBackingInfo = new VirtualDiskSparseVer2BackingInfo();
                    virtualDiskBackingInfo.setDiskMode(
                            ((VirtualDiskSparseVer2BackingInfo) virtualDisk.getBacking()).getDiskMode());
                    virtualDiskBackingInfo.setFileName("[" + dsName + "] " + vmName + "/" + deviceName + ".vmdk");
                    virtualDiskBackingInfo.setParent((VirtualDiskSparseVer2BackingInfo) virtualDisk.getBacking());

                    newDisk.setBacking(virtualDiskBackingInfo);
                }
                deviceSpec.setDevice(newDisk);

                VirtualDeviceConfigSpec removeDeviceSpec = new VirtualDeviceConfigSpec();
                removeDeviceSpec.setOperation(VirtualDeviceConfigSpecOperation.REMOVE);
                removeDeviceSpec.setDevice(virtualDisk);

                List<VirtualDeviceConfigSpec> virtualDeviceConfigSpecs = new ArrayList<>();
                virtualDeviceConfigSpecs.add(removeDeviceSpec);
                virtualDeviceConfigSpecs.add(deviceSpec);

                VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
                configSpec.getDeviceChange().addAll(virtualDeviceConfigSpecs);

                try {
                    ManagedObjectReference taskMoRef = vimPort.reconfigVMTask(vmMoRef, configSpec);
                    if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                        log.info("Delta Disk Created successfully.");
                    } else {
                        log.error("Failure - Delta Disk cannot be created");
                    }
                } catch (Exception e) {
                    log.error("Exception: ", e);
                    throw new RuntimeException(e);
                }
            } else {
                log.error("Virtual Disk {} not found", diskName);
            }
        }
    }

    private static VirtualDisk findVirtualDisk(String diskName, VirtualHardware virtualHardware) {
        VirtualDisk virtualDisk = null;
        List<VirtualDevice> deviceArray = virtualHardware.getDevice();

        for (VirtualDevice virtualDevice : deviceArray) {
            if (virtualDevice instanceof VirtualDisk) {
                if (diskName.equalsIgnoreCase(virtualDevice.getDeviceInfo().getLabel())) {
                    virtualDisk = (VirtualDisk) virtualDevice;
                    break;
                }
            }
        }
        return virtualDisk;
    }
}
