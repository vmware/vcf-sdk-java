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

import java.util.Hashtable;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayOfEventDescriptionEventDetail;
import com.vmware.vim25.Event;
import com.vmware.vim25.EventDescriptionEventDetail;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.UserLoginSessionEvent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VmPoweredOnEvent;
import com.vmware.vim25.VmRenamedEvent;

/** This sample retrieves and formats the lastEvent from Hostd or Vpxd. */
public class EventFormat {
    private static final Logger log = LoggerFactory.getLogger(EventFormat.class);
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
        SampleCommandLineParser.load(EventFormat.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference eventManagerMoRef = serviceContent.getEventManager();
            log.info("Event Manager Value {}", eventManagerMoRef.getValue());

            ArrayOfEventDescriptionEventDetail arrayEventDetails =
                    propertyCollectorHelper.fetch(eventManagerMoRef, "description.eventInfo");

            List<EventDescriptionEventDetail> eventDetails = arrayEventDetails.getEventDescriptionEventDetail();

            Hashtable<String, EventDescriptionEventDetail> eventDetail = new Hashtable<>();
            for (EventDescriptionEventDetail ed : eventDetails) {
                eventDetail.put(ed.getKey(), ed);
            }

            Event event = propertyCollectorHelper.fetch(eventManagerMoRef, "latestEvent");

            // Get the 'latestEvent' property of the EventManager
            log.info("The latestEvent was : {}", event.getClass().getName());
            formatEvent(0, eventDetail, event);
            formatEvent(1, eventDetail, event);
            formatEvent(2, eventDetail, event);
            formatEvent(3, eventDetail, event);
            formatEvent(4, eventDetail, event);
        }
    }

    private static void formatEvent(
            int fType, Hashtable<String, EventDescriptionEventDetail> eventDetail, Event event) {
        String typeName = event.getClass().getName();
        // Remove package information...
        int lastDot = typeName.lastIndexOf('.');
        if (lastDot != -1) {
            typeName = typeName.substring(lastDot + 1);
        }

        EventDescriptionEventDetail detail = eventDetail.get(typeName);
        // Determine format string
        String format = detail.getFullFormat();
        switch (fType) {
            case 2:
                format = detail.getFormatOnComputeResource();
                break;
            case 3:
                format = detail.getFormatOnDatacenter();
                break;
            case 1:
                format = detail.getFormatOnHost();
                break;
            case 0:
                format = detail.getFormatOnVm();
                break;
            case 4:
                format = detail.getFullFormat();
                break;
            default:
                log.debug("Unknown event: {} for description: {} ", fType, detail);
        }
        String ret = "";
        switch (typeName) {
            case "VmPoweredOnEvent":
                ret = replaceText(format, (VmPoweredOnEvent) event);
                if (ret != null) {
                    log.info(ret);
                }
                break;
            case "VmRenamedEvent":
                ret = replaceText(format, (VmRenamedEvent) event);
                if (ret != null) {
                    log.info(ret);
                }
                break;
            case "UserLoginSessionEvent":
                ret = replaceText(format, (UserLoginSessionEvent) event);
                if (ret != null) {
                    log.info(ret);
                }
                break;
            default:
                // Try generic, if all values are replaced by base type
                // return that, else return fullFormattedMessage;
                ret = replaceText(format, event);
                if (ret.isEmpty() || ret.contains("{")) {
                    ret = event.getFullFormattedMessage();
                }
                if (ret != null) {
                    log.info(ret);
                }
                break;
        }
    }

    private static String replaceText(String format, UserLoginSessionEvent theEvent) {
        // Do base first
        format = replaceText(format, (Event) theEvent);
        // Then specific values
        format = format.replaceAll("\\{ipAddress\\}", theEvent.getIpAddress());
        return format;
    }

    private static String replaceText(String format, VmPoweredOnEvent theEvent) {
        // Same as base type
        return replaceText(format, (Event) theEvent);
    }

    private static String replaceText(String format, VmRenamedEvent theEvent) {
        // Do base first
        format = replaceText(format, (Event) theEvent);
        // Then specific values
        format = format.replaceAll("\\{oldName\\}", theEvent.getOldName());
        format = format.replaceAll("\\{newName\\}", theEvent.getNewName());
        return format;
    }

    private static String replaceText(String format, Event theEvent) {
        format = format.replaceAll("\\{userName\\}", theEvent.getUserName());
        if (theEvent.getComputeResource() != null) {
            format = format.replaceAll(
                    "\\{computeResource.name\\}", theEvent.getComputeResource().getName());
        }
        if (theEvent.getDatacenter() != null) {
            format = format.replaceAll(
                    "\\{datacenter.name\\}", theEvent.getDatacenter().getName());
        }
        if (theEvent.getHost() != null) {
            format = format.replaceAll("\\{host.name\\}", theEvent.getHost().getName());
        }
        if (theEvent.getVm() != null) {
            format = format.replaceAll("\\{vm.name\\}", theEvent.getVm().getName());
        }
        return format;
    }
}
