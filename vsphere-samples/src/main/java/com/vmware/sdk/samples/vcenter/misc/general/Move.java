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
import static com.vmware.vim25.ManagedObjectType.FOLDER;
import static com.vmware.vim25.ManagedObjectType.MANAGED_ENTITY;

import java.util.List;

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
 * This sample moves a managed entity from its current location in the inventory to a new location, in a specified
 * folder.
 *
 * <p>This sample finds both the managed entity and the target folder in the inventory tree before attempting the move.
 * If either of these is not found, an error message displays.
 */
public class Move {
    private static final Logger log = LoggerFactory.getLogger(Move.class);
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

    /** REQUIRED: Name of the inventory object - a managed entity. */
    public static String entityName = "entityName";
    /** REQUIRED: Name of folder to move inventory object into. */
    public static String folderName = "folderName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(Move.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference managedEntityMoRef =
                    propertyCollectorHelper.getMoRefByName(entityName, MANAGED_ENTITY);
            if (managedEntityMoRef == null) {
                log.error("Unable to find a managed entity named '{}' in the Inventory", entityName);
                return;
            }

            ManagedObjectReference folderMoRef = propertyCollectorHelper.getMoRefByName(folderName, FOLDER);
            if (folderMoRef == null) {
                log.error("Unable to find folder '{}' in the Inventory", folderName);
            } else {
                ManagedObjectReference taskMoRef = vimPort.moveIntoFolderTask(folderMoRef, List.of(managedEntityMoRef));
                if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                    log.info("ManagedEntity '{}' moved to folder '{}' successfully.", entityName, folderName);
                } else {
                    log.error("Failure -: Managed Entity cannot be moved");
                }
            }
        }
    }
}
