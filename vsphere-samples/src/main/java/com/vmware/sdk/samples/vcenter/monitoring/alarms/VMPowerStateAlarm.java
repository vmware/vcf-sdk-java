/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.monitoring.alarms;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.AlarmAction;
import com.vmware.vim25.AlarmExpression;
import com.vmware.vim25.AlarmSpec;
import com.vmware.vim25.AlarmTriggeringAction;
import com.vmware.vim25.DuplicateNameFaultMsg;
import com.vmware.vim25.InvalidNameFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.MethodAction;
import com.vmware.vim25.MethodActionArgument;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.StateAlarmExpression;
import com.vmware.vim25.StateAlarmOperator;
import com.vmware.vim25.VimPortType;

/** This sample creates an alarm to monitor the virtual machine's power state. */
public class VMPowerStateAlarm {
    private static final Logger log = LoggerFactory.getLogger(VMPowerStateAlarm.class);
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

    /** REQUIRED: Name of the alarms. */
    public static String alarm = "MyAlarm";
    /** REQUIRED: Name of the virtual machine to monitor. */
    public static String vmname = "VMName";

    private static ManagedObjectReference alarmManager;
    private static ManagedObjectReference vmMoRef;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VMPowerStateAlarm.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            alarmManager = serviceContent.getAlarmManager();

            vmMoRef = propertyCollectorHelper.getMoRefByName(vmname, VIRTUAL_MACHINE);
            if (vmMoRef != null) {
                StateAlarmExpression expression = createStateAlarmExpression();
                MethodAction methodAction = createPowerOnAction();
                AlarmAction alarmAction = createAlarmTriggerAction(methodAction);
                AlarmSpec alarmSpec = createAlarmSpec(alarmAction, expression);

                createAlarm(vimPort, alarmSpec);
            } else {
                log.error("Virtual Machine {} Not Found", vmname);
            }
        }
    }

    /**
     * Creates the state alarm expression.
     *
     * @return the state alarm expression
     */
    private static StateAlarmExpression createStateAlarmExpression() {
        StateAlarmExpression expression = new StateAlarmExpression();
        expression.setType("VirtualMachine");
        expression.setStatePath("runtime.powerState");
        expression.setOperator(StateAlarmOperator.IS_EQUAL);
        expression.setRed("poweredOff");
        return expression;
    }

    /**
     * Creates the power on action.
     *
     * @return the method action
     */
    private static MethodAction createPowerOnAction() {
        MethodAction action = new MethodAction();
        action.setName("PowerOnVM_Task");

        MethodActionArgument argument = new MethodActionArgument();
        argument.setValue(null);

        action.getArgument().add(argument);
        return action;
    }

    /**
     * Creates the alarm trigger action.
     *
     * @param methodAction the method action
     * @return the alarm triggering action
     */
    private static AlarmTriggeringAction createAlarmTriggerAction(MethodAction methodAction) {
        AlarmTriggeringAction alarmAction = new AlarmTriggeringAction();
        alarmAction.setYellow2Red(true);
        alarmAction.setAction(methodAction);
        return alarmAction;
    }

    /**
     * Creates the alarm spec.
     *
     * @param action the action
     * @param expression the expression
     * @return the alarm spec object
     */
    private static AlarmSpec createAlarmSpec(AlarmAction action, AlarmExpression expression) {
        AlarmSpec spec = new AlarmSpec();
        spec.setAction(action);
        spec.setExpression(expression);
        spec.setName(alarm);
        spec.setDescription("Monitor VM state and send email if VM power's off");
        spec.setEnabled(true);
        return spec;
    }

    /**
     * Creates the alarm.
     *
     * @param alarmSpec the alarm spec object
     */
    private static void createAlarm(VimPortType vimPort, AlarmSpec alarmSpec)
            throws DuplicateNameFaultMsg, RuntimeFaultFaultMsg, InvalidNameFaultMsg {
        ManagedObjectReference alarmMoRef = vimPort.createAlarm(alarmManager, vmMoRef, alarmSpec);
        log.info("Successfully created Alarm: {}", alarmMoRef.getValue());
    }
}
