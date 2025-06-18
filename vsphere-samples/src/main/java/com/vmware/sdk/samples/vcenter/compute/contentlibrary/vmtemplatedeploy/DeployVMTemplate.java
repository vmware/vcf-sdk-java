/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.compute.contentlibrary.vmtemplatedeploy;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.cxf.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.content.library.Item;
import com.vmware.content.library.ItemTypes;
import com.vmware.sdk.samples.helpers.DatacenterHelper;
import com.vmware.sdk.samples.helpers.DatastoreHelper;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.Datacenter;
import com.vmware.vcenter.Datastore;
import com.vmware.vcenter.Folder;
import com.vmware.vcenter.FolderTypes;
import com.vmware.vcenter.ResourcePool;
import com.vmware.vcenter.ResourcePoolTypes;
import com.vmware.vcenter.VM;
import com.vmware.vcenter.vm_template.LibraryItems;
import com.vmware.vcenter.vm_template.LibraryItemsTypes;

/**
 * Demonstrates how to deploy a virtual machine from a library item containing a virtual machine template.
 *
 * <p>Sample Prerequisites:
 *
 * <ul>
 *   <li>A library item containing a virtual machine template
 *   <li>A datacenter
 *   <li>A VM folder
 *   <li>A resource pool
 *   <li>A datastore
 * </ul>
 */
public class DeployVMTemplate {
    private static final Logger log = LoggerFactory.getLogger(DeployVMTemplate.class);
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

    /** REQUIRED: The name of the vm folder in which to create the vm. */
    public static String vmFolderName = "vmFolderName";
    /** OPTIONAL: The name of the vm to be created. */
    public static String vmName = null;
    /** REQUIRED: The name of the datastore in which to create the vm. */
    public static String datastoreName = "datastoreName";
    /** REQUIRED: The name of the datacenter in which to create the vm. */
    public static String datacenterName = "datacenterName";
    /** REQUIRED: The name of the resource pool in the datacenter in which to place the deployed VM. */
    public static String resourcePoolName = "resourcePoolName";
    /** REQUIRED: The name of the vm template in a content library. */
    public static String libItemName = "libItemName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(DeployVMTemplate.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VM vmService = client.createStub(VM.class);
            LibraryItems vmLibraryItemService = client.createStub(LibraryItems.class);
            Datastore datastoreService = client.createStub(Datastore.class);
            Datacenter datacenterService = client.createStub(Datacenter.class);
            Folder folderService = client.createStub(Folder.class);
            Item itemService = client.createStub(Item.class);

