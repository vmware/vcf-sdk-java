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
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer1BackingInfo;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualDiskRawDiskMappingVer1BackingInfo;
import com.vmware.vim25.VirtualDiskSparseVer1BackingInfo;
import com.vmware.vim25.VirtualDiskSparseVer2BackingInfo;
import com.vmware.vim25.VirtualHardware;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineRelocateDiskMoveOptions;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.VirtualMachineRelocateSpecDiskLocator;
import com.vmware.vim25.VirtualMachineSnapshotInfo;
import com.vmware.vim25.VirtualMachineSnapshotTree;

/**
 * This sample creates a linked clone from an existing snapshot. Each independent disk needs a DiskLocator with
 * diskmovetype as moveAllDiskBackingsAndDisallowSharing.
 */
public class VMLinkedClone {
    private static final Logger log = LoggerFactory.getLogger(VMLinkedClone.class);
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

    /** REQUIRED: Name of the clone virtual machine. */
    public static String cloneName = "cloneName";
    /** REQUIRED: Name of the virtual machine. */
    public static String virtualMachineName = "virtualMachineName";
    /** REQUIRED: Name of the snapshot. */
    public static String snapshotName = "snapshotName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VMLinkedClone.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference vmMoRef =
                    propertyCollectorHelper.getMoRefByName(virtualMachineName, VIRTUAL_MACHINE);

            if (vmMoRef != null) {
                ManagedObjectReference snapMoRef = getSnapshotReference(propertyCollectorHelper, vmMoRef, snapshotName);
                if (snapMoRef != null) {
                    ArrayList<Integer> independentVirtualDiskKeys =
                            getIndependentVirtualDiskKeys(propertyCollectorHelper, vmMoRef);

                    VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();
                    if (!independentVirtualDiskKeys.isEmpty()) {
                        List<ManagedObjectReference> datastores = ((ArrayOfManagedObjectReference)
                                        propertyCollectorHelper.fetch(vmMoRef, "datastore"))
                                .getManagedObjectReference();

                        List<VirtualMachineRelocateSpecDiskLocator> diskLocators = new ArrayList<>();

                        for (Integer iDiskKey : independentVirtualDiskKeys) {
                            VirtualMachineRelocateSpecDiskLocator disklocator =
                                    new VirtualMachineRelocateSpecDiskLocator();
                            disklocator.setDatastore(datastores.get(0));
                            disklocator.setDiskMoveType(
                                    VirtualMachineRelocateDiskMoveOptions.MOVE_ALL_DISK_BACKINGS_AND_DISALLOW_SHARING
                                            .value());
                            disklocator.setDiskId(iDiskKey);

                            diskLocators.add(disklocator);
                        }

                        relocateSpec.setDiskMoveType(
                                VirtualMachineRelocateDiskMoveOptions.CREATE_NEW_CHILD_DISK_BACKING.value());
                        relocateSpec.getDisk().addAll(diskLocators);
                    } else {
                        relocateSpec.setDiskMoveType(
                                VirtualMachineRelocateDiskMoveOptions.CREATE_NEW_CHILD_DISK_BACKING.value());
                    }

                    VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
                    cloneSpec.setPowerOn(false);
                    cloneSpec.setTemplate(false);
                    cloneSpec.setLocation(relocateSpec);
                    cloneSpec.setSnapshot(snapMoRef);

                    ManagedObjectReference parentMoRef = propertyCollectorHelper.fetch(vmMoRef, "parent");
                    if (parentMoRef == null) {
                        throw new RuntimeException("The selected VM is a part of vAPP. This sample only "
                                + "works with virtual machines that are not a part "
                                + "of any vAPP");
                    }

                    ManagedObjectReference cloneTask = vimPort.cloneVMTask(vmMoRef, parentMoRef, cloneName, cloneSpec);
                    if (propertyCollectorHelper.awaitTaskCompletion(cloneTask)) {
                        log.info("Cloning Successful");
                    } else {
                        log.error("Cloning Failure");
                    }
                } else {
                    log.error("Snapshot {} doesn't exist", snapshotName);
                }
            } else {
                log.error("Virtual Machine {} doesn't exist", virtualMachineName);
            }
        }
    }

    /**
     * Gets the independent virtual disk keys.
     *
     * @param vmMoRef the {@link ManagedObjectReference} of the virtual machine
     * @return the independent virtual disk keys
     */
    private static ArrayList<Integer> getIndependentVirtualDiskKeys(
            PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference vmMoRef)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        ArrayList<Integer> independentVirtualDiskKeys = new ArrayList<>();

        VirtualHardware virtualHardware = propertyCollectorHelper.fetch(vmMoRef, "config.hardware");
        List<VirtualDevice> virtualDevices = virtualHardware.getDevice();

        for (VirtualDevice vDisk : virtualDevices) {
            if (vDisk instanceof VirtualDisk) {
                String diskMode = "";
                if (vDisk.getBacking() instanceof VirtualDiskFlatVer1BackingInfo) {
                    diskMode = ((VirtualDiskFlatVer1BackingInfo) vDisk.getBacking()).getDiskMode();
                } else if (vDisk.getBacking() instanceof VirtualDiskFlatVer2BackingInfo) {
                    diskMode = ((VirtualDiskFlatVer2BackingInfo) vDisk.getBacking()).getDiskMode();
                } else if (vDisk.getBacking() instanceof VirtualDiskRawDiskMappingVer1BackingInfo) {
                    diskMode = ((VirtualDiskRawDiskMappingVer1BackingInfo) vDisk.getBacking()).getDiskMode();
                } else if (vDisk.getBacking() instanceof VirtualDiskSparseVer1BackingInfo) {
                    diskMode = ((VirtualDiskSparseVer1BackingInfo) vDisk.getBacking()).getDiskMode();
                } else if (vDisk.getBacking() instanceof VirtualDiskSparseVer2BackingInfo) {
                    diskMode = ((VirtualDiskSparseVer2BackingInfo) vDisk.getBacking()).getDiskMode();
                }
                if (diskMode.contains("independent")) {
                    independentVirtualDiskKeys.add(vDisk.getKey());
                }
            }
        }
        return independentVirtualDiskKeys;
    }

    /**
     * Gets the snapshot reference.
     *
     * @param vmMoRef the {@link ManagedObjectReference} of the virtual machine
     * @param snapshotName the snapshot name
     * @return the snapshot reference
     */
    private static ManagedObjectReference getSnapshotReference(
            PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference vmMoRef, String snapshotName)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        VirtualMachineSnapshotInfo snapInfo = propertyCollectorHelper.fetch(vmMoRef, "snapshot");

        ManagedObjectReference snapshotMoRef = null;
        if (snapInfo != null) {
            snapshotMoRef = traverseSnapshotInTree(snapInfo.getRootSnapshotList(), snapshotName);
        }
        return snapshotMoRef;
    }

    /** Traverse snapshot in tree. */
    private static ManagedObjectReference traverseSnapshotInTree(
            List<VirtualMachineSnapshotTree> snapshotTree, String findName) {
        ManagedObjectReference snapshotMoRef = null;
        if (snapshotTree == null) {
            return snapshotMoRef;
        }

        for (int i = 0; i < snapshotTree.size() && snapshotMoRef == null; i++) {
            VirtualMachineSnapshotTree node = snapshotTree.get(i);
            if (findName != null && node.getName().equals(findName)) {
                snapshotMoRef = node.getSnapshot();
            } else {
                List<VirtualMachineSnapshotTree> childTree = node.getChildSnapshotList();
                snapshotMoRef = traverseSnapshotInTree(childTree, findName);
            }
        }
        return snapshotMoRef;
    }
}
