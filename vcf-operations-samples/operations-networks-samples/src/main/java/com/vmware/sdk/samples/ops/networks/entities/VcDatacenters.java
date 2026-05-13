/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.networks.entities;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import com.vmware.sdk.ops.networks.model.EntityIdWithTime;
import com.vmware.sdk.ops.networks.model.VCDatacenter;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.vapi.client.ApiClient;
import com.vmware.sdk.ops.networks.utils.VcfOpsNetworksClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * Demonstrates how to manage vCenter Datacenters in VCF Operations for networks.
 *
 * <p>This sample shows how to:
 * <ol>
 *   <li>Create an authenticated API client using VcfOpsNetworksClientFactory
 *   <li>List all vCenter datacenters
 *   <li>Get a specific vCenter datacenter by ID
 * </ol>
 *
 * <p>Sample Prerequisites:
 *
 * <ul>
 *   <li>VCF Operations for networks instance running and accessible
 *   <li>Valid credentials (username/password) with appropriate permissions
 *   <li>Network connectivity to the VCF Operations for networks host
 *   <li>Java 11 or later installed
 *   <li>For the "get" operation, a valid vCenter datacenter entity ID (obtained from the "list" operation)
 * </ul>
 */
public class VcDatacenters {
    private static final Logger log = LoggerFactory.getLogger(VcDatacenters.class);

    /** REQUIRED: VCF Operations for networks host address or FQDN. */
    public static String hostName = "hostName";

    /** REQUIRED: Username for authentication. */
    public static String username = "username";

    /** REQUIRED: Password for authentication. */
    public static String password = "password";

    /** OPTIONAL: Trust store path for SSL/TLS certificate validation. */
    public static String trustStorePath = null;
    
    /** OPTIONAL: Operation to perform (list, get). Default is 'list'. */
    public static String operation = null;

    /** OPTIONAL: vCenter Datacenter entity ID for get operation. */
    public static String entityId = null;

    /**
     * Main method to execute the VcDatacenters sample.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        try {
            // Parse command-line arguments
            SampleCommandLineParser.load(VcDatacenters.class, args);
            initUnsetOptionalParameters();

            log.info("=== VCF Operations for networks - vCenter Datacenters Sample ===");
            log.info("Host: {}", hostName);
            log.info("Username: {}", username);
            log.info("Operation: {}", operation);

            // Create API client using VcfOpsNetworksClientFactory
            log.info("Creating API client...");
            KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);
            VcfOpsNetworksClientFactory factory = new VcfOpsNetworksClientFactory();
            ApiClient apiClient = factory.createClient(hostName, username, password, keyStore);
            log.info("API client created successfully");

            // Perform the requested operation
            switch (operation.toLowerCase()) {
                case "list":
                    performListOperation(apiClient);
                    break;
                case "get":
                    performGetOperation(apiClient);
                    break;
                default:
                    log.error("Invalid operation: {}. Valid operations are: list, get", operation);
                    System.exit(1);
            }

            log.info("=== vCenter Datacenters sample completed successfully ===");

        } catch (ExecutionException e) {
            log.error("Execution error occurred: {}", e.getMessage(), e);
            System.exit(1);
        } catch (InterruptedException e) {
            log.error("Operation was interrupted: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            System.exit(1);
        } catch (Exception e) {
            log.error("Unexpected error occurred: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Performs the list vCenter datacenters operation.
     *
     * @param apiClient the authenticated API client
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static void performListOperation(ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        log.info("Retrieving all vCenter datacenters...");
        final List<EntityIdWithTime> entityIdWithTimes = listVcDatacenters(apiClient);
        displayVcDatacentersList(entityIdWithTimes);
    }

    /**
     * Performs the get vCenter datacenter operation.
     *
     * @param apiClient the authenticated API client
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static void performGetOperation(ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        if (entityId == null || entityId.isEmpty()) {
            log.error("Entity ID is required for get operation. Use --entityId parameter.");
            System.exit(1);
        }

        log.info("Retrieving vCenter datacenter with ID: {}", entityId);
        final VCDatacenter vcDatacenter = getVcDatacenter(apiClient, entityId);
        displayVcDatacenterInfo(vcDatacenter, "vCenter Datacenter Details");
    }

    /**
     * Retrieves all vCenter datacenters from the server.
     *
     * @param apiClient the authenticated API client
     * @return List of EntityIdWithTime objects containing vCenter datacenter identifiers
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static List<EntityIdWithTime> listVcDatacenters(final ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.entities.VcDatacenters vcDatacentersStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.entities.VcDatacenters.class);

        return vcDatacentersStub.listDatacenters().invoke().get().getResults();
    }

    /**
     * Retrieves a specific vCenter datacenter by ID.
     *
     * @param apiClient the authenticated API client
     * @param entityId the ID of the vCenter datacenter to retrieve
     * @return VCDatacenter object containing detailed vCenter datacenter information
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static VCDatacenter getVcDatacenter(final ApiClient apiClient, final String entityId)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.entities.VcDatacenters vcDatacentersStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.entities.VcDatacenters.class);

        return vcDatacentersStub.getDatacenter(entityId).invoke().get();
    }

    /**
     * Displays list of vCenter datacenters in a formatted manner.
     * Shows total count and details for each vCenter datacenter including entity ID and type.
     *
     * @param vcDatacenterList the list of EntityIdWithTime objects containing vCenter datacenter identifiers
     */
    private static void displayVcDatacentersList(final List<EntityIdWithTime> vcDatacenterList) {
        log.info("========================================");
        log.info("vCenter Datacenters List:");
        log.info("========================================");

        if (vcDatacenterList != null && !vcDatacenterList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nTotal vCenter Datacenters: ").append(vcDatacenterList.size());
            sb.append("\n");
            
            int index = 1;
            for (EntityIdWithTime entity : vcDatacenterList) {
                sb.append("\nvCenter Datacenter #").append(index);
                sb.append("\n  Entity: ").append(entity);
                sb.append("\n");
                index++;
            }
            
            log.info(sb.toString());
        } else {
            log.warn("No vCenter datacenters information received");
        }

        log.info("========================================");
    }

    /**
     * Displays detailed vCenter datacenter information in a formatted manner.
     *
     * @param vcDatacenter the VCDatacenter object containing detailed vCenter datacenter information
     * @param title the title to display for this vCenter datacenter information
     */
    private static void displayVcDatacenterInfo(final VCDatacenter vcDatacenter, final String title) {
        log.info("========================================");
        log.info("{}", title);
        log.info("========================================");

        if (vcDatacenter != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nFull vCenter Datacenter Details: ").append(vcDatacenter);
            log.info(sb.toString());
        } else {
            log.warn("No vCenter datacenter information received");
        }

        log.info("========================================");
    }

    /**
     * Initializes optional parameters with default values if they were not set via command line.
     */
    private static void initUnsetOptionalParameters() {
        if (Objects.isNull(operation)) {
            operation = "list";
        }
    }
}