            String virtualMachineName = vmName;
            // Generate a default VM name if it is not provided
            if (StringUtils.isEmpty(virtualMachineName)) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-kkmmss");
                virtualMachineName = "VM-" + sdf.format(new Date());
            }

            // find template id by name
            String templateId = findTemplateId(itemService);

            // Find folder id by name
            String folderId = findFolderId(folderService, datacenterService);

            // Find resource Pool id by name
            ResourcePool resourcePoolService = client.createStub(ResourcePool.class);
            String resourcePoolId = findResourcePoolId(resourcePoolService, datacenterService);

            // Find datastore id by name
            String datastoreId = findDatastoreId(datastoreService);

            // Specify the place in the inventory on which to deploy the VM such as an ESXi
            // host, resource pool and VM folder
            // If getHost() and getResourcePool() are both specified, getResourcePool() must
            // belong to getHost().
            // If getHost() and getCluster() are both specified, getHost() must be a member
            // of getCluster().
            // This property may be null if getResourcePool() or getCluster() is specified.
            LibraryItemsTypes.DeployPlacementSpec placementSpec = new LibraryItemsTypes.DeployPlacementSpec.Builder()
                    .setResourcePool(
                            resourcePoolId) // Resource pool into which the deployed virtual machine should be placed.
                    .setFolder(
                            folderId) // Required. Virtual machine folder into which the deployed virtual machine should
                    // be placed.
                    .build();

            // Specify the place in the inventory on which to deploy the virtual machine
            // such as an ESXi host, resource pool, and VM folder.
            LibraryItems.DeploySpecVmHomeStorage vmHomeStorageSpec =
                    new LibraryItemsTypes.DeploySpecVmHomeStorage.Builder()
                            .setDatastore(datastoreId)
                            .build();

            LibraryItems.DeploySpecDiskStorage diskStorageSpec = new LibraryItemsTypes.DeploySpecDiskStorage.Builder()
                    .setDatastore(datastoreId)
                    .build();

            // (Optional) Specify the guest operating system and hardware customization
            // specifications that you want to
            // apply to the VM during the deployment process and include them in the
            // deployment specification.
            // You can use the GuestCustomizationSpec and HardwareCustomizationSpec classes

            // deployment specification
            LibraryItems.DeploySpec deploySpec = new LibraryItems.DeploySpec.Builder(virtualMachineName)
                    .setPlacement(placementSpec)
                    .setVmHomeStorage(vmHomeStorageSpec)
                    .setDiskStorage(diskStorageSpec)
                    .build();

            // Deploy a virtual machine from the VM template item
            log.info("Deploying a virtual machine from VM template item...");
            String vmId = vmLibraryItemService.deploy(templateId, deploySpec);

            if (vmId == null) {
                throw new RuntimeException("Assertion: VM must be deployed");
            }
            log.info("Vm {} created with id: {}", virtualMachineName, vmId);

            // cleanup
            // Delete the VM
            log.info("#### Deleting the VM");
            vmService.delete(vmId);
        }
    }

    private static String findDatastoreId(Datastore datastoreService) {
        String datastoreId = DatastoreHelper.getDatastore(datastoreService, datastoreName);

        if (datastoreId.isEmpty()) {
            throw new RuntimeException("Unable to find datastore : " + datastoreName);
        }

        log.info("Datastore Id found: {}", datastoreId);

        return datastoreId;
    }

    private static String findResourcePoolId(ResourcePool resourcePoolService, Datacenter datacenterService) {
        String resourcePoolId =
                getResourcePool(resourcePoolService, resourcePoolName, datacenterService, datacenterName);

        if (resourcePoolId.isEmpty()) {
            throw new RuntimeException("Unable to find resource Pool : " + resourcePoolName);
        }

        log.info("Resource Pool Id found: {}", resourcePoolId);

        return resourcePoolId;
    }

    private static String findFolderId(Folder folderService, Datacenter datacenterService) {
        String folderId = getFolder(folderService, vmFolderName, datacenterService, datacenterName);

        if (folderId.isEmpty()) {
            throw new RuntimeException("Unable to find folder: " + vmFolderName);
        }

        log.info("Folder id found: {}", folderId);

        return folderId;
    }

    private static String findTemplateId(Item itemService) {
        ItemTypes.FindSpec findSpec = new ItemTypes.FindSpec();
        findSpec.setName(libItemName);

        List<String> itemIds = itemService.find(findSpec);

        if (itemIds.isEmpty()) {
            throw new RuntimeException("Unable to find template with name: " + libItemName);
        }

        String templateId = itemIds.get(0);

        log.info("Template id found: {}", templateId);

        return templateId;
    }

    /**
     * Returns the identifier of a folder.
     *
     * <p>Note: The method assumes that there is only one folder and datacenter with the specified names.
     *
     * @param folderService Folder service stub
     * @param folderName name of the folder
     * @param datacenterService Datacenter service stub
     * @param datacenterName name of the datacenter
     * @return identifier of a folder
     */
    private static String getFolder(
            Folder folderService, String folderName, Datacenter datacenterService, String datacenterName) {
        // Get the folder
        Set<String> vmFolders = Collections.singleton(folderName);
        FolderTypes.FilterSpec.Builder vmFolderFilterSpecBuilder =
                new FolderTypes.FilterSpec.Builder().setNames(vmFolders);

        if (null != datacenterName) {
            // Get the datacenter
            Set<String> datacenters =
                    Collections.singleton(DatacenterHelper.getDatacenter(datacenterService, datacenterName));
            vmFolderFilterSpecBuilder.setDatacenters(datacenters);
        }
        List<FolderTypes.Summary> folderSummaries = folderService.list(vmFolderFilterSpecBuilder.build());

        if (folderSummaries.isEmpty()) {
            throw new RuntimeException("Folder " + folderName + "not found in datacenter: " + datacenterName);
        }
        return folderSummaries.get(0).getFolder();
    }

    /**
     * Returns the identifier of a resource pool.
     *
     * <p>Note: The method assumes that there is only one resource pool and datacenter with the mentioned names.
     *
     * @param resourcePoolService ResourcePool service stub
     * @param resourcePoolName name of the resource pool
     * @param datacenterService Datacenter service stub
     * @param datacenterName name of the datacenter
     * @return identifier of a resource pool
     */
    private static String getResourcePool(
            ResourcePool resourcePoolService,
            String resourcePoolName,
            Datacenter datacenterService,
            String datacenterName) {
        // Get the resource pool
        Set<String> resourcePools = Collections.singleton(resourcePoolName);

        ResourcePoolTypes.FilterSpec resourcePoolFilterSpec;
        List<ResourcePoolTypes.Summary> resourcePoolSummaries;

        if (null == datacenterName) {
            resourcePoolFilterSpec = new ResourcePoolTypes.FilterSpec.Builder()
                    .setNames(resourcePools)
                    .build();
            resourcePoolSummaries = resourcePoolService.list(resourcePoolFilterSpec);

            if (resourcePoolSummaries.isEmpty()) {
                throw new RuntimeException("Resource Pool " + resourcePoolName + "not found");
            }
        } else {
            // Get the datacenter
            Set<String> datacenters =
                    Collections.singleton(DatacenterHelper.getDatacenter(datacenterService, datacenterName));

            resourcePoolFilterSpec = new ResourcePoolTypes.FilterSpec.Builder()
                    .setNames(resourcePools)
                    .setDatacenters(datacenters)
                    .build();

            resourcePoolSummaries = resourcePoolService.list(resourcePoolFilterSpec);

            if (resourcePoolSummaries.isEmpty()) {
                throw new RuntimeException(
                        "Resource Pool " + resourcePoolName + "not found in datacenter: " + datacenterName);
            }
        }
        return resourcePoolSummaries.get(0).getResourcePool();
    }
}
