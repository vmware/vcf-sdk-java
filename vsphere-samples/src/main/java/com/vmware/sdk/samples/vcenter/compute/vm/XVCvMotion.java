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
import static com.vmware.vim25.ManagedObjectType.CLUSTER_COMPUTE_RESOURCE;
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.xml.ws.soap.SOAPFaultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayOfVirtualDevice;
import com.vmware.vim25.ClusterAction;
import com.vmware.vim25.ClusterRecommendation;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PlacementAction;
import com.vmware.vim25.PlacementResult;
import com.vmware.vim25.PlacementSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.ResourceAllocationInfo;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.ServiceLocator;
import com.vmware.vim25.ServiceLocatorNamePassword;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualMachineMovePriority;
import com.vmware.vim25.VirtualMachineRelocateSpec;

/** Used to Relocate VM from one VC to another. */
public class XVCvMotion {
    private static final Logger log = LoggerFactory.getLogger(XVCvMotion.class);
    /** REQUIRED: vCenter FQDN or IP address. */
    public static String sourceVcenterAddress = "vcenter1.mycompany.com";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";
    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (do not leave it empty on production environments).
     */
    public static String trustStorePath = null;

    /** REQUIRED: Name of the virtual machine to be migrated. */
    public static String vmName = "vmName";
    /** REQUIRED: Name of the cluster on target virtual center where to migrate the virtual machine. */
    public static String destCluster = "destCluster";
    /** REQUIRED: vCenter FQDN or IP address of target virtual center. */
    public static String destinationVcenterAddress = "remoteURL";
    /** REQUIRED: Username for the authentication to the target virtual center. */
    public static String ruser = "ruser";
    /** REQUIRED: Password for the authentication to the target virtual center. */
    public static String rpassword = "rpassword";
    /** REQUIRED: Thumbprint of the target virtual center. */
    public static String rthumbprint = "rthumbprint";
    /** OPTIONAL: Folder on the target virtual center where to migrate the virtual machine. */
    public static String targetFolder = null; // default value for targetFolder is vm

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(XVCvMotion.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(sourceVcenterAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient sourceVcenterClient = factory.createClient(username, password, null)) {
            VimPortType sourceVimPort = sourceVcenterClient.getVimPort();
            ServiceContent sourceServiceContent = sourceVcenterClient.getVimServiceContent();
            PropertyCollectorHelper sourcePropertyCollectorHelper =
                    new PropertyCollectorHelper(sourceVimPort, sourceServiceContent);

            log.info("Searching for VM '{}'..", vmName);
            ManagedObjectReference vmMoRef = sourcePropertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);
            if (vmMoRef == null) {
                throw new IllegalStateException("No VM by the name of '" + vmName + "' found!");
            }
            log.info("Found VM: {}", vmName);

            Map<String, Object> vmProperties = sourcePropertyCollectorHelper.fetchProperties(
                    vmMoRef,
                    "config.version",
                    "config.cpuAllocation",
                    "config.memoryAllocation",
                    "config.hardware.numCPU",
                    "config.hardware.memoryMB",
                    "config.files",
                    "config.swapPlacement",
                    "config.hardware.device",
                    "config.name");

            // Setting VirtualMachineConfigSpec properties
            log.info("Setting VirtualMachineConfigSpec properties--");
            VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
            configSpec.setVersion((String) vmProperties.get("config.version"));
            configSpec.setCpuAllocation((ResourceAllocationInfo) vmProperties.get("config.cpuAllocation"));
            configSpec.setMemoryAllocation((ResourceAllocationInfo) vmProperties.get("config.memoryAllocation"));
            configSpec.setNumCPUs((Integer) vmProperties.get("config.hardware.numCPU"));

            Integer memoryMBs = (Integer) vmProperties.get("config.hardware.memoryMB");
            configSpec.setMemoryMB(memoryMBs.longValue());
            configSpec.setFiles((VirtualMachineFileInfo) vmProperties.get("config.files"));
            configSpec.setSwapPlacement((String) vmProperties.get("config.swapPlacement"));
            configSpec.setName((String) vmProperties.get("config.name"));

            List<VirtualDevice> virtualDevices =
                    ((ArrayOfVirtualDevice) vmProperties.get("config.hardware.device")).getVirtualDevice();
            for (VirtualDevice device : virtualDevices) {
                VirtualDeviceConfigSpec virtualDeviceConfigSpec = new VirtualDeviceConfigSpec();
                virtualDeviceConfigSpec.setDevice(device);

                configSpec.getDeviceChange().add(virtualDeviceConfigSpec);
            }

            PlacementSpec placementSpec = new PlacementSpec();
            placementSpec.setConfigSpec(configSpec);

            // Connection to Destination VC
            log.info("Connecting to Destination vCenter - {}", destinationVcenterAddress);
            try (VcenterClient destVcenterClient = factory.createClient(username, password, null)) {
                VimPortType destVimPort = destVcenterClient.getVimPort();
                ServiceContent destServiceContent = destVcenterClient.getVimServiceContent();
                PropertyCollectorHelper destPropertyCollectorHelper =
                        new PropertyCollectorHelper(destVimPort, destServiceContent);

                // clusters will contain the list of Clusters available
                log.info("Looking for the Cluster defined on Destination VC");
                ManagedObjectReference destClusterMoRef =
                        destPropertyCollectorHelper.getMoRefByName(destCluster, CLUSTER_COMPUTE_RESOURCE);
                if (destClusterMoRef == null) {
                    throw new IllegalStateException("No Cluster by the name of '" + destCluster + "' found!");
                }
                log.info("Found Cluster '{}'on Destination vCenter!", destCluster);

                log.info("Getting Recommendations from DRS for XVCvMotion--");
                PlacementAction action;
                PlacementResult placementResult;
                try {
                    placementResult = destVimPort.placeVm(destClusterMoRef, placementSpec);
                    action = getPlacementAction(placementResult);
                } catch (SOAPFaultException e) {
                    if (e.getMessage().contains("vim.fault.InvalidState")) {
                        throw new IllegalStateException("Check the Cluster setting and make sure that DRS is enabled");
                    } else {
                        throw new IllegalStateException(e.getMessage());
                    }
                }

                if (action != null) {
                    ManagedObjectReference vmFolderMoRef;

                    if (targetFolder == null) {
                        log.info("Target Folder undefined Using Default VM Folder");
                        vmFolderMoRef = getVMFolderMoRef(destPropertyCollectorHelper, destClusterMoRef, "Folder", "vm");
                    } else {
                        log.info("Setting Defined TargetFolder as VMFolder for XVCvMotion");
                        vmFolderMoRef =
                                getVMFolderMoRef(destPropertyCollectorHelper, destClusterMoRef, "Folder", targetFolder);
                    }

                    if (vmFolderMoRef != null) {
                        ServiceLocatorNamePassword serviceLocatorNamePassword = new ServiceLocatorNamePassword();
                        serviceLocatorNamePassword.setPassword(rpassword);
                        serviceLocatorNamePassword.setUsername(ruser);

                        ServiceLocator locator = new ServiceLocator();
                        locator.setCredential(serviceLocatorNamePassword);
                        locator.setUrl(destinationVcenterAddress);
                        locator.setInstanceUuid(destServiceContent.getAbout().getInstanceUuid());
                        locator.setSslThumbprint(rthumbprint);

                        VirtualMachineRelocateSpec relocateSpec = action.getRelocateSpec();
                        relocateSpec.setService(locator);
                        // Manually set the folder else Exception would be thrown
                        relocateSpec.setFolder(vmFolderMoRef);

                        log.info("Relocation in Progress!");
                        ManagedObjectReference taskMoRef = sourceVimPort.relocateVMTask(
                                vmMoRef, relocateSpec, VirtualMachineMovePriority.DEFAULT_PRIORITY);

                        if (sourcePropertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                            log.info("Relocation done successfully");
                        } else {
                            log.error("Relocation failed");
                        }
                    } else {
                        throw new IllegalStateException("No Folder by the name of '" + targetFolder + "' found!");
                    }
                } else {
                    log.error("Recommendations are not correct");
                }
            }
        }
    }

    /**
     * This method returns {@link ManagedObjectReference} to VM root Folder.
     *
     * @param moRef for the starting point of the filter
     * @param type of the entity we are looking for
     * @return {@link ManagedObjectReference} to VM root folder
     */
    private static ManagedObjectReference getVMFolderMoRef(
            PropertyCollectorHelper destPropertyCollectorHelper, ManagedObjectReference moRef, String type, String name)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {

        List<SelectionSpec> traverseSpec = buildVmFolderTraversal();
        ManagedObjectReference vmFolderMoRef = null;

        // Create PropertySpec
        PropertySpec propertySpec = new PropertySpec();
        propertySpec.setAll(Boolean.FALSE);
        propertySpec.getPathSet().add("name");
        propertySpec.setType(type);

        // Now create ObjectSpec
        ObjectSpec objectSpec = new ObjectSpec();
        objectSpec.setObj(moRef);
        objectSpec.setSkip(Boolean.TRUE);
        objectSpec.getSelectSet().addAll(traverseSpec);

        // Create PropertyFilterSpec using the PropertySpec and ObjectSpec
        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
        propertyFilterSpec.getPropSet().add(propertySpec);
        propertyFilterSpec.getObjectSet().add(objectSpec);

        List<PropertyFilterSpec> propertyFilterSpecs = new ArrayList<>(1);
        propertyFilterSpecs.add(propertyFilterSpec);

        List<ObjectContent> objectContentList = destPropertyCollectorHelper.retrieveAllProperties(propertyFilterSpecs);

        for (ObjectContent objectContent : objectContentList) {
            ManagedObjectReference mr = objectContent.getObj();
            String entityName = null;

            List<DynamicProperty> listDynamicProps = objectContent.getPropSet();
            DynamicProperty[] dps = listDynamicProps.toArray(new DynamicProperty[listDynamicProps.size()]);
            if (dps != null) {
                for (DynamicProperty dp : dps) {
                    entityName = (String) dp.getVal();
                }
            }
            if (entityName != null && entityName.equals(name)) {
                vmFolderMoRef = mr;
                break;
            }
        }

        return vmFolderMoRef;
    }

    /**
     * This is used to get the MOR for the Folder where to place the VM. It includes traversal from CLuster to
     * datacenter and then to VMFolder.
     */
    private static List<SelectionSpec> buildVmFolderTraversal() {
        // DC -> VM Folder
        TraversalSpec dcToVmFolderSpec = new TraversalSpec();
        dcToVmFolderSpec.setType("Datacenter");
        dcToVmFolderSpec.setSkip(Boolean.FALSE);
        dcToVmFolderSpec.setPath("vmFolder");
        dcToVmFolderSpec.setName("dcToVmf");
        dcToVmFolderSpec.getSelectSet().add(getSelectionSpec("visitFolders"));

        // CLUSTER -> Parent
        TraversalSpec clusterToParentSpec = new TraversalSpec();
        clusterToParentSpec.setType("ClusterComputeResource");
        clusterToParentSpec.setSkip(Boolean.FALSE);
        clusterToParentSpec.setPath("parent");
        clusterToParentSpec.setName("cctoparent");
        clusterToParentSpec.getSelectSet().add(getSelectionSpec("foldertoparent"));

        // Folder -> Parent
        TraversalSpec folderToParentSpec = new TraversalSpec();
        folderToParentSpec.setType("Folder");
        folderToParentSpec.setSkip(Boolean.FALSE);
        folderToParentSpec.setPath("parent");
        folderToParentSpec.setName("foldertoparent");
        folderToParentSpec.getSelectSet().add(getSelectionSpec("foldertoparent"));
        folderToParentSpec.getSelectSet().add(getSelectionSpec("dcToVmf"));

        // For Folder -> Folder recursion
        TraversalSpec visitFoldersSpec = new TraversalSpec();
        visitFoldersSpec.setType("Folder");
        visitFoldersSpec.setPath("childEntity");
        visitFoldersSpec.setSkip(Boolean.FALSE);
        visitFoldersSpec.setName("visitFolders");
        visitFoldersSpec.getSelectSet().add(getSelectionSpec("visitFolders"));

        List<SelectionSpec> resultSpec = new ArrayList<>();
        resultSpec.add(visitFoldersSpec);
        resultSpec.add(dcToVmFolderSpec);
        resultSpec.add(clusterToParentSpec);
        resultSpec.add(folderToParentSpec);

        return resultSpec;
    }

    private static SelectionSpec getSelectionSpec(String name) {
        SelectionSpec genericSpec = new SelectionSpec();
        genericSpec.setName(name);
        return genericSpec;
    }

    /** This method returns the first valid {@link PlacementAction} out of the DRS recommendations. */
    private static PlacementAction getPlacementAction(PlacementResult placementResult) {
        List<ClusterRecommendation> recommendations = placementResult.getRecommendations();

        PlacementAction placementAction = null;
        int size = recommendations.size();
        boolean actionOk = false;

        if (size > 0) {
            log.info("Total number of recommendations are {}", size);
            log.info("Processing the xvcvmotion placement recommendations out of the recommendations received");
            for (ClusterRecommendation clusterRecommendation : recommendations) {
                if (clusterRecommendation.getReason().equalsIgnoreCase("xvmotionPlacement")) {
                    List<ClusterAction> actions = clusterRecommendation.getAction();

                    for (ClusterAction action : actions) {
                        if (action instanceof PlacementAction) {
                            placementAction = (PlacementAction) action;
                            break;
                        }
                    }

                    if (placementAction != null) {
                        if (placementAction.getVm() == null || placementAction.getTargetHost() == null) {
                            log.info("Placement Action doesn't have a vm and targethost set");
                        } else {
                            if (placementAction.getRelocateSpec() != null) {
                                actionOk = checkRelocateSpec(placementAction.getRelocateSpec());
                                if (actionOk) {
                                    break;
                                } else {
                                    placementAction = null;
                                }
                            }
                        }
                    } else {
                        log.error("Recommendation doesn't have a placement action");
                    }
                }
            }
        } else {
            log.warn("No recommendations by DRS");
        }

        return placementAction;
    }

    /** This method validates the {@link VirtualMachineRelocateSpec}. */
    private static boolean checkRelocateSpec(VirtualMachineRelocateSpec relocateSpec) {
        boolean check = false;

        if (relocateSpec.getHost() != null) {
            if (relocateSpec.getPool() != null) {
                if (relocateSpec.getDatastore() != null) {
                    check = true;
                } else {
                    log.error("RelocateSpec doesn't have a datastore");
                }
            } else {
                log.error("RelocateSpec doesn't have a resource pool");
            }
        } else {
            log.error("RelocateSpec doesn't have a host");
        }
        return check;
    }
}
