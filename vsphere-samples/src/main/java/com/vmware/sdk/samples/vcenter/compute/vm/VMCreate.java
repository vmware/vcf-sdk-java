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
import static com.vmware.vim25.ManagedObjectType.DATACENTER;
import static com.vmware.vim25.ManagedObjectType.HOST_SYSTEM;
import static java.util.Objects.requireNonNullElse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ConfigTarget;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.DistributedVirtualPortgroupInfo;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.FileFaultFaultMsg;
import com.vmware.vim25.InsufficientResourcesFaultFaultMsg;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.OpaqueNetworkSummary;
import com.vmware.vim25.OpaqueNetworkTargetInfo;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TaskInProgressFaultMsg;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualCdrom;
import com.vmware.vim25.VirtualCdromIsoBackingInfo;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceBackingInfo;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecFileOperation;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDeviceConnectInfo;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardDistributedVirtualPortBackingInfo;
import com.vmware.vim25.VirtualEthernetCardNetworkBackingInfo;
import com.vmware.vim25.VirtualEthernetCardOpaqueNetworkBackingInfo;
import com.vmware.vim25.VirtualFloppy;
import com.vmware.vim25.VirtualFloppyDeviceBackingInfo;
import com.vmware.vim25.VirtualIDEController;
import com.vmware.vim25.VirtualLsiLogicController;
import com.vmware.vim25.VirtualMachineConfigOption;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineDatastoreInfo;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualMachineNetworkInfo;
import com.vmware.vim25.VirtualSCSISharing;
import com.vmware.vim25.VirtualVmxnet3;
import com.vmware.vim25.VmConfigFaultFaultMsg;

/** This sample creates a VM */
public class VMCreate {
    private static final Logger log = LoggerFactory.getLogger(VMCreate.class);
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
    /** OPTIONAL: Name of dataStore. Default value "DatastoreName" */
    public static String datastoreName = null;
    /** REQUIRED: Name of the virtual machine. */
    public static String virtualMachineName = "VmName";
    /** OPTIONAL: Size of the Memory in 1024MB blocks, default is 1024 */
    public static Long memorySize = null;
    /** OPTIONAL: Total cpu count, default is 1 */
    public static Integer cpuCount = null;
    /** REQUIRED: Name of the datacenter */
    public static String dataCenterName = "DataCenterName";
    /** OPTIONAL: Size of the disk in megabytes, default is 1 */
    public static Integer diskSize = null;
    /** REQUIRED: Name of the host */
    public static String hostName = "Hostname";
    /** OPTIONAL: Type of Guest OS, default is windows7Guest */
    public static String guestOsId = null;
    /** REQUIRED: Network Name (Types can be Standard/Distributed/Opaque) */
    public static String networkName = "NetworkName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VMCreate.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference datacenterMoRef = propertyCollectorHelper.getMoRefByName(dataCenterName, DATACENTER);
            if (datacenterMoRef == null) {
                log.error("Datacenter {} not found.", dataCenterName);
                return;
            }

            ManagedObjectReference hostMoRef =
                    propertyCollectorHelper.getMoRefByName(datacenterMoRef, hostName, HOST_SYSTEM);
            if (hostMoRef == null) {
                log.error("Host {} not found", hostName);
                return;
            }

            ManagedObjectReference computeResourceMoRef = propertyCollectorHelper.fetch(hostMoRef, "parent");
            if (computeResourceMoRef == null) {
                log.error("No Compute Resource Found On Specified Host");
                return;
            }

            ManagedObjectReference resourcePoolMoRef =
                    propertyCollectorHelper.fetch(computeResourceMoRef, "resourcePool");

            ManagedObjectReference vmFolderMoRef = propertyCollectorHelper.fetch(datacenterMoRef, "vmFolder");

            VirtualMachineConfigSpec vmConfigSpec = createVmConfigSpec(
                    vimPort,
                    propertyCollectorHelper,
                    requireNonNullElse(datastoreName, "DatastoreName"),
                    requireNonNullElse(diskSize, 1),
                    hostMoRef,
                    computeResourceMoRef,
                    networkName);

            vmConfigSpec.setName(virtualMachineName);
            vmConfigSpec.setAnnotation("VirtualMachine Annotation");
            vmConfigSpec.setMemoryMB(requireNonNullElse(memorySize, 1024L));
            vmConfigSpec.setNumCPUs(requireNonNullElse(cpuCount, 1));
            vmConfigSpec.setGuestId(requireNonNullElse(guestOsId, "windows7Guest"));

