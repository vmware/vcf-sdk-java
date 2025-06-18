/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.appliance.services.list;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.services.Service;
import com.vmware.vcenter.services.ServiceTypes;

/**
 * Demonstrates getting list of Services present in vCenter.
 *
 * <p>Sample Requirements:
 *
 * <ul>
 *   <li>1 vCenter Server
 *   <li>2 ESX hosts
 *   <li>1 datastore
 * </ul>
 */
public class ListServices {
    private static final Logger log = LoggerFactory.getLogger(ListServices.class);
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

    /** OPTIONAL: Specify the name of the service whose state is being queried. */
    public static String service = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(ListServices.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Service serviceApiStub = client.createStub(Service.class);

            // List all vCenter services' information if service name is not specified
            if (service == null || service.isEmpty()) {
                // Get the list of all services on the server
                Map<String, ServiceTypes.Info> servicesList = serviceApiStub.listDetails();
                for (Map.Entry<String, ServiceTypes.Info> svc : servicesList.entrySet()) {
                    formattedOutputDisplay(svc.getValue(), svc.getKey());
                }
            } else {
                // List information of service provided as arg
                ServiceTypes.Info serviceInfo = serviceApiStub.get(service);
                formattedOutputDisplay(serviceInfo, service);
            }
        }
    }

    protected static void formattedOutputDisplay(ServiceTypes.Info info, String serviceName) {
        System.out.println("-----------------------------");
        System.out.println("Service Name : " + serviceName);
        System.out.println("Service Name Key : " + info.getNameKey());
        System.out.println("Service Health : " + info.getHealth());
        System.out.println("Service Status : " + info.getState());
        System.out.println("Service Startup Type : " + info.getStartupType());
    }
}
