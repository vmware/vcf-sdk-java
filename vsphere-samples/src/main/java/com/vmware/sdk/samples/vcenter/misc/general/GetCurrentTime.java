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
import static com.vmware.sdk.vsphere.utils.VimClient.getVimServiceInstanceRef;

import java.text.SimpleDateFormat;

import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.ESXiClient;
import com.vmware.sdk.vsphere.utils.ESXiClientFactory;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.VimPortType;

/** This sample gets the current time of the vSphere Server. */
public class GetCurrentTime {
    private static final Logger log = LoggerFactory.getLogger(GetCurrentTime.class);
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

    public static void main(String[] args) throws RuntimeFaultFaultMsg {
        SampleCommandLineParser.load(GetCurrentTime.class, args);

        ESXiClientFactory factory = new ESXiClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (ESXiClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            XMLGregorianCalendar ct = vimPort.currentTime(getVimServiceInstanceRef());

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd 'T' HH:mm:ss.SSSZ");
            log.info(
                    "Server current time: {}",
                    sdf.format(ct.toGregorianCalendar().getTime()));
        }
    }
}
