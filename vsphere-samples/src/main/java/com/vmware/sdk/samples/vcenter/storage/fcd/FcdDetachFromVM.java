/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.storage.fcd;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.storage.fcd.helpers.FcdHelper;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/** This sample detaches a given virtual storage object (FCD) from the given virtual machine. */
public class FcdDetachFromVM {
    private static final Logger log = LoggerFactory.getLogger(FcdDetachFromVM.class);
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

    /** REQUIRED: UUID of the disk. */
    public static String vStorageObjectId = "vStorageObjectId";
    /** REQUIRED: Name of virtual machine. */
    public static String vmName = "vmName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FcdDetachFromVM.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference vmMoRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);

            log.info("Operation: Attaching a given vStorageObject to the given virtualMachine.");
            ManagedObjectReference detachDiskTaskMoRef =
                    vimPort.detachDiskTask(vmMoRef, FcdHelper.makeId(vStorageObjectId));

            if (propertyCollectorHelper.awaitTaskCompletion(detachDiskTaskMoRef)) {
                log.info(
                        "Success: Detached vStorageObjectId : [ Id = {} ] from VM [ Name = {} ]\n",
                        vStorageObjectId,
                        vmName);
            } else {
                String msg = "Error: Detaching [ " + vStorageObjectId + " ] from the VM [ " + vmName + " ] failed.";
                throw new RuntimeException(msg);
            }
        }
    }
}
