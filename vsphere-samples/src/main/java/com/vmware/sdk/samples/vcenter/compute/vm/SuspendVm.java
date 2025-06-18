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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/** Demonstrates suspending the specified virtual machine. */
public class SuspendVm {
    private static final Logger log = LoggerFactory.getLogger(SuspendVm.class);
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

    /** REQUIRED: Name of the virtual machine to be suspended. */
    public static String vmName = "vmName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(SuspendVm.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference vmMoRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);
            if (vmMoRef == null) {
                log.error("No Virtual Machine found with name:{}", vmName);
            } else {
                try {
                    log.info("Suspending virtual machine : {}[{}]", vmName, vmMoRef.getValue());

                    ManagedObjectReference taskMoRef = vimPort.suspendVMTask(vmMoRef);

                    if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                        log.info("{}[{}] suspended successfully", vmName, vmMoRef.getValue());
                    }
                } catch (Exception e) {
                    log.error("Unable to suspend vm : {}[{}]", vmName, vmMoRef.getValue());
                    log.error("Exception: ", e);
                }
            }
        }
    }
}
