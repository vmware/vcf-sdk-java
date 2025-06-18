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
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayOfVirtualDevice;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecFileOperation;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDeviceConnectInfo;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskCompatibilityMode;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualDiskMode;
import com.vmware.vim25.VirtualDiskRawDiskMappingVer1BackingInfo;
import com.vmware.vim25.VirtualMachineConfigOption;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualSCSIController;

/** This sample demonstrates how to create a virtual disk. */
public class VMDiskCreate {
    private static final Logger log = LoggerFactory.getLogger(VMDiskCreate.class);
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
    /** REQUIRED: Size of the virtual disk in MB. */
    public static int diskSize = 0;
    /** OPTIONAL: Virtual Disk Type (thin | preallocated | eagerzeroed | rdm | rdmp). */
    public static String diskType = null;
    /**
     * OPTIONAL: Persistence mode of the virtual disk (persistent | independent_persistent | independent_nonpersistent).
     */
    public static String persistence = null;
    /** OPTIONAL: Canonical name of the LUN to use for disk types, e.g (vmhba0:0:0:0). */
    public static String deviceName = null;

    private static final Map<String, DISKTYPE> disktypehm = new HashMap<>();

    /** Disk Types allowed. */
    private enum DISKTYPE {
        THIN,
        THICK,
        PRE_ALLOCATED,
        RDM,
        RDMP,
        EAGERZEROED
    }

    private enum CONTROLLERTYPE {
        /**
         * Default device count max for SCSI controller is 16, with unit# 7 being the reserver slot. Similarly, for IDE
         * controller the count is 2 a.k.a. primary and secondary. These are mentioned here just to keep the logic of
         * this sample simple and should NOT be used in production. Use {@link VirtualMachineConfigOption} to retrieve
         * these at the runtime instead in production.
         */
        SCSI(16, 7),
        IDE(2);

        private final int maxdevice;
        private int reserveSlot = -1;

        CONTROLLERTYPE(int maxdevice) {
            this.maxdevice = maxdevice;
        }

        CONTROLLERTYPE(int maxdevice, int reserveSlot) {
            this.maxdevice = maxdevice;
            this.reserveSlot = reserveSlot;
        }

        public int getMaxDevice() {
            return this.maxdevice;
        }

        public int getReserveSlot() {
            return this.reserveSlot;
        }
    }

    private static class HardDiskBean {
        private DISKTYPE diskType;
        private String deviceName;
        private int disksize;

        public HardDiskBean() {}

        public void setDiskSize(int key) {
            this.disksize = key;
        }

        public int getDiskSize() {
            return this.disksize;
        }

        public void setDiskType(DISKTYPE dsktype) {
            this.diskType = dsktype;
        }

        public DISKTYPE getDiskType() {
            return this.diskType;
        }

        public void setDeviceName(String dvcname) {
            this.deviceName = dvcname;
        }

        public String getDeviceName() {
            return this.deviceName;
        }
    }

