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
import static com.vmware.vim25.ManagedObjectType.HOST_SYSTEM;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.AlreadyExistsFaultMsg;
import com.vmware.vim25.HostConfigFaultFaultMsg;
import com.vmware.vim25.HostConfigManager;
import com.vmware.vim25.HostVirtualSwitchSpec;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/** This sample is used to add a virtual switch. */
public class AddVirtualSwitch {
    private static final Logger log = LoggerFactory.getLogger(AddVirtualSwitch.class);
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

    /** REQUIRED: Name of the host. */
    public static String hostname = "hostname";
    /** REQUIRED: Name of the switch to be added. */
    public static String vSwitchId = "vSwitchId";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(AddVirtualSwitch.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference hostMoRef = propertyCollectorHelper.getMoRefByName(hostname, HOST_SYSTEM);
            if (hostMoRef != null) {
                try {
                    HostConfigManager configManager = propertyCollectorHelper.fetch(hostMoRef, "configManager");
                    ManagedObjectReference nwSystem = configManager.getNetworkSystem();

                    HostVirtualSwitchSpec spec = new HostVirtualSwitchSpec();
                    spec.setNumPorts(8);

                    vimPort.addVirtualSwitch(nwSystem, vSwitchId, spec);
                    log.info("Successfully created vSwitch : {}", vSwitchId);
                } catch (AlreadyExistsFaultMsg ex) {
                    log.error("Failed : Switch already exists Reason : {}", ex.getMessage());
                } catch (HostConfigFaultFaultMsg ex) {
                    log.error("Failed : Configuration failures. Reason : {}", ex.getMessage());
                } catch (Exception ex) {
                    log.error("Failed adding switch: {} Reason : {}", vSwitchId, ex.getMessage());
                }
            } else {
                log.error("Host not found");
            }
        }
    }
}
