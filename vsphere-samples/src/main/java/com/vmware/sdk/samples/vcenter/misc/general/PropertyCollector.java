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
import com.vmware.vim25.ConcurrentAccessFaultMsg;
import com.vmware.vim25.DuplicateNameFaultMsg;
import com.vmware.vim25.FileFaultFaultMsg;
import com.vmware.vim25.HostListSummaryQuickStats;
import com.vmware.vim25.HostRuntimeInfo;
import com.vmware.vim25.InsufficientResourcesFaultFaultMsg;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidDatastoreFaultMsg;
import com.vmware.vim25.InvalidNameFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.ObjectUpdate;
import com.vmware.vim25.ObjectUpdateKind;
import com.vmware.vim25.PropertyChange;
import com.vmware.vim25.PropertyChangeOp;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertyFilterUpdate;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TaskInProgressFaultMsg;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.UpdateSet;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineQuickStats;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.VmConfigFaultFaultMsg;
import com.vmware.vim25.WaitOptions;

/** This sample illustrates the property collector features added in version 4.1. */
public class PropertyCollector {
    private static final Logger log = LoggerFactory.getLogger(PropertyCollector.class);
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

    /** REQUIRED: Extension to be demonstrated [retrieveproperties | updates | filterspec | propertycollector]. */
    public static String extension = "extension";
    /** OPTIONAL: Name of the virtual machine. */
    public static String vmName = null;
    /** OPTIONAL: The maximum number of ObjectContent data objects that should be returned in a single result. */
    public static String maxObjects = null;
    /** OPTIONAL: Update type - [waitforupdates | checkforupdates| extension]. */
    public static String updateType = null;
    /** OPTIONAL: Number of tasks to be created. */
    public static String noofTasks = null;

    private static ManagedObjectReference propCollectorRef = null;
    private static ManagedObjectReference rootFolderRef;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(PropertyCollector.class, args);

        validate();

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            rootFolderRef = serviceContent.getRootFolder();
            propCollectorRef = serviceContent.getPropertyCollector();

