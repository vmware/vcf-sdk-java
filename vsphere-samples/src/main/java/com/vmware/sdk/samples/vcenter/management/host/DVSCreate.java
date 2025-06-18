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
import static com.vmware.vim25.ManagedObjectType.VMWARE_DISTRIBUTED_VIRTUAL_SWITCH;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.DVPortgroupConfigSpec;
import com.vmware.vim25.DVSCapability;
import com.vmware.vim25.DVSConfigSpec;
import com.vmware.vim25.DVSCreateSpec;
import com.vmware.vim25.DistributedVirtualSwitchHostProductSpec;
import com.vmware.vim25.DistributedVirtualSwitchProductSpec;
import com.vmware.vim25.DuplicateNameFaultMsg;
import com.vmware.vim25.DvsFaultFaultMsg;
import com.vmware.vim25.DvsNotAuthorizedFaultMsg;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidNameFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NotFoundFaultMsg;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/** This sample demonstrates how to create/update Distributed Virtual Switch. */
public class DVSCreate {
    private static final Logger log = LoggerFactory.getLogger(DVSCreate.class);
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

    /** OPTIONAL: Datacenter name. */
    public static String dcName = null;
    /** REQUIRED: Distributed Virtual Switch name. */
    public static String dvsName = "dvsName";
    /** OPTIONAL: Description string of the switch. */
    public static String dvsDesc = null;
    /** OPTIONAL: Distributed Virtual Switch either 4.0, 4.1.0, 5.0.0 or 5.1.0. */
    public static String dvsVersion = null;
    /** OPTIONAL: Number of ports in the portgroup. */
    public static String noOfPorts = null;
    /** OPTIONAL: Name of the port group. */
    public static String portGroupName = null;
    /** REQUIRED: "createdvs" for creating a new DVS, "addportgroup" for adding a port group to DVS. */
    public static String option = "option";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(DVSCreate.class, args);

