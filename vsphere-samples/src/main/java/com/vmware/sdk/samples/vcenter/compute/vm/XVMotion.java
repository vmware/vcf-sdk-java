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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ClusterAction;
import com.vmware.vim25.ClusterRecommendation;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PlacementAction;
import com.vmware.vim25.PlacementResult;
import com.vmware.vim25.PlacementSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachineMovePriority;
import com.vmware.vim25.VirtualMachineRelocateSpec;

/** This sample is used to migrate/relocate VM to another host and datastore using the drs placement recommendations. */
public class XVMotion {
    private static final Logger log = LoggerFactory.getLogger(XVMotion.class);
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

    /** REQUIRED: Name of the virtual machine to be migrated. */
    public static String vmName = "vmName";
    /** REQUIRED: Target cluster name. */
    public static String destCluster = "destCluster";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(XVMotion.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            // relocate the VM to the computing resource recommended by the DRS.
            ManagedObjectReference clusterMoRef =
                    propertyCollectorHelper.getMoRefByName(destCluster, CLUSTER_COMPUTE_RESOURCE);
            if (clusterMoRef == null) {
                throw new IllegalStateException("No Cluster by the name of '" + destCluster + "' found!");
            }

            ManagedObjectReference vmMoRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);
            if (vmMoRef == null) {
                throw new IllegalStateException("No VM by the name of '" + vmName + "' found!");
            }

            PlacementSpec placementSpec = new PlacementSpec();
            placementSpec.setVm(vmMoRef);

            PlacementResult placementResult;
            placementResult = vimPort.placeVm(clusterMoRef, placementSpec);

            // Check Placement Results
            PlacementAction placementAction = getPlacementAction(placementResult);
            if (placementAction != null) {
                VirtualMachineRelocateSpec relocateSpec = placementAction.getRelocateSpec();

                ManagedObjectReference taskMoRef =
                        vimPort.relocateVMTask(vmMoRef, relocateSpec, VirtualMachineMovePriority.DEFAULT_PRIORITY);

                if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                    log.info("Relocation done successfully");
                } else {
                    log.error("Relocation failed");
                }
            } else {
                log.info("Recommendations are not correct");
            }
        }
    }

    /** This method returns the first valid {@link PlacementAction} out of the DRS recommendations. */
    private static PlacementAction getPlacementAction(PlacementResult placementResult) {
        List<ClusterRecommendation> recommendations = placementResult.getRecommendations();
        PlacementAction placementAction = null;
        int size = recommendations.size();
        boolean actionOk = false;

        if (size > 0) {
            log.info("Total number of recommendations are {}", size);
            log.info("Processing the xvmotion placement recommendations out of the recommendations received");

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
                        log.info("Recommendation doesn't have a placement action");
                    }
                }
            }
        } else {
            log.info("No recommendations by DRS");
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
                    log.info("RelocateSpec doesn't have a datastore");
                }
            } else {
                log.info("RelocateSpec doesn't have a resource pool");
            }
        } else {
            log.info("RelocateSpec doesn't have a host");
        }
        return check;
    }
}
