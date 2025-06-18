/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.compute.contentlibrary.isomount;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.content.library.Item;
import com.vmware.content.library.ItemTypes;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.VM;
import com.vmware.vcenter.VMTypes;
import com.vmware.vcenter.iso.Image;

/**
 * Demonstrates the content library ISO item mount and unmount workflow via the mount and unmount APIs from the ISO
 * service.
 *
 * <p>Sample Prerequisites: Running this sample requires creation of a VM as well as a library item of type ISO.
 */
public class IsoMount {
    private static final Logger log = LoggerFactory.getLogger(IsoMount.class);
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
    /** REQUIRED: Name of an existing iso library item to mount. */
    private static final String ISO_TYPE = "iso";

    public static String itemName = "itemName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(IsoMount.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            // Look up the library item using the specified itemName
            Item itemService = client.createStub(Item.class);

            ItemTypes.FindSpec itemFindSpec = new ItemTypes.FindSpec();
            itemFindSpec.setName(itemName);
            itemFindSpec.setType(ISO_TYPE);

            List<String> isoItemIds = itemService.find(itemFindSpec);
            if (isoItemIds.size() != 1) {
                throw new RuntimeException(
                        "library item by name " + itemName + " and type " + ISO_TYPE + " must exist");
            }
            String itemId = isoItemIds.get(0);

            // Look up the VM using the specified vmName
            VM vmService = client.createStub(VM.class);

            VMTypes.FilterSpec vmFilterSpec = new VMTypes.FilterSpec.Builder()
                    .setNames(new HashSet<>(Collections.singletonList(vmName)))
                    .build();

            List<VMTypes.Summary> vmList = vmService.list(vmFilterSpec);
            if (vmList.isEmpty()) {
                throw new RuntimeException("VM By Name '" + vmName + "' does not exist");
            }

            String vmId = vmList.get(0).getVm();
            log.info("Mounting ISO item {} ({}) on VM {} ({})", itemName, itemId, vmName, vmId);

            Image isoImageService = client.createStub(Image.class);

            // Mount the specified iso item on the given VM
            // Return the id of the mounted device
            String deviceId = isoImageService.mount(itemId, vmId);
            log.info("Mounted device: {}", deviceId);

            // Unmount the given device from the VM
            isoImageService.unmount(vmId, deviceId);
            log.info("Unmounted device: {}", deviceId);
        }
    }
}
