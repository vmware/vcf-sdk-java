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

import com.vmware.sdk.ops.networks.model.Cluster;
import com.vmware.sdk.ops.networks.model.EntityIdWithTime;
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
 * Demonstrates how to manage Clusters in VCF Operations for networks.
 *
 * <p>This sample shows how to:
 * <ol>
 *   <li>Create an authenticated API client using VcfOpsNetworksClientFactory
 *   <li>List all clusters
 *   <li>Get a specific cluster by ID
 * </ol>
 *
 * <p>Sample Prerequisites:
 *
 * <ul>
 *   <li>VCF Operations for networks instance running and accessible
 *   <li>Valid credentials (username/password) with appropriate permissions
 *   <li>Network connectivity to the VCF Operations for networks host
 *   <li>Java 11 or later installed
 *   <li>For the "get" operation, a valid cluster entity ID (obtained from the "list" operation)
 * </ul>
 */
public class Clusters {
    private static final Logger log = LoggerFactory.getLogger(Clusters.class);

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

    /** OPTIONAL: Cluster entity ID for get operation. */
    public static String entityId = null;

    /**
     * Main method to execute the Clusters sample.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        try {
            // Parse command-line arguments
            SampleCommandLineParser.load(Clusters.class, args);
            initUnsetOptionalParameters();

            log.info("=== VCF Operations for networks - Clusters Sample ===");
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

            log.info("=== Clusters sample completed successfully ===");

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
     * Performs the list clusters operation.
     *
     * @param apiClient the authenticated API client
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static void performListOperation(ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        log.info("Retrieving all clusters...");
        final List<EntityIdWithTime> entityIdWithTimes = listClusters(apiClient);
        displayClustersList(entityIdWithTimes);
    }

    /**
     * Performs the get cluster operation.
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

        log.info("Retrieving cluster with ID: {}", entityId);
        final Cluster cluster = getCluster(apiClient, entityId);
        displayClusterInfo(cluster, "Cluster Details");
    }

    /**
     * Retrieves all clusters from the server.
     *
     * @param apiClient the authenticated API client
     * @return List of EntityId objects containing cluster identifiers
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static List<EntityIdWithTime> listClusters(final ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.entities.Clusters clustersStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.entities.Clusters.class);

        return clustersStub.listClusters().invoke().get().getResults();
    }

    /**
     * Retrieves a specific cluster by ID.
     *
     * @param apiClient the authenticated API client
     * @param entityId the ID of the cluster to retrieve
     * @return Entity object containing detailed cluster information
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static Cluster getCluster(final ApiClient apiClient, final String entityId)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.entities.Clusters clustersStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.entities.Clusters.class);

        return clustersStub.getCluster(entityId).invoke().get();
    }

    /**
     * Displays list of clusters in a formatted manner.
     * Shows total count and details for each cluster including entity ID and type.
     *
     * @param clusterList the list of EntityId objects containing cluster identifiers
     */
    private static void displayClustersList(final List<EntityIdWithTime> clusterList) {
        log.info("========================================");
        log.info("Clusters List:");
        log.info("========================================");

        if (clusterList != null && !clusterList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nTotal Clusters: ").append(clusterList.size());
            sb.append("\n");
            
            int index = 1;
            for (EntityIdWithTime entity : clusterList) {
                sb.append("\nCluster #").append(index);
                sb.append("\n  Entity: ").append(entity);
                sb.append("\n");
                index++;
            }
            
            log.info(sb.toString());
        } else {
            log.warn("No clusters information received");
        }

        log.info("========================================");
    }

    /**
     * Displays detailed cluster information in a formatted manner.
     *
     * @param cluster the Cluster object containing detailed cluster information
     * @param title the title to display for this cluster information
     */
    private static void displayClusterInfo(final Cluster cluster, final String title) {
        log.info("========================================");
        log.info("{}", title);
        log.info("========================================");

        if (cluster != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nFull Cluster Details: ").append(cluster);
            log.info(sb.toString());
        } else {
            log.warn("No cluster information received");
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
