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
import com.vmware.vim25.HostNetworkPolicy;
import com.vmware.vim25.HostPortGroupSpec;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/** This sample is used to add a Virtual Machine Port Group to a vSwitch. */
public class AddVirtualSwitchPortGroup {
    private static final Logger log = LoggerFactory.getLogger(AddVirtualSwitchPortGroup.class);
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

    /** OPTIONAL: Name of the host. */
    public static String hostname = null;
    /** REQUIRED: Name of the port group. */
    public static String portgroupName = "portgroupName";
    /** REQUIRED: Name of the vSwitch to add portgroup to. */
    public static String vSwitchId = "vSwitchId";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(AddVirtualSwitchPortGroup.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference hostMoRef = propertyCollectorHelper.getMoRefByName(hostname, HOST_SYSTEM);
            try {
                if (hostMoRef != null) {
                    HostConfigManager configManager = propertyCollectorHelper.fetch(hostMoRef, "configManager");
                    ManagedObjectReference nwSystem = configManager.getNetworkSystem();

                    HostPortGroupSpec hostPortGroupSpec = new HostPortGroupSpec();
                    hostPortGroupSpec.setName(portgroupName);
                    hostPortGroupSpec.setVswitchName(vSwitchId);
                    hostPortGroupSpec.setPolicy(new HostNetworkPolicy());

                    vimPort.addPortGroup(nwSystem, hostPortGroupSpec);

                    log.info("Successfully created : {}/{}", vSwitchId, portgroupName);
                } else {
                    log.error("Host not found");
                }
            } catch (AlreadyExistsFaultMsg ex) {
                log.error("Failed creating : {}/{}", vSwitchId, portgroupName);
                log.info("Portgroup name already exists");
            } catch (HostConfigFaultFaultMsg ex) {
                log.error("Failed : Configuration failures. Reason : {}", ex.getMessage());
            } catch (Exception ex) {
                log.error("Failed creating : {}/{}", vSwitchId, portgroupName);
            }
        }
    }
}
