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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/** Demonstrates removing alarm for a specific Virtual Machine. */
public class RemoveVMAlarms {
    private static final Logger log = LoggerFactory.getLogger(RemoveVMAlarms.class);
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

    /** REQUIRED: Name of the virtual machine to remove alarms for. */
    public static String vmname = "VMName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(RemoveVMAlarms.class, args);

        VcenterClientFactory clientFactory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));
        try (VcenterClient client = clientFactory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference alarmManagerMoRef = serviceContent.getAlarmManager();

            ManagedObjectReference vmMoRef = propertyCollectorHelper.getMoRefByName(vmname, VIRTUAL_MACHINE);
            if (vmMoRef != null) {
                List<ManagedObjectReference> alarmMoRefs = vimPort.getAlarm(alarmManagerMoRef, vmMoRef);
                if (alarmMoRefs.isEmpty()) {
                    log.error("Alarm Not Found for VM {} ", vmname);
                } else {
                    for (ManagedObjectReference alarmMoRef : alarmMoRefs) {
                        vimPort.removeAlarm(alarmMoRef);
                        log.info("Successfully removed Alarm: {}", alarmMoRef.getValue());
                    }
                }
            } else {
                log.error("Virtual Machine {} Not Found", vmname);
            }
        }
    }
}
