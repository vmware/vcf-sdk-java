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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.sddcm.tasks.TaskHelper;
import com.vmware.sdk.samples.sddcm.utils.SddcUtil;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.sddcm.model.HostDecommissionSpec;
import com.vmware.sdk.sddcm.model.Task;
import com.vmware.sdk.sddcm.v1.Tasks;
import com.vmware.sdk.sddcm.v1.V1Factory;

/**
 * Demonstrates de-commission of the hosts.
 *
 * <p>Sample Prerequisites: Prepare HostDecommissionSpec with host details to de-commission hosts.
 */
public class HostDecommissionExample {
    private static final Logger log = LoggerFactory.getLogger(HostDecommissionExample.class);
    /** REQUIRED: SDDC Manager host address or FQDN. */
    public static String sddcManagerHostname = "sddcManagerHostname";
    /** REQUIRED: SDDC Manager SSO username. */
    public static String sddcManagerSsoUserName = "username";
    /** REQUIRED: SDDC Manager SSO password. */
    public static String sddcManagerSsoPassword = "password";
    /** REQUIRED: Provide FQDN of the ESXi Hosts to be decommissioned. */
    public static String[] hostDeCommissionList = new String[] {"esxi-7.sample.local", "esxi-8.sample.local"};

    public static void main(String[] args) {
        SampleCommandLineParser.load(HostDecommissionExample.class, args);

        try (SddcUtil.SddcFactory factory =
                new SddcUtil.SddcFactory(sddcManagerHostname, sddcManagerSsoUserName, sddcManagerSsoPassword)) {
            V1Factory v1Factory = factory.getV1Factory();
            // Prepare the host de-commission spec
            List<HostDecommissionSpec> hostDecommissionSpecList = new ArrayList<>();
            for (String host : hostDeCommissionList) {
                hostDecommissionSpecList.add(
                        new HostDecommissionSpec.Builder().setFqdn(host).build());
            }

            // Trigger Host de-commission workflow
            Task hostDecommissionTask = v1Factory
                    .hostsService()
                    .decommissionHosts(hostDecommissionSpecList)
                    .invoke()
                    .get();

            // Use Tasks service TaskHelper utility to keep track of the host de-commission workflow task
            Tasks taskService = v1Factory.tasksService();
            boolean status = new TaskHelper().monitorTask(hostDecommissionTask, taskService);
            if (status) {
                log.info("Host de-commission task succeeded");
            } else {
                log.error("Host de-commission task failed");
            }
        } catch (Exception exception) {
            log.error("Exception while running the host de-commission workflow", exception);
        }
    }
}
