/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.storage.vslm.fcd;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.DATASTORE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.storage.vslm.fcd.helpers.FcdVslmHelper;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vslm.VslmPortType;

/**
 * This sample executes reconcile related operations on a given VStorageObject from vslm:
 *
 * <ol>
 *   <li>Reconcile the datastore inventory info of virtual storage objects.
 *   <li>Schedule reconcile of the inventory info of virtual storage objects on one of the hosts that is connected with
 *       the datastore.
 * </ol>
 */
public class FcdReconcileDatastoreInventoryOperations {
    private static final Logger log = LoggerFactory.getLogger(FcdReconcileDatastoreInventoryOperations.class);

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

    /** REQUIRED: Name of datastore that needs to be reconciled from vslm. */
    public static String datastoreName = "datastoreName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FcdReconcileDatastoreInventoryOperations.class, args);

        VcenterClientFactory clientFactory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = clientFactory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);
            VslmPortType vslmPort = client.getVslmPort();

            FcdVslmHelper vslmHelper = new FcdVslmHelper(vslmPort);

            ManagedObjectReference vStorageObjectManager =
                    client.getVslmServiceInstanceContent().getVStorageObjectManager();

            log.info("Invoking reconcile datastore inventory operations on VStorageObject from VSLM ::");

            // Get all the input moRefs required for retrieving VStorageObject.
            ManagedObjectReference datastoreMoRef = propertyCollectorHelper.getMoRefByName(datastoreName, DATASTORE);

            // Retrieve a vStorageObject.
            log.info("Operation: Scheduling reconcile of the inventory from vslm");
            vslmPort.vslmScheduleReconcileDatastoreInventory(vStorageObjectManager, datastoreMoRef);

            log.info(
                    "Scheduled reconcile of the inventory info of virtual storage objects on one of the hosts that is connected with the datastore: [ {} ] from vslm.",
                    datastoreMoRef);

            // Reconcile the datastore inventory info of virtual storage objects.
            log.info("Operation: Reconciling the datastore inventory info of virtual storage objects from vslm.");
            ManagedObjectReference taskMoRef =
                    vslmPort.vslmReconcileDatastoreInventoryTask(vStorageObjectManager, datastoreMoRef);

            boolean isReconcileDatastoreInventorySucceeded = vslmHelper.waitForTask(taskMoRef);
            if (isReconcileDatastoreInventorySucceeded) {
                log.info("Success: Reconciled datastore : [ {} ] from vslm", datastoreMoRef);
            } else {
                String message = "Error: Reconciling datastore [ " + datastoreMoRef + " ] from vslm.";
                throw new RuntimeException(message);
            }
        }
    }
}
