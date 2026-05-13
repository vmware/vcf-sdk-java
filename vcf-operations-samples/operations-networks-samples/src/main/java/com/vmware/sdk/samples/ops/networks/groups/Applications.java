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

import com.vmware.sdk.ops.networks.model.Application;
import com.vmware.sdk.ops.networks.model.ApplicationRequest;
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
 * Demonstrates a complete workflow for managing Applications in VCF Operations for networks.
 *
 * <p>This sample demonstrates a complete workflow that:
 * <ol>
 *   <li>Creates an authenticated API client using VcfOpsNetworksClientFactory
 *   <li>Lists all applications (initial state)
 *   <li>Creates a new application
 *   <li>Lists applications again to demonstrate the newly created application
 *   <li>Retrieves the created application by ID to view its details
 *   <li>Deletes the created application
 *   <li>Lists applications one final time to confirm the application was deleted
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
public class Applications {
    private static final Logger log = LoggerFactory.getLogger(Applications.class);

    /** REQUIRED: VCF Operations for networks host address or FQDN. */
    public static String hostName = "hostName";

    /** REQUIRED: Username for authentication. */
    public static String username = "username";

    /** REQUIRED: Password for authentication. */
    public static String password = "password";

    /** OPTIONAL: Trust store path for SSL/TLS certificate validation. */
    public static String trustStorePath = null;

    /**
     * Main method to execute the Applications workflow sample.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        try {
            // Parse command-line arguments
            SampleCommandLineParser.load(Applications.class, args);

            log.info("=== VCF Operations for networks - Applications Workflow Sample ===");
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

            log.info("=== Applications workflow sample completed successfully ===");

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
     * Executes the complete workflow for managing applications.
     * This workflow demonstrates the full lifecycle of an application:
     * list -> create -> list -> get -> delete -> list
     *
     * @param apiClient the authenticated API client
     * @throws ExecutionException if any API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static void executeWorkflow(ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        // Step 1: List applications (initial state)
        log.info("\n========================================");
        log.info("Step 1: List Applications (Initial State)");
        log.info("========================================");
        List<EntityId> initialApplications = listApplications(apiClient);
        displayApplicationsList(initialApplications);

        // Step 2: Create a new application
        log.info("\n========================================");
        log.info("Step 2: Create Application");
        log.info("========================================");
        log.info("Creating new application...");
        Application createdApplication = createApplication(apiClient);
        String createdApplicationId = createdApplication.getEntityId();
        log.info("Application created successfully with ID: {}", createdApplicationId);
        displayApplicationInfo(createdApplication, "Created Application Details");

        // Step 3: List applications again to show the newly created application
        log.info("\n========================================");
        log.info("Step 3: List Applications (After Creation)");
        log.info("========================================");
        List<EntityId> applicationsAfterCreate = listApplications(apiClient);
        displayApplicationsList(applicationsAfterCreate);
        log.info("Notice: The newly created application (ID: {}) should now appear in the list", createdApplicationId);

        // Step 4: Get the created application by ID
        log.info("\n========================================");
        log.info("Step 4: Get Application by ID");
        log.info("========================================");
        log.info("Retrieving application with ID: {}", createdApplicationId);
        Application retrievedApplication = getApplication(apiClient, createdApplicationId);
        displayApplicationInfo(retrievedApplication, "Retrieved Application Details");

        // Step 5: Delete the created application
        log.info("\n========================================");
        log.info("Step 5: Delete Application");
        log.info("========================================");
        log.info("Deleting application with ID: {}", createdApplicationId);
        deleteApplication(apiClient, createdApplicationId);
        log.info("Application deleted successfully");

        // Step 6: List applications one final time to confirm deletion
        log.info("\n========================================");
        log.info("Step 6: List Applications (After Deletion)");
        log.info("========================================");
        List<EntityId> finalApplications = listApplications(apiClient);
        displayApplicationsList(finalApplications);
        log.info("Notice: The deleted application (ID: {}) should no longer appear in the list", createdApplicationId);
    }

    /**
     * Retrieves all applications from the server.
     *
     * @param apiClient the authenticated API client
     * @return List of EntityId objects containing application identifiers
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static List<EntityId> listApplications(final ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.groups.Applications applicationsStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.groups.Applications.class);

        final PagedListResponse pagedListResponse = applicationsStub.listApplications().invoke().get();
        return pagedListResponse.getResults();
    }

    /**
     * Retrieves a specific application by ID.
     *
     * @param apiClient the authenticated API client
     * @param applicationId the ID of the application to retrieve
     * @return Application object containing detailed application information
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static Application getApplication(final ApiClient apiClient, final String applicationId)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.groups.Applications applicationsStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.groups.Applications.class);

        return applicationsStub.getApplicationById(applicationId).invoke().get();
    }

    /**
     * Creates a new application with a test name.
     *
     * @param apiClient the authenticated API client
     * @return Application object containing the created application information
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static Application createApplication(final ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.groups.Applications applicationsStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.groups.Applications.class);

        ApplicationRequest request = new ApplicationRequest();
        request.setName("Test SDK app1");
        return applicationsStub.addApplication(request).invoke().get();
    }

    /**
     * Deletes an application.
     *
     * @param apiClient the authenticated API client
     * @param applicationId the ID of the application to delete
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static void deleteApplication(final ApiClient apiClient, final String applicationId)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.groups.Applications applicationsStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.groups.Applications.class);
        applicationsStub.deleteApplication(applicationId).invoke().get();
    }

    /**
     * Displays list of applications in a formatted manner.
     * Shows total count and details for each application including entity ID and type.
     *
     * @param applicationList the list of EntityId objects containing application identifiers
     */
    private static void displayApplicationsList(final List<EntityId> applicationList) {
        log.info("========================================");
        log.info("Applications List:");
        log.info("========================================");

        if (applicationList != null && !applicationList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nTotal Applications: ").append(applicationList.size());
            sb.append("\n");
            
            int index = 1;
            for (EntityId entity : applicationList) {
                sb.append("\nApplication #").append(index);
                sb.append("\n  Entity ID: ").append(entity.getEntityId());
                sb.append("\n  Entity Type: ").append(entity.getEntityType());
                sb.append("\n");
                index++;
            }
            
            log.info(sb.toString());
        } else {
            log.warn("No applications information received");
        }

        log.info("========================================");
    }

    /**
     * Displays detailed application information in a formatted manner.
     *
     * @param application the Application object containing detailed application information
     * @param title the title to display for this application information
     */
    private static void displayApplicationInfo(final Application application, final String title) {
        log.info("========================================");
        log.info("{}", title);
        log.info("========================================");

        if (application != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nFull Application Details: ").append(application);
            log.info(sb.toString());
        } else {
            log.warn("No application information received");
        }

        log.info("========================================");
    }
}

