/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.compute.vapp;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_APP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/** Demonstrates power off operation on a specified Virtual App. */
public class PowerOffVApp {
    private static final Logger log = LoggerFactory.getLogger(PowerOffVApp.class);
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

    /** REQUIRED: Name of the vApp to be powered off. */
    public static String vAppName = "vAppName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(PowerOffVApp.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference vAppMoRef = propertyCollectorHelper.getMoRefByName(vAppName, VIRTUAL_APP);
            if (vAppMoRef == null) {
                log.error("No Virtual App found with name: {}", vAppName);
            } else {
                powerOffVApp(vimPort, propertyCollectorHelper, vAppName, vAppMoRef);
            }
        }
    }

    /** Powers off vApp and waits for the power off operation to complete. */
    private static void powerOffVApp(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            String vAppName,
            ManagedObjectReference vAppMoRef) {
        log.info("Powering off Virtual App : {}[{}]", vAppName, vAppMoRef.getValue());
        try {
            ManagedObjectReference taskMoRef = vimPort.powerOffVAppTask(vAppMoRef, true);
            if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                log.info("[{}] powered off successfully, {}", vAppName, vAppMoRef.getValue());
            } else {
                log.error("Unable to power off vApp : {}[{}]", vAppName, vAppMoRef.getValue());
            }
        } catch (Exception e) {
            log.error("Unable to power off vApp : {}[{}]", vAppName, vAppMoRef.getValue());
            log.error("Reason :{}", e.getLocalizedMessage());
        }
    }
}
