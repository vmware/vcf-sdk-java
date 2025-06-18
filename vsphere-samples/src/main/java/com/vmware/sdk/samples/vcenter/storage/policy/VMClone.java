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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.pbm.InvalidArgumentFaultMsg;
import com.vmware.pbm.PbmCapabilityProfile;
import com.vmware.pbm.PbmPortType;
import com.vmware.pbm.PbmServiceInstanceContent;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.pbm.PbmUtil;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineDefinedProfileSpec;
import com.vmware.vim25.VirtualMachineRelocateSpec;

/**
 * This sample makes a clone of an existing VM to a new VM and associates it with the specified storage profile. Note:
 * This sample does not relocate the vm disks. Thus, the clone may or may not be compliant with the given storage
 * profile.
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

    /** REQUIRED: Name of Datacenter. */
    public static String dataCenterName = "datacenterName";
    /** REQUIRED: Inventory path of the VM. */
    public static String vmPathName = "vmPathName";
    /** REQUIRED: Name of the clone. */
    public static String cloneName = "cloneName";
    /** REQUIRED: Name of the storage profile. */
    public static String profileName = "profileName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VMClone.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent vimServiceContent = client.getVimServiceContent();

            PbmPortType pbmPort = client.getPbmPort();
            PbmServiceInstanceContent pbmServiceInstanceContent = client.getPbmServiceInstanceContent();

            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, vimServiceContent);

            // Step 1: Find the Datacenter reference by using findByInventoryPath().
            ManagedObjectReference datacenterRef =
                    vimPort.findByInventoryPath(vimServiceContent.getSearchIndex(), dataCenterName);
            if (datacenterRef == null) {
                log.error("The specified datacenter [ {} ]is not found", dataCenterName);
                return;
            }
            // Step 2: Find the virtual machine folder for this datacenter.
            ManagedObjectReference vmFolderRef = propertyCollectorHelper.fetch(datacenterRef, "vmFolder");

            // Step 3: Find the virtual machine reference
            ManagedObjectReference vmRef = vimPort.findByInventoryPath(vimServiceContent.getSearchIndex(), vmPathName);
            if (vmRef == null) {
                log.error("The VMPath specified [ {} ] is not found \n", vmPathName);
                return;
            }

            // Step 4: Create Specs
            VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();
            VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
            VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();

            // Step 5: Associate Storage Profile
            relocateSpec.getProfile().add(getVMDefinedProfileSpec(pbmPort, pbmServiceInstanceContent, profileName));
            cloneSpec.setConfig(configSpec);
            cloneSpec.setLocation(relocateSpec);
            cloneSpec.setPowerOn(false);
            cloneSpec.setTemplate(false);

            // Step 6: Clone VM
            log.info(
                    "Cloning Virtual Machine [{}] to clone name [{}] \n",
                    vmPathName.substring(vmPathName.lastIndexOf("/") + 1),
                    cloneName);

            ManagedObjectReference cloneTaskMoRef = vimPort.cloneVMTask(vmRef, vmFolderRef, cloneName, cloneSpec);
            if (propertyCollectorHelper.awaitTaskCompletion(cloneTaskMoRef)) {
                log.info(
                        "Successfully cloned Virtual Machine [{}] to clone name [{}] \n",
                        vmPathName.substring(vmPathName.lastIndexOf("/") + 1),
                        cloneName);
            } else {
                log.error(
                        "Failure Cloning Virtual Machine [{}] to clone name [{}] \n",
                        vmPathName.substring(vmPathName.lastIndexOf("/") + 1),
                        cloneName);
            }
        }
    }

    /**
     * This method returns the {@link VirtualMachineDefinedProfileSpec} for a given storage profile name.
     *
     * @param profileName name of the policy based management profile
     */
    private static VirtualMachineDefinedProfileSpec getVMDefinedProfileSpec(
            PbmPortType pbmPort, PbmServiceInstanceContent pbmServiceInstanceContent, String profileName)
            throws InvalidArgumentFaultMsg, com.vmware.pbm.RuntimeFaultFaultMsg {

        PbmCapabilityProfile profile = PbmUtil.getPbmProfile(pbmPort, pbmServiceInstanceContent, profileName);

        VirtualMachineDefinedProfileSpec pbmProfile = new VirtualMachineDefinedProfileSpec();
        pbmProfile.setProfileId(profile.getProfileId().getUniqueId());

        return pbmProfile;
    }
}
