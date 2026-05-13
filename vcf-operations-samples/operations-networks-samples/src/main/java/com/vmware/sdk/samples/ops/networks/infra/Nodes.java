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

import com.vmware.sdk.ops.networks.model.Node;
import com.vmware.sdk.ops.networks.model.NodeId;
import com.vmware.sdk.ops.networks.model.NodeListResult;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.vapi.client.ApiClient;
import com.vmware.sdk.ops.networks.utils.VcfOpsNetworksClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Demonstrates a complete workflow for retrieving node information from VCF Operations for networks.
 *
 * <p>This sample demonstrates a complete workflow that:
 * <ol>
 *   <li>Creates an authenticated API client using VcfOpsNetworksClientFactory
 *   <li>Lists all nodes in the system
 *   <li>Retrieves detailed information for a specific node (uses the first node from the list if no nodeId is provided)
 *   <li>Displays comprehensive node information
 * </ol>
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
public class Nodes {
    private static final Logger log = LoggerFactory.getLogger(Nodes.class);

    /** REQUIRED: VCF Operations for networks host address or FQDN. */
    public static String hostName = "hostName";

    /** REQUIRED: Username for authentication. */
    public static String username = "username";

    /** REQUIRED: Password for authentication. */
    public static String password = "password";

    /** OPTIONAL: Trust store path for SSL/TLS certificate validation. */
    public static String trustStorePath = null;
    /** OPTIONAL: Node ID to retrieve specific node information. */
    public static String nodeId = null;

    /**
     * Main method to execute the Nodes workflow sample.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        try {
            // Parse command-line arguments
            SampleCommandLineParser.load(Nodes.class, args);

            log.info("=== VCF Operations for networks - Nodes Workflow Sample ===");
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

            log.info("=== Nodes workflow sample completed successfully ===");

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
     * Executes the complete workflow for retrieving node information.
     * This workflow demonstrates: list nodes -> get specific node details
     *
     * @param apiClient the authenticated API client
     * @throws ExecutionException if any API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static void executeWorkflow(ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        // Step 1: List all nodes
        log.info("\n========================================");
        log.info("Step 1: List All Nodes");
        log.info("========================================");
        log.info("Retrieving all nodes from the system...");
        final NodeListResult nodeList = getNodes(apiClient);
        displayNodesInfo(nodeList);

        // Step 2: Get specific node details
        final List<NodeId> nodeIdList = nodeList.getResults();
        if (nodeIdList != null && !nodeIdList.isEmpty()) {
            log.info("\n========================================");
            log.info("Step 2: Get Specific Node Details");
            log.info("========================================");
            
            // Determine which node to retrieve
            String targetNodeId = nodeId;
            if (targetNodeId == null || targetNodeId.isEmpty()) {
                targetNodeId = nodeIdList.get(0).getId();
                log.info("No specific nodeId provided, using first node from the list");
            }
            
            log.info("Retrieving detailed information for Node ID: {}", targetNodeId);
            final Node node = getNode(apiClient, targetNodeId);
            displayNodeInfo(node);
        } else {
            log.warn("No nodes found in the system. Cannot retrieve specific node details.");
        }
    }

    /**
     * Retrieves nodes information from the server.
     *
     * @param apiClient the authenticated API client
     * @return NodeListResult containing list of nodes
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static NodeListResult getNodes(ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.infra.Nodes nodesStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.infra.Nodes.class);

        return nodesStub.listNodes().invoke().get();
    }

    /**
     * Retrieves specific node information from the server.
     *
     * @param apiClient the authenticated API client
     * @param nodeId the ID of the node to retrieve
     * @return Node containing node information
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static Node getNode(final ApiClient apiClient, final String nodeId)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.infra.Nodes nodesStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.infra.Nodes.class);

        return nodesStub.getNode(nodeId).invoke().get();
    }

    /**
     * Displays nodes information in a formatted manner.
     *
     * @param nodeListResult the node list result object containing nodes information
     */
    private static void displayNodesInfo(final NodeListResult nodeListResult) {
        log.info("========================================");
        final List<NodeId> nodes = nodeListResult.getResults();
        if (nodes != null && !nodes.isEmpty()) {
            log.info("Total Nodes: {}", nodes.size());
            log.info("========================================");

            int nodeNumber = 1;
            StringBuilder sb =new StringBuilder();
            for (NodeId node : nodes) {
                sb.append("\nNode #").append(nodeNumber++);
                sb.append("\nFull Node Details: ").append(node);
            }
            log.info(sb.toString());
            log.info("========================================");
            log.info("Total Nodes Retrieved: {}", nodes.size());
        } else {
            log.warn("No nodes information received");
        }

        log.info("========================================");
    }

    /**
     * Displays specific node information in a formatted manner.
     *
     * @param node the node object containing node information
     */
    private static void displayNodeInfo(final Node node) {
        log.info("========================================");
        log.info("Specific Node Information:");
        log.info("========================================");

        if (node != null) {
            StringBuilder sb =new StringBuilder();
            sb.append("\nFull Node Details: ").append(node);
            log.info(sb.toString());
        } else {
            log.warn("No node information received");
        }

        log.info("========================================");
    }
}
