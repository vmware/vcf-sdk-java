/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.monitoring.events;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayOfEvent;
import com.vmware.vim25.Event;
import com.vmware.vim25.EventFilterSpec;
import com.vmware.vim25.EventFilterSpecByEntity;
import com.vmware.vim25.EventFilterSpecRecursionOption;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/**
 * This sample is responsible for creating EventHistoryCollector filtered for a single VM and monitoring events using
 * the latestPage attribute of the EventHistoryCollector.
 */
public class VMEventHistoryCollectorMonitor {
    private static final Logger log = LoggerFactory.getLogger(VMEventHistoryCollectorMonitor.class);
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

    /** REQUIRED: Name of the virtual machine. */
    public static String vmName = "vmName";

    private static ManagedObjectReference eventHistoryCollectorRef;
    private static ManagedObjectReference eventManagerRef;
    private static ManagedObjectReference vmRef;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VMEventHistoryCollectorMonitor.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            eventManagerRef = serviceContent.getEventManager();
            vmRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);
            if (vmRef != null) {
                createEventHistoryCollector(vimPort);

                ArrayOfEvent arrayEvents = propertyCollectorHelper.fetch(eventHistoryCollectorRef, "latestPage");

                ArrayList<Event> eventList = (ArrayList<Event>) arrayEvents.getEvent();
                log.info("Events In the latestPage are:");
                for (Event anEvent : eventList) {
                    log.info("Event: {}", anEvent.getClass().getName());
                }
            } else {
                log.error("Virtual Machine {} Not Found.", vmName);
            }
        }
    }

    /** Creates the event history collector. */
    private static void createEventHistoryCollector(VimPortType vimPort)
            throws RuntimeFaultFaultMsg, InvalidStateFaultMsg {
        EventFilterSpecByEntity entitySpec = new EventFilterSpecByEntity();
        entitySpec.setEntity(vmRef);
        entitySpec.setRecursion(EventFilterSpecRecursionOption.SELF);

        EventFilterSpec eventFilter = new EventFilterSpec();
        eventFilter.setEntity(entitySpec);

        eventHistoryCollectorRef = vimPort.createCollectorForEvents(eventManagerRef, eventFilter);
    }
}
