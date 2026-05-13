/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.networks.groups;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import com.vmware.sdk.ops.networks.model.EntityId;
import com.vmware.sdk.ops.networks.model.PagedListResponse;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.vapi.client.ApiClient;
import com.vmware.sdk.ops.networks.utils.VcfOpsNetworksClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Demonstrates a complete workflow for retrieving and analyzing Discovered Applications
 * in VCF Operations for networks.
 *
 * <p>This sample demonstrates a complete workflow that:
 * <ol>
 *   <li>Creates an authenticated API client using VcfOpsNetworksClientFactory
 *   <li>Lists discovered applications with SERVICE_NOW discovery type
 *   <li>Lists discovered applications with FLOW_BASED_DISCOVERY discovery type
 *   <li>Analyzes and summarizes the discovered applications from both sources
 * </ol>
 *
 * <p>Discovered Applications are applications that are automatically detected by VCF Operations for networks
 * through network traffic analysis and monitoring. Unlike user-defined
 * applications, discovered applications are read-only and cannot be created, modified, or deleted
 * through the API.
 *
 * <p>Discovery Types:
 * <ul>
 *   <li><b>SERVICE_NOW</b>: Applications synced from ServiceNow CMDB integration.
 *       No additional parameters required.
 *   <li><b>FLOW_BASED_DISCOVERY</b>: Applications automatically identified through network flow analysis.
 *       Requires a granularity parameter (allowed values: FINE, MEDIUM, COARSE).
 *       This sample uses FINE granularity for maximum detail.
 * </ul>
 *
 * <p>Sample Prerequisites:
 *
 * <ul>
 *   <li>VCF Operations for networks instance running and accessible
 *   <li>Valid credentials (username/password) with appropriate permissions
 *   <li>Network connectivity to the VCF Operations for networks host
 *   <li>Java 11 or later installed
 *   <li>Network traffic data collection enabled in your environment (for FLOW_BASED_DISCOVERY)
 *   <li>ServiceNow integration configured (for SERVICE_NOW discovery type, optional)
 * </ul>
 */
public class DiscoveredApplications {
    private static final Logger log = LoggerFactory.getLogger(DiscoveredApplications.class);

    /** REQUIRED: VCF Operations for networks host address or FQDN. */
    public static String hostName = "hostName";

    /** REQUIRED: Username for authentication. */
    public static String username = "username";

    /** REQUIRED: Password for authentication. */
    public static String password = "password";

    /** OPTIONAL: Trust store path for SSL/TLS certificate validation. */
    public static String trustStorePath = null;
    
