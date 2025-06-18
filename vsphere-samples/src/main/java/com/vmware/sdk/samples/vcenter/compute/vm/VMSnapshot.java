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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.FileFaultFaultMsg;
import com.vmware.vim25.InsufficientResourcesFaultFaultMsg;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidNameFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.SnapshotFaultFaultMsg;
import com.vmware.vim25.TaskInProgressFaultMsg;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachineSnapshotInfo;
import com.vmware.vim25.VirtualMachineSnapshotTree;
import com.vmware.vim25.VmConfigFaultFaultMsg;

/** This sample demonstrates VM snapshot operations. */
public class VMSnapshot {
    private static final Logger log = LoggerFactory.getLogger(VMSnapshot.class);
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
    /** REQUIRED: Operation type - [list|create|remove|revert]. */
    public static String operation = "operation";
    /** OPTIONAL: Name of the snapshot. */
    public static String snapshotName = null;
    /** OPTIONAL: Description of the snapshot. */
    public static String description = null;
    /** OPTIONAL: remove snapshot children - [1 | 0]. */
    public static String removeChild = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VMSnapshot.class, args);

        boolean valid;
        valid = verifyInputArguments();
        if (!valid) {
            return;
        }

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference vmMoRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);

            if (vmMoRef != null) {
                boolean result = false;
                if (operation.equalsIgnoreCase("create")) {
                    result = createSnapshot(vimPort, propertyCollectorHelper, vmMoRef);
                } else if (operation.equalsIgnoreCase("list")) {
                    result = listSnapshot(propertyCollectorHelper, vmMoRef);
                } else if (operation.equalsIgnoreCase("revert")) {
                    result = revertSnapshot(vimPort, propertyCollectorHelper, vmMoRef);
                } else if (operation.equalsIgnoreCase("removeall")) {
                    result = removeAllSnapshot(vimPort, propertyCollectorHelper, vmMoRef);
                } else if (operation.equalsIgnoreCase("remove")) {
                    result = removeSnapshot(vimPort, propertyCollectorHelper, vmMoRef);
                } else {
                    log.error("Invalid operation [create|list|revert|removeall|remove]");
                }
                if (result) {
                    log.info("Operation {} snapshot completed successfully", operation);
                }
            } else {
                log.error("Virtual Machine {} not found.", vmName);
            }
        }
    }

    private static boolean createSnapshot(
            VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference vmMoRef)
            throws FileFaultFaultMsg, InvalidNameFaultMsg, InvalidStateFaultMsg, RuntimeFaultFaultMsg,
                    SnapshotFaultFaultMsg, TaskInProgressFaultMsg, VmConfigFaultFaultMsg, InvalidPropertyFaultMsg,
                    InvalidCollectorVersionFaultMsg {
        ManagedObjectReference taskMoRef = vimPort.createSnapshotTask(vmMoRef, snapshotName, description, false, false);

        if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
            log.info("Creating Snapshot - [ {} ] Successful \n", snapshotName);
            return true;
        } else {
            log.error("Creating Snapshot - [ {} ] Failure \n", snapshotName);
            return false;
        }
    }

    private static boolean listSnapshot(PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference vmMoRef)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        VirtualMachineSnapshotInfo snapInfo = propertyCollectorHelper.fetch(vmMoRef, "snapshot");
        if (snapInfo == null) {
            log.error("No Snapshots found");
        } else {
            List<VirtualMachineSnapshotTree> virtualMachineSnapshotTreeList = snapInfo.getRootSnapshotList();
            traverseSnapshotInTree(virtualMachineSnapshotTreeList, null, true);
        }
        return true;
    }

    private static boolean revertSnapshot(
            VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference vmMoRef)
            throws RuntimeFaultFaultMsg, TaskInProgressFaultMsg, VmConfigFaultFaultMsg,
                    InsufficientResourcesFaultFaultMsg, FileFaultFaultMsg, InvalidStateFaultMsg,
                    InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg {
        ManagedObjectReference snapshotMoRef =
                getSnapshotReference(propertyCollectorHelper, vmMoRef, vmName, snapshotName);

        if (snapshotMoRef != null) {
            ManagedObjectReference taskMoRef = vimPort.revertToSnapshotTask(snapshotMoRef, null, true);

            if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                log.info("Reverting Snapshot - [ {} ] Successful \n", snapshotName);
                return true;
            } else {
                log.error("Reverting Snapshot - [ {} ] Failure \n", snapshotName);
                return false;
            }
        } else {
            log.error("Snapshot not found");
        }
        return false;
    }

    private static boolean removeAllSnapshot(
            VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference vmMoRef)
            throws RuntimeFaultFaultMsg, TaskInProgressFaultMsg, SnapshotFaultFaultMsg, InvalidStateFaultMsg,
                    InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg {
        ManagedObjectReference taskMoRef = vimPort.removeAllSnapshotsTask(vmMoRef, true, null);
        if (taskMoRef != null) {
            String[] opts = new String[] {"info.state", "info.error", "info.progress"};
            String[] opt = new String[] {"state"};
            Object[] results = propertyCollectorHelper.awaitManagedObjectUpdates(
                    taskMoRef, opts, opt, new Object[][] {new Object[] {TaskInfoState.SUCCESS, TaskInfoState.ERROR}});

            // Wait till the task completes.
            if (results[0].equals(TaskInfoState.SUCCESS)) {
                log.info("Removing All Snapshots on - [ {} ] Successful \n", vmName);
                return true;
            } else {
                log.error("Removing All Snapshots on - [ {} ] Failure \n", vmName);
                return false;
            }
        } else {
            return false;
        }
    }

    private static boolean removeSnapshot(
            VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference vmMoRef)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg,
                    TaskInProgressFaultMsg {
        int remove = Integer.parseInt(removeChild);
        boolean flag = remove != 0;

        ManagedObjectReference snapshotMoRef =
                getSnapshotReference(propertyCollectorHelper, vmMoRef, vmName, snapshotName);
        if (snapshotMoRef != null) {
            ManagedObjectReference taskMoRef = vimPort.removeSnapshotTask(snapshotMoRef, flag, true);

            if (taskMoRef != null) {
                String[] opts = new String[] {"info.state", "info.error", "info.progress"};
                String[] opt = new String[] {"state"};
                Object[] results = propertyCollectorHelper.awaitManagedObjectUpdates(
                        taskMoRef, opts, opt, new Object[][] {new Object[] {TaskInfoState.SUCCESS, TaskInfoState.ERROR}
                        });

                // Wait till the task completes.
                if (results[0].equals(TaskInfoState.SUCCESS)) {
                    log.info("Removing Snapshot - [ {} ] Successful \n", snapshotName);
                    return true;
                } else {
                    log.error("Removing Snapshot - [ {} ] Failure \n", snapshotName);
                    return false;
                }
            }
        } else {
            log.error("Snapshot not found");
        }
        return false;
    }

    private static ManagedObjectReference traverseSnapshotInTree(
            List<VirtualMachineSnapshotTree> snapTree, String findName, boolean print) {
        ManagedObjectReference snapshotMoRef = null;
        if (snapTree == null) {
            return snapshotMoRef;
        }

        for (VirtualMachineSnapshotTree node : snapTree) {
            if (print) {
                log.info("Snapshot Name : {}", node.getName());
            }
            if (findName != null && node.getName().equalsIgnoreCase(findName)) {
                return node.getSnapshot();
            } else {
                snapshotMoRef = traverseSnapshotInTree(node.getChildSnapshotList(), findName, print);
            }
        }
        return snapshotMoRef;
    }

    private static ManagedObjectReference getSnapshotReference(
            PropertyCollectorHelper propertyCollectorHelper,
            ManagedObjectReference vmMoRef,
            String vmName,
            String snapName)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        VirtualMachineSnapshotInfo snapInfo = propertyCollectorHelper.fetch(vmMoRef, "snapshot");

        ManagedObjectReference snapshotMoRef = null;
        if (snapInfo != null) {
            List<VirtualMachineSnapshotTree> virtualMachineSnapshotTreeList = snapInfo.getRootSnapshotList();
            snapshotMoRef = traverseSnapshotInTree(virtualMachineSnapshotTreeList, snapName, false);
            if (snapshotMoRef == null) {
                log.error("No Snapshot named : {} found for VirtualMachine : {}", snapName, vmName);
            }
        } else {
            log.error("No Snapshots found for VirtualMachine : {}", vmName);
        }
        return snapshotMoRef;
    }

    private static boolean isOptionEmpty(String test) {
        return test == null;
    }

    private static boolean verifyInputArguments() {
        boolean flag = true;
        String op = operation;

        if (op.equalsIgnoreCase("create")) {
            if ((isOptionEmpty(snapshotName)) || (isOptionEmpty(description))) {
                log.info("For Create operation SnapshotName and Description are the Mandatory options");
                flag = false;
            }
        }

        if (op.equalsIgnoreCase("remove")) {
            if ((isOptionEmpty(snapshotName)) || (isOptionEmpty(removeChild))) {
                log.error("For Remove operation Snapshotname and removechild are the Mandatory options");
                flag = false;
            } else {
                int child = Integer.parseInt(removeChild);
                if (child != 0 && child != 1) {
                    log.error("Value of removechild parameter must be either 0 or 1");
                    flag = false;
                }
            }
        }

        if (op.equalsIgnoreCase("revert")) {
            if ((isOptionEmpty(snapshotName))) {
                log.error("For Revert operation SnapshotName is the Mandatory option");
                flag = false;
            }
        }
        return flag;
    }
}
