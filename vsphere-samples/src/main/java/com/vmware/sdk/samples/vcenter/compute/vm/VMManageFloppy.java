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
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDeviceConnectInfo;
import com.vmware.vim25.VirtualFloppy;
import com.vmware.vim25.VirtualFloppyDeviceBackingInfo;
import com.vmware.vim25.VirtualFloppyImageBackingInfo;
import com.vmware.vim25.VirtualFloppyRemoteDeviceBackingInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VmConfigFaultFaultMsg;

/**
 * This sample adds/removes floppy to/from an existing VM
 *
 * <p>This sample lists information about a VMs Floppies.
 *
 * <p>This sample updates an existing floppy drive on a VM.
 */
public class VMManageFloppy {
    private static final Logger log = LoggerFactory.getLogger(VMManageFloppy.class);
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
    /** REQUIRED: Operation type - [get|add|remove|set]. */
    public static String operation = "operation";
    /** OPTIONAL: Path of image file. */
    public static String imgPath = null;
    /** OPTIONAL: Device is a remote or client device or iso. */
    public static String remote = null;
    /** OPTIONAL: Virtual floppy starts connected on VM poweron. */
    public static String startConnected = null;
    /** OPTIONAL: Path to the floppy on the VM's host. */
    public static String device = null;
    /** OPTIONAL: Used to find the device.key value. */
    public static String label = null;
    /** OPTIONAL: Virtual floppy is connected. Set only if the VM is powered on. */
    public static String connect = null;

