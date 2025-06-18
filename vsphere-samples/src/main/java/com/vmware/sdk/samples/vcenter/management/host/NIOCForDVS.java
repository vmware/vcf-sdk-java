/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.host;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.VMWARE_DISTRIBUTED_VIRTUAL_SWITCH;

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
 * This sample demonstrates how to add/modify NetworkResourcePool to Distributed Virtual Switch.
 *
 * <p>Sample prerequisites: Sample only works for DVS 5.0 onwards.
 */
public class NIOCForDVS {
    private static final Logger log = LoggerFactory.getLogger(NIOCForDVS.class);
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

    /** REQUIRED: Distributed Virtual Switch name. */
    public static String dvsName = "dvsName";
    /** REQUIRED: If true, enables I/O control. If false, disables network I/O control. */
    public static String enableNIOC = "enableNIOC";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(NIOCForDVS.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference dvsMoRef =
                    propertyCollectorHelper.getMoRefByName(dvsName, VMWARE_DISTRIBUTED_VIRTUAL_SWITCH);
            if (dvsMoRef != null) {
                vimPort.enableNetworkResourceManagement(dvsMoRef, Boolean.parseBoolean(enableNIOC));
                log.info("Set network I/O control");
            } else {
                log.error("DVS Switch {} Not Found", dvsName);
            }
        }
    }
}
