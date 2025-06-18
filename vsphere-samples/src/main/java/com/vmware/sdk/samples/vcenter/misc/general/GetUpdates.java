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
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.ObjectUpdate;
import com.vmware.vim25.ObjectUpdateKind;
import com.vmware.vim25.PropertyChange;
import com.vmware.vim25.PropertyChangeOp;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertyFilterUpdate;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.UpdateSet;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.WaitOptions;

/**
 * This sample demonstrates how to use the PropertyCollector to monitor one or more properties of one or more managed
 * objects. In particular this sample monitors all or one Virtual Machine for changes to some basic properties.
 */
public class GetUpdates {
    private static final Logger log = LoggerFactory.getLogger(GetUpdates.class);
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
     * OPTIONAL: Name of the virtual machine. If not defined sample will get the updates for all the VM under VMfolder.
     */
    public static String virtualMachineName = null;

    /** OPTIONAL: Number of update iterations to perform. Default value 1. */
    public static Integer iterations = null;

    /** OPTIONAL: Maximum time to wait in seconds for a new update. Default value is 10. */
    public static Integer waitForUpdateInSeconds = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(GetUpdates.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            getUpdates(vimPort, serviceContent, propertyCollectorHelper, serviceContent.getPropertyCollector());
        }
    }

    /**
     * @return An array of SelectionSpec covering the entity that provide performance statistics for Virtual Machine.
     */
    private static SelectionSpec[] buildVMTraversal() {
        // Terminal traversal specs
        // For Folder -> Folder recursion
        SelectionSpec visitFoldersSpec = new SelectionSpec();
        visitFoldersSpec.setName("VisitFolders");
        // DC -> VMF
        TraversalSpec dcToVmFolder = new TraversalSpec();
        dcToVmFolder.setType("Datacenter");
        dcToVmFolder.setSkip(Boolean.FALSE);
        dcToVmFolder.setPath("vmFolder");
        dcToVmFolder.setName("dcToVmf");
        dcToVmFolder.getSelectSet().add(visitFoldersSpec);

        // Folder -> Child
        TraversalSpec visitFolders = new TraversalSpec();
        visitFolders.setType("Folder");
        visitFolders.setPath("childEntity");
        visitFolders.setSkip(Boolean.FALSE);
        visitFolders.setName("VisitFolders");

        List<SelectionSpec> visitFoldersSpecs = new ArrayList<>();
        visitFoldersSpecs.add(dcToVmFolder);
        visitFoldersSpecs.add(visitFoldersSpec);

        visitFolders.getSelectSet().addAll(visitFoldersSpecs);
        return new SelectionSpec[] {visitFolders};
    }

    private static void getUpdates(
            VimPortType vimPort,
            ServiceContent serviceContent,
            PropertyCollectorHelper propertyCollectorHelper,
            ManagedObjectReference propCollectorRef)
            throws RuntimeFaultFaultMsg, InvalidCollectorVersionFaultMsg, InvalidPropertyFaultMsg {
        ManagedObjectReference vmMoRef = propertyCollectorHelper.getMoRefByName(virtualMachineName, VIRTUAL_MACHINE);
        if (vmMoRef == null && virtualMachineName != null) {
            log.error("Virtual Machine {} Not Found", virtualMachineName);
            return;
        }

        String[][] typeInfo = {new String[] {"VirtualMachine", "name", "runtime"}};
        List<PropertySpec> propertySpecs = buildPropertySpecArray(typeInfo);
        List<ObjectSpec> objectSpecs = new ArrayList<>();

        boolean oneOnly = vmMoRef != null;
        ObjectSpec objectSpec = new ObjectSpec();
        objectSpec.setObj(oneOnly ? vmMoRef : serviceContent.getRootFolder());
        objectSpec.setSkip(!oneOnly);
        if (!oneOnly) {
            objectSpec.getSelectSet().addAll(Arrays.asList(buildVMTraversal()));
        }
        objectSpecs.add(objectSpec);

        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
        propertyFilterSpec.getPropSet().addAll(propertySpecs);
        propertyFilterSpec.getObjectSet().addAll(objectSpecs);

        ManagedObjectReference propertyFilter = vimPort.createFilter(propCollectorRef, propertyFilterSpec, false);

        String version = "";
        int updatesRemaining = iterations != null ? iterations : 1;

        while (updatesRemaining-- > 0) {
            WaitOptions opts = new WaitOptions();
            opts.setMaxWaitSeconds(waitForUpdateInSeconds != null ? waitForUpdateInSeconds : 10);

            UpdateSet update = vimPort.waitForUpdatesEx(propCollectorRef, version, opts);
            if (update != null && update.getFilterSet() != null) {
                handleUpdate(update);
                version = update.getVersion();
            } else {
                log.info("No update is present!");
            }
        }
        vimPort.destroyPropertyFilter(propertyFilter);
    }

    private static void handleUpdate(UpdateSet update) {
        List<ObjectUpdate> vmUpdates = new ArrayList<>();
        List<PropertyFilterUpdate> propFilterUpdates = update.getFilterSet();

        for (PropertyFilterUpdate pfu : propFilterUpdates) {
            List<ObjectUpdate> objectUpdates = pfu.getObjectSet();
            for (ObjectUpdate oup : objectUpdates) {
                if (oup.getObj().getType().equals("VirtualMachine")) {
                    vmUpdates.add(oup);
                }
            }
        }

        if (!vmUpdates.isEmpty()) {
            log.info("Virtual Machine updates:");
            for (ObjectUpdate up : vmUpdates) {
                handleObjectUpdate(up);
            }
        }
    }

    private static void handleObjectUpdate(ObjectUpdate oUpdate) {
        List<PropertyChange> propertyChanges = oUpdate.getChangeSet();
        if (oUpdate.getKind() == ObjectUpdateKind.ENTER) {
            log.info("New Data:");
            handleChanges(propertyChanges);
        } else if (oUpdate.getKind() == ObjectUpdateKind.LEAVE) {
            log.info("Removed Data:");
            handleChanges(propertyChanges);
        } else if (oUpdate.getKind() == ObjectUpdateKind.MODIFY) {
            log.info("Changed Data:");
            handleChanges(propertyChanges);
        }
    }

    private static void handleChanges(List<PropertyChange> changes) {
        for (PropertyChange change : changes) {
            String name = change.getName();
            Object value = change.getVal();
            PropertyChangeOp op = change.getOp();

            if (op != PropertyChangeOp.REMOVE) {
                log.info("Property Name: {}", name);
                if ("runtime".equals(name)) {
                    if (value instanceof VirtualMachineRuntimeInfo) {
                        VirtualMachineRuntimeInfo vmRuntimeInfo = (VirtualMachineRuntimeInfo) value;
                        log.info("Power State: {}", vmRuntimeInfo.getPowerState());
                        log.info("Connection State: {}", vmRuntimeInfo.getConnectionState());

                        XMLGregorianCalendar bTime = vmRuntimeInfo.getBootTime();
                        if (bTime != null) {
                            log.info(
                                    "Boot Time: {}", bTime.toGregorianCalendar().getTime());
                        }

                        Long memoryOverhead = vmRuntimeInfo.getMemoryOverhead();
                        if (memoryOverhead != null) {
                            log.info("Memory Overhead: {}", memoryOverhead);
                        }
                    }
                } else {
                    log.info("Change: {}", value);
                }
            } else {
                log.info("Property Name: {} value removed.", name);
            }
        }
    }

    /**
     * This code takes an array of [typename, property, property, ...] and converts it into a PropertySpec[]. Handles
     * case where multiple references to the same typename are specified.
     *
     * @param typeinfo 2D array of type and properties to retrieve
     * @return container filter specs
     */
    private static List<PropertySpec> buildPropertySpecArray(String[][] typeinfo) {
        // Eliminate duplicates
        HashMap<String, Set<String>> tInfo = new HashMap<>();
        for (String[] strings : typeinfo) {
            Set<String> props = tInfo.computeIfAbsent(strings[0], k -> new HashSet<>());
            boolean typeSkipped = false;
            for (String prop : strings) {
                if (typeSkipped) {
                    props.add(prop);
                } else {
                    typeSkipped = true;
                }
            }
        }

        // Create PropertySpecs
        ArrayList<PropertySpec> propertySpecs = new ArrayList<>();

        for (Map.Entry<String, Set<String>> e : tInfo.entrySet()) {
            PropertySpec propertySpec = new PropertySpec();

            Set<String> props = e.getValue();
            propertySpec.setType(e.getKey());
            propertySpec.setAll(props.isEmpty());
            for (String prop : props) {
                propertySpec.getPathSet().add(prop);
            }
            propertySpecs.add(propertySpec);
        }

        return propertySpecs;
    }
}
