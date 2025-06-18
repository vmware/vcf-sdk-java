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
import static com.vmware.vim25.ManagedObjectType.FOLDER;
import static java.lang.System.out;

import java.util.List;
import java.util.Map;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ClusterConfigSpec;
import com.vmware.vim25.ComputeResourceConfigSpec;
import com.vmware.vim25.HostConnectSpec;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachineConfigInfoSwapPlacementType;

/** This sample creates managed entity like Host-Standalone, Cluster, Datacenter, and Folder. */
public class Create {
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

    /** REQUIRED: License key. */
    public static String licenseKey = "licenseKey";
    /** REQUIRED: Specifies the name of the parent folder. */
    public static String parentName = "parentName";
    /** REQUIRED: Type of the object to be added e.g. Host-Standalone | Cluster | Datacenter | Folder. */
    public static String itemType = "itemType";
    /** REQUIRED: Name of the item added. */
    public static String itemName = "itemName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(Create.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference taskMoRef;
            Map<String, List<ManagedObjectReference>> folders =
                    propertyCollectorHelper.getObjects(serviceContent.getRootFolder(), FOLDER);
            if (folders.containsKey(parentName) && !folders.get(parentName).isEmpty()) {
                ManagedObjectReference folderMoRef = folders.get(parentName).get(0);
                switch (itemType) {
                    case "Folder":
                        vimPort.createFolder(folderMoRef, itemName);
                        out.println("Successfully created::" + itemName);
                        break;
                    case "Datacenter":
                        vimPort.createDatacenter(folderMoRef, itemName);
                        out.println("Successfully created::" + itemName);
                        break;
                    case "Cluster":
                        ClusterConfigSpec clusterSpec = new ClusterConfigSpec();
                        vimPort.createCluster(folderMoRef, itemName, clusterSpec);
                        out.println("Successfully created::" + itemName);
                        break;
                    case "Host-Standalone":
                        HostConnectSpec hostSpec = new HostConnectSpec();
                        hostSpec.setHostName(itemName);
                        hostSpec.setUserName(username);
                        hostSpec.setPassword(password);
                        hostSpec.setPort(443);

                        ComputeResourceConfigSpec resourceConfigSpec = new ComputeResourceConfigSpec();
                        resourceConfigSpec.setVmSwapPlacement(
                                VirtualMachineConfigInfoSwapPlacementType.VM_DIRECTORY.value());

                        taskMoRef = vimPort.addStandaloneHostTask(
                                folderMoRef, hostSpec, resourceConfigSpec, true, licenseKey);

                        if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                            out.println("Successfully created::" + itemName);
                        } else {
                            out.println("Host'" + itemName + " not created::");
                        }
                        break;
                    default:
                        out.println("Unknown Type. Allowed types are:");
                        out.println(" Host-Standalone");
                        out.println(" Cluster");
                        out.println(" Datacenter");
                        out.println(" Folder");
                        break;
                }
            } else {
                out.println("Parent folder '" + parentName + "' not found");
            }
        }
    }
}
