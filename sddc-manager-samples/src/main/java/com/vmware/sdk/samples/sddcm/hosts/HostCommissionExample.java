/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.sddcm.hosts;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.sddcm.tasks.TaskHelper;
import com.vmware.sdk.samples.sddcm.utils.SddcUtil;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.sddcm.model.HostCommissionSpec;
import com.vmware.sdk.sddcm.model.NetworkPool;
import com.vmware.sdk.sddcm.model.PageOfNetworkPool;
import com.vmware.sdk.sddcm.model.Task;
import com.vmware.sdk.sddcm.v1.Tasks;
import com.vmware.sdk.sddcm.v1.V1Factory;

/**
 * Demonstrates commission the hosts to the SDDC free inventory pool.
 *
 * <p>Prerequisites before executing Host Commission Workflow:
 *
 * <ul>
 *   <li>Install ESXi on VMware Cloud Foundation Hosts Using the ISO
 *   <li>Configure the Network on VMware Cloud Foundation Hosts
 *   <li>Configure the Virtual Machine Network Port Group on VMware Cloud Foundation Hosts
 *   <li>Configure FQDN and NTP on VMware Cloud Foundation Hosts
 *   <li>Regenerate the Self-Signed Certificate on All Hosts or Configure ESXi Hosts with Signed Certificates
 *   <li>Create the network pool using CreateNetworkpool sample
 *   <li>Prepare HostCommissionSpec with host details to commission host to SDDC free inventory pool
 * </ul>
 */
public class HostCommissionExample {
    private static final Logger log = LoggerFactory.getLogger(HostCommissionExample.class);
    /** REQUIRED: SDDC Manager host address or FQDN. */
    public static String sddcManagerHostname = "sddcManagerHostname";
    /** REQUIRED: SDDC Manager SSO username. */
    public static String sddcManagerSsoUserName = "username";
    /** REQUIRED: SDDC Manager SSO password. */
    public static String sddcManagerSsoPassword = "password";

    /** REQUIRED: Host root username. */
    public static String esxiRootUsername = "root";
    /** REQUIRED: SDDC Manager SSO password. */
    public static String esxiRootPassword = "xxxxx";
    /** REQUIRED: Host root password. */
    public static String esxiStorageType = "VSAN";
    /** REQUIRED: Provide FQDN of the ESX Hosts to be commission to the SDDC free inventory pool. */
    public static String[] hostCommissionList = new String[] {"esxi-7.sample.local,esxi-8.sample.local"};
    /** REQUIRED: Provide the networkpool name which is already available in VCF. */
    public static String networkPoolName = "bringup-networkpool";

    public static void main(String[] args) {
        SampleCommandLineParser.load(HostCommissionExample.class, args);

        try (SddcUtil.SddcFactory factory =
                new SddcUtil.SddcFactory(sddcManagerHostname, sddcManagerSsoUserName, sddcManagerSsoPassword)) {
            V1Factory v1Factory = factory.getV1Factory();

            // Get the networkpool details by invoking networkpool API
            NetworkPool networkPool = getNetworkPool(v1Factory);

            // Prepare the host commission spec
            List<HostCommissionSpec> hostCommissionSpecs = getHostCommissionSpecs(networkPool);
            log.info("About to add the host to free inventory pool.");

            // Trigger host commission workflow
            Task hostCommissionTask = v1Factory
                    .hostsService()
                    .commissionHosts(hostCommissionSpecs)
                    .invoke()
                    .get();

            // TaskHelper utility to keep track of the host commission workflow task
            Tasks taskService = v1Factory.tasksService();
            boolean status = new TaskHelper().monitorTask(hostCommissionTask, taskService);
            if (status) {
                log.info("Host commission task succeeded");
            } else {
                log.error("Host commission task failed");
            }
        } catch (Exception exception) {
            log.error("Exception while creating the host commission workflow", exception);
        }
    }

    /**
     * Create host commission spec required to commission the hosts/host.
     *
     * @param networkPool required to set networkpool name {@link NetworkPool#getName()} and networkpool ID
     *     {@link NetworkPool#getId()} in HostCommissionSpec
     * @return list of HostCommissionSpec
     */
    private static List<HostCommissionSpec> getHostCommissionSpecs(NetworkPool networkPool) {
        List<HostCommissionSpec> hostCommissionSpecs = new ArrayList<>();
        if (networkPool != null) {
            for (String host : hostCommissionList) {
                hostCommissionSpecs.add(new HostCommissionSpec.Builder()
                        .setUsername(esxiRootUsername)
                        .setPassword(esxiRootPassword)
                        .setStorageType(esxiStorageType)
                        .setNetworkPoolName(networkPool.getName())
                        .setNetworkPoolId(networkPool.getId())
                        .setFqdn(host)
                        .build());
            }
        }
        return hostCommissionSpecs;
    }

    /**
     * Get the specific networkpool by providing the network pool name as constant e.g: "bringup-networkpool".
     *
     * @return NetworkPool by name
     */
    private static NetworkPool getNetworkPool(V1Factory v1Factory) throws Exception {
        PageOfNetworkPool pageOfNetworkPool =
                v1Factory.networkPoolsService().getNetworkPool().invoke().get();
        Optional<NetworkPool> optionalNetworkPool = pageOfNetworkPool.getElements().stream()
                .filter(networkPool -> networkPool.getName().equals(networkPoolName))
                .findFirst();
        return optionalNetworkPool.orElse(null);
    }
}
