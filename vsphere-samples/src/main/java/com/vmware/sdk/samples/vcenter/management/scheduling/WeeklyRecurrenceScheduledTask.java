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
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.Action;
import com.vmware.vim25.DuplicateNameFaultMsg;
import com.vmware.vim25.InvalidNameFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.MethodAction;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ScheduledTaskSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TaskScheduler;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.WeeklyTaskScheduler;

/**
 * This sample demonstrates creation of weekly recurring ScheduledTask using the ScheduledTaskManager. This sample will
 * create a task to reboot Guest VM's at 11.59 pm every Saturday.
 */
public class WeeklyRecurrenceScheduledTask {
    private static final Logger log = LoggerFactory.getLogger(WeeklyRecurrenceScheduledTask.class);
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

    /** REQUIRED: Name of the virtual machine to be powered off. */
    public static String vmName = "vmName";
    /** REQUIRED: Name of the task to be created. */
    public static String taskName = "taskName";

    private static ManagedObjectReference virtualMachine;
    private static ManagedObjectReference scheduleManager;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(WeeklyRecurrenceScheduledTask.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            scheduleManager = serviceContent.getScheduledTaskManager();
            // find the VM by dns name to create a scheduled task for
            virtualMachine = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);
            // create the power Off action to be scheduled
            Action taskAction = createTaskAction();

            // create a One time scheduler to run
            TaskScheduler taskScheduler = createTaskScheduler();

            // Create Scheduled Task
            createScheduledTask(vimPort, taskAction, taskScheduler);
        }
    }

    /**
     * Create method action to reboot the guest in a vm.
     *
     * @return the action to run when the schedule runs
     */
    private static Action createTaskAction() {
        MethodAction action = new MethodAction();

        // Method Name is the WSDL name of the
        // ManagedObject's method to be run, in this Case,
        // the rebootGuest method for the VM
        action.setName("RebootGuest");
        return action;
    }

    /**
     * Create a Weekly task scheduler to run at 11:59 pm every Saturday.
     *
     * @return weekly task scheduler
     */
    private static TaskScheduler createTaskScheduler() {
        WeeklyTaskScheduler scheduler = new WeeklyTaskScheduler();

        // Set the Day of the Week to be Saturday
        scheduler.setSaturday(true);

        // Set the Time to be 23:59 hours or 11:59 pm
        scheduler.setHour(23);
        scheduler.setMinute(59);

        // set the interval to 1 to run the task only
        // Once every Week at the specified time
        scheduler.setInterval(1);

        return scheduler;
    }

    /**
     * Create a Scheduled Task using the reboot method action and the weekly scheduler, for the VM found.
     *
     * @param taskAction action to be performed when schedule executes
     * @param scheduler the scheduler used to execute the action
     */
    private static void createScheduledTask(VimPortType vimPort, Action taskAction, TaskScheduler scheduler)
            throws DuplicateNameFaultMsg, RuntimeFaultFaultMsg, InvalidNameFaultMsg {
        // Create the Scheduled Task Spec and set a unique task name
        // and description, and enable the task as soon as it is created
        ScheduledTaskSpec scheduleSpec = new ScheduledTaskSpec();
        scheduleSpec.setName(taskName);
        scheduleSpec.setDescription("Reboot VM's Guest at 11.59 pm every Saturday");
        scheduleSpec.setEnabled(true);

        // Set the RebootGuest Method Task action and
        // the Weekly scheduler in the spec
        scheduleSpec.setAction(taskAction);
        scheduleSpec.setScheduler(scheduler);

        // Create the ScheduledTask for the VirtualMachine we found earlier
        ManagedObjectReference taskMoRef = vimPort.createScheduledTask(scheduleManager, virtualMachine, scheduleSpec);

        // printout the MoRef id of the Scheduled Task
        log.info("Successfully created Weekly Task: {}", taskMoRef.getValue());
    }
}
