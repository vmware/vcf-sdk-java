/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.appliance.services;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appliance.Services;
import com.vmware.appliance.ServicesTypes.Info;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;

/**
 * Demonstrates services api workflow:
 *
 * <ol>
 *   <li>List all services
 *   <li>Stop a running service
 *   <li>Get details of stopped service
 *   <li>Start the service stopped in step 2
 *   <li>Get details of service
 *   <li>Restart the service
 *   <li>Get details of service
 * </ol>
 */
public class ServicesWorkflow {
    private static final Logger log = LoggerFactory.getLogger(ServicesWorkflow.class);
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

    /** OPTIONAL: Specify service name for all operations. */
    public static String serviceName = null;
    /** OPTIONAL: Lists all the Operations. Default value is false. */
    public static Boolean listServices = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(ServicesWorkflow.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Services applianceServicesApiStub = client.createStub(Services.class);

            if (Boolean.TRUE.equals(listServices)) {
                // List all appliance services using services api
                log.info("#### Example: List Appliance Services:");
                // get the list of services
                Map<String, Info> svcList = applianceServicesApiStub.list();

                // formatting the list api output
                for (Map.Entry<String, Info> svc : svcList.entrySet()) {
                    formattedOutputDisplay(svc.getValue(), svc.getKey());
                }
            }
            if (null != serviceName) {
                // Stop a service using services api
                log.info("#### Example: Stopping service {}\n", serviceName);
                applianceServicesApiStub.stop(serviceName);

                // Get details of the service stopped in previous step using services api
                formattedOutputDisplay(applianceServicesApiStub.get(serviceName), serviceName);

                // Start a stopped service using services api
                log.info("#### Example: Starting service {}\n", serviceName);
                applianceServicesApiStub.start(serviceName);

                // Get details of the service started in previous step using services api
                formattedOutputDisplay(applianceServicesApiStub.get(serviceName), serviceName);

                // Restart a service using services api
                log.info("#### Example: Restarting service {}\n", serviceName);
                applianceServicesApiStub.restart(serviceName);

                // Get details of the service restarted in previous step using services api
                log.info("#### Example: Getting service details for {}\n", serviceName);
                formattedOutputDisplay(applianceServicesApiStub.get(serviceName), serviceName);
            }
        }
    }

    protected static void formattedOutputDisplay(Info info, String serviceName) {
        System.out.println("Service: " + serviceName);
        System.out.println("Description: " + info.getDescription());
        System.out.println("State: " + info.getState());
        System.out.println("----------");
    }
}
