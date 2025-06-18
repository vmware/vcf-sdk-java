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

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

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
import com.vmware.vim25.OnceTaskScheduler;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ScheduledTaskSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TaskScheduler;
import com.vmware.vim25.VimPortType;

/** This sample demonstrates creation of ScheduledTask using the ScheduledTaskManager. */
public class OneTimeScheduledTask {
    private static final Logger log = LoggerFactory.getLogger(OneTimeScheduledTask.class);
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

    /** REQUIRED: Name of VM to power off. */
    public static String vmName = "vmName";
    /** REQUIRED: Name of the task. */
    public static String taskName = "taskName";

    private static ManagedObjectReference scheduleManager;
    private static ManagedObjectReference virtualMachine;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(OneTimeScheduledTask.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            scheduleManager = serviceContent.getScheduledTaskManager();

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
     * Create method action to power off a vm.
     *
     * @return the action to run when the schedule runs
     */
    private static Action createTaskAction() {
        MethodAction action = new MethodAction();

        // Method Name is the WSDL name of the
        // ManagedObject's method that is to be run, in this case, the powerOff method of the VM
        action.setName("PowerOffVM_Task");
        return action;
    }

    /**
     * Create a Once task scheduler to run 30 minutes from now.
     *
     * @return one time task scheduler
     */
    private static TaskScheduler createTaskScheduler() throws DatatypeConfigurationException {
        // Create a Calendar Object and add 30 minutes to allow the Action to be run 30 minutes from now
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.add(Calendar.MINUTE, 30);
        XMLGregorianCalendar runTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);

        // Create a OnceTaskScheduler and set the time to run the Task Action at in the Scheduler.
        OnceTaskScheduler scheduler = new OnceTaskScheduler();
        scheduler.setRunAt(runTime);
        return scheduler;
    }

    /**
     * Create a Scheduled Task using the poweroff method action and the onetime scheduler, for the VM found.
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
        scheduleSpec.setDescription("PowerOff VM in 30 minutes");
        scheduleSpec.setEnabled(true);

        // Set the PowerOff Method Task Action and the once scheduler in the spec
        scheduleSpec.setAction(taskAction);
        scheduleSpec.setScheduler(scheduler);

        // Create ScheduledTask for the VirtualMachine we found earlier
        if (virtualMachine != null) {
            ManagedObjectReference taskMoRef =
                    vimPort.createScheduledTask(scheduleManager, virtualMachine, scheduleSpec);
            // printout the MoRef id of the Scheduled Task
            log.info("Successfully created Once Task: {}", taskMoRef.getValue());
        } else {
            log.error("Virtual Machine {} not found", vmName);
        }
    }
}
