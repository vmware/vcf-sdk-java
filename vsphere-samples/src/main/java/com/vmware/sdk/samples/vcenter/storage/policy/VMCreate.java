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
import static com.vmware.vim25.ManagedObjectType.DATACENTER;
import static com.vmware.vim25.ManagedObjectType.HOST_SYSTEM;
import static java.util.Objects.requireNonNullElse;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.pbm.InvalidArgumentFaultMsg;
import com.vmware.pbm.PbmCapabilityProfile;
import com.vmware.pbm.PbmPortType;
import com.vmware.pbm.PbmProfile;
import com.vmware.pbm.PbmProfileId;
import com.vmware.pbm.PbmServiceInstanceContent;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.pbm.PbmUtil;
import com.vmware.vim25.ConfigTarget;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.FileFaultFaultMsg;
import com.vmware.vim25.InsufficientResourcesFaultFaultMsg;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NetworkSummary;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TaskInProgressFaultMsg;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualCdrom;
import com.vmware.vim25.VirtualCdromIsoBackingInfo;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecFileOperation;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardNetworkBackingInfo;
import com.vmware.vim25.VirtualFloppy;
import com.vmware.vim25.VirtualFloppyDeviceBackingInfo;
import com.vmware.vim25.VirtualIDEController;
import com.vmware.vim25.VirtualLsiLogicController;
import com.vmware.vim25.VirtualMachineConfigOption;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineDatastoreInfo;
import com.vmware.vim25.VirtualMachineDefinedProfileSpec;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualMachineNetworkInfo;
import com.vmware.vim25.VirtualPCNet32;
import com.vmware.vim25.VirtualSCSISharing;
import com.vmware.vim25.VmConfigFaultFaultMsg;

