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

import org.apache.cxf.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidPowerStateFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ManagedObjectType;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TaskInProgressFaultMsg;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.VimFaultFaultMsg;
import com.vmware.vim25.VimPortType;

/** This sample demonstrates Destroy or Unregister Managed Inventory Object like a Host, VM, Folder, etc. */
public class RemoveManagedObject {
    private static final Logger log = LoggerFactory.getLogger(RemoveManagedObject.class);
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

    private static final String[] OBJECT_TYPES = {"HostSystem", "VirtualMachine", "Folder", "ResourcePool", "Datacenter"
    };

    /** REQUIRED: Name of the object. */
    public static String objName = "objName";
    /** REQUIRED: Type of managedobject to remove or unregister, e.g. HostSystem, Datacenter, ResourcePool, Folder. */
    public static String objType = "objType";
    /** OPTIONAL: Name of the operation - [remove | unregister]. */
    public static String operation = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(RemoveManagedObject.class, args);

        String op = operation;

        validateTheInput(op);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            if (StringUtils.isEmpty(op) && (objType.equalsIgnoreCase("VirtualMachine"))) {
                op = "unregisterVM";
            } else if (StringUtils.isEmpty(op) && !(objType.equalsIgnoreCase("VirtualMachine"))) {
                op = "remove";
            } else if (!("remove".equals(op)) && !("unregisterVM".equals(op))) {
                op = "unregisterVM";
            }
            deleteManagedObjectReference(vimPort, propertyCollectorHelper, op);
        }
    }

    private static boolean validateObjectType(final String type) {
        boolean found = false;

        for (String name : OBJECT_TYPES) {
            found |= name.equalsIgnoreCase(type);
        }

        return found;
    }

    private static boolean validateTheInput(String op) {
        if (op != null) {
            if (!(op.equalsIgnoreCase("remove")) && (!(op.equalsIgnoreCase("unregister")))) {
                throw new IllegalArgumentException("Invalid Operation type");
            }
        }

        if (!validateObjectType(objType)) {
            final StringBuilder list = new StringBuilder();
            for (final String name : OBJECT_TYPES) {
                list.append("'");
                list.append(name);
                list.append("' ");
            }
            throw new IllegalArgumentException(
                    String.format("Invalid --objtype %s! Object Type should be one of: %s", objType, list));
        }
        return true;
    }

    private static void deleteManagedObjectReference(
            VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper, String op)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, VimFaultFaultMsg, InvalidCollectorVersionFaultMsg,
                    TaskInProgressFaultMsg, InvalidPowerStateFaultMsg {

        ManagedObjectReference objMoRef =
                propertyCollectorHelper.getMoRefByName(objName, ManagedObjectType.fromValue(objType));

        if (objMoRef != null) {
            if ("remove".equals(op)) {
                ManagedObjectReference taskMoRef = vimPort.destroyTask(objMoRef);
                String[] opts = new String[] {"info.state", "info.error"};
                String[] opt = new String[] {"state"};

                Object[] result = propertyCollectorHelper.awaitManagedObjectUpdates(
                        taskMoRef, opts, opt, new Object[][] {new Object[] {TaskInfoState.SUCCESS, TaskInfoState.ERROR}
                        });
                if (result[0].equals(TaskInfoState.SUCCESS)) {
                    log.info("Success Managed Entity - [ {} ] deleted \n", objName);
                } else {
                    log.warn("Failure Deletion of Managed Entity - [ {} ] \n", objName);
                }
            } else if ("VirtualMachine".equalsIgnoreCase(objType)) {
                vimPort.unregisterVM(objMoRef);
            } else {
                throw new IllegalArgumentException("Invalid Operation specified.");
            }
            log.info("Successfully completed {} for {} : {}", op, objType, objName);
        } else {
            log.error("Unable to find object of type {} with name {}", objType, objName);
            log.error(": Failed {} of {} : {}", op, objType, objName);
        }
    }
}
