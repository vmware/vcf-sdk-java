/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.storage.sdrs;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.STORAGE_POD;
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.soap.SOAPException;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayUpdateOperation;
import com.vmware.vim25.ClusterAntiAffinityRuleSpec;
import com.vmware.vim25.ClusterRuleInfo;
import com.vmware.vim25.ClusterRuleSpec;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PodStorageDrsEntry;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.StorageDrsConfigSpec;
import com.vmware.vim25.StorageDrsPodConfigSpec;
import com.vmware.vim25.StorageDrsVmConfigInfo;
import com.vmware.vim25.StorageDrsVmConfigSpec;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDiskAntiAffinityRuleSpec;
import com.vmware.vim25.VirtualMachineConfigInfo;

/**
 * This sample demonstrates how to Add/List/Modify/Delete the rules for an existing SDRS cluster.
 *
 * <p>Sample Requirements: All the virtual disks will be added while adding Vmdk AntiAffinity Rule. A minimum of 2 disks
 * should be present in the VM for this rule. The VMs should be present in a datastore which is part of the POD.
 */
public class SDRSRules {
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

    /**
     * REQUIRED: Valid options are "addVmAntiAffinity", "addVmdkAntiAffinity", "list", "modifyVmAntiAffinity",
     * "modifyVmdkAntiAffinity", "deleteVmAntiAffinity" and "deleteVmdkAntiAffinity".
     */
    public static String option = "option";
    /** REQUIRED: StoragePod name. */
    public static String storagePodName = "storagePodName";
    /** OPTIONAL: Rule name. */
    public static String ruleName = null;
    /** OPTIONAL: New name for rule while modifying. */
    public static String newRuleName = null;
    /** OPTIONAL: Flag to indicate whether or not the rule is enabled. */
    public static Boolean enabled = null;
    /** OPTIONAL: Virtual machine name. */
    public static String vmName = null;
    /** OPTIONAL: Comma separated, list of VM name. It is required while adding VmAntiAffinity Rule. */
    public static String[] vmList = null;

    private static final String[] validOptions = {
        "addVmAntiAffinity",
        "addVmdkAntiAffinity",
        "list",
        "modifyVmAntiAffinity",
        "modifyVmdkAntiAffinity",
        "deleteVmAntiAffinity",
        "deleteVmdkAntiAffinity"
    };

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(SDRSRules.class, args);

