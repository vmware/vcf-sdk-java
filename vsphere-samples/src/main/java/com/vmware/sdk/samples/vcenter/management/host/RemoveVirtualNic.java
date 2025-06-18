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
import static com.vmware.vim25.ManagedObjectType.DATACENTER;
import static com.vmware.vim25.ManagedObjectType.HOST_SYSTEM;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayOfHostVirtualNic;
import com.vmware.vim25.HostConfigFaultFaultMsg;
import com.vmware.vim25.HostConfigManager;
import com.vmware.vim25.HostVirtualNic;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/** This sample removes a Virtual Nic from a PortGroup on a vSwitch. */
public class RemoveVirtualNic {
    private static final Logger log = LoggerFactory.getLogger(RemoveVirtualNic.class);

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

    /** OPTIONAL: Name of the datacenter. */
    public static String datacenterName = null;
    /** REQUIRED: Name of the host. */
    public static String hostname = "hostname";
    /** REQUIRED: Name of port group to remove Virtual Nic from. */
    public static String portgroupName = "portgroupName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(RemoveVirtualNic.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            try {
                ManagedObjectReference hostMoRef = null;
                if (((datacenterName != null) && (hostname != null))
                        || ((datacenterName != null) && (hostname == null))) {
                    ManagedObjectReference datacenterMoRef =
                            propertyCollectorHelper.getMoRefByName(datacenterName, DATACENTER);
                    if (datacenterMoRef == null) {
                        log.error("Datacenter with name:{} not found", datacenterName);
                        return;
                    }

                    ManagedObjectReference hostFolderMoRef =
                            propertyCollectorHelper.fetch(datacenterMoRef, "hostFolder");

                    hostMoRef = propertyCollectorHelper.getMoRefByName(hostFolderMoRef, hostname, HOST_SYSTEM);
                } else if ((datacenterName == null) && (hostname != null)) {
                    hostMoRef = propertyCollectorHelper.getMoRefByName(hostname, HOST_SYSTEM);
                }

                if (hostMoRef != null) {
                    HostConfigManager configManager = propertyCollectorHelper.fetch(hostMoRef, "configManager");

                    ManagedObjectReference nwSystemMoRef = configManager.getNetworkSystem();

                    ArrayOfHostVirtualNic arrayHostVirtualNic =
                            propertyCollectorHelper.fetch(nwSystemMoRef, "networkInfo.vnic");

                    List<HostVirtualNic> hostVirtualNic = arrayHostVirtualNic.getHostVirtualNic();

                    boolean foundOne = false;
                    for (HostVirtualNic nic : hostVirtualNic) {
                        String portGroup = nic.getPortgroup();
                        if (portGroup.equals(portgroupName)) {
                            vimPort.removeVirtualNic(nwSystemMoRef, nic.getDevice());
                            foundOne = true;
                        }
                    }
                    if (foundOne) {
                        log.info("Successfully removed virtual nic from portgroup : {}", portgroupName);
                    } else {
                        log.info("No virtual nic found on portgroup : {}", portgroupName);
                    }
                } else {
                    log.error("Host not found");
                }
            } catch (HostConfigFaultFaultMsg ex) {
                log.error("Failed : Configuration failures.");
            } catch (Exception e) {
                log.error("Failed :", e);
            }
        }
    }
}
