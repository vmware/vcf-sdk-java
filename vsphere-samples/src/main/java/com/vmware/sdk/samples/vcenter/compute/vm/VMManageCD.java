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
import com.vmware.vim25.ArrayOfVirtualDevice;
import com.vmware.vim25.ConcurrentAccessFaultMsg;
import com.vmware.vim25.Description;
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
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualCdrom;
import com.vmware.vim25.VirtualCdromAtapiBackingInfo;
import com.vmware.vim25.VirtualCdromIsoBackingInfo;
import com.vmware.vim25.VirtualCdromPassthroughBackingInfo;
import com.vmware.vim25.VirtualCdromRemoteAtapiBackingInfo;
import com.vmware.vim25.VirtualCdromRemotePassthroughBackingInfo;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDeviceConnectInfo;
import com.vmware.vim25.VirtualIDEController;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VmConfigFaultFaultMsg;

/**
 * This sample adds/removes CDROM to/from an existing VM.
 *
 * <p>This sample lists information about a VMs CDROMs.
 *
 * <p>This sample updates an existing CDROM a VM.
 */
public class VMManageCD {
    private static final Logger log = LoggerFactory.getLogger(VMManageCD.class);
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
    public static String virtualMachineName = "virtualMachineName";
    /** REQUIRED: Operation type - [get|add|remove|set]. */
    public static String operation = "operation";
    /** OPTIONAL: Used to find the device.key value. */
    public static String labelName = null;
    /** OPTIONAL: Virtual CD is connected after creation or update. Set only if VM is powered on. */
    public static String connect = null;
    /** OPTIONAL: Full datastore path to the iso file. */
    public static String isoPath = null;
    /** OPTIONAL: Specify the path to the CD on the VM's host. */
    public static String deviceName = null;
    /** OPTIONAL: Specify the device is a remote or client device or iso. */
    public static String remote = null;
    /** OPTIONAL: Virtual CD starts connected when VM powers on. */
    public static String startConnected = null;

    private static ManagedObjectReference vmRef;

    private static final String[] operations = {"get", "add", "remove", "set"};

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VMManageCD.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            if (!check(operation, operations)) {
                throw new RuntimeException("Invalid operation: " + operation + "must be [get|add|remove|set]");
            }

            vmRef = propertyCollectorHelper.getMoRefByName(virtualMachineName, VIRTUAL_MACHINE);

