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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.pbm.InvalidArgumentFaultMsg;
import com.vmware.pbm.PbmCapabilityProfile;
import com.vmware.pbm.PbmPlacementHub;
import com.vmware.pbm.PbmPortType;
import com.vmware.pbm.PbmServiceInstanceContent;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.pbm.PbmUtil;
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachineDefinedProfileSpec;
import com.vmware.vim25.VirtualMachineRelocateSpec;

/**
 * Used to relocate a virtual machine's virtual disks to a datastore compliant with the given storage policy based
 * management profile. If multiple datastores are compliant, this sample picks one of them.
 */
public class VMRelocate {
    private static final Logger log = LoggerFactory.getLogger(VMRelocate.class);

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

    /** REQUIRED: Inventory path of the VM. */
    public static String vmPath = "vmPath";
    /** REQUIRED: Name of the storage profile. */
    public static String profileName = "profileName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VMRelocate.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent vimServiceContent = client.getVimServiceContent();

            PbmPortType pbmPort = client.getPbmPort();

            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, vimServiceContent);

            // Step 1: Instantiate SPBM Service
            PbmServiceInstanceContent pbmServiceInstanceContent = client.getPbmServiceInstanceContent();

            // Step 2: Verify if specified VM Path Exists
            ManagedObjectReference vmMoRef = vimPort.findByInventoryPath(vimServiceContent.getSearchIndex(), vmPath);
            if (vmMoRef == null) {
                log.error("The VMPath specified [ {} ] is not found", vmPath);
                return;
            }

            // Step 3: Get VM's Host
            ManagedObjectReference hostMoRef = propertyCollectorHelper.fetch(vmMoRef, "runtime.host");

            // Step 4: Get available Datastores in the host
            ArrayOfManagedObjectReference dsRefs = propertyCollectorHelper.fetch(hostMoRef, "datastore");

            // Step 5: Populate Allowed Hubs
            List<PbmPlacementHub> allowedHubs = new ArrayList<>();
            for (ManagedObjectReference dsMoRef : dsRefs.getManagedObjectReference()) {
                PbmPlacementHub hub = new PbmPlacementHub();
                hub.setHubId(dsMoRef.getValue());
                hub.setHubType(dsMoRef.getType());

                allowedHubs.add(hub);
            }
            if (allowedHubs.isEmpty()) {
                log.error("There should be at least one datastore available on the current host to relocate.");
                return;
            }

            // Step 6: Get compliant datastores for the given storage profile
            PbmCapabilityProfile pbmProfile = PbmUtil.getPbmProfile(pbmPort, pbmServiceInstanceContent, profileName);
            List<PbmPlacementHub> hubs = pbmPort.pbmQueryMatchingHub(
                    pbmServiceInstanceContent.getPlacementSolver(), allowedHubs, pbmProfile.getProfileId());
            if (hubs.isEmpty()) {
                log.error("No compliant datastores matching the storage profile found on the host");
                return;
            }

            // Step 7: Use the first compliant datastore
            ManagedObjectReference relocateDSMoRef = new ManagedObjectReference();
            PbmPlacementHub targetHub = hubs.get(0);
            relocateDSMoRef.setType(targetHub.getHubType());
            relocateDSMoRef.setValue(targetHub.getHubId());

            // Step 8: Create Relocate Spec & Relocate
            VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();
            relocateSpec.setDatastore(relocateDSMoRef);
            relocateSpec.getProfile().add(getVMDefinedProfileSpec(pbmPort, pbmServiceInstanceContent, profileName));

            ManagedObjectReference taskMoRef = vimPort.relocateVMTask(vmMoRef, relocateSpec, null);
            if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                String datastoreName = propertyCollectorHelper.fetch(relocateDSMoRef, "name");
                log.info("VM's storage relocated successfully to datastore {}", datastoreName);
            } else {
                log.error("Failure -: VM's storage cannot be relocated");
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
