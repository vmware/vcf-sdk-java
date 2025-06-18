/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.compute.vm;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.DATACENTER;
import static com.vmware.vim25.ManagedObjectType.HOST_SYSTEM;
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/**
 * Demonstrates powering on operation of a virtual machine. Works with groups of virtual machines all at one time. You
 * must specify one of --vmname or --datacentername or --host or --all. All virtual machines that match these criteria
 * will have the power on operation issued to them. For example to power on all the visible virtual machines, use the
 * options --all true and all virtual machines will be powered on.
 */
public class PowerOnVm {
    private static final Logger log = LoggerFactory.getLogger(PowerOnVm.class);
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

    /**
     * OPTIONAL: Name of the virtual machine, use this option to send the power operation to only one virtual machine.
     */
    public static String vmName = null;
    /**
     * OPTIONAL: Name of the datacenter. Use this option to send power operations to all the virtual machines in an
     * entire data center.
     */
    public static String datacenterName = null;
    /** OPTIONAL: Guest id of the vm. Use this option to send power operations to a single guest. */
    public static String guestId = null;
    /**
     * OPTIONAL: Name of the host. Use this option to send power operations to all the virtual machines on a single
     * host.
     */
    public static String host = null;
    /**
     * OPTIONAL: Perform power operations on ALL virtual machines under our control. defaults to false. Set to true to
     * send power operation to all virtual machines that can be found. Overrides all other options.
     */
    public static Boolean all = false;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(PowerOnVm.class, args);

        validate();

        if (checkOptions()) {
            VcenterClientFactory factory =
                    new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

            try (VcenterClient client = factory.createClient(username, password, null)) {
                VimPortType vimPort = client.getVimPort();
                ServiceContent serviceContent = client.getVimServiceContent();
                PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

                Map<String, List<ManagedObjectReference>> vmMap = getVms(serviceContent, propertyCollectorHelper);
                if (vmMap == null || vmMap.isEmpty()) {
                    log.error("No Virtual Machine found matching the specified criteria");
                } else {
                    powerOnVM(vimPort, propertyCollectorHelper, vmMap);
                }
            }
        }
    }

    private static void powerOnVM(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            Map<String, List<ManagedObjectReference>> vmMap) {
        for (Map.Entry<String, List<ManagedObjectReference>> vm : vmMap.entrySet()) {
            String vmname = vm.getKey();
            if (vm.getValue().isEmpty()) {
                log.error("No managed object reference found for Virtual Machine with name:{}", vmname);
                continue;
            }

            ManagedObjectReference vmMoRef = vm.getValue().get(0);

            try {
                log.info("Powering on virtual machine : {}[{}]", vmname, vmMoRef.getValue());
                ManagedObjectReference taskMoRef = vimPort.powerOnVMTask(vmMoRef, null);
                if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                    log.info("[{}] powered on successfully: {}", vmname, vmMoRef.getValue());
                }
            } catch (Exception e) {
                log.error("Unable to power on vm : {}[{}]", vmname, vmMoRef.getValue());
                log.error("Exception: ", e);
            }
        }
    }

    private static void validate() throws IllegalArgumentException {
        if (all && (vmName != null || datacenterName != null || guestId != null || host != null)) {
            log.error(
                    "Did you really mean all? Use '--all true' by itself not with --vmname or --datacentername or --guestid or --host");
            throw new IllegalArgumentException("--all true occurred in conjunction with other options");
        }
    }

    /**
     * This could be a list of every Virtual Machine in an entire vCenter's control, or you can use --vmname to limit
     * the list to a single virtual machine. Or, you could use the --datacentername option to perform power operations
     * on every virtual machine in a datacenter or --host to perform power operations on every virtual machine on an ESX
     * host!
     */
    private static Map<String, List<ManagedObjectReference>> getVms(
            ServiceContent serviceContent, PropertyCollectorHelper propertyCollectorHelper)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        Map<String, List<ManagedObjectReference>> vmList = new HashMap<>();

        // Start from the root folder
        ManagedObjectReference container = serviceContent.getRootFolder();
        if (datacenterName != null) {
            ManagedObjectReference datacenterMoRef = propertyCollectorHelper.getMoRefByName(datacenterName, DATACENTER);
            if (datacenterMoRef == null) {
                log.error("No datacenter by the name {} found!", datacenterName);
            }
            container = datacenterMoRef;
        }

        if (host != null) {
            ManagedObjectReference hostMoRef = propertyCollectorHelper.getMoRefByName(container, host, HOST_SYSTEM);
            if (hostMoRef == null) {
                log.error("No host by the name {} found!", host);
                return vmList;
            }
            container = hostMoRef;
        }

        Map<String, List<ManagedObjectReference>> vms = propertyCollectorHelper.getObjects(container, VIRTUAL_MACHINE);

        if (vmName != null) {
            ManagedObjectReference vmMoRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);
            if (vmMoRef != null) {
                vmList.put(vmName, Collections.singletonList(vmMoRef));
            } else {
                throw new IllegalStateException("No VM by the name of '" + vmName + "' found!");
            }
            return vmList;
        }

        if (guestId != null) {
            Map<ManagedObjectReference, Map<String, Object>> vmListProp = propertyCollectorHelper.fetchProperties(
                    vms.values().stream()
                            .filter(v -> !v.isEmpty())
                            .map(v -> v.get(0))
                            .collect(Collectors.toList()),
                    "summary.config.guestId",
                    "name");

            for (Map.Entry<ManagedObjectReference, Map<String, Object>> e : vmListProp.entrySet()) {
                ManagedObjectReference vmRef = e.getKey();
                if (guestId.equalsIgnoreCase((String) e.getValue().get("summary.config.guestId"))) {
                    String key = (String) e.getValue().get("name");
                    vmList.computeIfAbsent(key, k -> new ArrayList<>()).add(vmRef);
                }
            }
            return vmList;
        }

        // If no filters are there then just the container based containment is used.
        vmList = vms;

        return vmList;
    }

    /**
     * The user must specify one of vmName or datacenter or host. We add this check here to help prevent programmers
     * from power cycling all the virtual machines on their entire vCenter server on accident.
     */
    private static boolean checkOptions() {
        boolean run;

        if (all) {
            if (vmName != null) {
                throw new RuntimeException("--all true cannot be used with custom --vmName");
            }
            if (datacenterName != null) {
                throw new RuntimeException("--all true cannot be used with custom --datacenterName");
            }
            if (host != null) {
                throw new RuntimeException("--all true cannot be used with custom --host");
            }
            log.info("Power on operation will be broadcast to ALL virtual machines.");
            run = true;
        } else if (vmName == null
                && datacenterName == null
                && guestId == null
                && host == null
                && System.console() != null) {
            throw new IllegalStateException("You must specify one of --vmname or --datacentername or --host or --all");
        } else {
            run = true;
        }

        return run;
    }
}
