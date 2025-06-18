/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.compute.contentlibrary.ovfdeploy;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.CLUSTER_COMPUTE_RESOURCE;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.cxf.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.content.library.Item;
import com.vmware.content.library.ItemTypes;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.ovf.LibraryItem;
import com.vmware.vcenter.ovf.LibraryItemTypes.DeploymentResult;
import com.vmware.vcenter.ovf.LibraryItemTypes.DeploymentTarget;
import com.vmware.vcenter.ovf.LibraryItemTypes.OvfSummary;
import com.vmware.vcenter.ovf.LibraryItemTypes.ResourcePoolDeploymentSpec;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/**
 * Demonstrates the workflow to deploy an OVF library item to a resource pool.
 *
 * <p>Sample Prerequisites: The sample needs an existing OVF library item and an existing cluster with resources for
 * creating the VM.
 */
public class DeployOvfTemplate {
    private static final Logger log = LoggerFactory.getLogger(DeployOvfTemplate.class);
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

    /** REQUIRED: The name of the library item to deploy. The library item should contain an OVF package. */
    public static String libItemName = "libItemName";
    /** REQUIRED: Name of the Cluster in which VM would be created. */
    public static String clusterName = "clusterName";
    /**
     * OPTIONAL: The name of the VM to be created in the cluster. Defaults to a generated VM name based on the current
     * date if not specified.
     */
    public static String vmName = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(DeployOvfTemplate.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            LibraryItem ovfLibraryItemService = client.createStub(LibraryItem.class);
            Item itemService = client.createStub(Item.class);

            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            String virtualMachineName = vmName;
            // Generate a default VM name if it is not provided
            if (StringUtils.isEmpty(virtualMachineName)) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-kkmmss");
                virtualMachineName = "VM-" + sdf.format(new Date());
            }

            // Find the MoRef of the VC cluster using VIM APIs
            ManagedObjectReference clusterMoRef =
                    propertyCollectorHelper.getMoRefByName(clusterName, CLUSTER_COMPUTE_RESOURCE);
            if (clusterMoRef == null) {
                throw new RuntimeException("Cluster by name " + clusterName + " must exist");
            }
            log.info("Cluster MoRef : {} : {}", clusterMoRef.getType(), clusterMoRef.getValue());

            // Find the cluster's root resource pool
            ManagedObjectReference rootResPoolMoRef = propertyCollectorHelper.fetch(clusterMoRef, "resourcePool");
            if (rootResPoolMoRef == null) {
                throw new RuntimeException("Could not fetch cluster's root resource pool");
            }
            log.info("Resource pool MoRef : {} : {}", rootResPoolMoRef.getType(), rootResPoolMoRef.getValue());

            // Find the library item by name
            ItemTypes.FindSpec findSpec = new ItemTypes.FindSpec();
            findSpec.setName(libItemName);

            List<String> itemIds = itemService.find(findSpec);
            if (itemIds.isEmpty()) {
                throw new RuntimeException("Unable to find a library item with name: " + libItemName);
            }

            String itemId = itemIds.get(0);
            log.info("Library item ID : {}", itemId);

            // Deploy a VM from the library item on the given cluster
            log.info("Deploying Vm : {}", virtualMachineName);

            String vmId = deployVMFromOvfItem(ovfLibraryItemService, rootResPoolMoRef, virtualMachineName, itemId);
            if (vmId == null) {
                throw new RuntimeException("Assertion: VM must be deployed");
            }
            log.info("Vm created : {}", vmId);

            // Power on the VM and wait for the power on operation to complete
            ManagedObjectReference vmMoRef = new ManagedObjectReference();
            vmMoRef.setType("VirtualMachine");
            vmMoRef.setValue(vmId);

            powerOnVM(vimPort, propertyCollectorHelper, virtualMachineName, vmMoRef);

