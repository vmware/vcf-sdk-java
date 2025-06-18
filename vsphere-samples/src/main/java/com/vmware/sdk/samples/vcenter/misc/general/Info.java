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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.AboutInfo;

/**
 * Demonstrates how to use the AboutInfo object to find out more about the vSphere WS API end-point you are currently
 * connected to. Prints information about the vSphere WS API end-point you are currently connected to. This sample can
 * be used with an ESX, ESXi, or vCenter vSphere WS API end-point.
 *
 * <p>Note: ESX and ESXi hosts are reported as HostAgent connections.
 */
public class Info {
    private static final Logger log = LoggerFactory.getLogger(Info.class);
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

    public static void main(String[] args) {
        SampleCommandLineParser.load(Info.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            AboutInfo aboutInfo = client.getVimServiceContent().getAbout();
            log.info("Host {} is running SDK version: {} \n", serverAddress, aboutInfo.getVersion());
            log.info("Host {} is a {} \n", serverAddress, aboutInfo.getApiType());
        }
    }
}
