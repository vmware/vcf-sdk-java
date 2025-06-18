/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.compute.vm;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VimPortType;

/**
 * Demonstrates finding a Virtual machine by its name using {@link com.vmware.vim25.TraversalSpec} specification to get
 * to the VirtualMachine managed object.
 */
public class FindVmByName {
    private static final Logger log = LoggerFactory.getLogger(FindVmByName.class);
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

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FindVmByName.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            TraversalSpec traversalSpec = getVMTraversalSpec();

            PropertySpec propertySpec = new PropertySpec();
            propertySpec.setAll(Boolean.FALSE);
            propertySpec.getPathSet().add("name");
            propertySpec.setType("VirtualMachine");

            ObjectSpec objectSpec = new ObjectSpec();
            objectSpec.setObj(serviceContent.getRootFolder());
            objectSpec.setSkip(Boolean.TRUE);
            objectSpec.getSelectSet().add(traversalSpec);

            PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
            propertyFilterSpec.getPropSet().add(propertySpec);
            propertyFilterSpec.getObjectSet().add(objectSpec);

            RetrieveResult retrieveResult = vimPort.retrievePropertiesEx(
                    serviceContent.getPropertyCollector(),
                    Collections.singletonList(propertyFilterSpec),
                    new RetrieveOptions());

            AtomicReference<ManagedObjectReference> vmMoRef = new AtomicReference<>();
            propertyCollectorHelper.iterateObjects(retrieveResult, oc -> {
                ManagedObjectReference moRef = oc.getObj();
                String name = null;
                List<DynamicProperty> dps = oc.getPropSet();
                if (dps != null) {
                    for (DynamicProperty dp : dps) {
                        name = (String) dp.getVal();
                    }
                }
                if (name != null && name.equals(vmName)) {
                    vmMoRef.set(moRef);
                    return true;
                }
                return false;
            });

            if (vmMoRef.get() != null) {
                log.info(
                        "Found Virtual machine: {} with VM name: {}",
                        vmMoRef.get().getValue(),
                        vmName);
            } else {
                log.error("No Virtual machine was found with VM name: {}", vmName);
            }
        }
    }

    /** @return TraversalSpec specification to get to the VirtualMachine managed object. */
    private static TraversalSpec getVMTraversalSpec() {
        // Create a traversal spec that starts from the 'root' objects
        // and traverses the inventory tree to get to the VirtualMachines.
        // Build the traversal specs bottoms up

        SelectionSpec vAppToVMSelectionSpec = new SelectionSpec();
        vAppToVMSelectionSpec.setName("vAppToVApp");

        SelectionSpec vAppToVAppSelectionSpec = new SelectionSpec();
        vAppToVAppSelectionSpec.setName("vAppToVM");

        // Traversal to get to the VM in a VApp
        TraversalSpec vAppToVM = new TraversalSpec();
        vAppToVM.setName("vAppToVM");
        vAppToVM.setType("VirtualApp");
        vAppToVM.setPath("vm");

        // Traversal spec for VApp to VApp
        TraversalSpec vAppToVApp = new TraversalSpec();
        vAppToVApp.setName("vAppToVApp");
        vAppToVApp.setType("VirtualApp");
        vAppToVApp.setPath("resourcePool");
        vAppToVApp.getSelectSet().addAll(List.of(vAppToVMSelectionSpec, vAppToVAppSelectionSpec));

        // This SelectionSpec is used for recursion for Folder recursion
        SelectionSpec visitFolders = new SelectionSpec();
        visitFolders.setName("VisitFolders");

        // Traversal to get to the vmFolder from DataCenter
        TraversalSpec dataCenterToVMFolder = new TraversalSpec();
        dataCenterToVMFolder.setName("DataCenterToVMFolder");
        dataCenterToVMFolder.setType("Datacenter");
        dataCenterToVMFolder.setPath("vmFolder");
        dataCenterToVMFolder.setSkip(Boolean.FALSE);
        dataCenterToVMFolder.getSelectSet().add(visitFolders);

        // TraversalSpec to get to the DataCenter from rootFolder
        TraversalSpec vmTraversalSpec = new TraversalSpec();
        vmTraversalSpec.setName("VisitFolders");
        vmTraversalSpec.setType("Folder");
        vmTraversalSpec.setPath("childEntity");
        vmTraversalSpec.setSkip(Boolean.FALSE);
        vmTraversalSpec.getSelectSet().addAll(List.of(visitFolders, dataCenterToVMFolder, vAppToVM, vAppToVApp));

        return vmTraversalSpec;
    }
}