            if (vmRef == null) {
                log.error("Virtual Machine {} not found.", virtualMachineName);
                return;
            }
            if (operation.equalsIgnoreCase("get")) {
                getInfo(propertyCollectorHelper);
            } else if (operation.equalsIgnoreCase("add")) {
                addCdRom(vimPort, propertyCollectorHelper);
            }
            if (operation.equalsIgnoreCase("remove")) {
                removeCdRom(vimPort, propertyCollectorHelper);
            }
            if (operation.equalsIgnoreCase("set")) {
                setCdRom(vimPort, propertyCollectorHelper);
            }
        }
    }

    // Prints the information for all the CD Roms attached
    private static void getInfo(PropertyCollectorHelper propertyCollectorHelper)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        List<VirtualDevice> virtualDevices = ((ArrayOfVirtualDevice)
                        propertyCollectorHelper.fetch(vmRef, "config.hardware.device"))
                .getVirtualDevice();

        int count = 0;
        for (VirtualDevice device : virtualDevices) {
            if (device instanceof VirtualCdrom) {
                String name = device.getDeviceInfo().getLabel();
                int key = device.getKey();
                boolean isCdConnected = device.getConnectable().isConnected();
                boolean isConnectedAtPowerOn = device.getConnectable().isStartConnected();
                boolean isRemote = false;
                String deviceName = "";
                String isoPath = "";

                if (device.getBacking() instanceof VirtualCdromRemoteAtapiBackingInfo) {
                    isRemote = true;
                    deviceName = ((VirtualCdromRemoteAtapiBackingInfo) device.getBacking()).getDeviceName();
                } else if (device.getBacking() instanceof VirtualCdromRemotePassthroughBackingInfo) {
                    isRemote = true;
                    deviceName = ((VirtualCdromRemotePassthroughBackingInfo) device.getBacking()).getDeviceName();
                } else if (device.getBacking() instanceof VirtualCdromAtapiBackingInfo) {
                    deviceName = ((VirtualCdromAtapiBackingInfo) device.getBacking()).getDeviceName();
                } else if (device.getBacking() instanceof VirtualCdromPassthroughBackingInfo) {
                    deviceName = ((VirtualCdromPassthroughBackingInfo) device.getBacking()).getDeviceName();
                } else if (device.getBacking() instanceof VirtualCdromIsoBackingInfo) {
                    isoPath = ((VirtualCdromIsoBackingInfo) device.getBacking()).getFileName();
                }

                System.out.println("ISO Path                : " + isoPath);
                System.out.println("Device                  : " + deviceName);
                System.out.println("Remote                  : " + isRemote);
                System.out.println("Connected               : " + isCdConnected);
                System.out.println("ConnectedAtPowerOn      : " + isConnectedAtPowerOn);
                System.out.println("Id                      : " + "VirtualMachine-" + vmRef.getValue() + "/" + key);
                System.out.println("Name                    : " + "CD/" + name);

                count++;
            }
        }
        if (count == 0) {
            System.out.println("No CdRom device attached to this VM.");
        }
    }

    private static void addCdRom(VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper)
            throws DuplicateNameFaultMsg, RuntimeFaultFaultMsg, TaskInProgressFaultMsg, VmConfigFaultFaultMsg,
                    InsufficientResourcesFaultFaultMsg, InvalidDatastoreFaultMsg, FileFaultFaultMsg,
                    ConcurrentAccessFaultMsg, InvalidStateFaultMsg, InvalidNameFaultMsg, InvalidPropertyFaultMsg,
                    InvalidCollectorVersionFaultMsg {
        int controllerKey = -1;
        int unitNumber = 0;

        List<VirtualDevice> virtualDevices = ((ArrayOfVirtualDevice)
                        propertyCollectorHelper.fetch(vmRef, "config.hardware.device"))
                .getVirtualDevice();

        Map<Integer, VirtualDevice> deviceMap = new HashMap<>();
        for (VirtualDevice virtualDevice : virtualDevices) {
            deviceMap.put(virtualDevice.getKey(), virtualDevice);
        }

        boolean found = false;

        for (VirtualDevice virtualDevice : virtualDevices) {
            if (virtualDevice instanceof VirtualIDEController) {
                VirtualIDEController virtualIDEController = (VirtualIDEController) virtualDevice;

                int[] slots = new int[2];

                List<Integer> devicelist = virtualIDEController.getDevice();
                for (Integer deviceKey : devicelist) {
                    if (deviceMap.get(deviceKey).getUnitNumber() != null) {
                        slots[deviceMap.get(deviceKey).getUnitNumber()] = 1;
                    }
                }
                for (int i = 0; i < slots.length; i++) {
                    if (slots[i] != 1) {
                        controllerKey = virtualIDEController.getKey();
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

        VirtualCdrom cdRom = new VirtualCdrom();
        cdRom.setControllerKey(controllerKey);
        cdRom.setUnitNumber(unitNumber);
        cdRom.setKey(-1);

        VirtualDeviceConnectInfo deviceConnectInfo = new VirtualDeviceConnectInfo();
        if (connect != null) {
            deviceConnectInfo.setConnected(Boolean.parseBoolean(connect));
        }
        if (startConnected != null) {
            deviceConnectInfo.setStartConnected(Boolean.parseBoolean(startConnected));
        }

        cdRom.setConnectable(deviceConnectInfo);

        if (deviceName == null && isoPath == null) {
            if ("true".equalsIgnoreCase(remote)) {
                VirtualCdromRemotePassthroughBackingInfo backingInfo = new VirtualCdromRemotePassthroughBackingInfo();
                backingInfo.setExclusive(false);
                backingInfo.setDeviceName("");
                backingInfo.setUseAutoDetect(true);

                cdRom.setBacking(backingInfo);
            } else {
                log.error("For Local option, either specify ISOPath or Device Name");
                return;
            }
        } else {
            if (deviceName != null) {
                if ("true".equalsIgnoreCase(remote)) {
                    log.error("For Device name option is only valid for Local CD Rom");
                    return;
                } else {
                    VirtualCdromAtapiBackingInfo backingInfo = new VirtualCdromAtapiBackingInfo();
                    backingInfo.setDeviceName(deviceName);

                    cdRom.setBacking(backingInfo);
                }
            } else if (isoPath != null) {
                VirtualCdromIsoBackingInfo backingInfo = new VirtualCdromIsoBackingInfo();
                if ("true".equalsIgnoreCase(remote)) {
                    log.error("Iso path option is only valid for Local CD Rom");
                    return;
                } else {
                    backingInfo.setFileName(isoPath);
                }
                cdRom.setBacking(backingInfo);
            }
        }

        VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();
        deviceConfigSpec.setDevice(cdRom);
        deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

        List<VirtualDeviceConfigSpec> virtualDevicesConfigSpecs = new ArrayList<>();
        virtualDevicesConfigSpecs.add(deviceConfigSpec);

        VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
        configSpec.getDeviceChange().addAll(virtualDevicesConfigSpecs);

        ManagedObjectReference taskMoRef = vimPort.reconfigVMTask(vmRef, configSpec);

        if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
            log.info("Reconfiguring the Virtual Machine - [ {} ] Successful on {}\n", virtualMachineName, operation);
        } else {
            log.error("Reconfiguring the Virtual Machine - [ {} ] Failure on {}\n", virtualMachineName, operation);
        }
    }

    private static void removeCdRom(VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, DuplicateNameFaultMsg, TaskInProgressFaultMsg,
                    VmConfigFaultFaultMsg, InsufficientResourcesFaultFaultMsg, InvalidDatastoreFaultMsg,
                    FileFaultFaultMsg, ConcurrentAccessFaultMsg, InvalidStateFaultMsg, InvalidNameFaultMsg,
                    InvalidCollectorVersionFaultMsg {
        if (labelName == null) {
            log.info("Option label is required for remove option");
            return;
        }
        List<VirtualDevice> virtualDevices = ((ArrayOfVirtualDevice)
                        propertyCollectorHelper.fetch(vmRef, "config.hardware.device"))
                .getVirtualDevice();

        VirtualDevice cdRom = null;

        for (VirtualDevice device : virtualDevices) {
            if (device instanceof VirtualCdrom) {
                Description info = device.getDeviceInfo();
                if (info != null) {
                    if (info.getLabel().equalsIgnoreCase(labelName)) {
                        cdRom = device;
                        break;
                    }
                }
            }
        }
        if (cdRom == null) {
            log.error("Specified Device Not Found");
            return;
        }

        VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();
        deviceConfigSpec.setDevice(cdRom);
        deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.REMOVE);

        List<VirtualDeviceConfigSpec> virtualDevicesConfigSpecs = new ArrayList<>();
        virtualDevicesConfigSpecs.add(deviceConfigSpec);

        VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
        configSpec.getDeviceChange().addAll(virtualDevicesConfigSpecs);

        ManagedObjectReference taskMoRef = vimPort.reconfigVMTask(vmRef, configSpec);

        if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
            log.info("Reconfiguring the Virtual Machine - [ {} ] Successful on {}\n", virtualMachineName, operation);
        } else {
            log.error("Reconfiguring the Virtual Machine - [ {} ] Failure on {}\n", virtualMachineName, operation);
        }
    }

    // Reconfigure the CdRom
    private static void setCdRom(VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg,
                    DuplicateNameFaultMsg, TaskInProgressFaultMsg, VmConfigFaultFaultMsg,
                    InsufficientResourcesFaultFaultMsg, InvalidDatastoreFaultMsg, FileFaultFaultMsg,
                    ConcurrentAccessFaultMsg, InvalidStateFaultMsg, InvalidNameFaultMsg {
        List<VirtualDevice> virtualDevices = ((ArrayOfVirtualDevice)
                        propertyCollectorHelper.fetch(vmRef, "config.hardware.device"))
                .getVirtualDevice();

        VirtualDevice cdRom = null;
        for (VirtualDevice device : virtualDevices) {
            if (device instanceof VirtualCdrom) {
                Description info = device.getDeviceInfo();
                if (info != null) {
                    if (info.getLabel().equalsIgnoreCase(labelName)) {
                        cdRom = device;
                        break;
                    }
                }
            }
        }
        if (cdRom == null) {
            log.error("Specified Device Not Found");
            return;
        }

        VirtualDeviceConnectInfo connectInfo = new VirtualDeviceConnectInfo();
        if (connect != null) {
            connectInfo.setConnected(Boolean.parseBoolean(connect));
        }

        if (startConnected != null) {
            connectInfo.setStartConnected(Boolean.parseBoolean(startConnected));
        }

        cdRom.setConnectable(connectInfo);

        if (deviceName == null && isoPath == null) {
            if ("true".equalsIgnoreCase(remote)) {
                VirtualCdromRemotePassthroughBackingInfo backingInfo = new VirtualCdromRemotePassthroughBackingInfo();
                backingInfo.setExclusive(false);
                backingInfo.setDeviceName("");
                backingInfo.setUseAutoDetect(true);

                cdRom.setBacking(backingInfo);
            }
        } else {
            if (deviceName != null) {
                if ("true".equalsIgnoreCase(remote)) {
                    log.info("For Device name option is only valid for Local CD Rom");
                    return;
                } else {
                    VirtualCdromAtapiBackingInfo backingInfo = new VirtualCdromAtapiBackingInfo();
                    backingInfo.setDeviceName(deviceName);

                    cdRom.setBacking(backingInfo);
                }
            } else if (isoPath != null) {
                VirtualCdromIsoBackingInfo backingInfo = new VirtualCdromIsoBackingInfo();
                if ("true".equalsIgnoreCase(remote)) {
                    log.info("Iso path option is only valid for Local CD Rom");
                    return;
                } else {
                    backingInfo.setFileName(isoPath);
                }
                cdRom.setBacking(backingInfo);
            }
        }

        VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();
        deviceConfigSpec.setDevice(cdRom);
        deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.EDIT);

        List<VirtualDeviceConfigSpec> virtualDevicesConfigSpecs = new ArrayList<>();
        virtualDevicesConfigSpecs.add(deviceConfigSpec);

        VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
        configSpec.getDeviceChange().addAll(virtualDevicesConfigSpecs);

        ManagedObjectReference taskMoRef = vimPort.reconfigVMTask(vmRef, configSpec);

        if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
            log.info("Reconfiguring the Virtual Machine - [ {} ] Successful on {}\n", virtualMachineName, operation);
        } else {
            log.error("Reconfiguring the Virtual Machine - [ {} ] Failure on {}\n", virtualMachineName, operation);
        }
    }

    private static boolean check(final String operation, final String[] operations) {
        boolean found = false;
        for (String op : operations) {
            if (op.equals(operation)) {
                found = true;
                break;
            }
        }
        return found;
    }
}
