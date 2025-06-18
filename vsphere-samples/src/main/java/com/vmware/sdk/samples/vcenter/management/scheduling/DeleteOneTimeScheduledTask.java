/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.scheduling;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VimPortType;

/** This sample demonstrates deleting a scheduled task. */
public class DeleteOneTimeScheduledTask {
    private static final Logger log = LoggerFactory.getLogger(DeleteOneTimeScheduledTask.class);
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

    private static ManagedObjectReference scheduleManager;

    /** REQUIRED: Name of the task to be deleted. */
    public static String taskName = "taskName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(DeleteOneTimeScheduledTask.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            scheduleManager = serviceContent.getScheduledTaskManager();

            // create a Property Filter Spec to get names of all scheduled tasks
            PropertyFilterSpec taskFilterSpec = createTaskPropertyFilterSpec();

            // Retrieve names of all ScheduledTasks and find the named one time Scheduled Task
            ManagedObjectReference oneTimeTaskMoRef = findOneTimeScheduledTask(propertyCollectorHelper, taskFilterSpec);

            // Delete the one time scheduled task
            if (oneTimeTaskMoRef != null) {
                deleteScheduledTask(vimPort, oneTimeTaskMoRef);
            }
        }
    }

    /**
     * Create Property Filter Spec to get names of all ScheduledTasks the ScheduledTaskManager has.
     *
     * @return PropertyFilterSpec to get properties
     */
    private static PropertyFilterSpec createTaskPropertyFilterSpec() {
        // The traversal spec traverses the "scheduledTask" property of
        // ScheduledTaskManager to get names of ScheduledTask ManagedEntities
        // A Traversal Spec allows traversal into a ManagedObjects
        // using a single attribute of the managedObject
        TraversalSpec scheduledTaskTraversal = new TraversalSpec();

        scheduledTaskTraversal.setType(scheduleManager.getType());
        scheduledTaskTraversal.setPath("scheduledTask");

        // We want to get values of the scheduleTask property
        // of the scheduledTaskManager, which are the ScheduledTasks
        // so we set skip = false.
        scheduledTaskTraversal.setSkip(Boolean.FALSE);
        scheduledTaskTraversal.setName("scheduleManagerToScheduledTasks");

        // Setup a PropertySpec to return names of Scheduled Tasks so
        // we can find the named ScheduleTask ManagedEntity to delete
        // Name is an attribute of ScheduledTaskInfo so
        // the path set will contain "info.name"
        PropertySpec propertySpec = new PropertySpec();
        propertySpec.setAll(false);
        propertySpec.getPathSet().add("info.name");
        propertySpec.setType("ScheduledTask");

        // PropertySpecs are wrapped in a PropertySpec array
        // since we only have a propertySpec for the ScheduledTask,
        // the only values we will get back are names of scheduledTasks
        List<PropertySpec> propertySpecs = new ArrayList<>();
        propertySpecs.add(propertySpec);

        // Create an Object Spec to specify the starting or root object
        // and the SelectionSpec to traverse to each ScheduledTask in the
        // array of scheduledTasks in the ScheduleManager
        ObjectSpec objectSpec = new ObjectSpec();
        objectSpec.setObj(scheduleManager);
        objectSpec.getSelectSet().add(scheduledTaskTraversal);

        // Set skip = true so properties of ScheduledTaskManager
        // are not returned, and only values of info.name property of
        // each ScheduledTask is returned
        objectSpec.setSkip(Boolean.TRUE);

        // ObjectSpecs used in PropertyFilterSpec are wrapped in an array
        List<ObjectSpec> objectSpecs = new ArrayList<>();
        objectSpecs.add(objectSpec);

        // Create the PropertyFilter spec with
        // ScheduledTaskManager as "root" object
        PropertyFilterSpec spec = new PropertyFilterSpec();
        spec.getPropSet().addAll(propertySpecs);
        spec.getObjectSet().addAll(objectSpecs);
        return spec;
    }

    private static ManagedObjectReference findOneTimeScheduledTask(
            PropertyCollectorHelper propertyCollectorHelper, PropertyFilterSpec scheduledTaskSpec)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        boolean found = false;
        ManagedObjectReference oneTimeTaskMoRef = null;

        // Use PropertyCollector to get all scheduled tasks the ScheduleManager has
        List<PropertyFilterSpec> propertyFilterSpecList = new ArrayList<>();
        propertyFilterSpecList.add(scheduledTaskSpec);

        List<ObjectContent> scheduledTasks = propertyCollectorHelper.retrieveAllProperties(propertyFilterSpecList);

        // Find the task name we're looking for and return the
        // ManagedObjectReference for the ScheduledTask with the
        // name that matched the name of the OneTimeTask created earlier
        if (scheduledTasks != null) {
            for (int i = 0; i < scheduledTasks.size() && !found; i++) {
                ObjectContent taskContent = scheduledTasks.get(i);

                List<DynamicProperty> props = taskContent.getPropSet();
                for (int p = 0; p < props.size() && !found; p++) {
                    DynamicProperty prop = props.get(p);

                    String taskNameVal = (String) prop.getVal();
                    if (taskName.equals(taskNameVal)) {
                        oneTimeTaskMoRef = taskContent.getObj();
                        found = true;
                    }
                }
            }
        }
        if (!found) {
            log.warn("Scheduled task '{}' not found", taskName);
        }
        return oneTimeTaskMoRef;
    }

    private static void deleteScheduledTask(VimPortType vimPort, ManagedObjectReference oneTimeTaskMoRef)
            throws RuntimeFaultFaultMsg, InvalidStateFaultMsg {
        // Remove the One Time Scheduled Task
        vimPort.removeScheduledTask(oneTimeTaskMoRef);
        log.info("Successfully Deleted ScheduledTask: {}", oneTimeTaskMoRef.getValue());
    }
}
