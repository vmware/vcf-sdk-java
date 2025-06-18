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
import static com.vmware.vim25.ManagedObjectType.DATASTORE;
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
import com.vmware.vim25.VirtualMachineRelocateSpec;

/** Used to relocate a linked clone using disk move type. */
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

    /** REQUIRED: Name of the virtual machine. */
    public static String vmname = "vmName";
    /** REQUIRED: Either of [moveChildMostDiskBacking | moveAllDiskBackingsAndAllowSharing]. */
    public static String diskMoveType = "diskMoveType";
    /** REQUIRED: Name of the datastore. */
    public static String datastoreName = "datastoreName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VMRelocate.class, args);

        customValidation();

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference vmMoRef = propertyCollectorHelper.getMoRefByName(vmname, VIRTUAL_MACHINE);
            ManagedObjectReference dsMoRef = propertyCollectorHelper.getMoRefByName(datastoreName, DATASTORE);

            if (dsMoRef == null) {
                log.error("Datastore {} Not Found", datastoreName);
                return;
            }

            if (vmMoRef != null) {
                VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();

                String moveType = diskMoveType;
                if (moveType.equalsIgnoreCase("moveChildMostDiskBacking")) {
                    relocateSpec.setDiskMoveType("moveChildMostDiskBacking");
                } else if (moveType.equalsIgnoreCase("moveAllDiskBackingsAndAllowSharing")) {
                    relocateSpec.setDiskMoveType("moveAllDiskBackingsAndAllowSharing");
                }

                relocateSpec.setDatastore(dsMoRef);

                ManagedObjectReference taskMoRef = vimPort.relocateVMTask(vmMoRef, relocateSpec, null);

                if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                    log.info("Linked Clone relocated successfully.");
                } else {
                    log.error("Failure -: Linked clone cannot be relocated");
                }
            } else {
                log.error("Virtual Machine {} doesn't exist", vmname);
            }
        }
    }

    private static boolean customValidation() {
        boolean flag = true;
        String moveType = diskMoveType;
        if ((!moveType.equalsIgnoreCase("moveChildMostDiskBacking"))
                && (!moveType.equalsIgnoreCase("moveAllDiskBackingsAndAllowSharing"))) {
            log.info(
                    "diskmovetype option must be either moveChildMostDiskBacking or moveAllDiskBackingsAndAllowSharing");
            flag = false;
        }
        return flag;
    }
}
