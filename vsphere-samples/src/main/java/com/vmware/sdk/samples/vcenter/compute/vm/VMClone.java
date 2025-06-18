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
import static com.vmware.vim25.ManagedObjectType.HOST_SYSTEM;

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayOfVirtualDevice;
import com.vmware.vim25.ConfigTarget;
import com.vmware.vim25.DistributedVirtualPortgroupInfo;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.OpaqueNetworkSummary;
import com.vmware.vim25.OpaqueNetworkTargetInfo;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceBackingInfo;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardDistributedVirtualPortBackingInfo;
import com.vmware.vim25.VirtualEthernetCardNetworkBackingInfo;
import com.vmware.vim25.VirtualEthernetCardOpaqueNetworkBackingInfo;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineCloneSpecTpmProvisionPolicy;
import com.vmware.vim25.VirtualMachineNetworkInfo;
import com.vmware.vim25.VirtualMachineRelocateSpec;

/**
 * This sample makes a template of an existing VM, change target network deploy multiple instances of this template onto
 * a datacenter
 */
public class VMClone {
    private static final Logger log = LoggerFactory.getLogger(VMClone.class);
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

    /** REQUIRED: Name of the datacenter */
    public static String dataCenterName = "DataCenterName";
    /** REQUIRED: Inventory path of the VM */
    public static String vmPath = "VMPathName";
    /** REQUIRED: name of the clone */
    public static String cloneName = "CloneName";
    /** OPTIONAL: Name of the target network */
    public static String targetNetworkName = null;
    /** REQUIRED: Name of the host */
    public static String hostName = "Hostname";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VMClone.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            // Find the Datacenter reference by using findByInventoryPath().
            ManagedObjectReference datacenterMoRef =
                    vimPort.findByInventoryPath(serviceContent.getSearchIndex(), dataCenterName);
            if (datacenterMoRef == null) {
                log.error("The specified datacenter [ {} ] is not found", dataCenterName);
                return;
            }

            // Find the virtual machine folder for this datacenter.
            ManagedObjectReference vmFolderMoRef = propertyCollectorHelper.fetch(datacenterMoRef, "vmFolder");
            if (vmFolderMoRef == null) {
                log.error("The virtual machine is not found");
                return;
            }

            ManagedObjectReference vmMoRef = vimPort.findByInventoryPath(serviceContent.getSearchIndex(), vmPath);
            if (vmMoRef == null) {
                log.error("The VMPath specified [ {} ] is not found \n", vmPath);
                return;
            }

            VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();
            if (targetNetworkName != null && hostName != null) {
                ManagedObjectReference hostMoRef =
                        propertyCollectorHelper.getMoRefByName(datacenterMoRef, hostName, HOST_SYSTEM);
                if (hostMoRef == null) {
                    log.error("Host {} not found", hostName);
                    return;
                }

                VirtualDeviceConfigSpec nicSpec;
                nicSpec = changeVmNicSpec(vimPort, propertyCollectorHelper, vmMoRef, targetNetworkName, hostMoRef);
                relocateSpec.getDeviceChange().add(nicSpec);
            }

            VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
            cloneSpec.setLocation(relocateSpec);
            cloneSpec.setPowerOn(false);
            cloneSpec.setTemplate(false);

            // If the source virtual machine has a virtual TPM device, specify the clone policy for the TPM secrets
            // here.
            cloneSpec.setTpmProvisionPolicy(VirtualMachineCloneSpecTpmProvisionPolicy.REPLACE.value());

            log.info(
                    "Cloning Virtual Machine [{}] to clone name [{}] \n",
                    vmPath.substring(vmPath.lastIndexOf("/") + 1),
                    cloneName);

            ManagedObjectReference cloneTask = vimPort.cloneVMTask(vmMoRef, vmFolderMoRef, cloneName, cloneSpec);

            if (propertyCollectorHelper.awaitTaskCompletion(cloneTask)) {
                log.info(
                        "Successfully cloned Virtual Machine [{}] to clone name [{}] \n",
                        vmPath.substring(vmPath.lastIndexOf("/") + 1),
                        cloneName);
            } else {
                log.error(
                        "Failure Cloning Virtual Machine [{}] to clone name [{}] \n",
                        vmPath.substring(vmPath.lastIndexOf("/") + 1),
                        cloneName);
            }
        }
    }

    /**
     * This method returns the ConfigTarget for a HostSystem.
     *
     * @param hostMoRef A {@link ManagedObjectReference} to the HostSystem
     * @return Instance of {@link ConfigTarget} for the supplied HostSystem/ComputeResource
     */
    private static ConfigTarget getConfigTargetForHost(
            VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference hostMoRef)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        ManagedObjectReference computeResourceMoRef = propertyCollectorHelper.fetch(hostMoRef, "parent");
        if (computeResourceMoRef == null) {
            throw new RuntimeException("No Compute Resource Found On Specified Host\"");
        }

        ManagedObjectReference envBrowseMoRef =
                propertyCollectorHelper.fetch(computeResourceMoRef, "environmentBrowser");

        ConfigTarget configTarget = vimPort.queryConfigTarget(envBrowseMoRef, hostMoRef);
        if (configTarget == null) {
            throw new RuntimeException("No ConfigTarget found in ComputeResource");
        }

        return configTarget;
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

    /** Config Specification to change network of a virtual machine. */
    private static VirtualDeviceConfigSpec changeVmNicSpec(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            ManagedObjectReference vmRef,
            String targetNetworkName,
            ManagedObjectReference hostRef)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        List<VirtualDevice> virtualDevices;
        virtualDevices = ((ArrayOfVirtualDevice) propertyCollectorHelper.fetch(vmRef, "config.hardware.device"))
                .getVirtualDevice();

        VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();

        ConfigTarget configTarget = getConfigTargetForHost(vimPort, propertyCollectorHelper, hostRef);

        HashMap<String, VirtualDeviceBackingInfo> availableNetworksMap =
                getAvailableHostNetworkDevice(propertyCollectorHelper, configTarget);

        if (availableNetworksMap.containsKey(targetNetworkName)) {
            for (VirtualDevice virtualDevice : virtualDevices) {
                if (virtualDevice.getDeviceInfo().getLabel().contains("Network adapter")) {
                    VirtualEthernetCard nic = (VirtualEthernetCard) virtualDevice;

                    nicSpec.setOperation(VirtualDeviceConfigSpecOperation.EDIT);
                    nic.setBacking(availableNetworksMap.get(targetNetworkName));
                    nicSpec.setDevice(nic);
                }
            }
        }
        return nicSpec;
    }
}
