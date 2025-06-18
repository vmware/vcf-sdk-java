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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.ArrayOfVirtualDevice;
import com.vmware.vim25.ConcurrentAccessFaultMsg;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.DuplicateNameFaultMsg;
import com.vmware.vim25.FileFaultFaultMsg;
import com.vmware.vim25.InsufficientResourcesFaultFaultMsg;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidDatastoreFaultMsg;
import com.vmware.vim25.InvalidNameFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ResourceAllocationInfo;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.SharesInfo;
import com.vmware.vim25.SharesLevel;
import com.vmware.vim25.TaskInProgressFaultMsg;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualCdrom;
import com.vmware.vim25.VirtualCdromRemoteAtapiBackingInfo;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecFileOperation;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardNetworkBackingInfo;
import com.vmware.vim25.VirtualIDEController;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualPCNet32;
import com.vmware.vim25.VirtualSCSIController;
import com.vmware.vim25.VmConfigFaultFaultMsg;

/** Reconfigures a virtual machine, which include reconfiguring the disk size, disk mode, etc. */
public class VMReconfig {
    private static final Logger log = LoggerFactory.getLogger(VMReconfig.class);
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
    /** REQUIRED: add|remove|update. */
    public static String operation = "operation";
    /**
     * REQUIRED: cpu|memory|disk|cd|nic. Update operation is only possible for cpu and memory, add|remove are not
     * allowed for cpu and memory.
     */
    public static String device = "device";
    /** REQUIRED: high|low|normal|numeric value, label of device when removing. */
    public static String value = "value";
    /** OPTIONAL: Size of virtual disk. */
    public static String disksize = null;
    /** OPTIONAL: persistent|independent_persistent,independent_nonpersistent. */
    public static String diskmode = null;