            doOperations(vimPort, propertyCollectorHelper);
        }
    }

    private static void validate() {
        if (extension == null) {
            throw new IllegalArgumentException("Expected --extension argument.");
        }

        if (!extension.equalsIgnoreCase("retrieveproperties")
                && !extension.equalsIgnoreCase("updates")
                && !extension.equalsIgnoreCase("filterspec")
                && !extension.equalsIgnoreCase("propertycollector")) {
            throw new IllegalArgumentException("Invalid value for option extension."
                    + " Possible values [retrieveproperties | updates | filterspec |"
                    + " propertycollector]");
        }
        if (extension.equalsIgnoreCase("updates") && (vmName == null || updateType == null)) {
            throw new IllegalArgumentException(
                    "For update extension. vmname and" + " updatetype are mandatory argument");
        }
        if (extension.equalsIgnoreCase("filterspec") && ((vmName == null) || (noofTasks == null))) {
            throw new IllegalArgumentException(
                    "For filterspec extension. vmname and" + " nooftasks are mandatory argument");
        }
        if (updateType != null) {
            if (!updateType.equalsIgnoreCase("waitforupdates")
                    && !updateType.equalsIgnoreCase("checkforupdates")
                    && !updateType.equalsIgnoreCase("extension")) {
                throw new IllegalArgumentException("Invalid value for option updatetype."
                        + " Possible values [waitforupdates | checkforupdates | extension]");
            }
        }
    }

    private static void doOperations(VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg,
                    DuplicateNameFaultMsg, TaskInProgressFaultMsg, InsufficientResourcesFaultFaultMsg,
                    VmConfigFaultFaultMsg, InvalidDatastoreFaultMsg, FileFaultFaultMsg, ConcurrentAccessFaultMsg,
                    InvalidStateFaultMsg, InvalidNameFaultMsg {

        if (extension.equalsIgnoreCase("retrieveproperties")) {
            if (maxObjects == null) {
                // Call to API RetrievePropertiesEx Without Setting "MaxObjects" property.
                callRetrievePropertiesEx(vimPort, null);
            } else {
                // Call to API RetrievePropertiesEx by Setting "MaxObjects" property.
                callRetrievePropertiesEx(vimPort, maxObjects);
            }
        } else if (extension.equalsIgnoreCase("updates")) {
            if (updateType.equalsIgnoreCase("waitforupdates")) {
                // Call to API WaitForUpdatesEx, as equivalent for "WaitForUpdates"
                callWaitForUpdatesEx(vimPort, propertyCollectorHelper, null, null);
            } else if (updateType.equalsIgnoreCase("checkforupdates")) {
                // Call to API WaitForUpdatesEx, as equivalent for "CheckForUpdates"
                callWaitForUpdatesEx(vimPort, propertyCollectorHelper, "60", null);
            } else if (updateType.equalsIgnoreCase("extension")) {
                // New Feature
                callWaitForUpdatesEx(vimPort, propertyCollectorHelper, "60", "1");
            }
        } else if (extension.equalsIgnoreCase("filterspec")) {
            int taskLength = Integer.parseInt(noofTasks);
            List<ManagedObjectReference> tasks = createTasks(vimPort, propertyCollectorHelper, taskLength);
            // Create a FilterSpec by setting the new property
            // "ReportMissingObjectsInResults"
            List<String> vals = new ArrayList<>();
            vals.add("info.state");
            vals.add("info.error");
            callCreateFilterSpecEx(vimPort, tasks, vals);
        } else if (extension.equalsIgnoreCase("propertycollector")) {
            // Create, use and delete "PropertyCollector"
            callCreatePropertyCollectorEx(vimPort);
        }
    }

    /*
     * Illustrating how to create, use and destroy additional property collectors
     * This allows multiple modules to create their own property
     * filter and process updates independently.
     * Also applies to get time-sensitive updated being monitored on one collector,
     * while a large updated being monitored by another.
     */
    private static void callCreatePropertyCollectorEx(VimPortType vimPort)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        ManagedObjectReference propertyCollectorMoRef = vimPort.createPropertyCollector(propCollectorRef);

        PropertySpec propertySpec = new PropertySpec();
        propertySpec.setAll(false);
        propertySpec.getPathSet().add("name");
        propertySpec.setType("VirtualMachine");

        List<PropertySpec> propertySpecs = new ArrayList<>();
        propertySpecs.add(propertySpec);

        ObjectSpec objectSpec = new ObjectSpec();
        objectSpec.setObj(rootFolderRef);
        objectSpec.setSkip(false);
        objectSpec.getSelectSet().addAll(buildFullTraversal());

        List<ObjectSpec> objectSpecs = new ArrayList<>(0);
        objectSpecs.add(objectSpec);

        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
        propertyFilterSpec.getPropSet().addAll(propertySpecs);
        propertyFilterSpec.getObjectSet().addAll(objectSpecs);

        List<PropertyFilterSpec> propertyFilterSpecs = new ArrayList<>();
        propertyFilterSpecs.add(propertyFilterSpec);

        RetrieveOptions retrieveOptions = new RetrieveOptions();

        RetrieveResult retrieveResult =
                vimPort.retrievePropertiesEx(propertyCollectorMoRef, propertyFilterSpecs, retrieveOptions);
        List<ObjectContent> objectContents = retrieveResult.getObjects();
        for (ObjectContent objectContent : objectContents) {
            log.info("VM : {}", objectContent.getObj().getValue());
        }
        vimPort.destroyPropertyCollector(propertyCollectorMoRef);
    }

    private static void callCreateFilterSpecEx(
            VimPortType vimPort, List<ManagedObjectReference> tasks, List<String> filterProps)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg {
        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
        List<ObjectSpec> objectSpecs = new ArrayList<>();
        List<PropertySpec> propertySpecs = new ArrayList<>();

        for (ManagedObjectReference managedObjectReference : tasks) {
            PropertySpec propertySpec = new PropertySpec();
            propertySpec.getPathSet().addAll(filterProps);
            propertySpec.setType("Task");

            propertySpecs.add(propertySpec);

            ObjectSpec objectSpec = new ObjectSpec();
            objectSpec.setObj(managedObjectReference);
            objectSpec.getSelectSet().add(null);
            objectSpec.setSkip(false);
            objectSpecs.add(objectSpec);
        }
        propertyFilterSpec.getPropSet().addAll(propertySpecs);
        propertyFilterSpec.getObjectSet().addAll(objectSpecs);

        /*
         * Illustrating the usage of property "ReportMissingObjectsInResults"
         * Property is useful in the scenario when monitoring large number of
         * managed objectsform where some didn't exists by the time filter is
         * created.
         */
        propertyFilterSpec.setReportMissingObjectsInResults(true);

        ManagedObjectReference filterSpecMoRef = vimPort.createFilter(propCollectorRef, propertyFilterSpec, false);

        UpdateSet updateset = null;
        updateset = vimPort.waitForUpdates(propCollectorRef, "");

        List<PropertyFilterUpdate> update = updateset.getFilterSet();
        if (update.get(0).getMissingSet() != null
                && !update.get(0).getMissingSet().isEmpty()) {
            for (int i = 0; i < update.get(0).getMissingSet().size(); i++) {
                log.info(
                        "Managed Object Reference {} Not Found",
                        update.get(0).getMissingSet().get(i).getObj().getValue());
            }
        }
        log.info("FilterSpec created successfully");
        vimPort.destroyPropertyFilter(filterSpecMoRef);
    }

    private static List<ManagedObjectReference> createTasks(
            VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper, int taskLength)
            throws DuplicateNameFaultMsg, RuntimeFaultFaultMsg, TaskInProgressFaultMsg, VmConfigFaultFaultMsg,
                    InsufficientResourcesFaultFaultMsg, InvalidDatastoreFaultMsg, FileFaultFaultMsg,
                    ConcurrentAccessFaultMsg, InvalidStateFaultMsg, InvalidNameFaultMsg, InvalidPropertyFaultMsg,
                    InvalidCollectorVersionFaultMsg {
        List<ManagedObjectReference> tasks = new ArrayList<>();

        ManagedObjectReference vmMoRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);
        if (vmMoRef == null) {
            log.error("Virtual Machine {} Not Found", vmName);
            return null;
        }
        for (int i = 0; i < taskLength; i++) {
            String tempString = "Temp";
            VirtualMachineConfigSpec spec = new VirtualMachineConfigSpec();
            spec.setAnnotation(tempString);

            tasks.add(vimPort.reconfigVMTask(vmMoRef, spec));

            propertyCollectorHelper.awaitTaskCompletion(tasks.get(i));
        }
        return tasks;
    }

    private static void callWaitForUpdatesEx(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            String maxWaitSeconds,
            String maxObjectUpdates)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg {
        ManagedObjectReference vmMoRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);
        if (vmMoRef == null) {
            log.error("Virtual Machine {} Not Found", vmName);
            return;
        }

        String[][] typeInfo = {new String[] {"VirtualMachine", "name", "summary.quickStats", "runtime"}};

        List<PropertySpec> propertySpecs = buildPropertySpecArray(typeInfo);
        List<ObjectSpec> objectSpecs = new ArrayList<>();

        ObjectSpec objectSpec = new ObjectSpec();
        objectSpec.setObj(vmMoRef);
        objectSpec.setSkip(false);
        objectSpecs.add(objectSpec);

        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();

        propertyFilterSpec.getPropSet().addAll(propertySpecs);
        propertyFilterSpec.getObjectSet().addAll(objectSpecs);
        propertyFilterSpec.setReportMissingObjectsInResults(false);

        ManagedObjectReference propertyCollectorMoRef = propCollectorRef;

        ManagedObjectReference propFilter = vimPort.createFilter(propertyCollectorMoRef, propertyFilterSpec, false);

        WaitOptions waitOptions = new WaitOptions();
        if (maxWaitSeconds != null) {
            waitOptions.setMaxWaitSeconds(Integer.parseInt(maxWaitSeconds));
        } else if (maxObjectUpdates != null) {
            waitOptions.setMaxObjectUpdates(Integer.parseInt(maxObjectUpdates));
        }

        UpdateSet update = vimPort.waitForUpdatesEx(propCollectorRef, null, waitOptions);

        if (update.isTruncated() != null && update.isTruncated()) {
            callWaitForUpdatesEx(vimPort, propertyCollectorHelper, maxWaitSeconds, maxObjectUpdates);
        } else {
            if (update.getFilterSet() != null) {
                handleUpdate(update);
            } else {
                log.info("No update is present!");
            }
        }
        vimPort.destroyPropertyFilter(propFilter);
    }

    private static void callRetrievePropertiesEx(VimPortType vimPort, String maxObjects)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        PropertySpec propertySpec = new PropertySpec();
        propertySpec.setAll(false);
        propertySpec.getPathSet().add("name");
        propertySpec.setType("VirtualMachine");

        List<PropertySpec> propertySpecs = new ArrayList<>(1);
        propertySpecs.add(propertySpec);

        ObjectSpec objectSpec = new ObjectSpec();
        objectSpec.setObj(rootFolderRef);
        objectSpec.setSkip(false);
        objectSpec.getSelectSet().addAll(buildFullTraversal());

        List<ObjectSpec> objectSpecs = new ArrayList<>();
        objectSpecs.add(objectSpec);

        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
        propertyFilterSpec.getPropSet().addAll(propertySpecs);
        propertyFilterSpec.getObjectSet().addAll(objectSpecs);

        List<PropertyFilterSpec> propertyFilterSpecs = new ArrayList<>();
        propertyFilterSpecs.add(propertyFilterSpec);

        RetrieveOptions retrieveOptions = new RetrieveOptions();
        if (maxObjects != null) {
            retrieveOptions.setMaxObjects(Integer.parseInt(maxObjects));
        }

        RetrieveResult retrieveResult =
                vimPort.retrievePropertiesEx(propCollectorRef, propertyFilterSpecs, retrieveOptions);
        if (retrieveResult != null) {
            List<ObjectContent> objectContents = retrieveResult.getObjects();
            if (retrieveResult.getToken() != null && maxObjects == null) {
                callContinueRetrieveProperties(vimPort, retrieveResult.getToken());
            }
            for (ObjectContent objectContent : objectContents) {
                log.info(
                        "VM Managed Object Reference Value: {}",
                        objectContent.getObj().getValue());
            }
        } else {
            log.info("No VMs in inventory");
        }
    }

    private static void callContinueRetrieveProperties(VimPortType vimPort, String token)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        List<ObjectContent> objectContentList;

        RetrieveResult retrieveResult = vimPort.continueRetrievePropertiesEx(propCollectorRef, token);
        objectContentList = retrieveResult.getObjects();
        if (retrieveResult.getToken() != null) {
            callContinueRetrieveProperties(vimPort, retrieveResult.getToken());
        }
        for (ObjectContent objectContent : objectContentList) {
            log.info(
                    "VM Managed Object Reference Value : {}",
                    objectContent.getObj().getValue());
        }
    }

    private static void handleUpdate(UpdateSet update) {
        ArrayList<ObjectUpdate> vmUpdates = new ArrayList<>();
        ArrayList<ObjectUpdate> hostUpdates = new ArrayList<>();

        List<PropertyFilterUpdate> filterUpdates = update.getFilterSet();
        for (PropertyFilterUpdate propertyFilterUpdate : filterUpdates) {
            List<ObjectUpdate> objectUpdates = propertyFilterUpdate.getObjectSet();
            for (ObjectUpdate objectUpdate : objectUpdates) {
                if (objectUpdate.getObj().getType().equals("VirtualMachine")) {
                    vmUpdates.add(objectUpdate);
                } else if (objectUpdate.getObj().getType().equals("HostSystem")) {
                    hostUpdates.add(objectUpdate);
                }
            }
        }
        if (!vmUpdates.isEmpty()) {
            log.info("Virtual Machine updates:");
            for (ObjectUpdate vmUpdate : vmUpdates) {
                handleObjectUpdate(vmUpdate);
            }
        }
        if (!hostUpdates.isEmpty()) {
            log.info("Host updates:");
            for (ObjectUpdate hostUpdate : hostUpdates) {
                handleObjectUpdate(hostUpdate);
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
                if ("summary.quickStats".equals(name)) {
                    if (value instanceof VirtualMachineQuickStats) {
                        VirtualMachineQuickStats vmQuickStats = (VirtualMachineQuickStats) value;
                        String cpu = vmQuickStats.getOverallCpuUsage() == null
                                ? "unavailable"
                                : vmQuickStats.getOverallCpuUsage().toString();
                        String memory = vmQuickStats.getHostMemoryUsage() == null
                                ? "unavailable"
                                : vmQuickStats.getHostMemoryUsage().toString();
                        log.info("Guest Status: {}", vmQuickStats.getGuestHeartbeatStatus());
                        log.info("CPU Load %: {}", cpu);
                        log.info("Memory Load %: {}", memory);
                    } else if (value instanceof HostListSummaryQuickStats) {
                        HostListSummaryQuickStats hostListSummaryStats = (HostListSummaryQuickStats) value;
                        String cpu = hostListSummaryStats.getOverallCpuUsage() == null
                                ? "unavailable"
                                : hostListSummaryStats.getOverallCpuUsage().toString();
                        String memory = hostListSummaryStats.getOverallMemoryUsage() == null
                                ? "unavailable"
                                : hostListSummaryStats.getOverallMemoryUsage().toString();
                        log.info("CPU Load %: {}", cpu);
                        log.info("Memory Load %: {}", memory);
                    }
                } else if ("runtime".equals(name)) {
                    if (value instanceof VirtualMachineRuntimeInfo) {
                        VirtualMachineRuntimeInfo vmRuntimeInfo = (VirtualMachineRuntimeInfo) value;
                        log.info("Power State: {}", vmRuntimeInfo.getPowerState());
                        log.info("Connection State: {}", vmRuntimeInfo.getConnectionState());
                        XMLGregorianCalendar bTime = vmRuntimeInfo.getBootTime();
                        if (bTime != null) {
                            log.info("Boot Time: {}", bTime);
                        }
                        Long memoryOverhead = vmRuntimeInfo.getMemoryOverhead();
                        if (memoryOverhead != null) {
                            log.info("Memory Overhead: {}", memoryOverhead);
                        }
                    } else if (value instanceof HostRuntimeInfo) {
                        HostRuntimeInfo hostRuntimeInfo = (HostRuntimeInfo) value;
                        log.info("Connection State: {}", hostRuntimeInfo.getConnectionState());
                        XMLGregorianCalendar bTime = hostRuntimeInfo.getBootTime();
                        if (bTime != null) {
                            log.info("Boot Time: {}", bTime);
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

    /*
     * @return An array of SelectionSpec covering VM, Host, Resource pool,
     * Cluster Compute Resource and Datastore.
     */
    private static List<SelectionSpec> buildFullTraversal() {
        // Terminal traversal specs

        // RP -> VM
        TraversalSpec rpToVm = new TraversalSpec();
        rpToVm.setName("rpToVm");
        rpToVm.setType("ResourcePool");
        rpToVm.setPath("vm");
        rpToVm.setSkip(Boolean.FALSE);

        // vApp -> VM
        TraversalSpec vAppToVM = new TraversalSpec();
        vAppToVM.setName("vAppToVM");
        vAppToVM.setType("VirtualApp");
        vAppToVM.setPath("vm");

        // DC -> DS
        TraversalSpec dcToDs = new TraversalSpec();
        dcToDs.setType("Datacenter");
        dcToDs.setPath("datastore");
        dcToDs.setName("dcToDs");
        dcToDs.setSkip(Boolean.FALSE);

        // Recurse through all ResourcePools
        TraversalSpec rpToRp = new TraversalSpec();
        rpToRp.setType("ResourcePool");
        rpToRp.setPath("resourcePool");
        rpToRp.setSkip(Boolean.FALSE);
        rpToRp.setName("rpToRp");
        rpToRp.getSelectSet().add(getSelectionSpec("rpToRp"));

        TraversalSpec crToRp = new TraversalSpec();
        crToRp.setType("ComputeResource");
        crToRp.setPath("resourcePool");
        crToRp.setSkip(Boolean.FALSE);
        crToRp.setName("crToRp");
        crToRp.getSelectSet().add(getSelectionSpec("rpToRp"));

        TraversalSpec crToH = new TraversalSpec();
        crToH.setSkip(Boolean.FALSE);
        crToH.setType("ComputeResource");
        crToH.setPath("host");
        crToH.setName("crToH");

        TraversalSpec dcToHf = new TraversalSpec();
        dcToHf.setSkip(Boolean.FALSE);
        dcToHf.setType("Datacenter");
        dcToHf.setPath("hostFolder");
        dcToHf.setName("dcToHf");
        dcToHf.getSelectSet().add(getSelectionSpec("VisitFolders"));

        TraversalSpec vAppToRp = new TraversalSpec();
        vAppToRp.setName("vAppToRp");
        vAppToRp.setType("VirtualApp");
        vAppToRp.setPath("resourcePool");
        vAppToRp.getSelectSet().add(getSelectionSpec("rpToRp"));

        TraversalSpec dcToVmf = new TraversalSpec();
        dcToVmf.setType("Datacenter");
        dcToVmf.setSkip(Boolean.FALSE);
        dcToVmf.setPath("vmFolder");
        dcToVmf.setName("dcToVmf");
        dcToVmf.getSelectSet().add(getSelectionSpec("VisitFolders"));

        TraversalSpec dcToNetwork = new TraversalSpec();
        dcToNetwork.setSkip(Boolean.FALSE);
        dcToNetwork.setType("Datacenter");
        dcToNetwork.setPath("network");
        dcToNetwork.setName("dcToNetwork");

        // For Folder -> Folder recursion
        TraversalSpec visitFolders = new TraversalSpec();
        visitFolders.setType("Folder");
        visitFolders.setPath("childEntity");
        visitFolders.setSkip(Boolean.FALSE);
        visitFolders.setName("VisitFolders");
        List<SelectionSpec> sspecarrvf = new ArrayList<>();
        sspecarrvf.add(getSelectionSpec("crToRp"));
        sspecarrvf.add(getSelectionSpec("crToH"));
        sspecarrvf.add(getSelectionSpec("dcToVmf"));
        sspecarrvf.add(getSelectionSpec("dcToHf"));
        sspecarrvf.add(getSelectionSpec("vAppToRp"));
        sspecarrvf.add(getSelectionSpec("vAppToVM"));
        sspecarrvf.add(getSelectionSpec("dcToDs"));
        sspecarrvf.add(getSelectionSpec("rpToVm"));
        sspecarrvf.add(getSelectionSpec("VisitFolders"));

        visitFolders.getSelectSet().addAll(sspecarrvf);

        List<SelectionSpec> resultspec = new ArrayList<>();
        resultspec.add(visitFolders);
        resultspec.add(crToRp);
        resultspec.add(crToH);
        resultspec.add(dcToVmf);
        resultspec.add(dcToHf);
        resultspec.add(vAppToRp);
        resultspec.add(vAppToVM);
        resultspec.add(dcToDs);
        resultspec.add(rpToVm);
        resultspec.add(rpToRp);
        resultspec.add(dcToNetwork);

        return resultspec;
    }

    private static SelectionSpec getSelectionSpec(String name) {
        SelectionSpec genericSpec = new SelectionSpec();
        genericSpec.setName(name);
        return genericSpec;
    }

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
            // pSpec.setPathSet(new String[props.size()]);
            int index = 0;
            for (Object o : props) {
                String prop = (String) o;
                propertySpec.getPathSet().add(index++, prop);
            }
            propertySpecs.add(propertySpec);
        }
        return new ArrayList<>(propertySpecs);
    }
}
