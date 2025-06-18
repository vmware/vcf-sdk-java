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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.HostConfigManager;
import com.vmware.vim25.HostIpConfig;
import com.vmware.vim25.HostVirtualNicSpec;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/** This sample is used to add a Virtual Nic to a PortGroup or a DVPortGroup. */
public class AddVirtualNic {
    private static final Logger log = LoggerFactory.getLogger(AddVirtualNic.class);

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
    /** REQUIRED: Name of the port group. */
    public static String portgroupName = "portgroupName";
    /** OPTIONAL: IP address for the NIC, if not set - DHCP will be in effect for the nic. */
    public static String ipaddress = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(AddVirtualNic.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference hostMoRef = propertyCollectorHelper.getMoRefByName(hostname, HOST_SYSTEM);

            if (hostMoRef == null) {
                log.error("Host {} not found", hostname);
                return;
            }

            ArrayOfManagedObjectReference hostNetworkMoRefs = propertyCollectorHelper.fetch(hostMoRef, "network");

            ManagedObjectReference targetPortgroupMoRef = null;
            for (ManagedObjectReference portGroup : hostNetworkMoRefs.getManagedObjectReference()) {
                if (portgroupName.equals(propertyCollectorHelper.fetch(portGroup, "name"))) {
                    targetPortgroupMoRef = portGroup;
                }
            }

            if (targetPortgroupMoRef == null) {
                log.error("Portgroup {} not found", portgroupName);
                return;
            }

            HostConfigManager configMgr = propertyCollectorHelper.fetch(hostMoRef, "configManager");
            ManagedObjectReference nwSystemMoRef = configMgr.getNetworkSystem();

            HostVirtualNicSpec vNicSpec = createVirtualNicSpecification(propertyCollectorHelper, targetPortgroupMoRef);

            String vNic;
            if (targetPortgroupMoRef.getType().equals("DistributedVirtualPortgroup")) {
                vNic = vimPort.addVirtualNic(nwSystemMoRef, "", vNicSpec);
            } else {
                vNic = vimPort.addVirtualNic(nwSystemMoRef, portgroupName, vNicSpec);
            }
            log.info("Successful in creating nic : {} with PortGroup :{}", vNic, portgroupName);
        }
    }

    private static HostVirtualNicSpec createVirtualNicSpecification(
            PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference portGroupMoRef)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        HostIpConfig hostIpConfig = new HostIpConfig();

        if (ipaddress != null && !ipaddress.isEmpty()) {
            hostIpConfig.setDhcp(Boolean.FALSE);
            hostIpConfig.setIpAddress(ipaddress);
            hostIpConfig.setSubnetMask("255.255.255.0");
        } else {
            hostIpConfig.setDhcp(Boolean.TRUE);
        }

        HostVirtualNicSpec hostVirtualNicSpec = new HostVirtualNicSpec();
        hostVirtualNicSpec.setIp(hostIpConfig);

        if (portGroupMoRef.getType().equals("DistributedVirtualPortgroup")) {
            Map<String, Object> result =
                    propertyCollectorHelper.fetchProperties(portGroupMoRef, "config.distributedVirtualSwitch", "key");

            ManagedObjectReference dvsMoRef = (ManagedObjectReference) result.get("config.distributedVirtualSwitch");
            String dvsUuid = propertyCollectorHelper.fetch(dvsMoRef, "uuid");

            DistributedVirtualSwitchPortConnection dvsConnection = new DistributedVirtualSwitchPortConnection();
            dvsConnection.setPortgroupKey((String) result.get("key"));
            dvsConnection.setSwitchUuid(dvsUuid);

            hostVirtualNicSpec.setDistributedVirtualPort(dvsConnection);
        }
        return hostVirtualNicSpec;
    }
}