    private static ManagedObjectReference virtualMachine;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VMReconfig.class, args);

        if (customValidation()) {
            VcenterClientFactory factory =
                    new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

            try (VcenterClient client = factory.createClient(username, password, null)) {
                VimPortType vimPort = client.getVimPort();
                ServiceContent serviceContent = client.getVimServiceContent();
                PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

                virtualMachine = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);
                if (virtualMachine != null) {
                    reConfig(vimPort, propertyCollectorHelper);
                } else {
                    log.error("Virtual Machine {} Not Found", vmName);
                }
            }
        }
    }

    /** Gets the controller key and the next available free unit number on the SCSI controller. */
    private static List<Integer> getControllerKey(
            PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference vmMoRef)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        List<Integer> result = new ArrayList<>();

        List<VirtualDevice> virtualDevices = ((ArrayOfVirtualDevice)
                        propertyCollectorHelper.fetch(vmMoRef, "config.hardware.device"))
                .getVirtualDevice();

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
                    + "capacity. Please add an additional SCSI controller");
        }
        return result;
    }

    private static boolean customValidation() {
        boolean flag = true;
        if (device.equalsIgnoreCase("disk")) {
            if (operation.equalsIgnoreCase("add")) {
                if ((disksize == null) || (diskmode == null)) {
                    log.error("For add disk operation, disksize and diskmode are the Mandatory options");
                    flag = false;
                }
                if (disksize != null && Integer.parseInt(disksize) <= 0) {
                    log.error("Disksize must be a greater than zero");
                    flag = false;
                }
            }
            if (operation.equalsIgnoreCase("remove")) {
                if (value == null) {
                    log.error("Please specify a label in value field to remove the disk");
                }
            }
        }
        if (device.equalsIgnoreCase("nic")) {
            if (operation == null) {
                log.error("For add nic operation is the Mandatory option");
                flag = false;
            }
        }
        if (device.equalsIgnoreCase("cd")) {
            if (operation == null) {
                log.error("For add cd operation is the Mandatory options");
                flag = false;
            }
        }
        if (operation != null) {
            if (operation.equalsIgnoreCase("add")
                    || operation.equalsIgnoreCase("remove")
                    || operation.equalsIgnoreCase("update")) {
                if (device.equals("cpu") || device.equals("memory")) {
                    if (operation.equals("update")) {
                    } else {
                        log.error("Invalid operation specified for device cpu or memory");
                        flag = false;
                    }
                }
            } else {
                log.error("Operation must be either add, remove or update");
                flag = false;
            }
        }
        return flag;
    }

    private static ResourceAllocationInfo getShares() {
        ResourceAllocationInfo raInfo = new ResourceAllocationInfo();
        SharesInfo sharesInfo = new SharesInfo();
        if (value.equalsIgnoreCase(SharesLevel.HIGH.toString())) {
            sharesInfo.setLevel(SharesLevel.HIGH);
        } else if (value.equalsIgnoreCase(SharesLevel.NORMAL.toString())) {
            sharesInfo.setLevel(SharesLevel.NORMAL);
        } else if (value.equalsIgnoreCase(SharesLevel.LOW.toString())) {
            sharesInfo.setLevel(SharesLevel.LOW);
        } else {
            sharesInfo.setLevel(SharesLevel.CUSTOM);
            sharesInfo.setShares(Integer.parseInt(value));
        }
        raInfo.setShares(sharesInfo);
        return raInfo;
    }

    private static String getDatastoreNameWithFreeSpace(
            PropertyCollectorHelper propertyCollectorHelper, int minFreeSpace)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        String dsName = null;
        List<ManagedObjectReference> datastores = ((ArrayOfManagedObjectReference)
                        propertyCollectorHelper.fetch(virtualMachine, "datastore"))
                .getManagedObjectReference();
        for (ManagedObjectReference datastore : datastores) {
            DatastoreSummary ds = propertyCollectorHelper.fetch(datastore, "summary");
            if (ds.getFreeSpace() > minFreeSpace) {
                dsName = ds.getName();
                break;
            }
        }
        return dsName;
    }

    private static VirtualDeviceConfigSpec getDiskDeviceConfigSpec(PropertyCollectorHelper propertyCollectorHelper)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        String ops = operation;
        VirtualDeviceConfigSpec diskSpec = new VirtualDeviceConfigSpec();

        if (ops.equalsIgnoreCase("Add")) {
            VirtualDisk disk = new VirtualDisk();
            VirtualDiskFlatVer2BackingInfo diskfileBacking = new VirtualDiskFlatVer2BackingInfo();
            String dsName = getDatastoreNameWithFreeSpace(propertyCollectorHelper, Integer.parseInt(disksize));

            int ckey = 0;
            int unitNumber = 0;
            List<Integer> getControllerKeyReturnArr = getControllerKey(propertyCollectorHelper, virtualMachine);
            if (!getControllerKeyReturnArr.isEmpty()) {
                ckey = getControllerKeyReturnArr.get(0);
                unitNumber = getControllerKeyReturnArr.get(1);
            }
            String fileName = "[" + dsName + "] " + vmName + "/" + value + ".vmdk";
            diskfileBacking.setFileName(fileName);
            diskfileBacking.setDiskMode(diskmode);

            disk.setControllerKey(ckey);
            disk.setUnitNumber(unitNumber);
            disk.setBacking(diskfileBacking);
            int size = 1024 * (Integer.parseInt(disksize));
            disk.setCapacityInKB(size);
            disk.setKey(-1);

            diskSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
            diskSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.CREATE);
            diskSpec.setDevice(disk);
        } else if (ops.equalsIgnoreCase("Remove")) {
            VirtualDisk disk = null;
            List<VirtualDevice> deviceList = ((ArrayOfVirtualDevice)
                            propertyCollectorHelper.fetch(virtualMachine, "config.hardware.device"))
                    .getVirtualDevice();
            for (VirtualDevice device : deviceList) {
                if (device instanceof VirtualDisk) {
                    if (value.equalsIgnoreCase(device.getDeviceInfo().getLabel())) {
                        disk = (VirtualDisk) device;
                        break;
                    }
                }
            }
            if (disk != null) {
                diskSpec.setOperation(VirtualDeviceConfigSpecOperation.REMOVE);
                diskSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.DESTROY);
                diskSpec.setDevice(disk);
            } else {
                log.error("No device found {}", value);
                return null;
            }
        }
        return diskSpec;
    }

    private static VirtualDeviceConfigSpec getCDDeviceConfigSpec(PropertyCollectorHelper propertyCollectorHelper)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        String ops = operation;
        VirtualDeviceConfigSpec cdSpec = new VirtualDeviceConfigSpec();
        List<VirtualDevice> listvd = ((ArrayOfVirtualDevice)
                        propertyCollectorHelper.fetch(virtualMachine, "config.hardware.device"))
                .getVirtualDevice();

        if (ops.equalsIgnoreCase("Add")) {
            cdSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

            VirtualCdrom cdrom = new VirtualCdrom();

            VirtualCdromRemoteAtapiBackingInfo vcrabi = new VirtualCdromRemoteAtapiBackingInfo();
            vcrabi.setDeviceName("");
            vcrabi.setUseAutoDetect(true);

            Map<Integer, VirtualDevice> deviceMap = new HashMap<>();
            for (VirtualDevice virtualDevice : listvd) {
                deviceMap.put(virtualDevice.getKey(), virtualDevice);
            }
            int controllerKey = 0;
            int unitNumber = 0;
            boolean found = false;
            for (VirtualDevice virtualDevice : listvd) {
                if (virtualDevice instanceof VirtualIDEController) {
                    VirtualIDEController vscsic = (VirtualIDEController) virtualDevice;
                    int[] slots = new int[2];
                    List<Integer> devicelist = vscsic.getDevice();
                    for (Integer deviceKey : devicelist) {
                        if (deviceMap.get(deviceKey).getUnitNumber() != null) {
                            slots[deviceMap.get(deviceKey).getUnitNumber()] = 1;
                        }
                    }
                    for (int i = 0; i < slots.length; i++) {
                        if (slots[i] != 1) {
                            controllerKey = vscsic.getKey();
                            unitNumber = i;
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
                throw new RuntimeException("The IDE controller on the vm has maxed out its "
                        + "capacity. Please add an additional IDE controller");
            }

            cdrom.setBacking(vcrabi);
            cdrom.setControllerKey(controllerKey);
            cdrom.setUnitNumber(unitNumber);
            cdrom.setKey(-1);

            cdSpec.setDevice(cdrom);
            return cdSpec;
        } else {
            VirtualCdrom cdRemove = null;
            cdSpec.setOperation(VirtualDeviceConfigSpecOperation.REMOVE);
            for (VirtualDevice device : listvd) {
                if (device instanceof VirtualCdrom) {
                    if (value.equalsIgnoreCase(device.getDeviceInfo().getLabel())) {
                        cdRemove = (VirtualCdrom) device;
                        break;
                    }
                }
            }
            if (cdRemove != null) {
                cdSpec.setDevice(cdRemove);
            } else {
                log.error("No device available {}", value);
                return null;
            }
        }
        return cdSpec;
    }

    private static VirtualDeviceConfigSpec getNICDeviceConfigSpec(PropertyCollectorHelper propertyCollectorHelper)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        String ops = operation;
        VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();
        if (ops.equalsIgnoreCase("Add")) {
            nicSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
            VirtualEthernetCard nic = new VirtualPCNet32();
            VirtualEthernetCardNetworkBackingInfo nicBacking = new VirtualEthernetCardNetworkBackingInfo();
            nicBacking.setDeviceName(value);
            nic.setAddressType("generated");
            nic.setBacking(nicBacking);
            nic.setKey(-1);
            nicSpec.setDevice(nic);
        } else if (ops.equalsIgnoreCase("Remove")) {
            VirtualEthernetCard nic = null;
            nicSpec.setOperation(VirtualDeviceConfigSpecOperation.REMOVE);
            List<VirtualDevice> listvd = ((ArrayOfVirtualDevice)
                            propertyCollectorHelper.fetch(virtualMachine, "config.hardware.device"))
                    .getVirtualDevice();
            for (VirtualDevice device : listvd) {
                if (device instanceof VirtualEthernetCard) {
                    if (value.equalsIgnoreCase(device.getDeviceInfo().getLabel())) {
                        nic = (VirtualEthernetCard) device;
                        break;
                    }
                }
            }
            if (nic != null) {
                nicSpec.setDevice(nic);
            } else {
                log.error("No device available {}", value);
                return null;
            }
        }
        return nicSpec;
    }

    private static void reConfig(VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper)
            throws InvalidPropertyFaultMsg, DuplicateNameFaultMsg, TaskInProgressFaultMsg, VmConfigFaultFaultMsg,
                    InsufficientResourcesFaultFaultMsg, InvalidDatastoreFaultMsg, FileFaultFaultMsg,
                    ConcurrentAccessFaultMsg, InvalidStateFaultMsg, InvalidNameFaultMsg,
                    InvalidCollectorVersionFaultMsg, RuntimeFaultFaultMsg {
        String deviceType = device;
        VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();

        if (deviceType.equalsIgnoreCase("memory") && operation.equals("update")) {
            log.info("Reconfiguring The Virtual Machine For Memory Update {}", vmName);
            try {
                vmConfigSpec.setMemoryAllocation(getShares());
            } catch (java.lang.NumberFormatException nfe) {
                log.error("Value of Memory update must be one of high|low|normal|[numeric value]");
                return;
            }
        } else if (deviceType.equalsIgnoreCase("cpu") && operation.equals("update")) {
            log.info("Reconfiguring The Virtual Machine For CPU Update {}", vmName);
            try {
                vmConfigSpec.setCpuAllocation(getShares());
            } catch (java.lang.NumberFormatException nfe) {
                log.error("Value of CPU update must be one of high|low|normal|[numeric value]");
                return;
            }
        } else if (deviceType.equalsIgnoreCase("disk") && !operation.equals("update")) {
            log.info("Reconfiguring The Virtual Machine For Disk Update {}", vmName);
            VirtualDeviceConfigSpec vdiskSpec = getDiskDeviceConfigSpec(propertyCollectorHelper);
            if (vdiskSpec != null) {
                List<VirtualDeviceConfigSpec> vdiskSpecArray = new ArrayList<>();
                vdiskSpecArray.add(vdiskSpec);
                vmConfigSpec.getDeviceChange().addAll(vdiskSpecArray);
            } else {
                return;
            }
        } else if (deviceType.equalsIgnoreCase("nic") && !operation.equals("update")) {
            log.info("Reconfiguring The Virtual Machine For NIC Update {}", vmName);
            VirtualDeviceConfigSpec nicSpec = getNICDeviceConfigSpec(propertyCollectorHelper);
            if (nicSpec != null) {
                List<VirtualDeviceConfigSpec> nicSpecArray = new ArrayList<>();
                nicSpecArray.add(nicSpec);
                vmConfigSpec.getDeviceChange().addAll(nicSpecArray);
            } else {
                return;
            }
        } else if (deviceType.equalsIgnoreCase("cd") && !operation.equals("update")) {
            log.info("Reconfiguring The Virtual Machine For CD Update {}", vmName);
            VirtualDeviceConfigSpec cdSpec = getCDDeviceConfigSpec(propertyCollectorHelper);
            if (cdSpec != null) {
                List<VirtualDeviceConfigSpec> cdSpecArray = new ArrayList<>();
                cdSpecArray.add(cdSpec);
                vmConfigSpec.getDeviceChange().addAll(cdSpecArray);
            } else {
                return;
            }
        } else {
            log.error("Invalid device type [memory|cpu|disk|nic|cd]");
            return;
        }

        ManagedObjectReference taskMoRef = vimPort.reconfigVMTask(virtualMachine, vmConfigSpec);
        if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
            log.info("Virtual Machine reconfigured successfully");
        } else {
            log.error("Virtual Machine reconfigure failed");
        }
    }
}
