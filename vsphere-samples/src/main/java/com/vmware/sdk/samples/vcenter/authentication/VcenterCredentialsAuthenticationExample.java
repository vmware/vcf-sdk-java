/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.authentication;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.sdk.vsphere.utils.VimClient.getVimServiceInstanceRef;

import java.text.SimpleDateFormat;

import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appliance.system.Version;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.VimPortType;

public class VcenterCredentialsAuthenticationExample {

    private static final Logger log = LoggerFactory.getLogger(VcenterCredentialsAuthenticationExample.class);

    /** REQUIRED: vCenter FQDN or IP address. */
    public static String serverAddress = "vcenter1.mycompany.com";

    /** REQUIRED: STS username used for authentication. */
    public static String username = "Administrator@vsphere.local";

    /** REQUIRED: Password for the {@link #username}. */
    public static String password = "password";

    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (DO NOT USE IN PRODUCTION ENVIRONMENTS).
     */
    public static String trustStorePath = null;

    public static void main(String[] args) throws Exception {

        SampleCommandLineParser.load(VcenterCredentialsAuthenticationExample.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            XMLGregorianCalendar time = vimPort.currentTime(getVimServiceInstanceRef());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd 'T' HH:mm:ss.SSSZ");
            log.info(
                    "Server current time: {}",
                    sdf.format(time.toGregorianCalendar().getTime()));

            Version version = client.createStub(Version.class);
            log.info("Server version: {}", version.get().getVersion());
        }
    }
}
