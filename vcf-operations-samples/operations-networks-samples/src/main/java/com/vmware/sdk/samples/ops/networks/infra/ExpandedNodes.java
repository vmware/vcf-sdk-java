/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.networks.infra;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import com.vmware.sdk.ops.networks.model.ExpandedNodeListResult;
import com.vmware.sdk.ops.networks.model.Node;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.vapi.client.ApiClient;
import com.vmware.sdk.ops.networks.utils.VcfOpsNetworksClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Demonstrates a complete workflow for retrieving expanded node information from VCF Operations for networks.
 *
 * <p>This sample demonstrates a complete workflow that:
 * <ol>
 *   <li>Creates an authenticated API client using VcfOpsNetworksClientFactory
 *   <li>Lists all nodes with expanded information (includes detailed node properties)
 *   <li>Displays comprehensive expanded node information
 * </ol>
 *
 * <p>Expanded nodes provide more detailed information compared to the basic node list,
 * including additional properties and relationships that are not included in the standard node listing.
 *
 * <p>Sample Prerequisites:
 *
 * <ul>
 *   <li>VCF Operations for networks instance running and accessible
 *   <li>Valid credentials (username/password) with appropriate permissions
 *   <li>Network connectivity to the VCF Operations for networks host
 *   <li>Java 11 or later installed
 * </ul>
 */
public class ExpandedNodes {
    private static final Logger log = LoggerFactory.getLogger(ExpandedNodes.class);

    /** REQUIRED: VCF Operations for networks host address or FQDN. */
    public static String hostName = "hostName";

    /** REQUIRED: Username for authentication. */
    public static String username = "username";

    /** REQUIRED: Password for authentication. */
    public static String password = "password";

    /** OPTIONAL: Trust store path for SSL/TLS certificate validation. */
    public static String trustStorePath = null;
    /**
     * Main method to execute the ExpandedNodes workflow sample.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        try {
            // Parse command-line arguments
            SampleCommandLineParser.load(ExpandedNodes.class, args);

            log.info("=== VCF Operations for networks - Expanded Nodes Workflow Sample ===");
            log.info("Host: {}", hostName);
            log.info("Username: {}", username);

            // Create API client using VcfOpsNetworksClientFactory
            log.info("Creating API client...");
            KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);
            VcfOpsNetworksClientFactory factory = new VcfOpsNetworksClientFactory();
            ApiClient apiClient = factory.createClient(hostName, username, password, keyStore);
            log.info("API client created successfully");

            // Execute the complete workflow
            executeWorkflow(apiClient);

            log.info("=== Expanded Nodes workflow sample completed successfully ===");

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
     * Executes the complete workflow for retrieving expanded node information.
     * This workflow demonstrates: list expanded nodes -> display expanded node information
     *
     * @param apiClient the authenticated API client
     * @throws ExecutionException if any API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static void executeWorkflow(ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        // Step 1: List all expanded nodes
        log.info("\n========================================");
        log.info("Step 1: List Expanded Nodes");
        log.info("========================================");
        log.info("Retrieving expanded nodes information from the system...");
        log.info("Note: Expanded nodes include detailed information not available in the basic node list");
        final ExpandedNodeListResult expandedNodeList = getExpandedNodes(apiClient);
        displayExpandedNodesInfo(expandedNodeList);
    }

    /**
     * Retrieves expanded nodes information from the server.
     *
     * <p>Expanded nodes provide comprehensive node information including detailed properties
     * and relationships that are not included in the standard node listing.
     *
     * @param apiClient the authenticated API client
     * @return ExpandedNodeListResult containing list of expanded nodes with detailed information
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static ExpandedNodeListResult getExpandedNodes(ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        final com.vmware.sdk.ops.networks.infra.ExpandedNodes expandedNodesStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.infra.ExpandedNodes.class);

        return expandedNodesStub.listExpandedNodes().invoke().get();
    }

    /**
     * Displays expanded nodes information in a formatted manner.
     *
     * @param nodeListResult the node list result object containing expanded nodes information
     */
    private static void displayExpandedNodesInfo(ExpandedNodeListResult nodeListResult) {
        log.info("========================================");
        final List<Node> nodes = nodeListResult.getResults();
        if (nodes != null && !nodes.isEmpty()) {
            log.info("Total Expanded Nodes: {}", nodes.size());
            log.info("========================================");

            int nodeNumber = 1;
            StringBuilder sb = new StringBuilder();
            for (Node node : nodes) {
                sb.append("\nExpanded Node #").append(nodeNumber++);
                sb.append("\nFull Expanded Node Details: ").append(node);
            }
            log.info(sb.toString());
            log.info("========================================");
            log.info("Total Expanded Nodes Retrieved: {}", nodes.size());
        } else {
            log.warn("No expanded nodes information received");
        }

        log.info("========================================");
    }
}