            // cleanup
            if (vmMoRef != null) {
                // Power off the VM and wait for the power off operation to complete
                powerOffVM(vimPort, propertyCollectorHelper, virtualMachineName, vmMoRef);

                // Delete the VM
                deleteManagedEntity(vimPort, propertyCollectorHelper, vmMoRef);
            }
        }
    }

    /**
     * Deploying a VM from the Content Library into a cluster.
     *
     * @param rootResPoolMoRef managed object reference of the root resource pool
     * @param vmName the name of the VM to create
     * @param libItemId identifier of the OVF library item to deploy
     * @return the identifier of the created VM
     */
    private static String deployVMFromOvfItem(
            LibraryItem ovfLibraryItemService,
            ManagedObjectReference rootResPoolMoRef,
            String vmName,
            String libItemId) {
        // Creating the deployment.
        DeploymentTarget deploymentTarget = new DeploymentTarget();
        // Setting the target resource pool.
        deploymentTarget.setResourcePoolId(rootResPoolMoRef.getValue());

        // Creating and setting the resource pool deployment spec.
        ResourcePoolDeploymentSpec deploymentSpec = new ResourcePoolDeploymentSpec();
        deploymentSpec.setName(vmName);
        deploymentSpec.setAcceptAllEULA(true);

        // Retrieve the library items OVF information and use it for populating deployment spec.
        OvfSummary ovfSummary = ovfLibraryItemService.filter(libItemId, deploymentTarget);

        // Setting the annotation retrieved from the OVF summary.
        deploymentSpec.setAnnotation(ovfSummary.getAnnotation());

        // Calling the deploy and getting the deployment result.
        DeploymentResult deploymentResult =
                ovfLibraryItemService.deploy(UUID.randomUUID().toString(), libItemId, deploymentTarget, deploymentSpec);

        if (deploymentResult.getSucceeded()) {
            return deploymentResult.getResourceId().getId();
        } else {
            throw new RuntimeException(deploymentResult.getError().toString());
        }
    }

    /** Deletes a managed object and waits for the delete operation to complete. */
    private static void deleteManagedEntity(
            VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference moRef) {
        log.info("Deleting : [{}]", moRef.getValue());

        try {
            ManagedObjectReference taskMoRef = vimPort.destroyTask(moRef);
            if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                log.info(
                        "Successfully deleted Managed Entity - [{}] and Entity Type - [{}]",
                        moRef.getValue(),
                        moRef.getType());
            } else {
                log.error("Unable to delete : [{}]", moRef.getValue());
            }
        } catch (Exception e) {
            log.error("Unable to delete : [{}]", moRef.getValue());
            log.error("Reason :{}", e.getLocalizedMessage());
        }
    }

    /** Powers on VM and wait for power on operation to complete. */
    private static void powerOnVM(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            String vmName,
            ManagedObjectReference vmMoRef) {
        log.info("Powering on virtual machine : {}[{}]", vmName, vmMoRef.getValue());

        try {
            ManagedObjectReference taskMoRef = vimPort.powerOnVMTask(vmMoRef, null);
            if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                log.info("[{}] powered on successfully, {}", vmName, vmMoRef.getValue());
            } else {
                log.error("Unable to power on vm : {}[{}]", vmName, vmMoRef.getValue());
            }
        } catch (Exception e) {
            log.error("Unable to power on vm : {}[{}]", vmName, vmMoRef.getValue());
            log.error("Reason :{}", e.getLocalizedMessage());
        }
    }

    /** Powers off VM and waits for power off operation to complete. */
    private static void powerOffVM(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            String vmName,
            ManagedObjectReference vmMoRef) {
        log.info("Powering off virtual machine : {}[{}]", vmName, vmMoRef.getValue());

        try {
            ManagedObjectReference taskMoRef = vimPort.powerOffVMTask(vmMoRef);
            if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                log.info("[{}] powered off successfully, {}", vmName, vmMoRef.getValue());
            } else {
                log.error("Unable to power off vm : {}[{}]", vmName, vmMoRef.getValue());
            }
        } catch (Exception e) {
            log.error("Unable to power off vm : {}[{}]", vmName, vmMoRef.getValue());
            log.error("Reason :{}", e.getLocalizedMessage());
        }
    }
}
