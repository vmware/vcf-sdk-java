/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.compute.vm.guest;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NamePasswordAuthentication;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachinePowerState;

/** This sample creates a temporary file inside a virtual machine. Since vSphere API 5.0. */
public class CreateTemporaryFile {
    private static final Logger log = LoggerFactory.getLogger(CreateTemporaryFile.class);
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
    /** REQUIRED: Username in the guest. */
    public static String guestUsername = "guestUsername";
    /** REQUIRED: Password in the guest. */
    public static String guestPassword = "guestPassword";
    /** OPTIONAL: Prefix to be added to the file name. */
    public static String prefix = null;
    /** OPTIONAL: Suffix to be added to the file name. */
    public static String suffix = null;
    /** OPTIONAL: Path to the directory inside the guest. */
    public static String directoryPath = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(CreateTemporaryFile.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference vmMoRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);
            VirtualMachinePowerState powerState;
            if (vmMoRef != null) {
                log.info("Virtual Machine {} found", vmName);

                powerState = propertyCollectorHelper.fetch(vmMoRef, "runtime.powerState");
                if (!powerState.equals(VirtualMachinePowerState.POWERED_ON)) {
                    log.error("VirtualMachine: {} needs to be powered on", vmName);
                    return;
                }
            } else {
                log.error("Virtual Machine {} not found.", vmName);
                return;
            }

            String[] opts = new String[] {"guest.guestOperationsReady"};
            String[] opt = new String[] {"guest.guestOperationsReady"};
            propertyCollectorHelper.awaitManagedObjectUpdates(vmMoRef, opts, opt, new Object[][] {new Object[] {true}});

            log.info("Guest Operations are ready for the VM");
            ManagedObjectReference guestOpManger = serviceContent.getGuestOperationsManager();
            ManagedObjectReference fileManagerRef = propertyCollectorHelper.fetch(guestOpManger, "fileManager");

            NamePasswordAuthentication auth = new NamePasswordAuthentication();
            auth.setUsername(guestUsername);
            auth.setPassword(guestPassword);
            auth.setInteractiveSession(false);

            log.info("Executing CreateTemporaryFile guest operation");
            String result =
                    vimPort.createTemporaryFileInGuest(fileManagerRef, vmMoRef, auth, prefix, suffix, directoryPath);

            log.info("Temporary file was successfully created at: {} inside the guest", result);
            // A Temporary file is created inside the guest. The user can use the file and delete it.
        }
    }
}
