/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.sddcm.domains;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.sddcm.helpers.SddcManagerHelper;
import com.vmware.sdk.samples.sddcm.tasks.TaskHelper;
import com.vmware.sdk.samples.sddcm.utils.SddcUtil;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.sddcm.model.DomainUpdateSpec;
import com.vmware.sdk.sddcm.model.Task;
import com.vmware.sdk.sddcm.v1.V1Factory;

/**
 * Demonstrates how to delete a domain.
 *
 * <p>Prerequisites before deleting the cluster:
 *
 * <ul>
 *   <li>Ensure that a domain with the given name exists.
 *   <li>Migrate the VMs that you want to retain, to another domain before deleting the domain.
 * </ul>
 */
public class DeleteDomainExample {
    private static final Logger log = LoggerFactory.getLogger(DeleteDomainExample.class);
    /** REQUIRED: SDDC Manager host address or FQDN. */
    public static String sddcManagerHostname = "sddcManagerHostname";
    /** REQUIRED: SDDC Manager SSO username. */
    public static String sddcManagerSsoUserName = "username";
    /** REQUIRED: SDDC Manager SSO password. */
    public static String sddcManagerSsoPassword = "password";
    /** REQUIRED: Domain name to be deleted. */
    public static String domainName = "domain";

    private static final int TASK_POLL_TIME_IN_SECONDS = 300;

    public static void main(String[] args) {
        SampleCommandLineParser.load(DeleteDomainExample.class, args);

        try (SddcUtil.SddcFactory factory =
                new SddcUtil.SddcFactory(sddcManagerHostname, sddcManagerSsoUserName, sddcManagerSsoPassword)) {
            V1Factory v1Factory = factory.getV1Factory();

            // Get the domainID which needs to be deleted
            String domainID =
                    SddcManagerHelper.getDomainByName(v1Factory, domainName).getId();

            // Get the domain update spec
            DomainUpdateSpec domainUpdateSpec =
                    new DomainUpdateSpec.Builder().setMarkForDeletion(true).build();
            // Domain deletion is a 2-step process.
            // This 2-step deletion process ensures that a domain is not deleted accidentally.
            // Step-1: Initialize the deletion or mark the domain for deletion.
            v1Factory
                    .domainsService()
                    .updateDomain(domainID, domainUpdateSpec)
                    .invoke()
                    .get();
            log.info("Completed marking the domain for deletion");

            // Step-2: Trigger the domain deletion
            Task deleteDomainTask =
                    v1Factory.domainsService().deleteDomain(domainID).invoke().get();
            // TaskHelper utility to keep track of domain deletion workflow task
            boolean status = new TaskHelper()
                    .monitorTasks(
                            List.of(deleteDomainTask),
                            sddcManagerHostname,
                            sddcManagerSsoUserName,
                            sddcManagerSsoPassword,
                            TASK_POLL_TIME_IN_SECONDS);
            if (status) {
                log.info("Delete Domain task succeeded");
            } else {
                log.error("Delete domain task failed");
            }
        } catch (Exception exception) {
            log.error("Exception while running the delete domain workflow", exception);
        }
    }
}