    private static ManagedObjectReference vmRef;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VMManageFloppy.class, args);

        if (validateTheInput()) {
            VcenterClientFactory factory =
                    new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

            try (VcenterClient client = factory.createClient(username, password, null)) {
                VimPortType vimPort = client.getVimPort();
                ServiceContent serviceContent = client.getVimServiceContent();
                PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

                vmRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);
                if (vmRef == null) {
                    log.error("Virtual Machine {} not found.", vmName);
                    return;
                }
                if (operation.equalsIgnoreCase("get")) {
                    getInfo(propertyCollectorHelper);
                } else if (operation.equalsIgnoreCase("add")) {
                    addFloppy(vimPort, propertyCollectorHelper);
                }
                if (operation.equalsIgnoreCase("remove")) {
                    removeFloppy(vimPort, propertyCollectorHelper);
                }
                if (operation.equalsIgnoreCase("set")) {
                    setFloppy(vimPort, propertyCollectorHelper);
                }
            }
        }
    }

    private static boolean validateTheInput() {
        boolean valid = true;

        if (operation != null) {
            if (!operation.equalsIgnoreCase("add")
                    && !operation.equalsIgnoreCase("get")
                    && !operation.equalsIgnoreCase("remove")
                    && !operation.equalsIgnoreCase("set")) {
                log.error("Invalid option for operation");
                log.error("Valid Options : get | remove | add | set");
                valid = false;
            }
        }
        if (connect != null) {
            if (!connect.equalsIgnoreCase("true") && !connect.equalsIgnoreCase("false")) {
                log.error("Invalid option for connect");
                log.error("Valid Options : true | false");
                valid = false;
            }
        }
        if (startConnected != null) {
            if (!startConnected.equalsIgnoreCase("true") && !startConnected.equalsIgnoreCase("false")) {
                log.error("Invalid option for startConnected");
                log.error("Valid Options : true | false");
                valid = false;
            }
        }
        if (remote != null) {
            if (!"true".equalsIgnoreCase(remote) && !"false".equalsIgnoreCase(remote)) {
                log.error("Invalid option for remote");
                log.error("Valid Options : true | false");
                valid = false;
            }
        }
        return valid;
    }

    private static void getInfo(PropertyCollectorHelper propertyCollectorHelper)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        List<VirtualDevice> virtualDevices = ((ArrayOfVirtualDevice)
                        propertyCollectorHelper.fetch(vmRef, "config.hardware.device"))
                .getVirtualDevice();

        int count = 0;
        for (VirtualDevice virtualDevice : virtualDevices) {
            if (virtualDevice instanceof VirtualFloppy) {
                String name = virtualDevice.getDeviceInfo().getLabel();
                int key = virtualDevice.getKey();
                boolean isConnected = virtualDevice.getConnectable().isConnected();
                boolean isConnectedAtPowerOn = virtualDevice.getConnectable().isStartConnected();

                boolean isRemote = false;
                String deviceName = "";
                String imgPath = "";

                if (virtualDevice.getBacking() instanceof VirtualFloppyRemoteDeviceBackingInfo) {
                    isRemote = true;
                    deviceName = ((VirtualFloppyRemoteDeviceBackingInfo) virtualDevice.getBacking()).getDeviceName();
                }

                if (virtualDevice.getBacking() instanceof VirtualFloppyDeviceBackingInfo) {
                    deviceName = ((VirtualFloppyDeviceBackingInfo) virtualDevice.getBacking()).getDeviceName();
                }

                if (virtualDevice.getBacking() instanceof VirtualFloppyImageBackingInfo) {
                    imgPath = ((VirtualFloppyImageBackingInfo) virtualDevice.getBacking()).getFileName();
                }

                System.out.println("Image Path              : " + imgPath);
                System.out.println("Device                  : " + deviceName);
                System.out.println("Remote                  : " + isRemote);
                System.out.println("Connected               : " + isConnected);
                System.out.println("ConnectedAtPowerOn      : " + isConnectedAtPowerOn);
                System.out.println("Id                      : " + "VirtualMachine-" + vmRef.getValue() + "/" + key);
                System.out.println("Name                    : " + "Floppy/" + name);
                System.out.println("---------------------------------------------");
                count++;
            }
        }
        if (count == 0) {
            System.out.println("No Floppy device attached to this VM.");
        }
    }

    private static void addFloppy(VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper)
            throws ConcurrentAccessFaultMsg, DuplicateNameFaultMsg, FileFaultFaultMsg,
                    InsufficientResourcesFaultFaultMsg, InvalidDatastoreFaultMsg, InvalidNameFaultMsg,
                    InvalidStateFaultMsg, RuntimeFaultFaultMsg, TaskInProgressFaultMsg, VmConfigFaultFaultMsg,
                    InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg {

        VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();
        VirtualFloppy floppyDevice = new VirtualFloppy();
        floppyDevice.setKey(-1);

        VirtualDeviceConnectInfo connectInfo = new VirtualDeviceConnectInfo();
        if (connect != null) {
            connectInfo.setConnected(Boolean.parseBoolean(connect));
        }

        if (startConnected != null) {
            connectInfo.setStartConnected(Boolean.parseBoolean(startConnected));
        }

        floppyDevice.setConnectable(connectInfo);

        if ("true".equalsIgnoreCase(remote)) {
            VirtualFloppyRemoteDeviceBackingInfo backingInfo = new VirtualFloppyRemoteDeviceBackingInfo();
            backingInfo.setDeviceName("/dev/fd0");

            floppyDevice.setBacking(backingInfo);
        } else if (imgPath != null) {
            VirtualFloppyImageBackingInfo backingInfo = new VirtualFloppyImageBackingInfo();
            backingInfo.setFileName(imgPath);

            floppyDevice.setBacking(backingInfo);
        } else if (device != null) {
            VirtualFloppyDeviceBackingInfo backingInfo = new VirtualFloppyDeviceBackingInfo();
            backingInfo.setDeviceName(device);

            floppyDevice.setBacking(backingInfo);
        } else {
            throw new IllegalArgumentException("Please specify the --imgpath or --device option if --remote "
                    + "is either omitted or set to false while adding a floppy\n");
        }

        deviceConfigSpec.setDevice(floppyDevice);
        deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

        List<VirtualDeviceConfigSpec> virtualDevicesConfigSpecs = new ArrayList<>();
        virtualDevicesConfigSpecs.add(deviceConfigSpec);

        VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
        configSpec.getDeviceChange().addAll(virtualDevicesConfigSpecs);

        ManagedObjectReference taskMoRef = vimPort.reconfigVMTask(vmRef, configSpec);

        if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
            log.info("Reconfiguring the Virtual Machine - [ {} ] Successful on {}\n", vmName, operation);
        } else {
            log.error("Reconfiguring the Virtual Machine - [ {} ] Failure on {}\n", vmName, operation);
        }
    }

    private static void removeFloppy(VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg,
                    DuplicateNameFaultMsg, TaskInProgressFaultMsg, VmConfigFaultFaultMsg,
                    InsufficientResourcesFaultFaultMsg, InvalidDatastoreFaultMsg, FileFaultFaultMsg,
                    ConcurrentAccessFaultMsg, InvalidStateFaultMsg, InvalidNameFaultMsg {
        if (label == null) {
            log.error("Option label is required for remove option");
            return;
        }
        List<VirtualDevice> virtualDevices = ((ArrayOfVirtualDevice)
                        propertyCollectorHelper.fetch(vmRef, "config.hardware.device"))
                .getVirtualDevice();

        VirtualDevice floppy = null;

        for (VirtualDevice device : virtualDevices) {
            if (device instanceof VirtualFloppy) {
                Description info = device.getDeviceInfo();
                if (info != null) {
                    if (info.getLabel().equalsIgnoreCase(label)) {
                        floppy = device;
                        break;
                    }
                }
            }
        }
        if (floppy == null) {
            log.error("Specified Device Not Found");
            return;
        }

        VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();
        deviceConfigSpec.setDevice(floppy);
        deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.REMOVE);

        List<VirtualDeviceConfigSpec> virtualDevicesConfigSpecs = new ArrayList<>();
        virtualDevicesConfigSpecs.add(deviceConfigSpec);

        VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
        configSpec.getDeviceChange().addAll(virtualDevicesConfigSpecs);

        ManagedObjectReference taskMoRef = vimPort.reconfigVMTask(vmRef, configSpec);

        if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
            log.info("Reconfiguring the Virtual Machine - [ {} ] Successful on {}\n", vmName, operation);
        } else {
            log.error("Reconfiguring the Virtual Machine - [ {} ] Failure on {}\n", vmName, operation);
        }
    }

    private static void setFloppy(VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, DuplicateNameFaultMsg, TaskInProgressFaultMsg,
                    VmConfigFaultFaultMsg, InsufficientResourcesFaultFaultMsg, InvalidDatastoreFaultMsg,
                    FileFaultFaultMsg, ConcurrentAccessFaultMsg, InvalidStateFaultMsg, InvalidNameFaultMsg,
                    InvalidCollectorVersionFaultMsg {
        if (label == null) {
            log.error("Option label is required for set option");
            return;
        }

        List<VirtualDevice> virtualDevices = ((ArrayOfVirtualDevice)
                        propertyCollectorHelper.fetch(vmRef, "config.hardware.device"))
                .getVirtualDevice();

        VirtualDevice floppy = null;

        for (VirtualDevice device : virtualDevices) {
            if (device instanceof VirtualFloppy) {
                Description info = device.getDeviceInfo();
                if (info != null) {
                    if (info.getLabel().equalsIgnoreCase(label)) {
                        floppy = device;
                        break;
                    }
                }
            }
        }
        if (floppy == null) {
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

        floppy.setConnectable(connectInfo);

        if ("true".equalsIgnoreCase(remote)) {
            VirtualFloppyRemoteDeviceBackingInfo backingInfo = new VirtualFloppyRemoteDeviceBackingInfo();
            backingInfo.setDeviceName("/dev/fd0");

            floppy.setBacking(backingInfo);
        } else if (imgPath != null) {
            VirtualFloppyImageBackingInfo backingInfo = new VirtualFloppyImageBackingInfo();
            backingInfo.setFileName(imgPath);

            floppy.setBacking(backingInfo);
        } else if (device != null) {
            VirtualFloppyDeviceBackingInfo backingInfo = new VirtualFloppyDeviceBackingInfo();
            backingInfo.setDeviceName(device);

            floppy.setBacking(backingInfo);
        }

        VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();
        deviceConfigSpec.setDevice(floppy);
        deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.EDIT);

        List<VirtualDeviceConfigSpec> deviceConfigSpecArr = new ArrayList<>();
        deviceConfigSpecArr.add(deviceConfigSpec);

        VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
        configSpec.getDeviceChange().addAll(deviceConfigSpecArr);

        ManagedObjectReference taskMoRef = vimPort.reconfigVMTask(vmRef, configSpec);

        if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
            log.info("Reconfiguring the Virtual Machine - [ {} ] Successful on {}\n", vmName, operation);
        } else {
            log.error("Reconfiguring the Virtual Machine - [ {} ] Failure on {}\n", vmName, operation);
        }
    }
}
