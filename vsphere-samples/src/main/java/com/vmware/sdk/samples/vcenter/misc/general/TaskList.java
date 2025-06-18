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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.LocalizedMethodFault;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VimPortType;

/** This sample prints out a list of tasks if any are running. */
public class TaskList {
    private static final Logger log = LoggerFactory.getLogger(TaskList.class);
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

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(TaskList.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference taskManagerRef = serviceContent.getTaskManager();

            List<PropertyFilterSpec> propertyFilterSpecs = createPFSForRecentTasks(taskManagerRef);
            List<ObjectContent> objectContents = propertyCollectorHelper.retrieveAllProperties(propertyFilterSpecs);

            displayTasks(objectContents);
        }
    }

    private static List<PropertyFilterSpec> createPFSForRecentTasks(ManagedObjectReference taskManagerRef) {
        PropertySpec propertySpec = new PropertySpec();
        propertySpec.setAll(Boolean.FALSE);
        propertySpec.setType("Task");

        List<String> propertiesList = new ArrayList<>();
        propertiesList.add("info.entity");
        propertiesList.add("info.entityName");
        propertiesList.add("info.name");
        propertiesList.add("info.state");
        propertiesList.add("info.cancelled");
        propertiesList.add("info.error");
        propertySpec.getPathSet().addAll(propertiesList);

        ObjectSpec objectSpec = new ObjectSpec();
        objectSpec.setObj(taskManagerRef);
        objectSpec.setSkip(Boolean.FALSE);

        TraversalSpec traversalSpec = new TraversalSpec();
        traversalSpec.setType("TaskManager");
        traversalSpec.setPath("recentTask");
        traversalSpec.setSkip(Boolean.FALSE);

        objectSpec.getSelectSet().add(traversalSpec);

        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
        propertyFilterSpec.getPropSet().add(propertySpec);
        propertyFilterSpec.getObjectSet().add(objectSpec);

        List<PropertyFilterSpec> result = new ArrayList<>();
        result.add(propertyFilterSpec);
        return result;
    }

    private static void displayTasks(List<ObjectContent> objectContents) {
        log.info("Displaying {} tasks.", objectContents.size());
        for (ObjectContent oContent : objectContents) {
            System.out.println("Task");
            List<DynamicProperty> dynamicProperties = oContent.getPropSet();

            if (dynamicProperties != null) {
                String op = "", name = "", type = "", state = "", error = "";
                for (DynamicProperty dp : dynamicProperties) {
                    if ("info.entity".equals(dp.getName())) {
                        type = ((ManagedObjectReference) dp.getVal()).getType();
                    } else if ("info.entityName".equals(dp.getName())) {
                        name = ((String) dp.getVal());
                    } else if ("info.name".equals(dp.getName())) {
                        op = ((String) dp.getVal());
                    } else if ("info.state".equals(dp.getName())) {
                        TaskInfoState tis = (TaskInfoState) dp.getVal();
                        if (TaskInfoState.ERROR.equals(tis)) {
                            state = "-Error";
                        } else if (TaskInfoState.QUEUED.equals(tis)) {
                            state = "-Queued";
                        } else if (TaskInfoState.RUNNING.equals(tis)) {
                            state = "-Running";
                        } else if (TaskInfoState.SUCCESS.equals(tis)) {
                            state = "-Success";
                        }
                    } else if ("info.cancelled".equals(dp.getName())) {
                        Boolean b = (Boolean) dp.getVal();
                        if (b != null && b) {
                            state += "-Cancelled";
                        }
                    } else if ("info.error".equals(dp.getName())) {
                        LocalizedMethodFault methodFault = (LocalizedMethodFault) dp.getVal();
                        if (methodFault != null) {
                            error = methodFault.getLocalizedMessage();
                        }
                    } else {
                        op = "Got unexpected property: " + dp.getName() + " Value: "
                                + dp.getVal().toString();
                    }
                }
                System.out.println("Operation " + op);
                System.out.println("Name " + name);
                System.out.println("Type " + type);
                System.out.println("State " + state);
                System.out.println("Error " + error);
                System.out.println("======================");
            }
        }
        if (objectContents.isEmpty()) {
            System.out.println("Currently no task running");
        }
    }
}
