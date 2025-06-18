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
import static com.vmware.sdk.vsphere.utils.VimClient.getVimServiceInstanceRef;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.Capability;
import com.vmware.vim25.EVCMode;
import com.vmware.vim25.HostFeatureMask;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachineCapability;

/** This sample applies a Per-VM EVC setting to an existing VM. */
public class VMApplyEvc {
    private static final Logger log = LoggerFactory.getLogger(VMApplyEvc.class);
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
    public static String vmPathName = "vmPathName";
    /** OPTIONAL: Key to apply. If unset, will remove the mode. */
    public static String evcKey = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VMApplyEvc.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference vmMoRef = vimPort.findByInventoryPath(serviceContent.getSearchIndex(), vmPathName);
            if (vmMoRef == null) {
                log.error("The VMPath specified {} is not found.", vmPathName);
                return;
            }

            boolean perVmEvcSupported = isPerVmEvcSupported(propertyCollectorHelper, vmMoRef);
            if (!perVmEvcSupported) {
                log.error("Per-VM EVC is not supported for this virtual hardware version.");
                return;
            }

            List<HostFeatureMask> masks = null;
            if (evcKey != null) { // Removing EVC settings is easier.
                Capability capability = propertyCollectorHelper.fetch(getVimServiceInstanceRef(), "capability");

                List<EVCMode> evcModes = capability.getSupportedEVCMode();
                for (EVCMode evcMode : evcModes) {
                    if (evcMode.getKey().equals(evcKey)) {
                        masks = evcMode.getFeatureMask();
                        break;
                    }
                }

                if (masks == null) {
                    System.out.format("Failed to find EVC mode with key %s", evcKey);
                    return;
                }
            }
            ManagedObjectReference applyTaskMoRef = vimPort.applyEvcModeVMTask(vmMoRef, masks, true);

            String evcKeyString = evcKey == null ? "none" : evcKey;
            String taskResultMessage = propertyCollectorHelper.awaitTaskCompletion(applyTaskMoRef)
                    ? "Successfully applied"
                    : "Failed to apply";
            log.info("{} EVC mode to {}", taskResultMessage, evcKeyString);
        }
    }

    /**
     * Returns whether Per-VM EVC is supported by the VirtualMachine. It might not be supported if virtual hardware
     * version is too old.
     *
     * @param moRef {@link ManagedObjectReference} representing the VirtualMachine.
     * @return boolean value of support
     */
    private static boolean isPerVmEvcSupported(
            PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference moRef)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        VirtualMachineCapability cap = propertyCollectorHelper.fetch(moRef, "capability");

        // The property might be null. Check that first.
        if (cap.isPerVmEvcSupported() != null) {
            return cap.isPerVmEvcSupported();
        }
        return false;
    }
}