        validate();

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            if (option.equalsIgnoreCase("list")) {
                listRules(propertyCollectorHelper, storagePodName);
            } else if (option.equalsIgnoreCase("addVmAntiAffinity")) {
                addVmAntiAffinityRule(
                        vimPort,
                        serviceContent,
                        propertyCollectorHelper,
                        storagePodName,
                        ruleName,
                        enabled,
                        List.of(vmList));
            } else if (option.equalsIgnoreCase("addVmdkAntiAffinity")) {
                addVmdkAntiAffinityRule(
                        vimPort, serviceContent, propertyCollectorHelper, storagePodName, ruleName, enabled, vmName);
            } else if (option.equalsIgnoreCase("modifyVmAntiAffinity")) {
                modifyVmAntiAffinityRule(
                        vimPort,
                        serviceContent,
                        propertyCollectorHelper,
                        storagePodName,
                        ruleName,
                        newRuleName,
                        enabled,
                        vmName);
            } else if (option.equalsIgnoreCase("modifyVmdkAntiAffinity")) {
                modifyVmdkAntiAffinityRule(
                        vimPort,
                        serviceContent,
                        propertyCollectorHelper,
                        storagePodName,
                        ruleName,
                        newRuleName,
                        enabled);
            } else if (option.equalsIgnoreCase("deleteVmAntiAffinity")) {
                deleteVmAntiAffinityRule(vimPort, serviceContent, propertyCollectorHelper, storagePodName, ruleName);
            } else if (option.equalsIgnoreCase("deleteVmdkAntiAffinity")) {
                deleteVmdkAntiAffinityRule(vimPort, serviceContent, propertyCollectorHelper, storagePodName, ruleName);
            }
        }
    }

    private static void validate() {
        if (!isValidOption(option)) {
            throw new IllegalArgumentException("the option '--option " + option + "' is not a valid value");
        }
    }

    private static boolean isValidOption(String option) {
        boolean found = false;

        for (String it : validOptions) {
            if (it.equals(option)) {
                found = true;
                break;
            }
        }

        return found;
    }

    /**
     * Add VmAntiAffinity Rule.
     *
     * @param storagePodName StoragePod name
     * @param ruleName Name of the rule to be added
     * @param enabled Flag to indicate whether or not the rule is enabled
     * @param vmNames list of VMs that needs to be added in the Rule
     */
    private static void addVmAntiAffinityRule(
            VimPortType vimPort,
            ServiceContent serviceContent,
            PropertyCollectorHelper propertyCollectorHelper,
            String storagePodName,
            String ruleName,
            boolean enabled,
            List<String> vmNames)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg {
        ManagedObjectReference storageResourceManager = serviceContent.getStorageResourceManager();

        ManagedObjectReference storagePod = propertyCollectorHelper.getMoRefByName(storagePodName, STORAGE_POD);
        if (storagePod != null) {
            ManagedObjectReference vmMoRef = null;

            ClusterAntiAffinityRuleSpec vmAntiAffinityRuleSpec = new ClusterAntiAffinityRuleSpec();
            vmAntiAffinityRuleSpec.setName(ruleName);
            vmAntiAffinityRuleSpec.setEnabled(enabled);
            for (String vmName : vmNames) {
                vmMoRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);
                if (vmMoRef != null) {
                    vmAntiAffinityRuleSpec.getVm().add(vmMoRef);
                } else {
                    String msg = "\nFailure: Virtual Machine " + vmName + " not found.";
                    throw new RuntimeException(msg);
                }
            }
            vmAntiAffinityRuleSpec.setUserCreated(true);
            vmAntiAffinityRuleSpec.setMandatory(false);

            ClusterRuleSpec ruleSpec = new ClusterRuleSpec();
            ruleSpec.setInfo(vmAntiAffinityRuleSpec);
            ruleSpec.setOperation(ArrayUpdateOperation.ADD);

            StorageDrsPodConfigSpec podConfigSpec = new StorageDrsPodConfigSpec();
            podConfigSpec.getRule().add(ruleSpec);

            StorageDrsConfigSpec sdrsConfigSpec = new StorageDrsConfigSpec();
            sdrsConfigSpec.setPodConfigSpec(podConfigSpec);

            ManagedObjectReference taskMoRef =
                    vimPort.configureStorageDrsForPodTask(storageResourceManager, storagePod, sdrsConfigSpec, true);
            if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                System.out.print("\nSuccess: Adding VmAntiAffinity Rule.");
            } else {
                String msg = "\nFailure: Adding VmAntiAffinity Rule.";
                throw new RuntimeException(msg);
            }
        } else {
            String msg = "\nFailure: StoragePod " + storagePodName + " not found.";
            throw new RuntimeException(msg);
        }
    }

    /**
     * Add VmdkAntiAffinity Rule.
     *
     * @param storagePodName StoragePod name
     * @param ruleName Name of the rule to be added
     * @param enabled Flag to indicate whether or not the rule is enabled
     * @param vmName VM for which the rule needs to be added
     */
    private static void addVmdkAntiAffinityRule(
            VimPortType vimPort,
            ServiceContent serviceContent,
            PropertyCollectorHelper propertyCollectorHelper,
            String storagePodName,
            String ruleName,
            boolean enabled,
            String vmName)
            throws SOAPException, RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg {
        ManagedObjectReference storageResourceManager = serviceContent.getStorageResourceManager();

        ManagedObjectReference storagePod = propertyCollectorHelper.getMoRefByName(storagePodName, STORAGE_POD);
        if (storagePod != null) {
            ManagedObjectReference vmMoRef = null;

            StorageDrsVmConfigInfo drsVmConfigInfo = new StorageDrsVmConfigInfo();

            VirtualDiskAntiAffinityRuleSpec vmdkAntiAffinityRuleSpec = new VirtualDiskAntiAffinityRuleSpec();
            vmdkAntiAffinityRuleSpec.setName(ruleName);
            vmdkAntiAffinityRuleSpec.setEnabled(enabled);

            vmMoRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);
            if (vmMoRef != null) {
                VirtualMachineConfigInfo vmConfigInfo = propertyCollectorHelper.fetch(vmMoRef, "config");
                List<VirtualDevice> virtualDevices = vmConfigInfo.getHardware().getDevice();
                List<VirtualDevice> virtualDisk = new ArrayList<>();
                for (VirtualDevice device : virtualDevices) {
                    if (device.getClass().getSimpleName().equalsIgnoreCase("VirtualDisk")) {
                        virtualDisk.add(device);
                        vmdkAntiAffinityRuleSpec.getDiskId().add(device.getKey());
                    }
                }
                if (virtualDisk.size() < 2) {
                    throw new SOAPException(
                            "VM should have minimum of 2 virtual disks" + " while adding VMDK AntiAffinity Rule.");
                }
                System.out.println("Adding below list of virtual disk to rule " + ruleName + " :");

                for (VirtualDevice device : virtualDisk) {
                    System.out.println(
                            "Virtual Disk : " + device.getDeviceInfo().getLabel() + ", Key : " + device.getKey());
                }
                vmdkAntiAffinityRuleSpec.setUserCreated(true);
                vmdkAntiAffinityRuleSpec.setMandatory(false);

                drsVmConfigInfo.setIntraVmAntiAffinity(vmdkAntiAffinityRuleSpec);
                drsVmConfigInfo.setVm(vmMoRef);
            } else {
                String msg = "\nFailure: Virtual Machine " + vmName + " not found.";
                throw new RuntimeException(msg);
            }

            StorageDrsVmConfigSpec drsVmConfigSpec = new StorageDrsVmConfigSpec();
            drsVmConfigSpec.setInfo(drsVmConfigInfo);
            drsVmConfigSpec.setOperation(ArrayUpdateOperation.EDIT);

            StorageDrsConfigSpec sdrsConfigSpec = new StorageDrsConfigSpec();
            sdrsConfigSpec.getVmConfigSpec().add(drsVmConfigSpec);

            ManagedObjectReference taskMoRef =
                    vimPort.configureStorageDrsForPodTask(storageResourceManager, storagePod, sdrsConfigSpec, true);
            if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                System.out.print("\nSuccess: Adding VmdkAntiAffinity Rule.");
            } else {
                String msg = "\nFailure: Adding VmdkAntiAffinity Rule.";
                throw new RuntimeException(msg);
            }
        } else {
            String msg = "\nFailure: StoragePod " + storagePodName + " not found.";
            throw new RuntimeException(msg);
        }
    }

    /**
     * List Rules for a StoragePod.
     *
     * @param storagePodName StoragePod name
     */
    private static void listRules(PropertyCollectorHelper propertyCollectorHelper, String storagePodName)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        ManagedObjectReference storagePod = propertyCollectorHelper.getMoRefByName(storagePodName, STORAGE_POD);
        if (storagePod != null) {
            PodStorageDrsEntry podSDrsEntry = propertyCollectorHelper.fetch(storagePod, "podStorageDrsEntry");

            System.out.println("\n List of VM anti-affinity rules: ");
            List<ClusterRuleInfo> vmRuleSpec =
                    podSDrsEntry.getStorageDrsConfig().getPodConfig().getRule();
            for (ClusterRuleInfo vmRule : vmRuleSpec) {
                System.out.println(vmRule.getName());
            }

            System.out.println("\n List of VMDK anti-affinity rules: ");
            List<StorageDrsVmConfigInfo> vmConfig =
                    podSDrsEntry.getStorageDrsConfig().getVmConfig();
            for (StorageDrsVmConfigInfo sdrsVmConfig : vmConfig) {
                if (sdrsVmConfig.getIntraVmAntiAffinity() != null) {
                    System.out.println(sdrsVmConfig.getIntraVmAntiAffinity().getName());
                }
            }
        } else {
            String msg = "\nFailure: StoragePod " + storagePodName + " not found.";
            throw new RuntimeException(msg);
        }
    }

    /**
     * Modify VmAntiAffinity Rule.
     *
     * @param storagePodName StoragePod name
     * @param ruleName Name of the rule to be modified
     * @param newRuleName new name for the rule
     * @param enabled Flag to indicate whether or not the rule is enabled
     * @param vmName VM to be added to the list of VMs in the Rule
     */
    private static void modifyVmAntiAffinityRule(
            VimPortType vimPort,
            ServiceContent serviceContent,
            PropertyCollectorHelper propertyCollectorHelper,
            String storagePodName,
            String ruleName,
            String newRuleName,
            Boolean enabled,
            String vmName)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg {
        ManagedObjectReference storageResourceManager = serviceContent.getStorageResourceManager();

        ManagedObjectReference storagePod = propertyCollectorHelper.getMoRefByName(storagePodName, STORAGE_POD);
        if (storagePod != null) {
            PodStorageDrsEntry podSDrsEntry = propertyCollectorHelper.fetch(storagePod, "podStorageDrsEntry");

            List<ClusterRuleInfo> vmRuleInfo =
                    podSDrsEntry.getStorageDrsConfig().getPodConfig().getRule();
            ManagedObjectReference vmMoRef = null;
            StorageDrsConfigSpec sdrsConfigSpec = new StorageDrsConfigSpec();

            ClusterAntiAffinityRuleSpec vmAntiAffinityRuleSpec = null;
            for (ClusterRuleInfo vmRule : vmRuleInfo) {
                if (vmRule.getName().equalsIgnoreCase(ruleName)) {
                    vmAntiAffinityRuleSpec = (ClusterAntiAffinityRuleSpec) vmRule;
                }
            }

            if (vmAntiAffinityRuleSpec != null) {
                if (newRuleName != null) {
                    vmAntiAffinityRuleSpec.setName(newRuleName);
                }
                if (enabled != null) {
                    vmAntiAffinityRuleSpec.setEnabled(enabled);
                }
                if (vmName != null) {
                    vmMoRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);
                    if (vmMoRef != null) {
                        vmAntiAffinityRuleSpec.getVm().add(vmMoRef);
                    } else {
                        String msg = "\nFailure: Virtual Machine " + vmName + " not found.";
                        throw new RuntimeException(msg);
                    }
                }

                ClusterRuleSpec ruleSpec = new ClusterRuleSpec();
                ruleSpec.setInfo(vmAntiAffinityRuleSpec);
                ruleSpec.setOperation(ArrayUpdateOperation.EDIT);

                StorageDrsPodConfigSpec podConfigSpec = new StorageDrsPodConfigSpec();
                podConfigSpec.getRule().add(ruleSpec);

                sdrsConfigSpec.setPodConfigSpec(podConfigSpec);
            } else {
                String msg = "\nFailure: Rule " + ruleName + " not found.";
                throw new RuntimeException(msg);
            }
            ManagedObjectReference taskMoRef =
                    vimPort.configureStorageDrsForPodTask(storageResourceManager, storagePod, sdrsConfigSpec, true);
            if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                System.out.print("\nSuccess: Modifying VmAntiAffinityRule.");
            } else {
                String msg = "\nFailure: Modifying VmAntiAffinityRule.";
                throw new RuntimeException(msg);
            }
        } else {
            String msg = "\nFailure: StoragePod " + storagePodName + " not found.";
            throw new RuntimeException(msg);
        }
    }

    /**
     * Modify VmdkAntiAffinity Rule.
     *
     * @param storagePodName StoragePod name
     * @param ruleName Name of the rule to be modified
     * @param newRuleName new name for the rule
     * @param enabled Flag to indicate whether or not the rule is enabled
     */
    private static void modifyVmdkAntiAffinityRule(
            VimPortType vimPort,
            ServiceContent serviceContent,
            PropertyCollectorHelper propertyCollectorHelper,
            String storagePodName,
            String ruleName,
            String newRuleName,
            Boolean enabled)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg {
        ManagedObjectReference storageResourceManager = serviceContent.getStorageResourceManager();

        ManagedObjectReference storagePod = propertyCollectorHelper.getMoRefByName(storagePodName, STORAGE_POD);
        if (storagePod != null) {
            PodStorageDrsEntry podSDrsEntry = propertyCollectorHelper.fetch(storagePod, "podStorageDrsEntry");

            StorageDrsVmConfigInfo drsVmConfigInfo = null;
            StorageDrsConfigSpec sdrsConfigSpec = new StorageDrsConfigSpec();

            List<StorageDrsVmConfigInfo> sdrsVmConfig =
                    podSDrsEntry.getStorageDrsConfig().getVmConfig();
            for (StorageDrsVmConfigInfo vmConfig : sdrsVmConfig) {
                if (vmConfig.getIntraVmAntiAffinity() != null) {
                    if (vmConfig.getIntraVmAntiAffinity().getName().equalsIgnoreCase(ruleName)) {
                        drsVmConfigInfo = vmConfig;
                    }
                }
            }
            if (drsVmConfigInfo != null) {
                if (newRuleName != null) {
                    drsVmConfigInfo.getIntraVmAntiAffinity().setName(newRuleName);
                }
                if (enabled != null) {
                    drsVmConfigInfo.getIntraVmAntiAffinity().setEnabled(enabled);
                }

                StorageDrsVmConfigSpec drsVmConfigSpec = new StorageDrsVmConfigSpec();
                drsVmConfigSpec.setInfo(drsVmConfigInfo);
                drsVmConfigSpec.setOperation(ArrayUpdateOperation.EDIT);

                sdrsConfigSpec.getVmConfigSpec().add(drsVmConfigSpec);
            } else {
                String msg = "\nFailure: Rule " + ruleName + " not found.";
                throw new RuntimeException(msg);
            }
            ManagedObjectReference taskMoRef =
                    vimPort.configureStorageDrsForPodTask(storageResourceManager, storagePod, sdrsConfigSpec, true);
            if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                System.out.print("\nSuccess: Modifying VmdkAntiAffinityRule.");
            } else {
                String msg = "\nFailure: Modifying VmdkAntiAffinityRule.";
                throw new RuntimeException(msg);
            }
        } else {
            String msg = "\nFailure: StoragePod " + storagePodName + " not found.";
            throw new RuntimeException(msg);
        }
    }

    /**
     * Delete VmAntiAffinity Rule.
     *
     * @param storagePodName StoragePod name
     * @param ruleName Name of the rule to be deleted
     */
    private static void deleteVmAntiAffinityRule(
            VimPortType vimPort,
            ServiceContent serviceContent,
            PropertyCollectorHelper propertyCollectorHelper,
            String storagePodName,
            String ruleName)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg {
        ManagedObjectReference storageResourceManager = serviceContent.getStorageResourceManager();

        ManagedObjectReference storagePod = propertyCollectorHelper.getMoRefByName(storagePodName, STORAGE_POD);
        if (storagePod != null) {
            PodStorageDrsEntry podSDrsEntry = propertyCollectorHelper.fetch(storagePod, "podStorageDrsEntry");

            List<ClusterRuleInfo> vmRuleInfo =
                    podSDrsEntry.getStorageDrsConfig().getPodConfig().getRule();
            StorageDrsConfigSpec sdrsConfigSpec = new StorageDrsConfigSpec();

            ClusterAntiAffinityRuleSpec vmAntiAffinityRuleSpec = null;
            for (ClusterRuleInfo vmRule : vmRuleInfo) {
                if (vmRule.getName().equalsIgnoreCase(ruleName)) {
                    vmAntiAffinityRuleSpec = (ClusterAntiAffinityRuleSpec) vmRule;
                }
            }
            if (vmAntiAffinityRuleSpec != null) {
                ClusterRuleSpec ruleSpec = new ClusterRuleSpec();
                ruleSpec.setInfo(vmAntiAffinityRuleSpec);
                ruleSpec.setOperation(ArrayUpdateOperation.REMOVE);
                ruleSpec.setRemoveKey(vmAntiAffinityRuleSpec.getKey());

                StorageDrsPodConfigSpec podConfigSpec = new StorageDrsPodConfigSpec();
                podConfigSpec.getRule().add(ruleSpec);

                sdrsConfigSpec.setPodConfigSpec(podConfigSpec);
            } else {
                String msg = "\nFailure: Rule " + ruleName + " not found.";
                throw new RuntimeException(msg);
            }
            ManagedObjectReference taskMoRef =
                    vimPort.configureStorageDrsForPodTask(storageResourceManager, storagePod, sdrsConfigSpec, true);
            if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                System.out.print("\nSuccess: Deleting VmAntiAffinity Rule.");
            } else {
                String msg = "\nFailure: Deleting VmAntiAffinity Rule.";
                throw new RuntimeException(msg);
            }
        } else {
            String msg = "\nFailure: StoragePod " + storagePodName + " not found.";
            throw new RuntimeException(msg);
        }
    }

    /**
     * Delete VmdkAntiAffinity Rule.
     *
     * @param storagePodName StoragePod name
     * @param ruleName Name of the rule to be deleted
     */
    private static void deleteVmdkAntiAffinityRule(
            VimPortType vimPort,
            ServiceContent serviceContent,
            PropertyCollectorHelper propertyCollectorHelper,
            String storagePodName,
            String ruleName)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg {
        ManagedObjectReference storageResourceManager = serviceContent.getStorageResourceManager();

        ManagedObjectReference storagePod = propertyCollectorHelper.getMoRefByName(storagePodName, STORAGE_POD);
        if (storagePod != null) {
            PodStorageDrsEntry podSDrsEntry = propertyCollectorHelper.fetch(storagePod, "podStorageDrsEntry");

            StorageDrsVmConfigInfo drsVmConfigInfo = null;
            StorageDrsConfigSpec sdrsConfigSpec = new StorageDrsConfigSpec();

            List<StorageDrsVmConfigInfo> sdrsVmConfig =
                    podSDrsEntry.getStorageDrsConfig().getVmConfig();
            for (StorageDrsVmConfigInfo vmConfig : sdrsVmConfig) {
                if (vmConfig.getIntraVmAntiAffinity() != null) {
                    if (vmConfig.getIntraVmAntiAffinity().getName().equalsIgnoreCase(ruleName)) {
                        drsVmConfigInfo = vmConfig;
                    }
                }
            }
            if (drsVmConfigInfo != null) {
                drsVmConfigInfo.setIntraVmAntiAffinity(null);

                StorageDrsVmConfigSpec drsVmConfigSpec = new StorageDrsVmConfigSpec();
                drsVmConfigSpec.setInfo(drsVmConfigInfo);
                drsVmConfigSpec.setOperation(ArrayUpdateOperation.EDIT);

                sdrsConfigSpec.getVmConfigSpec().add(drsVmConfigSpec);
            } else {
                String msg = "\nFailure: Rule " + ruleName + " not found.";
                throw new RuntimeException(msg);
            }
            ManagedObjectReference taskMoRef =
                    vimPort.configureStorageDrsForPodTask(storageResourceManager, storagePod, sdrsConfigSpec, true);
            if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                System.out.print("\nSuccess: Deleting VmdkAntiAffinity Rule.");
            } else {
                String msg = "\nFailure: Deleting VmdkAntiAffinity Rule.";
                throw new RuntimeException(msg);
            }
        } else {
            String msg = "\nFailure: StoragePod " + storagePodName + " not found.";
            throw new RuntimeException(msg);
        }
    }
}