        validate();

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            if (option.equals("createdvs")) {
                createDVS(vimPort, propertyCollectorHelper, serviceContent, dcName, dvsName, dvsDesc, dvsVersion);
            } else if (option.equals("addportgroup")) {
                addPortGroup(vimPort, propertyCollectorHelper, dvsName, Integer.parseInt(noOfPorts), portGroupName);
            } else {
                throw new IllegalArgumentException("unknown option: " + option);
            }
        }
    }

    // Get input parameters to run the sample
    private static void validate() {
        if (option != null) {
            if (!(option.equals("createdvs") || option.equals("addportgroup"))) {
                throw new IllegalArgumentException("Expected valid --option. createdvs" + " or addportgroup");
            }
        } else {
            throw new IllegalArgumentException("Expected --option argument. createdvs" + " or addportgroup");
        }

        if (option.equals("createdvs")) {
            if (dcName == null || dvsName == null) {
                throw new IllegalArgumentException("Expected --dcname and --dvsname arguments");
            }
        }

        if (option.equals("addportgroup")) {
            if (dvsName == null || noOfPorts == null || portGroupName == null) {
                throw new IllegalArgumentException("Expected --dvsname, --noofports and --portgroupname arguments");
            }
        }
    }

    // Create DVSConfigSpec for creating a DVS.
    private static DVSConfigSpec getDVSConfigSpec(String dvsName, String dvsDesc) {
        DVSConfigSpec dvsConfigSpec = new DVSConfigSpec();
        dvsConfigSpec.setName(dvsName);
        if (dvsDesc != null) {
            dvsConfigSpec.setDescription(dvsDesc);
        }
        return dvsConfigSpec;
    }

    // Fetch DistributedVirtualSwitchProductSpec.
    private static DistributedVirtualSwitchProductSpec getDVSProductSpec(
            VimPortType vimPort, ServiceContent serviceContent, String version) throws RuntimeFaultFaultMsg {
        List<DistributedVirtualSwitchProductSpec> dvsProdSpec =
                vimPort.queryAvailableDvsSpec(serviceContent.getDvSwitchManager(), null);

        DistributedVirtualSwitchProductSpec dvsSpec = null;
        if (version != null) {
            for (DistributedVirtualSwitchProductSpec prodSpec : dvsProdSpec) {
                if (version.equalsIgnoreCase(prodSpec.getVersion())) {
                    dvsSpec = prodSpec;
                }
            }
            if (dvsSpec == null) {
                throw new IllegalArgumentException("DVS Version " + version + " not supported.");
            }
        } else {
            dvsSpec = dvsProdSpec.get(dvsProdSpec.size() - 1);
        }
        return dvsSpec;
    }

    /**
     * Create a Distributed Virtual Switch.
     *
     * @param dcName The Datacenter name
     * @param dvsName The DVS name
     * @param dvsDesc The DVS description
     * @param version Dot-separated version string
     */
    private static void createDVS(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            ServiceContent serviceContent,
            String dcName,
            String dvsName,
            String dvsDesc,
            String version)
            throws InvalidCollectorVersionFaultMsg, RuntimeFaultFaultMsg, InvalidPropertyFaultMsg,
                    DuplicateNameFaultMsg, DvsFaultFaultMsg, DvsNotAuthorizedFaultMsg, InvalidNameFaultMsg,
                    NotFoundFaultMsg {
        DistributedVirtualSwitchProductSpec dvsProdSpec = getDVSProductSpec(vimPort, serviceContent, version);

        ManagedObjectReference datacenterMoRef = propertyCollectorHelper.getMoRefByName(dcName, DATACENTER);
        if (datacenterMoRef == null) {
            log.error("Datacenter {} not found.", dcName);
            return;
        }

        ManagedObjectReference networkMoRef = propertyCollectorHelper.fetch(datacenterMoRef, "networkFolder");

        List<DistributedVirtualSwitchHostProductSpec> dvsHostProdSpec =
                vimPort.queryDvsCompatibleHostSpec(serviceContent.getDvSwitchManager(), dvsProdSpec);
        DVSCapability dvsCapability = new DVSCapability();
        dvsCapability.getCompatibleHostComponentProductInfo().addAll(dvsHostProdSpec);

        DVSCreateSpec dvsCreateSpec = new DVSCreateSpec();
        dvsCreateSpec.setCapability(dvsCapability);
        dvsCreateSpec.setConfigSpec(getDVSConfigSpec(dvsName, dvsDesc));
        dvsCreateSpec.setProductInfo(dvsProdSpec);

        ManagedObjectReference taskMoRef = vimPort.createDVSTask(networkMoRef, dvsCreateSpec);

        if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
            log.info("Success: Creating Distributed Virtual Switch");
        } else {
            throw new RuntimeException("Failure: Creating Distributed Virtual Switch");
        }
    }

    /**
     * Add a DistributedVirtualPortgroup to the switch.
     *
     * @param dvSwitchName The DVS name
     * @param numOfPorts Number of ports in the portgroup
     * @param portGroupName The name of the portgroup
     */
    private static void addPortGroup(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            String dvSwitchName,
            int numOfPorts,
            String portGroupName)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, DuplicateNameFaultMsg, DvsFaultFaultMsg,
                    InvalidNameFaultMsg, InvalidCollectorVersionFaultMsg {
        ManagedObjectReference dvsMoRef =
                propertyCollectorHelper.getMoRefByName(dvSwitchName, VMWARE_DISTRIBUTED_VIRTUAL_SWITCH);
        if (dvsMoRef != null) {
            DVPortgroupConfigSpec portGroupConfigSpec = new DVPortgroupConfigSpec();

            portGroupConfigSpec.setName(portGroupName);
            portGroupConfigSpec.setNumPorts(numOfPorts);
            portGroupConfigSpec.setType("earlyBinding");

            List<DVPortgroupConfigSpec> listDVSPortConfigSpec = new ArrayList<>();
            listDVSPortConfigSpec.add(portGroupConfigSpec);

            ManagedObjectReference taskMoRef = vimPort.addDVPortgroupTask(dvsMoRef, listDVSPortConfigSpec);

            if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                log.info("Success: Adding Port Group");
            } else {
                throw new RuntimeException("Failure: Adding Port Group");
            }
        } else {
            log.error("DVS Switch {} Not Found", dvSwitchName);
        }
    }
}
