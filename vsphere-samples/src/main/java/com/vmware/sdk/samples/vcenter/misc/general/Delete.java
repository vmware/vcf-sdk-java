/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.misc.general;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.MANAGED_ENTITY;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/**
 * This sample deletes the specified managed entity from the inventory tree. The managed entity can be a virtual
 * machine, ClusterComputeResource or a Folder.
 */
public class Delete {
    private static final Logger log = LoggerFactory.getLogger(Delete.class);
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

    /** REQUIRED: Name of entity to delete [Virtual Machine|ClusterComputeResource|Folder]. */
    public static String managedEntityName = "Virtual Machine";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(Delete.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference managedEntityMoRef =
                    propertyCollectorHelper.getMoRefByName(managedEntityName, MANAGED_ENTITY);
            if (managedEntityMoRef == null) {
                log.error("Unable to find a Managed Entity By name [ {} ]", managedEntityName);
            } else {
                ManagedObjectReference taskMoRef = vimPort.destroyTask(managedEntityMoRef);
                if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                    log.info(
                            "Successful delete of Managed Entity Name - [ {} ] and Entity Type - [ {} ]\n",
                            managedEntityName,
                            managedEntityMoRef.getType());
                }
            }
        }
    }
}
