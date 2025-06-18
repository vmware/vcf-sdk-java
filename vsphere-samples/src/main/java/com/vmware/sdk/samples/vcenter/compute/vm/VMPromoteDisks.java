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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayOfVirtualDevice;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDisk;

/** Used to consolidate a linked clone by using promote API. */
public class VMPromoteDisks {
    private static final Logger log = LoggerFactory.getLogger(VMPromoteDisks.class);
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
    /** REQUIRED: True|False to unlink. */
    public static Boolean unLink = Boolean.FALSE;
    /** OPTIONAL: Disk name to unlink. E.g dname1:dname2 */
    public static String diskNames = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VMPromoteDisks.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference vmMoRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);

            boolean unlink = unLink;

            List<VirtualDisk> vDiskList = new ArrayList<>();
            if (vmMoRef != null) {
                if (diskNames != null) {
                    String disknames = diskNames;
                    String[] diskArr = disknames.split(":");

                    Map<String, String> disks = new HashMap<>();
                    for (String disk : diskArr) {
                        disks.put(disk, null);
                    }

                    List<VirtualDevice> devices = ((ArrayOfVirtualDevice)
                                    propertyCollectorHelper.fetch(vmMoRef, "config.hardware.device"))
                            .getVirtualDevice();
                    for (VirtualDevice device : devices) {
                        if (device instanceof VirtualDisk) {
                            if (disks.containsKey(device.getDeviceInfo().getLabel())) {
                                vDiskList.add((VirtualDisk) device);
                            }
                        }
                    }
                }

                ManagedObjectReference taskMoRef = vimPort.promoteDisksTask(vmMoRef, unlink, vDiskList);

                if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                    log.info("Virtual Disks Promoted successfully.");
                } else {
                    log.error("Failure -: Virtual Disks cannot be promoted");
                }
            } else {
                log.error("Virtual Machine {} doesn't exist", vmName);
            }
        }
    }
}