    /**
     * Main method to execute the Discovered Applications workflow sample.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        try {
            // Parse command-line arguments
            SampleCommandLineParser.load(DiscoveredApplications.class, args);

            log.info("=== VCF Operations for networks - Discovered Applications Workflow Sample ===");
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

            log.info("=== Discovered Applications workflow sample completed successfully ===");

        } catch (Exception e) {
            log.error("Unexpected error occurred: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Executes the complete workflow for discovering and analyzing applications.
     * This workflow demonstrates how to query and analyze discovered applications:
     * list SERVICE_NOW -> list FLOW_BASED_DISCOVERY -> analyze
     *
     * @param apiClient the authenticated API client
     */
    private static void executeWorkflow(ApiClient apiClient) {
        // Step 1: List discovered applications with SERVICE_NOW discovery type
        log.info("\n========================================");
        log.info("Step 1: List Discovered Applications (SERVICE_NOW Discovery Type)");
        log.info("========================================");
        log.info("Retrieving discovered applications from ServiceNow integration...");
        List<EntityId> serviceNowApplications = null;
        try {
            serviceNowApplications = listDiscoveredApplications(apiClient, "SERVICE_NOW");
            displayDiscoveredApplicationsList(serviceNowApplications, "SERVICE_NOW");
        } catch (ExecutionException | InterruptedException e) {
            log.warn("Failed to retrieve SERVICE_NOW applications: {}", e.getMessage());
            if (e.getCause() != null) {
                log.warn("Root cause: {}", e.getCause().getMessage());
            }
            log.info("This may indicate that:");
            log.info("  - ServiceNow integration is not configured or has no data");
            log.info("  - The discovery type 'SERVICE_NOW' is not enabled");
            serviceNowApplications = new java.util.ArrayList<>();
        } catch (Exception e) {
            log.warn("Unexpected error retrieving SERVICE_NOW applications: {}", e.getMessage());
            serviceNowApplications = new java.util.ArrayList<>();
        }

        // Step 2: List discovered applications with FLOW_BASED_DISCOVERY discovery type
        log.info("\n========================================");
        log.info("Step 2: List Discovered Applications (FLOW_BASED_DISCOVERY Discovery Type)");
        log.info("========================================");
        log.info("Retrieving discovered applications from network flow analysis...");
        List<EntityId> flowBasedApplications = null;
        try {
            flowBasedApplications = listDiscoveredApplications(apiClient, "FLOW_BASED_DISCOVERY");
            displayDiscoveredApplicationsList(flowBasedApplications, "FLOW_BASED_DISCOVERY");
        } catch (ExecutionException | InterruptedException e) {
            log.warn("Failed to retrieve FLOW_BASED_DISCOVERY applications: {}", e.getMessage());
            if (e.getCause() != null) {
                log.warn("Root cause: {}", e.getCause().getMessage());
            }
            log.info("This may indicate that:");
            log.info("  - The granularity parameter value is not supported in this environment");
            log.info("  - FLOW_BASED_DISCOVERY is not configured or enabled");
            log.info("  - Network flow analysis has not identified any applications");
            flowBasedApplications = new java.util.ArrayList<>();
        } catch (Exception e) {
            log.warn("Unexpected error retrieving FLOW_BASED_DISCOVERY applications: {}", e.getMessage());
            flowBasedApplications = new java.util.ArrayList<>();
        }

        // Step 3: Analyze and summarize discovered applications from both sources
        log.info("\n========================================");
        log.info("Step 3: Analyze Discovered Applications");
        log.info("========================================");
        analyzeDiscoveredApplications(serviceNowApplications, flowBasedApplications);
    }

    /**
     * Retrieves discovered applications from the server with a specific discovery type.
     * 
     * <p>Discovered applications are automatically identified by VCF Operations for networks
     * through network traffic analysis. These applications represent real network traffic patterns
     * and are read-only.
     *
     * <p><b>Discovery Types:</b>
     * <ul>
     *   <li><b>SERVICE_NOW</b>: Applications synced from ServiceNow CMDB integration.
     *       No additional parameters required.
     *   <li><b>FLOW_BASED_DISCOVERY</b>: Applications automatically identified through network flow analysis.
     *       Requires a granularity parameter with allowed values: FINE, MEDIUM, COARSE.
     *       This implementation uses FINE granularity for maximum detail.
     * </ul>
     *
     * @param apiClient the authenticated API client
     * @param discoveryType the discovery type to filter by (e.g., "SERVICE_NOW", "FLOW_BASED_DISCOVERY")
     * @return List of EntityId objects containing discovered application identifiers
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static List<EntityId> listDiscoveredApplications(final ApiClient apiClient, final String discoveryType)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.groups.DiscoveredApplications discoveredApplicationsStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.groups.DiscoveredApplications.class);

        com.vmware.sdk.ops.networks.groups.DiscoveredApplicationsStub.GetDiscoveredApplicationsInvocation invocation =
                discoveredApplicationsStub.getDiscoveredApplications()
                        .discoveryType(discoveryType);

        // FLOW_BASED_DISCOVERY requires a granularity parameter
        // The granularity parameter specifies the level of detail for flow-based discovery
        // Allowed values: FINE, MEDIUM, COARSE
        if ("FLOW_BASED_DISCOVERY".equals(discoveryType)) {
            invocation = invocation.granularity("FINE");
        }

        final PagedListResponse pagedListResponse = invocation.invoke().get();
        return pagedListResponse.getResults();
    }

    /**
     * Displays list of discovered applications in a formatted manner.
     * Shows total count and details for each discovered application including entity ID and type.
     *
     * @param applicationList the list of EntityId objects containing discovered application identifiers
     * @param discoveryType the discovery type used for this query
     */
    private static void displayDiscoveredApplicationsList(final List<EntityId> applicationList, final String discoveryType) {
        log.info("Discovery Type: {}", discoveryType);
        log.info("========================================");

        if (applicationList != null && !applicationList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nTotal Discovered Applications: ").append(applicationList.size());
            sb.append("\n");
            
            int index = 1;
            for (EntityId entity : applicationList) {
                sb.append("\nDiscovered Application #").append(index);
                sb.append("\n  Entity ID: ").append(entity.getEntityId());
                sb.append("\n  Entity Type: ").append(entity.getEntityType());
                sb.append("\n");
                index++;
            }
            
            log.info(sb.toString());
        } else {
            log.warn("No discovered applications found for discovery type: {}", discoveryType);
            log.info("This may indicate that:");
            log.info("  - Network traffic analysis has not yet identified any applications");
            log.info("  - The discovery type '{}' is not configured or has no data", discoveryType);
            log.info("  - The feature is not enabled in your environment");
        }

        log.info("========================================");
    }

