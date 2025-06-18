/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.compute.policies;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vapi.bindings.Structure;
import com.vmware.vcenter.compute.Policies;
import com.vmware.vcenter.compute.PoliciesTypes;

/** Demonstrates how to list Compute Policies */
public class ListComputePolicy {
    private static final Logger log = LoggerFactory.getLogger(ListComputePolicy.class);
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
        SampleCommandLineParser.load(ListComputePolicy.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Policies policies = client.createStub(Policies.class);

            List<PoliciesTypes.Summary> policiesSummary = policies.list();
            log.info("policiesSummary={}", policiesSummary);

            for (PoliciesTypes.Summary policySummary : policiesSummary) {
                Structure structure = policies.get(policySummary.getPolicy());

                if (structure._hasTypeNameOf(
                        com.vmware.vcenter.compute.policies.capabilities.vm_host_affinity.Info.class)) {
                    com.vmware.vcenter.compute.policies.capabilities.vm_host_affinity.Info info = structure._convertTo(
                            com.vmware.vcenter.compute.policies.capabilities.vm_host_affinity.Info.class);

                    log.info("vm_host_affinity.Info={}", info);
                } else if (structure._hasTypeNameOf(
                        com.vmware.vcenter.compute.policies.capabilities.vm_host_anti_affinity.Info.class)) {
                    com.vmware.vcenter.compute.policies.capabilities.vm_host_anti_affinity.Info info =
                            structure._convertTo(
                                    com.vmware.vcenter.compute.policies.capabilities.vm_host_anti_affinity.Info.class);

                    log.info("vm_host_anti_affinity.Info.={}", info);
                } else if (structure._hasTypeNameOf(
                        com.vmware.vcenter.compute.policies.capabilities.vm_vm_affinity.Info.class)) {
                    com.vmware.vcenter.compute.policies.capabilities.vm_vm_affinity.Info info = structure._convertTo(
                            com.vmware.vcenter.compute.policies.capabilities.vm_vm_affinity.Info.class);

                    log.info("vm_vm_affinity.Info={}", info);
                } else if (structure._hasTypeNameOf(
                        com.vmware.vcenter.compute.policies.capabilities.vm_vm_anti_affinity.Info.class)) {
                    com.vmware.vcenter.compute.policies.capabilities.vm_vm_anti_affinity.Info info =
                            structure._convertTo(
                                    com.vmware.vcenter.compute.policies.capabilities.vm_vm_anti_affinity.Info.class);

                    log.info("vm_vm_anti_affinity.Info={}", info);
                }
            }
        }
    }
}
