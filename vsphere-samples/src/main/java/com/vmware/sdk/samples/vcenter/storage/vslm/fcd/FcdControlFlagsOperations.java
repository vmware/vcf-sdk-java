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

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.storage.fcd.helpers.FcdHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vslm.VslmPortType;

/**
 * This sample executes control flag related operations on a given VStorageObject from vslm:
 *
 * <ol>
 *   <li>Set control flags on VStorageObject.
 *   <li>Clear control flags on VStorageObject.
 * </ol>
 *
 * <p>Sample Prerequisites: Existing VStorageObject ID
 */
public class FcdControlFlagsOperations {
    private static final Logger log = LoggerFactory.getLogger(FcdControlFlagsOperations.class);

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
    /** REQUIRED: Set control flags on VStorageObject. */
    public static String controlFlags = "controlFlags";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FcdControlFlagsOperations.class, args);

        VcenterClientFactory clientFactory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = clientFactory.createClient(username, password, null)) {
            log.info("Invoking control flag operations on VStorageObject from VSLM ::");

            VslmPortType vslmPort = client.getVslmPort();

            ManagedObjectReference vStorageObjMgrMoRef =
                    client.getVslmServiceInstanceContent().getVStorageObjectManager();

            // Retrieve a vStorageObject
            log.info("Operation: Setting control flags on VStorageObject from vslm : {}", controlFlags);
            // Set control flags on VStorageObject
            vslmPort.vslmSetVStorageObjectControlFlags(
                    vStorageObjMgrMoRef, FcdHelper.makeId(vStorageObjectId), Collections.singletonList(controlFlags));
            log.info(
                    "Control flag [flag = {} ] set for vStorageObject : \n [ Uuid = {} ]\n from vslm.",
                    controlFlags,
                    vStorageObjectId);

            // Clear control flags on VStorageObject
            log.info("Operation: Clearing control flags on VStorageObject from vslm.");
            vslmPort.vslmClearVStorageObjectControlFlags(
                    vStorageObjMgrMoRef, FcdHelper.makeId(vStorageObjectId), Collections.singletonList(controlFlags));
            log.info(
                    "Control flag [flag = {}] cleared from vStorageObject : \n [ Uuid = {} ] \n from vslm.",
                    controlFlags,
                    vStorageObjectId);
        }
    }
}
