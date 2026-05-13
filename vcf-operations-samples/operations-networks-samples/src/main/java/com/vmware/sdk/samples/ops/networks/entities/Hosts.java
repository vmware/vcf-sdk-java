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
import com.vmware.sdk.ops.networks.model.Host;
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
 * Demonstrates how to manage Hosts in VCF Operations for networks.
 *
 * <p>This sample shows how to:
 * <ol>
 *   <li>Create an authenticated API client using VcfOpsNetworksClientFactory
 *   <li>List all hosts
 *   <li>Get a specific host by ID
 * </ol>
 *
 * <p>Sample Prerequisites:
 *
 * <ul>
 *   <li>VCF Operations for networks instance running and accessible
 *   <li>Valid credentials (username/password) with appropriate permissions
 *   <li>Network connectivity to the VCF Operations for networks host
 *   <li>Java 11 or later installed
 *   <li>For the "get" operation, a valid host entity ID (obtained from the "list" operation)
 * </ul>
 */
public class Hosts {
    private static final Logger log = LoggerFactory.getLogger(Hosts.class);

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

    /** OPTIONAL: Host entity ID for get operation. */
    public static String entityId = null;

    /**
     * Main method to execute the Hosts sample.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        try {
            // Parse command-line arguments
            SampleCommandLineParser.load(Hosts.class, args);
            initUnsetOptionalParameters();

            log.info("=== VCF Operations for networks - Hosts Sample ===");
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

            log.info("=== Hosts sample completed successfully ===");

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
     * Performs the list hosts operation.
     *
     * @param apiClient the authenticated API client
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static void performListOperation(ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        log.info("Retrieving all hosts...");
        final List<EntityIdWithTime> entityIdWithTimes = listHosts(apiClient);
        displayHostsList(entityIdWithTimes);
    }

    /**
     * Performs the get host operation.
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

        log.info("Retrieving host with ID: {}", entityId);
        final Host host = getHost(apiClient, entityId);
        displayHostInfo(host, "Host Details");
    }

    /**
     * Retrieves all hosts from the server.
     *
     * @param apiClient the authenticated API client
     * @return List of EntityIdWithTime objects containing host identifiers
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static List<EntityIdWithTime> listHosts(final ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.entities.Hosts hostsStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.entities.Hosts.class);

        return hostsStub.listHosts().invoke().get().getResults();
    }

    /**
     * Retrieves a specific host by ID.
     *
     * @param apiClient the authenticated API client
     * @param entityId the ID of the host to retrieve
     * @return Host object containing detailed host information
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static Host getHost(final ApiClient apiClient, final String entityId)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.entities.Hosts hostsStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.entities.Hosts.class);

        return hostsStub.getHost(entityId).invoke().get();
    }

    /**
     * Displays list of hosts in a formatted manner.
     * Shows total count and details for each host including entity ID and type.
     *
     * @param hostList the list of EntityIdWithTime objects containing host identifiers
     */
    private static void displayHostsList(final List<EntityIdWithTime> hostList) {
        log.info("========================================");
        log.info("Hosts List:");
        log.info("========================================");

        if (hostList != null && !hostList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nTotal Hosts: ").append(hostList.size());
            sb.append("\n");

            int index = 1;
            for (EntityIdWithTime entity : hostList) {
                sb.append("\nHost #").append(index);
                sb.append("\n  Entity: ").append(entity);
                sb.append("\n");
                index++;
            }

            log.info(sb.toString());
        } else {
            log.warn("No hosts information received");
        }

        log.info("========================================");
    }

    /**
     * Displays detailed host information in a formatted manner.
     *
     * @param host the Host object containing detailed host information
     * @param title the title to display for this host information
     */
    private static void displayHostInfo(final Host host, final String title) {
        log.info("========================================");
        log.info("{}", title);
        log.info("========================================");

        if (host != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nFull Host Details: ").append(host);
            log.info(sb.toString());
        } else {
            log.warn("No host information received");
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
