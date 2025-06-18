/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.appliance.health;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.List;

import com.vmware.appliance.Health;
import com.vmware.appliance.Notification;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vapi.std.LocalizableMessage;

/** Demonstrates getting Health messages for various health items. */
public class HealthMessages {
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

    /** REQUIRED: Specify the name of health item to view the messages. */
    public static String item = "item";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(HealthMessages.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Health healthService = client.createStub(Health.class);

            List<Notification> messages = healthService.messages(item);
            if (messages.isEmpty()) {
                System.out.println("No health alarms for : " + item);
            } else {
                System.out.println("Health Alarms");
                System.out.println("-------------\n");

                for (Notification message : messages) {
                    System.out.println("------------------------------------------" + "-------------");
                    System.out.println("Alert time : " + message.getTime().getTime());
                    System.out.println("Alert message Id: " + message.getId());

                    LocalizableMessage msg = message.getMessage();
                    System.out.println("Alert message : " + msg.getDefaultMessage());

                    LocalizableMessage resolution = message.getResolution();
                    System.out.println("Resolution : " + resolution.getDefaultMessage());
                    System.out.println("-------------------------------------------" + "-------------");
                }
            }
        }
    }
}