/**
 * This sample creates a VM with a storage policy based management profile. The VM uses the datastores configured with
 * the host.
 */
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

    /** OPTIONAL: Name of dataStore. */
    public static String dataStore = null;
    /** REQUIRED: Name of the virtual machine. */
    public static String vmName = "vmName";
    /** OPTIONAL: Size of the Memory in 1024MB blocks. Default value is 1024. */
    public static Long memorySize = null;
    /** OPTIONAL: Total cpu count. Default value is 1. */
    public static Integer cpuCount = null;
    /** REQUIRED: Name of the datacenter. */
    public static String dataCenterName = "datacenterName";
    /** OPTIONAL: Size of the Disk. Default value 1. */
    public static Integer diskSize = null;
    /** REQUIRED: Name of the host. */
    public static String hostname = "hostname";
    /** OPTIONAL: Type of Guest OS. Default value "windows7Guest". */
    public static String guestOsId = null;
    /** REQUIRED: Name of the storage profile. */
    public static String profileName = "profileName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VMCreate.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent vimServiceContent = client.getVimServiceContent();

            PbmPortType pbmPort = client.getPbmPort();
            PbmServiceInstanceContent serviceInstanceContent = client.getPbmServiceInstanceContent();

            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, vimServiceContent);

            ManagedObjectReference dcMoRef = propertyCollectorHelper.getMoRefByName(
                    vimServiceContent.getRootFolder(), dataCenterName, DATACENTER);

            if (dcMoRef == null) {
                log.error("Datacenter {} not found.", dataCenterName);
                return;
            }

            ManagedObjectReference hostMoRef = propertyCollectorHelper.getMoRefByName(dcMoRef, hostname, HOST_SYSTEM);
            if (hostMoRef == null) {
                log.error("Host {} not found", hostname);
                return;
            }

            ManagedObjectReference computeResourceMoRef = propertyCollectorHelper.fetch(hostMoRef, "parent");
            if (computeResourceMoRef == null) {
                log.error("No Compute Resource Found On Specified Host");
                return;
            }

            ManagedObjectReference resourcePoolMoRef =
                    propertyCollectorHelper.fetch(computeResourceMoRef, "resourcePool");
            ManagedObjectReference vmFolderMoRef = propertyCollectorHelper.fetch(dcMoRef, "vmFolder");

            // VM Config Spec with a Storage Profile
            VirtualMachineConfigSpec vmConfigSpec = createVmConfigSpec(
                    vimPort,
                    propertyCollectorHelper,
                    vmName,
                    dataStore,
                    requireNonNullElse(diskSize, 1),
                    computeResourceMoRef,
                    hostMoRef,
                    getPbmProfileSpec(pbmPort, serviceInstanceContent, profileName));

            vmConfigSpec.setName(vmName);
            vmConfigSpec.setAnnotation("VirtualMachine Annotation");
            vmConfigSpec.setMemoryMB(requireNonNullElse(memorySize, 1024L));
            vmConfigSpec.setNumCPUs(requireNonNullElse(cpuCount, 1));
            vmConfigSpec.setGuestId(requireNonNullElse(guestOsId, "windows7Guest"));

            ManagedObjectReference taskMoRef =
                    vimPort.createVMTask(vmFolderMoRef, vmConfigSpec, resourcePoolMoRef, hostMoRef);
            if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                log.info("Success: Creating VM - [ {} ] \n", vmName);
            } else {
                String msg = "Failure: Creating [ " + vmName + "] VM";
                throw new RuntimeException(msg);
            }

            ManagedObjectReference vmMoRef = propertyCollectorHelper.fetch(taskMoRef, "info.result");

            log.info("Powering on the newly created VM {}", vmName);
            // Start the Newly Created VM.
            powerOnVM(vimPort, propertyCollectorHelper, vmMoRef);
        }
    }

    /**
     * Creates the virtual disk.
     *
     * @param volName the volume name
     * @param diskCtlrKey the disk controller key
     * @return the virtual device config spec object
     */
    private static VirtualDeviceConfigSpec createVirtualDisk(String volName, int diskCtlrKey) {
        String volumeName = getVolumeName(volName);
        VirtualDeviceConfigSpec diskSpec = new VirtualDeviceConfigSpec();

        diskSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.CREATE);
        diskSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

        VirtualDisk disk = new VirtualDisk();
        VirtualDiskFlatVer2BackingInfo diskfileBacking = new VirtualDiskFlatVer2BackingInfo();

        diskfileBacking.setFileName(volumeName);
        diskfileBacking.setDiskMode("persistent");

        disk.setKey(0);
        disk.setControllerKey(diskCtlrKey);
        disk.setUnitNumber(0);
        disk.setBacking(diskfileBacking);
        disk.setCapacityInKB(1024);

        diskSpec.setDevice(disk);

        return diskSpec;
    }

    /**
     * Creates the vm config spec object.
     *
     * @param vmName the vm name
     * @param datastoreName the datastore name
     * @param diskSizeMB the disk size in mb
     * @param computeResMoRef the {@link ManagedObjectReference} of the compute resource
     * @param hostMoRef the {@link ManagedObjectReference} of the host
     * @return the virtual machine config spec object
     */
    private static VirtualMachineConfigSpec createVmConfigSpec(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            String vmName,
            String datastoreName,
            int diskSizeMB,
            ManagedObjectReference computeResMoRef,
            ManagedObjectReference hostMoRef,
            VirtualMachineDefinedProfileSpec spbmProfile)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {

        ConfigTarget configTarget =
                getConfigTargetForHost(vimPort, propertyCollectorHelper, computeResMoRef, hostMoRef);
        List<VirtualDevice> defaultDevices =
                getDefaultDevices(vimPort, propertyCollectorHelper, computeResMoRef, hostMoRef);

        VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
        // Set SPBM profile
        configSpec.getVmProfile().add(spbmProfile);

        String networkName = null;
        if (configTarget.getNetwork() != null) {
            for (int i = 0; i < configTarget.getNetwork().size(); i++) {
                VirtualMachineNetworkInfo netInfo = configTarget.getNetwork().get(i);
                NetworkSummary netSummary = netInfo.getNetwork();
                if (netSummary.isAccessible()) {
                    networkName = netSummary.getName();
                    break;
                }
            }
        }

        ManagedObjectReference datastoreRef = null;
        if (datastoreName != null) {
            boolean flag = false;
            for (int i = 0; i < configTarget.getDatastore().size(); i++) {
                VirtualMachineDatastoreInfo vdsInfo =
                        configTarget.getDatastore().get(i);
                DatastoreSummary dsSummary = vdsInfo.getDatastore();
                if (dsSummary.getName().equals(datastoreName)) {
                    flag = true;
                    if (dsSummary.isAccessible()) {
                        datastoreRef = dsSummary.getDatastore();
                    } else {
                        throw new RuntimeException("Specified Datastore is not accessible");
                    }
                    break;
                }
            }
            if (!flag) {
                throw new RuntimeException("Specified Datastore is not Found");
            }
        } else {
            boolean flag = false;
            for (int i = 0; i < configTarget.getDatastore().size(); i++) {
                VirtualMachineDatastoreInfo vdsInfo =
                        configTarget.getDatastore().get(i);
                DatastoreSummary dsSummary = vdsInfo.getDatastore();
                if (dsSummary.isAccessible()) {
                    datastoreName = dsSummary.getName();
                    datastoreRef = dsSummary.getDatastore();
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                throw new RuntimeException("No Datastore found on host");
            }
        }

        String datastoreVolume = getVolumeName(datastoreName);

        VirtualMachineFileInfo vmfi = new VirtualMachineFileInfo();
        vmfi.setVmPathName(datastoreVolume);
        configSpec.setFiles(vmfi);

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
            cdDeviceBacking.setDatastore(datastoreRef);
            cdDeviceBacking.setFileName(datastoreVolume + "testcd.iso");
            cdrom.setBacking(cdDeviceBacking);
            cdrom.setKey(20);
            cdrom.setControllerKey(ideCtlr.getKey());
            cdrom.setUnitNumber(0);
            cdSpec.setDevice(cdrom);
        }

        // Create a new disk - file based - for the vm
        VirtualDeviceConfigSpec diskSpec = null;
        diskSpec = createVirtualDisk(datastoreName, diskCtlrKey);
        diskSpec.getProfile().add(spbmProfile);

        // Add a NIC. the network Name must be set as the device name to create the NIC.
        VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();
        if (networkName != null) {
            nicSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
            VirtualEthernetCard nic = new VirtualPCNet32();
            VirtualEthernetCardNetworkBackingInfo nicBacking = new VirtualEthernetCardNetworkBackingInfo();
            nicBacking.setDeviceName(networkName);
            nic.setAddressType("generated");
            nic.setBacking(nicBacking);
            nic.setKey(4);
            nicSpec.setDevice(nic);
        }

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
     * This method returns the ConfigTarget for a HostSystem.
     *
     * @param computeResMoRef a {@link ManagedObjectReference} to the ComputeResource used by the HostSystem
     * @param hostMoRef A {@link ManagedObjectReference} to the HostSystem
     * @return Instance of {@link ConfigTarget} for the supplied HostSystem/ComputeResource
     */
    private static ConfigTarget getConfigTargetForHost(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            ManagedObjectReference computeResMoRef,
            ManagedObjectReference hostMoRef)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {

        ManagedObjectReference envBrowserMoRef = propertyCollectorHelper.fetch(computeResMoRef, "environmentBrowser");

        ConfigTarget configTarget = vimPort.queryConfigTarget(envBrowserMoRef, hostMoRef);
        if (configTarget == null) {
            throw new RuntimeException("No ConfigTarget found in ComputeResource");
        }
        return configTarget;
    }

    /**
     * The method returns the default devices from the HostSystem.
     *
     * @param computeResMoRef a {@link ManagedObjectReference} to the ComputeResource used by the HostSystem
     * @param hostMoRef a {@link ManagedObjectReference} to the HostSystem
     * @return List of {@link VirtualDevice} containing the default devices for the HostSystem
     */
    private static List<VirtualDevice> getDefaultDevices(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            ManagedObjectReference computeResMoRef,
            ManagedObjectReference hostMoRef)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        ManagedObjectReference envBrowserMoRef = propertyCollectorHelper.fetch(computeResMoRef, "environmentBrowser");

        VirtualMachineConfigOption vmConfigOpt = vimPort.queryConfigOption(envBrowserMoRef, null, hostMoRef);
        List<VirtualDevice> defaultDevs = null;
        if (vmConfigOpt == null) {
            throw new RuntimeException("No VirtualHardwareInfo found in ComputeResource");
        } else {
            List<VirtualDevice> virtualDevices = vmConfigOpt.getDefaultDevice();
            if (virtualDevices == null) {
                throw new RuntimeException("No Datastore found in ComputeResource");
            } else {
                defaultDevs = virtualDevices;
            }
        }
        return defaultDevs;
    }

    /** This method returns the Profile Spec for the given Storage Profile name. */
    private static VirtualMachineDefinedProfileSpec getPbmProfileSpec(
            PbmPortType pbmPort, PbmServiceInstanceContent serviceInstanceContent, String name)
            throws InvalidArgumentFaultMsg, com.vmware.pbm.RuntimeFaultFaultMsg, RuntimeFaultFaultMsg {

        // Get PBM Profile Manager
        ManagedObjectReference profileMgrMoRef = serviceInstanceContent.getProfileManager();

        // Search for the given Profile Name
        List<PbmProfileId> profileIds =
                pbmPort.pbmQueryProfile(profileMgrMoRef, PbmUtil.getStorageResourceType(), null);
        if (profileIds == null || profileIds.isEmpty()) {
            throw new RuntimeFaultFaultMsg("No storage Profiles exist.");
        }

        List<PbmProfile> pbmProfiles = pbmPort.pbmRetrieveContent(profileMgrMoRef, profileIds);
        for (PbmProfile pbmProfile : pbmProfiles) {
            if (pbmProfile.getName().equals(name)) {
                PbmCapabilityProfile profile = (PbmCapabilityProfile) pbmProfile;
                VirtualMachineDefinedProfileSpec spbmProfile = new VirtualMachineDefinedProfileSpec();
                spbmProfile.setProfileId(profile.getProfileId().getUniqueId());

                return spbmProfile;
            }
        }

        // Throw exception if none found
        throw new InvalidArgumentFaultMsg("Specified storage profile name does not exist.");
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

    private static void powerOnVM(
            VimPortType vimPortType, PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference vmMoRef)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg,
                    TaskInProgressFaultMsg, VmConfigFaultFaultMsg, InsufficientResourcesFaultFaultMsg,
                    FileFaultFaultMsg, InvalidStateFaultMsg {
        ManagedObjectReference powerOnTaskMoRef = vimPortType.powerOnVMTask(vmMoRef, null);
        if (propertyCollectorHelper.awaitTaskCompletion(powerOnTaskMoRef)) {
            log.info("Success: VM started Successfully");
        } else {
            String msg = "Failure: starting [ " + vmMoRef.getValue() + "] VM";
            throw new RuntimeException(msg);
        }
    }
}