    private static final HardDiskBean hDiskBean = new HardDiskBean();

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VMDiskCreate.class, args);

        disktypehm.put("thin", DISKTYPE.THIN);
        disktypehm.put("thick", DISKTYPE.THICK);
        disktypehm.put("pre-allocated", DISKTYPE.PRE_ALLOCATED);
        disktypehm.put("rdm", DISKTYPE.RDM);
        disktypehm.put("rdmp", DISKTYPE.RDMP);
        disktypehm.put("eagerzeroed", DISKTYPE.EAGERZEROED);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference vmMoRef =
                    propertyCollectorHelper.getMoRefByName(virtualMachineName, VIRTUAL_MACHINE);

            if (vmMoRef == null) {
                log.error("Virtual Machine [ {} ] not found", virtualMachineName);
                return;
            }

            // Start Setting the Required Objects, to configure a hard newvirtualdisk to this Virtual machine.
            VirtualMachineConfigSpec vmConfigSpecReconfig = new VirtualMachineConfigSpec();

            // Initialize the Hard Disk bean.
            setDiskInformation();

            VirtualDeviceConfigSpec diskSpecification = virtualDiskOp(propertyCollectorHelper, vmMoRef, hDiskBean);

            List<VirtualDeviceConfigSpec> deviceConfigSpecs = new ArrayList<>();
            deviceConfigSpecs.add(diskSpecification);

            vmConfigSpecReconfig.getDeviceChange().addAll(deviceConfigSpecs);

            log.info("Reconfiguring the Virtual Machine - [ {} ]", virtualMachineName);
            ManagedObjectReference task = vimPort.reconfigVMTask(vmMoRef, vmConfigSpecReconfig);

            if (propertyCollectorHelper.awaitTaskCompletion(task)) {
                log.info("Reconfiguring the Virtual Machine - [ {} ] Successful", virtualMachineName);
            } else {
                log.error("Reconfiguring the Virtual Machine - [ {} ] Failed", virtualMachineName);
            }
        }
    }

    private static void setDiskInformation() throws IllegalArgumentException {
        DISKTYPE vmDiskType = null;
        // Set the Disk Type
        if (diskType == null || diskType.trim().isEmpty()) {
            log.info("Disktype is not specified Assuming disktype [thin]");
            vmDiskType = DISKTYPE.THIN;
        } else {
            vmDiskType = disktypehm.get(diskType.trim().toLowerCase());
            if (vmDiskType == null) {
                log.error("Invalid value for option disktype. Possible values are : {}", disktypehm.keySet());
                throw new IllegalArgumentException("The DISK Type " + diskType + " is Invalid");
            }
        }
        hDiskBean.setDiskType(vmDiskType);
        hDiskBean.setDiskSize(diskSize);

        // Set the Size of the newvirtualdisk
        hDiskBean.setDiskSize((diskSize <= 0) ? 1 : diskSize);

        // Set the device name for this newvirtualdisk
        if (deviceName == null || deviceName.trim().isEmpty()) {
            if ((vmDiskType == DISKTYPE.RDM) || (vmDiskType == DISKTYPE.RDMP)) {
                throw new IllegalArgumentException(
                        "The devicename is mandatory for specified disktype [ " + vmDiskType + " ]");
            }
        } else {
            hDiskBean.setDeviceName(deviceName);
        }
    }

    /**
     * @param vmMoRef {@link ManagedObjectReference} of the VM on which the operation is carried out
     * @param hardDiskBean {@link HardDiskBean}
     * @return {@link VirtualDeviceConfigSpec} spec for the device change
     */
    private static VirtualDeviceConfigSpec virtualDiskOp(
            PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference vmMoRef, HardDiskBean hardDiskBean)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        String deviceName = hardDiskBean.getDeviceName();
        DISKTYPE diskType = hardDiskBean.getDiskType();
        int diskSizeMB = hardDiskBean.getDiskSize();

        VirtualDeviceConfigSpec deviceConfigSpec = null;
        List<Integer> getControllerKeyReturnArr = getControllerKey(propertyCollectorHelper, vmMoRef);

        if (!getControllerKeyReturnArr.isEmpty()) {
            Integer controllerKey = getControllerKeyReturnArr.get(0);
            Integer unitNumber = getControllerKeyReturnArr.get(1);

            deviceConfigSpec = createVirtualDiskConfigSpec(deviceName, controllerKey, unitNumber, diskType, diskSizeMB);
        } else {
            throw new RuntimeException("Failure Disk Create : SCSI Controller not found");
        }
        return deviceConfigSpec;
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

                int[] slots = new int[CONTROLLERTYPE.SCSI.getMaxDevice()];
                slots[CONTROLLERTYPE.SCSI.getReserveSlot()] = 1;

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

    /**
     * This method constructs a {@link VirtualDeviceConfigSpec} for a Virtual Disk.
     *
     * @param deviceName Name of the device, must be the absolute path like /vmfs/devices/disks/vmhba1:0:0:0
     * @param controllerKey index on the controller
     * @param unitNumber of the device on the controller
     * @param diskType one of thin, thick, rdm, rdmp
     * @param diskSizeMB size of the newvirtualdisk in MB.
     * @return VirtualDeviceConfigSpec used for adding / removing an RDM based virtual newvirtualdisk.
     */
    private static VirtualDeviceConfigSpec createVirtualDiskConfigSpec(
            String deviceName, int controllerKey, int unitNumber, DISKTYPE diskType, int diskSizeMB) {

        VirtualDeviceConnectInfo deviceConnectInfo = new VirtualDeviceConnectInfo();
        deviceConnectInfo.setStartConnected(true);
        deviceConnectInfo.setConnected(true);
        deviceConnectInfo.setAllowGuestControl(false);

        VirtualDisk newVirtualDisk = new VirtualDisk();
        newVirtualDisk.setControllerKey(controllerKey);
        newVirtualDisk.setUnitNumber(unitNumber);
        newVirtualDisk.setCapacityInKB(1024L * diskSizeMB);
        newVirtualDisk.setKey(-1);
        newVirtualDisk.setConnectable(deviceConnectInfo);

        VirtualDiskFlatVer2BackingInfo backingInfo = new VirtualDiskFlatVer2BackingInfo();
        VirtualDiskRawDiskMappingVer1BackingInfo rawDiskMappingVer1BackingInfo =
                new VirtualDiskRawDiskMappingVer1BackingInfo();

        switch (diskType) {
            case RDM:
                rawDiskMappingVer1BackingInfo.setCompatibilityMode(VirtualDiskCompatibilityMode.VIRTUAL_MODE.value());
                rawDiskMappingVer1BackingInfo.setDeviceName(deviceName);
                rawDiskMappingVer1BackingInfo.setDiskMode(persistence);
                rawDiskMappingVer1BackingInfo.setFileName("");

                newVirtualDisk.setBacking(rawDiskMappingVer1BackingInfo);
                break;
            case RDMP:
                rawDiskMappingVer1BackingInfo.setCompatibilityMode(VirtualDiskCompatibilityMode.PHYSICAL_MODE.value());
                rawDiskMappingVer1BackingInfo.setDeviceName(deviceName);
                rawDiskMappingVer1BackingInfo.setFileName("");

                newVirtualDisk.setBacking(rawDiskMappingVer1BackingInfo);
                break;
            case THICK:
                backingInfo.setDiskMode(VirtualDiskMode.INDEPENDENT_PERSISTENT.value());
                backingInfo.setThinProvisioned(Boolean.FALSE);
                backingInfo.setEagerlyScrub(Boolean.FALSE);
                backingInfo.setFileName("");

                newVirtualDisk.setBacking(backingInfo);
                break;
            case THIN:
                backingInfo.setDiskMode(Objects.requireNonNull(persistence, "persistent"));
                backingInfo.setThinProvisioned(Boolean.TRUE);
                backingInfo.setEagerlyScrub(Boolean.FALSE);
                backingInfo.setFileName("");

                newVirtualDisk.setBacking(backingInfo);
                break;
            case PRE_ALLOCATED:
                backingInfo.setDiskMode(persistence);
                backingInfo.setThinProvisioned(Boolean.FALSE);
                backingInfo.setEagerlyScrub(Boolean.FALSE);
                backingInfo.setFileName("");

                newVirtualDisk.setBacking(backingInfo);
                break;
            case EAGERZEROED:
                backingInfo.setDiskMode(persistence);
                backingInfo.setThinProvisioned(Boolean.FALSE);
                backingInfo.setEagerlyScrub(Boolean.TRUE);
                backingInfo.setFileName("");

                newVirtualDisk.setBacking(backingInfo);
                break;
            default:
                break;
        }

        VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();
        deviceConfigSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.CREATE);
        deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
        deviceConfigSpec.setDevice(newVirtualDisk);

        return deviceConfigSpec;
    }
}
