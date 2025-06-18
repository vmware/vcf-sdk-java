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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.FileFaultFaultMsg;
import com.vmware.vim25.InsufficientResourcesFaultFaultMsg;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidDatastoreFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ManagedObjectType;
import com.vmware.vim25.MigrationFaultFaultMsg;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TimedoutFaultMsg;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachineMovePriority;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.VmConfigFaultFaultMsg;

/**
 * Used to validate if VMotion is feasible between two hosts or not. It is also used to perform migrate/relocate task
 * depending on the data given.
 */
public class VMotion {
    private static final Logger log = LoggerFactory.getLogger(VMotion.class);
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
    /** REQUIRED: Name of the target host. */
    public static String targetHost = "targetHost";
    /** REQUIRED: Name of the target resource pool. */
    public static String targetPool = "targetPool";
    /** REQUIRED: Name of the host containing the virtual machine. */
    public static String sourceHost = "sourceHost";
    /** REQUIRED: Name of the target datastore. */
    public static String targetDS = "targetDS";
    /** REQUIRED: The priority of the migration task: default_Priority, high_Priority,low_Priority. */
    public static String priority = "priority";
    /** OPTIONAL: State of the virtual machine. */
    public static String state = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VMotion.class, args);

        if (customValidation()) {
            VcenterClientFactory factory =
                    new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

            try (VcenterClient client = factory.createClient(username, password, null)) {
                VimPortType vimPort = client.getVimPort();
                ServiceContent serviceContent = client.getVimServiceContent();
                PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

                // first we need to check if the VM should be migrated of relocated
                // If target host and source host both contains the datastore, virtual machine needs to be migrated
                // If only target host contains the datastore, machine needs to be relocated
                String operationName = checkOperationType(propertyCollectorHelper, targetHost, sourceHost);
                if (operationName.equalsIgnoreCase("migrate")) {
                    migrateVM(vimPort, propertyCollectorHelper, vmName, targetPool, targetHost, sourceHost, priority);
                } else if (operationName.equalsIgnoreCase("relocate")) {
                    relocateVM(vimPort, propertyCollectorHelper, vmName, targetPool, targetHost, targetDS, sourceHost);
                } else if (operationName.equalsIgnoreCase("same")) {
                    throw new IllegalArgumentException("targethost and sourcehost must not be same");
                } else {
                    throw new IllegalArgumentException(operationName + " Not Found.");
                }
            }
        }
    }

    private static ManagedObjectReference browseDsMoRef(
            PropertyCollectorHelper propertyCollectorHelper, List<ManagedObjectReference> dsMoRefs)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        ManagedObjectReference dsMoRef = null;
        if (dsMoRefs != null && !dsMoRefs.isEmpty()) {
            for (ManagedObjectReference managedObjectReference : dsMoRefs) {
                DatastoreSummary ds = propertyCollectorHelper.fetch(managedObjectReference, "summary");
                String dsName = ds.getName();
                if (dsName.equalsIgnoreCase(targetDS)) {
                    dsMoRef = managedObjectReference;
                    break;
                }
            }
        }
        return dsMoRef;
    }

    /**
     * This function is used to check whether relocation is to be done or migration is to be done. If two hosts have a
     * shared datastore then migration will be done and if there is no shared datastore relocation will be done.
     *
     * @param sourceHost name of the source host
     * @param targetHost name of the target host
     * @return String mentioning migration or relocation
     */
    private static String checkOperationType(
            PropertyCollectorHelper propertyCollectorHelper, String targetHost, String sourceHost)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        String operation = "";
        if (targetHost.equalsIgnoreCase(sourceHost)) {
            return "same";
        }
        ManagedObjectReference targetHostMoRef =
                propertyCollectorHelper.getMoRefByName(targetHost, ManagedObjectType.HOST_SYSTEM);
        ManagedObjectReference sourceHostMoRef =
                propertyCollectorHelper.getMoRefByName(sourceHost, ManagedObjectType.HOST_SYSTEM);

        ArrayOfManagedObjectReference dsTargetArr = propertyCollectorHelper.fetch(targetHostMoRef, "datastore");
        List<ManagedObjectReference> dsTarget = dsTargetArr.getManagedObjectReference();
        ManagedObjectReference tarHostDS = browseDsMoRef(propertyCollectorHelper, dsTarget);

        ArrayOfManagedObjectReference dsSourceArr = propertyCollectorHelper.fetch(sourceHostMoRef, "datastore");
        List<ManagedObjectReference> dsSourceList = dsSourceArr.getManagedObjectReference();
        ManagedObjectReference srcHostDS = browseDsMoRef(propertyCollectorHelper, dsSourceList);

        if ((tarHostDS != null) && (srcHostDS != null)) {
            operation = "migrate";
        } else {
            operation = "relocate";
        }
        return operation;
    }

    private static void migrateVM(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            String vmname,
            String pool,
            String tHost,
            String srcHost,
            String priority)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, VmConfigFaultFaultMsg,
                    InsufficientResourcesFaultFaultMsg, FileFaultFaultMsg, MigrationFaultFaultMsg, InvalidStateFaultMsg,
                    TimedoutFaultMsg, InvalidCollectorVersionFaultMsg {
        VirtualMachinePowerState vmPowerState = null;
        VirtualMachineMovePriority vmMovePriority = null;
        if (state != null) {
            if (VirtualMachinePowerState.POWERED_OFF.toString().equalsIgnoreCase(state)) {
                vmPowerState = VirtualMachinePowerState.POWERED_OFF;
            } else if (VirtualMachinePowerState.POWERED_ON.toString().equalsIgnoreCase(state)) {
                vmPowerState = VirtualMachinePowerState.POWERED_ON;
            } else if (VirtualMachinePowerState.SUSPENDED.toString().equalsIgnoreCase(state)) {
                vmPowerState = VirtualMachinePowerState.SUSPENDED;
            }
        }

        if (priority == null) {
            vmMovePriority = VirtualMachineMovePriority.DEFAULT_PRIORITY;
        } else {
            if (VirtualMachineMovePriority.DEFAULT_PRIORITY.toString().equalsIgnoreCase(priority)) {
                vmMovePriority = VirtualMachineMovePriority.DEFAULT_PRIORITY;
            } else if (VirtualMachineMovePriority.HIGH_PRIORITY.toString().equalsIgnoreCase(priority)) {
                vmMovePriority = VirtualMachineMovePriority.HIGH_PRIORITY;
            } else if (VirtualMachineMovePriority.LOW_PRIORITY.toString().equalsIgnoreCase(priority)) {
                vmMovePriority = VirtualMachineMovePriority.LOW_PRIORITY;
            }
        }

        ManagedObjectReference srcMoRef =
                propertyCollectorHelper.getMoRefByName(srcHost, ManagedObjectType.HOST_SYSTEM);
        if (srcMoRef == null) {
            throw new IllegalArgumentException("Source Host" + sourceHost + " Not Found.");
        }

        ManagedObjectReference vmMoRef =
                propertyCollectorHelper.getMoRefByName(srcMoRef, vmname, ManagedObjectType.VIRTUAL_MACHINE);
        if (vmMoRef == null) {
            throw new IllegalArgumentException("Virtual Machine " + vmName + " Not Found.");
        }

        ManagedObjectReference poolMoRef =
                propertyCollectorHelper.getMoRefByName(pool, ManagedObjectType.RESOURCE_POOL);
        if (poolMoRef == null) {
            throw new IllegalArgumentException("Target Resource Pool " + targetPool + " Not Found.");
        }

        ManagedObjectReference hostMoRef = propertyCollectorHelper.getMoRefByName(tHost, ManagedObjectType.HOST_SYSTEM);
        if (hostMoRef == null) {
            throw new IllegalArgumentException("Target Host " + targetHost + " Not Found.");
        }

        log.info("Migrating the Virtual Machine {}", vmname);
        ManagedObjectReference taskMoRef =
                vimPort.migrateVMTask(vmMoRef, poolMoRef, hostMoRef, vmMovePriority, vmPowerState);

        if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
            log.info("Migration of Virtual Machine {} done successfully to {}", vmname, tHost);
        } else {
            log.error("Migration failed");
        }
    }

    /**
     * This method is used for doing the relocation VM task.
     *
     * @param tHost name of the target host
     * @param tDS name of the target datastore
     */
    private static void relocateVM(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            String vmname,
            String pool,
            String tHost,
            String tDS,
            String srcHost)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, VmConfigFaultFaultMsg,
                    InsufficientResourcesFaultFaultMsg, InvalidDatastoreFaultMsg, FileFaultFaultMsg,
                    MigrationFaultFaultMsg, InvalidStateFaultMsg, TimedoutFaultMsg, InvalidCollectorVersionFaultMsg {
        VirtualMachineMovePriority vmMovePriority = null;
        if (priority == null) {
            vmMovePriority = VirtualMachineMovePriority.DEFAULT_PRIORITY;
        } else {
            if (VirtualMachineMovePriority.DEFAULT_PRIORITY.toString().equalsIgnoreCase(priority)) {
                vmMovePriority = VirtualMachineMovePriority.DEFAULT_PRIORITY;
            } else if (VirtualMachineMovePriority.HIGH_PRIORITY.toString().equalsIgnoreCase(priority)) {
                vmMovePriority = VirtualMachineMovePriority.HIGH_PRIORITY;
            } else if (VirtualMachineMovePriority.LOW_PRIORITY.toString().equalsIgnoreCase(priority)) {
                vmMovePriority = VirtualMachineMovePriority.LOW_PRIORITY;
            }
        }

        ManagedObjectReference srcMoRef =
                propertyCollectorHelper.getMoRefByName(srcHost, ManagedObjectType.HOST_SYSTEM);
        if (srcMoRef == null) {
            throw new IllegalArgumentException(" Source Host " + sourceHost + " Not Found.");
        }

        ManagedObjectReference vmMoRef =
                propertyCollectorHelper.getMoRefByName(srcMoRef, vmname, ManagedObjectType.VIRTUAL_MACHINE);
        if (vmMoRef == null) {
            throw new IllegalArgumentException("Virtual Machine " + vmName + " Not Found.");
        }

        ManagedObjectReference poolMoRef =
                propertyCollectorHelper.getMoRefByName(pool, ManagedObjectType.RESOURCE_POOL);
        if (poolMoRef == null) {
            throw new IllegalArgumentException(" Target Resource Pool " + targetPool + " Not Found.");
        }

        ManagedObjectReference hostMoRef = propertyCollectorHelper.getMoRefByName(tHost, ManagedObjectType.HOST_SYSTEM);
        if (hostMoRef == null) {
            throw new IllegalArgumentException(" Target Host " + targetHost + " Not Found.");
        }

        ArrayOfManagedObjectReference dsSourceArr = propertyCollectorHelper.fetch(hostMoRef, "datastore");
        List<ManagedObjectReference> dsTarget = dsSourceArr.getManagedObjectReference();
        ManagedObjectReference dsMoRef = browseDsMoRef(propertyCollectorHelper, dsTarget);
        if (dsMoRef == null) {
            throw new IllegalArgumentException(" DataSource " + tDS + " Not Found.");
        }

        VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();
        relocateSpec.setDatastore(dsMoRef);
        relocateSpec.setHost(hostMoRef);
        relocateSpec.setPool(poolMoRef);

        log.info("Relocating the Virtual Machine {}", vmname);
        ManagedObjectReference taskMoRef = vimPort.relocateVMTask(vmMoRef, relocateSpec, vmMovePriority);

        if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
            log.info("Relocation done successfully of {} to host {}", vmname, tHost);
        } else {
            log.error("Relocation failed");
        }
    }

    private static boolean customValidation() {
        boolean flag = true;
        if (state != null) {
            if (!state.equalsIgnoreCase("poweredOn")
                    && !state.equalsIgnoreCase("poweredOff")
                    && !state.equalsIgnoreCase("suspended")) {
                log.error("Must specify 'poweredOn', 'poweredOff' or 'suspended' for 'state' option\n");
                flag = false;
            }
        }
        if (priority != null) {
            if (!priority.equalsIgnoreCase("default_Priority")
                    && !priority.equalsIgnoreCase("high_Priority")
                    && !priority.equalsIgnoreCase("low_Priority")) {
                log.error("Must specify 'default_Priority', 'high_Priority 'or 'low_Priority' for 'priority' option\n");
                flag = false;
            }
        }
        return flag;
    }
}