            ManagedObjectReference taskMoRef =
                    vimPort.createVMTask(vmFolderMoRef, vmConfigSpec, resourcePoolMoRef, hostMoRef);
            if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                log.info("Success: Creating VM - [ {} ] \n", virtualMachineName);
            } else {
                String msg = "Failure: Creating [ " + virtualMachineName + "] VM";
                throw new RuntimeException(msg);
            }

            ManagedObjectReference vmMoRef = propertyCollectorHelper.fetch(taskMoRef, "info.result");
            log.info("Powering on the newly created VM {}", virtualMachineName);

            // Start the Newly Created VM.
            powerOnVM(vimPort, propertyCollectorHelper, vmMoRef);
        }
    }

    /** Return the list of any available networks on ESX which will host the VM. */
    private static HashMap<String, VirtualDeviceBackingInfo> getAvailableHostNetworkDevice(
            PropertyCollectorHelper propertyCollectorHelper, ConfigTarget configTarget)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        HashMap<String, VirtualDeviceBackingInfo> deviceBackingMap = new HashMap<>();

        for (VirtualMachineNetworkInfo info : configTarget.getNetwork()) {
            VirtualEthernetCardNetworkBackingInfo networkDeviceBacking = new VirtualEthernetCardNetworkBackingInfo();
            networkDeviceBacking.setDeviceName(info.getNetwork().getName());

            deviceBackingMap.put(info.getNetwork().getName(), networkDeviceBacking);
        }

        for (DistributedVirtualPortgroupInfo info : configTarget.getDistributedVirtualPortgroup()) {
            VirtualEthernetCardDistributedVirtualPortBackingInfo dvsDeviceBacking =
                    new VirtualEthernetCardDistributedVirtualPortBackingInfo();

            dvsDeviceBacking.setPort(new DistributedVirtualSwitchPortConnection());
            dvsDeviceBacking.getPort().setPortgroupKey(info.getPortgroupKey());
            dvsDeviceBacking.getPort().setSwitchUuid(info.getSwitchUuid());

            deviceBackingMap.put(info.getPortgroupName(), dvsDeviceBacking);
        }

        for (OpaqueNetworkTargetInfo info : configTarget.getOpaqueNetwork()) {
            OpaqueNetworkSummary opaqueNetworkSummary =
                    propertyCollectorHelper.fetch(info.getNetwork().getNetwork(), "summary");

            VirtualEthernetCardOpaqueNetworkBackingInfo opaqueNetworkDeviceBacking =
                    new VirtualEthernetCardOpaqueNetworkBackingInfo();
            opaqueNetworkDeviceBacking.setOpaqueNetworkId(opaqueNetworkSummary.getOpaqueNetworkId());
            opaqueNetworkDeviceBacking.setOpaqueNetworkType(opaqueNetworkSummary.getOpaqueNetworkType());

            deviceBackingMap.put(info.getNetwork().getName(), opaqueNetworkDeviceBacking);
        }
        return deviceBackingMap;
    }

    /**
     * Creates the vm config spec object.
     *
     * @param datastoreName the datastore name
     * @param diskSizeMB the disk size in mb
     * @param computeResourceMoRef the {@link ManagedObjectReference} of the compute resource
     * @param hostMoRef the {@link ManagedObjectReference} of the host
     * @return the virtual machine config spec object
     */
    private static VirtualMachineConfigSpec createVmConfigSpec(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            String datastoreName,
            int diskSizeMB,
            ManagedObjectReference hostMoRef,
            ManagedObjectReference computeResourceMoRef,
            String networkName)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        // Find Networks
        ConfigTarget configTarget =
                getConfigTargetForHost(vimPort, propertyCollectorHelper, computeResourceMoRef, hostMoRef);

        List<VirtualDevice> defaultDevices =
                getDefaultDevices(vimPort, propertyCollectorHelper, computeResourceMoRef, hostMoRef);

        HashMap<String, VirtualDeviceBackingInfo> availableNetworks =
                getAvailableHostNetworkDevice(propertyCollectorHelper, configTarget);

        VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();

        VirtualDeviceConnectInfo deviceConnectInfo = new VirtualDeviceConnectInfo();
        deviceConnectInfo.setStartConnected(availableNetworks.containsKey(networkName));
        deviceConnectInfo.setConnected(availableNetworks.containsKey(networkName));

        if (availableNetworks.containsKey(networkName)) {
            nicSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

            VirtualEthernetCard nic = new VirtualVmxnet3();
            nic.setConnectable(deviceConnectInfo);
            nic.setAddressType("generated");
            nic.setBacking(availableNetworks.get(networkName));
            nic.setKey(-1);

            nicSpec.setDevice(nic);
        } else {
            throw new RuntimeException("Network " + networkName + " Not found");
        }

        ManagedObjectReference datastoreMoRef = null;
        if (datastoreName != null) {
            boolean found = false;
            for (int i = 0; i < configTarget.getDatastore().size(); i++) {
                VirtualMachineDatastoreInfo vmDsInfo =
                        configTarget.getDatastore().get(i);

                DatastoreSummary dsSummary = vmDsInfo.getDatastore();
                if (dsSummary.getName().equals(datastoreName)) {
                    found = true;
                    if (dsSummary.isAccessible()) {
                        datastoreMoRef = dsSummary.getDatastore();
                    } else {
                        throw new RuntimeException("Specified Datastore is not accessible");
                    }
                    break;
                }
            }
            if (!found) {
                throw new RuntimeException("Specified Datastore is not Found");
            }
        } else {
            boolean found = false;
            for (int i = 0; i < configTarget.getDatastore().size(); i++) {
                VirtualMachineDatastoreInfo vmDsInfo =
                        configTarget.getDatastore().get(i);

                DatastoreSummary dsSummary = vmDsInfo.getDatastore();
                if (dsSummary.isAccessible()) {
                    datastoreName = dsSummary.getName();
                    datastoreMoRef = dsSummary.getDatastore();
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new RuntimeException("No Datastore found on host");
            }
        }

        String datastoreVolume = getVolumeName(datastoreName);

        VirtualMachineFileInfo virtualMachineFileInfo = new VirtualMachineFileInfo();
        virtualMachineFileInfo.setVmPathName(datastoreVolume);

        VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
        configSpec.setFiles(virtualMachineFileInfo);

        // Add a scsi controller
        int diskCtlrKey = 1;
        VirtualDeviceConfigSpec scsiCtrlSpec = new VirtualDeviceConfigSpec();
        scsiCtrlSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
        VirtualLsiLogicController scsiCtrl = new VirtualLsiLogicController();
        scsiCtrl.setBusNumber(0);
        scsiCtrlSpec.setDevice(scsiCtrl);
        scsiCtrl.setKey(diskCtlrKey);
        scsiCtrl.setSharedBus(VirtualSCSISharing.NO_SHARING);

        // Find the IDE controller
        VirtualDevice ideCtlr = null;
        for (VirtualDevice defaultDevice : defaultDevices) {
            if (defaultDevice instanceof VirtualIDEController) {
                ideCtlr = defaultDevice;
                break;
            }
        }

        // Add a floppy
        VirtualDeviceConfigSpec floppySpec = new VirtualDeviceConfigSpec();
        floppySpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

        VirtualFloppy floppy = new VirtualFloppy();

        VirtualFloppyDeviceBackingInfo flpBacking = new VirtualFloppyDeviceBackingInfo();
        flpBacking.setDeviceName("/dev/fd0");

        floppy.setBacking(flpBacking);
        floppy.setKey(3);
        floppySpec.setDevice(floppy);

        // Add a cdrom based on a physical device
        VirtualDeviceConfigSpec cdSpec = null;

        if (ideCtlr != null) {
            cdSpec = new VirtualDeviceConfigSpec();
            cdSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

            VirtualCdrom cdrom = new VirtualCdrom();

            VirtualCdromIsoBackingInfo cdDeviceBacking = new VirtualCdromIsoBackingInfo();
            cdDeviceBacking.setDatastore(datastoreMoRef);
            cdDeviceBacking.setFileName(datastoreVolume + "testcd.iso");

            cdrom.setBacking(cdDeviceBacking);
            cdrom.setKey(20);
            cdrom.setControllerKey(ideCtlr.getKey());
            cdrom.setUnitNumber(0);

            cdSpec.setDevice(cdrom);
        }

        // Create a new disk - file based - for the vm
        VirtualDeviceConfigSpec diskSpec = null;
        diskSpec = createVirtualDisk(datastoreName, diskCtlrKey, diskSizeMB);

        List<VirtualDeviceConfigSpec> deviceConfigSpec = new ArrayList<>();
        deviceConfigSpec.add(scsiCtrlSpec);
        deviceConfigSpec.add(floppySpec);
        deviceConfigSpec.add(diskSpec);

        if (ideCtlr != null) {
            deviceConfigSpec.add(cdSpec);
            deviceConfigSpec.add(nicSpec);
        } else {
            deviceConfigSpec = new ArrayList<>();
            deviceConfigSpec.add(nicSpec);
        }

        configSpec.getDeviceChange().addAll(deviceConfigSpec);

        return configSpec;
    }

    /**
     * This method returns the {@link ConfigTarget} for a HostSystem.
     *
     * @param computeResourceMoRef A {@link ManagedObjectReference} to the ComputeResource used by the HostSystem
     * @param hostMoRef A {@link ManagedObjectReference} to the HostSystem
     * @return Instance of {@link ConfigTarget} for the supplied HostSystem/ComputeResource
     */
    private static ConfigTarget getConfigTargetForHost(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            ManagedObjectReference computeResourceMoRef,
            ManagedObjectReference hostMoRef)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        ManagedObjectReference envBrowserMoRef =
                propertyCollectorHelper.fetch(computeResourceMoRef, "environmentBrowser");

        ConfigTarget configTarget = vimPort.queryConfigTarget(envBrowserMoRef, hostMoRef);
        if (configTarget == null) {
            throw new RuntimeException("No ConfigTarget found in ComputeResource");
        }
        return configTarget;
    }

    /**
     * The method returns the default devices from the HostSystem.
     *
     * @param computeResourceMoRef A {@link ManagedObjectReference} to the ComputeResource used by the HostSystem
     * @param hostMoRef A {@link ManagedObjectReference} to the HostSystem
     * @return List of {@link VirtualDevice} containing the default devices for the HostSystem
     */
    private static List<VirtualDevice> getDefaultDevices(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            ManagedObjectReference computeResourceMoRef,
            ManagedObjectReference hostMoRef)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        ManagedObjectReference envBrowserMoRef =
                propertyCollectorHelper.fetch(computeResourceMoRef, "environmentBrowser");

        VirtualMachineConfigOption vmConfigOption = vimPort.queryConfigOption(envBrowserMoRef, null, hostMoRef);

        List<VirtualDevice> defaultDevices = null;
        if (vmConfigOption == null) {
            throw new RuntimeException("No VirtualHardwareInfo found in ComputeResource");
        } else {
            List<VirtualDevice> virtualDevices = vmConfigOption.getDefaultDevice();
            if (virtualDevices == null) {
                throw new RuntimeException("No Datastore found in ComputeResource");
            } else {
                defaultDevices = virtualDevices;
            }
        }
        return defaultDevices;
    }

    private static String getVolumeName(String volName) {
        String volumeName = null;
        if (volName != null && !volName.isEmpty()) {
            volumeName = "[" + volName + "]";
        } else {
            volumeName = "[Local]";
        }

        return volumeName;
    }

    /**
     * Creates the virtual disk.
     *
     * @param volName the volume name
     * @param diskCtlrKey the disk controller key
     * @param diskSizeMB the disk size in megabytes
     * @return the virtual device config spec object
     */
    private static VirtualDeviceConfigSpec createVirtualDisk(String volName, int diskCtlrKey, int diskSizeMB) {
        String volumeName = getVolumeName(volName);
        VirtualDeviceConfigSpec diskSpec = new VirtualDeviceConfigSpec();

        diskSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.CREATE);
        diskSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

        VirtualDisk disk = new VirtualDisk();
        VirtualDiskFlatVer2BackingInfo diskFileBacking = new VirtualDiskFlatVer2BackingInfo();

        diskFileBacking.setFileName(volumeName);
        diskFileBacking.setDiskMode("persistent");

        disk.setKey(0);
        disk.setControllerKey(diskCtlrKey);
        disk.setUnitNumber(0);
        disk.setBacking(diskFileBacking);
        disk.setCapacityInKB(1024L * diskSizeMB);

        diskSpec.setDevice(disk);

        return diskSpec;
    }

    private static void powerOnVM(
            VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference vmMoRef)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg,
                    TaskInProgressFaultMsg, VmConfigFaultFaultMsg, InsufficientResourcesFaultFaultMsg,
                    FileFaultFaultMsg, InvalidStateFaultMsg {
        ManagedObjectReference powerOnVMTask = vimPort.powerOnVMTask(vmMoRef, null);

        if (propertyCollectorHelper.awaitTaskCompletion(powerOnVMTask)) {
            log.info("Success: VM started Successfully");
        } else {
            String msg = "Failure: starting [ " + vmMoRef.getValue() + "] VM";
            throw new RuntimeException(msg);
        }
    }
}