    /**
     * Analyzes and provides insights about the discovered applications from both discovery types.
     *
     * @param serviceNowApplications the list of discovered applications from SERVICE_NOW
     * @param flowBasedApplications the list of discovered applications from FLOW_BASED_DISCOVERY
     */
    private static void analyzeDiscoveredApplications(final List<EntityId> serviceNowApplications,
                                                       final List<EntityId> flowBasedApplications) {
        log.info("Analysis Summary:");
        
        // SERVICE_NOW analysis
        if (serviceNowApplications != null && !serviceNowApplications.isEmpty()) {
            log.info("\nSERVICE_NOW Discovery Type:");
            log.info("  Total Applications: {}", serviceNowApplications.size());
            long distinctTypesServiceNow = serviceNowApplications.stream()
                    .map(EntityId::getEntityType)
                    .distinct()
                    .count();
            log.info("  Distinct Entity Types: {}", distinctTypesServiceNow);
            log.info("  Source: ServiceNow CMDB integration");
        } else {
            log.info("\nSERVICE_NOW Discovery Type:");
            log.info("  No applications found");
            log.info("  This may indicate ServiceNow integration is not configured or has no data");
        }
        
        // FLOW_BASED_DISCOVERY analysis
        if (flowBasedApplications != null && !flowBasedApplications.isEmpty()) {
            log.info("\nFLOW_BASED_DISCOVERY Discovery Type:");
            log.info("  Total Applications: {}", flowBasedApplications.size());
            long distinctTypesFlowBased = flowBasedApplications.stream()
                    .map(EntityId::getEntityType)
                    .distinct()
                    .count();
            log.info("  Distinct Entity Types: {}", distinctTypesFlowBased);
            log.info("  Source: Network flow traffic analysis");
        } else {
            log.info("\nFLOW_BASED_DISCOVERY Discovery Type:");
            log.info("  No applications found");
            log.info("  This may indicate network flow analysis has not identified any applications");
        }
        
        // Combined total
        int totalServiceNow = (serviceNowApplications != null) ? serviceNowApplications.size() : 0;
        int totalFlowBased = (flowBasedApplications != null) ? flowBasedApplications.size() : 0;
        log.info("\nCombined Total: {} discovered applications", totalServiceNow + totalFlowBased);
        
        log.info("\nKey Characteristics of Discovered Applications:");
        log.info("  - Automatically identified through network traffic analysis or integrations");
        log.info("  - Read-only (cannot be created, modified, or deleted via API)");
        log.info("  - Represent actual network traffic patterns in your environment");
        log.info("  - Updated dynamically as network traffic patterns change");
        log.info("  - SERVICE_NOW: Synced from ServiceNow CMDB integration");
        log.info("  - FLOW_BASED_DISCOVERY: Identified through network flow analysis");
    }
}
