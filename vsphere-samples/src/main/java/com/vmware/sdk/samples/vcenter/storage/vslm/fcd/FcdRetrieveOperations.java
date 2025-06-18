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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.storage.fcd.helpers.FcdHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VStorageObject;
import com.vmware.vim25.VStorageObjectStateInfo;
import com.vmware.vslm.VslmPortType;
import com.vmware.vslm.VslmVsoVStorageObjectQueryResult;
import com.vmware.vslm.VslmVsoVStorageObjectQuerySpec;
import com.vmware.vslm.VslmVsoVStorageObjectResult;

/**
 * This sample executes below retrieve related operations on a snapshot of a given VStorageObject from vslm:
 *
 * <ol>
 *   <li>Retrieve virtual storage object.
 *   <li>Retrieve multiple virtual storage objects.
 *   <li>Retrieve a virtual storage object state.
 *   <li>List all virtual storage objects or by matching queries.
 * </ol>
 *
 * <p>Sample Prerequisites: Existing VStorageObject ID
 */
public class FcdRetrieveOperations {
    private static final Logger log = LoggerFactory.getLogger(FcdRetrieveOperations.class);

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

    /** REQUIRED: UUID of the vstorageobject. */
    public static String vStorageObjectId = "vStorageObjectId";
    /** REQUIRED: Maximum number of virtual storage object IDs to return. */
    public static String maxResult = "maxResult";
    /** REQUIRED: The searchable field. */
    public static String queryField = "queryField";
    /** REQUIRED: The operator to compare with the searchable field. */
    public static String queryOperator = "queryOperator";
    /** REQUIRED: The value to compare with the searchable field. */
    public static String queryValue = "queryValue";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FcdRetrieveOperations.class, args);

        VcenterClientFactory clientFactory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = clientFactory.createClient(username, password, null)) {
            VslmPortType vslmPort = client.getVslmPort();

            ManagedObjectReference vStorageObjectManager =
                    client.getVslmServiceInstanceContent().getVStorageObjectManager();

            log.info("Invoking retrieve operations on virtual storage object from VSLM ::");

            // Retrieve vStorageObject
            log.info("Operation: Retrieve the vStorageObject from vslm");
            VStorageObject retrievedVStrObj =
                    vslmPort.vslmRetrieveVStorageObject(vStorageObjectManager, FcdHelper.makeId(vStorageObjectId));
            if (retrievedVStrObj != null) {
                log.info(
                        "Success: Retrieved vStorageObject :: \n [ UUid = {} ]\n vStorageObjectName \n [ Name = {} ]\n CapacityInMB \n [ Capacity = {} ]\n",
                        retrievedVStrObj.getConfig().getId().getId(),
                        retrievedVStrObj.getConfig().getName(),
                        retrievedVStrObj.getConfig().getCapacityInMB());
            } else {
                String msg = "Error: Retrieving VStorageObject [ " + vStorageObjectId + " ] from vslm.";
                throw new RuntimeException(msg);
            }

            // Retrieve vStorageObjects
            log.info("Operation: Retrieve the vStorageObjects from vslm");
            List<VslmVsoVStorageObjectResult> retrievedVStorageObjects = vslmPort.vslmRetrieveVStorageObjects(
                    vStorageObjectManager, List.of(FcdHelper.makeId(vStorageObjectId)));
            if (retrievedVStorageObjects != null) {
                log.info(
                        "Success: Retrieved vStorageObjects :: \n [ UUid = {} ]\n vStorageObjectName \n [ Name = {} ]\n CapacityInMB \n [ Capacity = {} ]\n",
                        retrievedVStorageObjects.get(0).getId().getId(),
                        retrievedVStorageObjects.get(0).getName(),
                        retrievedVStorageObjects.get(0).getCapacityInMB());
            } else {
                String msg = "Error: Retrieving VStorageObjects [ " + vStorageObjectId + " ] from vslm.";
                throw new RuntimeException(msg);
            }

            // Retrieve vStorageObject state
            log.info("Operation: Retrieve the vStorageObject state from datastore from vslm.");
            VStorageObjectStateInfo stateInfo =
                    vslmPort.vslmRetrieveVStorageObjectState(vStorageObjectManager, FcdHelper.makeId(vStorageObjectId));

            if (stateInfo != null) {
                log.info(
                        "Success: Retrieved state info [stateInfo = {} ] of vStorageObject :: [ {} ] from vslm.",
                        stateInfo.isTentative(),
                        vStorageObjectId);
            } else {
                String msg = "Error: Retrieving state info for VStorageObject [ " + vStorageObjectId + " ] from vslm.";
                throw new RuntimeException(msg);
            }

            log.info("Operation: lists all virtual storage object from vslm.");
            VslmVsoVStorageObjectQuerySpec querySpec = new VslmVsoVStorageObjectQuerySpec();
            querySpec.setQueryField(queryField);
            querySpec.setQueryOperator(queryOperator);
            querySpec.getQueryValue().add(queryValue);

            VslmVsoVStorageObjectQueryResult queryResult = null;
            // If queries are provided for particular virtual storage object
            if (queryField != null && queryOperator != null && queryValue != null) {
                queryResult = vslmPort.vslmListVStorageObjectForSpec(
                        vStorageObjectManager, List.of(querySpec), Integer.parseInt(maxResult));
            } else {
                // If no queries are provided
                queryResult = vslmPort.vslmListVStorageObjectForSpec(
                        vStorageObjectManager, null, Integer.parseInt(maxResult));
            }

            if (queryResult != null) {
                log.info("Success: List virtual storage objects :");
            } else {
                throw new RuntimeException("Error: List virtual storage objects");
            }

            log.info("List all vStorage objects IDs :");
            for (VslmVsoVStorageObjectResult results : queryResult.getQueryResults()) {
                log.info("vStorage object id :{}", results.getId().getId());
            }
        }
    }
}
