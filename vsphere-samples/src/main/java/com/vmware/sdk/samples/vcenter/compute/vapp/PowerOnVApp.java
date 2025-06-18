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

/** Demonstrates power on operation on a specified Virtual App. */
public class PowerOnVApp {
    private static final Logger log = LoggerFactory.getLogger(PowerOnVApp.class);
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

    /** REQUIRED: Name of the vApp to be powered on. */
    public static String vAppName = "vAppName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(PowerOnVApp.class, args);

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
                powerOnVApp(vimPort, propertyCollectorHelper, vAppName, vAppMoRef);
            }
        }
    }

    /** Powers on vApp and waits for the powerOn operation to complete. */
    private static void powerOnVApp(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            String vAppName,
            ManagedObjectReference vAppMoRef) {
        log.info("Powering on Virtual App : {}[{}]", vAppName, vAppMoRef.getValue());
        try {
            ManagedObjectReference taskMoRef = vimPort.powerOnVAppTask(vAppMoRef);
            if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                log.info("[{}] powered on successfully, {}", vAppName, vAppMoRef.getValue());
            } else {
                log.error("Unable to power on vApp : {}[{}]", vAppName, vAppMoRef.getValue());
            }
        } catch (Exception e) {
            log.error("Unable to power on vApp : {}[{}]", vAppName, vAppMoRef.getValue());
            log.error("Reason :{}", e.getLocalizedMessage());
        }
    }
}
