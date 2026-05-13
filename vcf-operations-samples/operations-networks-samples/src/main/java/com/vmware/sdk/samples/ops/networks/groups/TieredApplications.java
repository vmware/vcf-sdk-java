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

import com.vmware.sdk.ops.networks.model.ApiError;
import com.vmware.sdk.ops.networks.model.Application;
import com.vmware.sdk.ops.networks.model.ApplicationRequest;
import com.vmware.sdk.ops.networks.model.EntityId;
import com.vmware.sdk.ops.networks.model.GroupMembershipCriteria;
import com.vmware.sdk.ops.networks.model.PagedListResponse;
import com.vmware.sdk.ops.networks.model.SearchMembershipCriteria;
import com.vmware.sdk.ops.networks.model.Tier;
import com.vmware.sdk.ops.networks.model.TierRequest;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.vapi.client.ApiClient;
import com.vmware.sdk.ops.networks.utils.VcfOpsNetworksClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Demonstrates a complete workflow for managing Tiered Applications in VCF Operations for networks.
 *
 * <p>This sample demonstrates a complete workflow that:
 * <ol>
 *   <li>Creates an authenticated API client using VcfOpsNetworksClientFactory
 *   <li>Creates a new application
 *   <li>Adds a tier to the application
 *   <li>Gets the tier information to verify creation
 *   <li>Deletes the tier from the application
 *   <li>Deletes the application
 *   <li>Lists all applications to confirm deletion
 * </ol>
 *
 * <p>Tiered Applications are applications that are organized into multiple tiers
 * (e.g., web tier, application tier, database tier). This sample demonstrates how to
 * manage the complete lifecycle of a tiered application including tier management.
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
public class TieredApplications {
    private static final Logger log = LoggerFactory.getLogger(TieredApplications.class);

    /** REQUIRED: VCF Operations for networks host address or FQDN. */
    public static String hostName = "hostName";

    /** REQUIRED: Username for authentication. */
    public static String username = "username";

    /** REQUIRED: Password for authentication. */
    public static String password = "password";

    /** OPTIONAL: Trust store path for SSL/TLS certificate validation. */
    public static String trustStorePath = null;

    /**
     * Main method to execute the Tiered Applications workflow sample.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        try {
            // Parse command-line arguments
            SampleCommandLineParser.load(TieredApplications.class, args);

            log.info("=== VCF Operations for networks - Tiered Applications Workflow Sample ===");
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

            log.info("=== Tiered Applications workflow sample completed successfully ===");

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
     * Executes the complete workflow for managing tiered applications.
     * This workflow demonstrates: create application -> add tier -> get tier -> delete tier -> delete application -> list applications
     *
     * @param apiClient the authenticated API client
     * @throws ExecutionException if any API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static void executeWorkflow(ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        // Step 1: Create Application
        log.info("\n========================================");
        log.info("Step 1: Create Application");
        log.info("========================================");
        log.info("Creating new application...");
        Application createdApplication = createApplication(apiClient);
        String createdApplicationId = createdApplication.getEntityId();
        log.info("Application created successfully with ID: {}", createdApplicationId);
        displayApplicationInfo(createdApplication, "Created Application Details");

        // Step 2: Add Tier to Application
        log.info("\n========================================");
        log.info("Step 2: Add Tier to Application");
        log.info("========================================");
        log.info("Adding tier to application with ID: {}", createdApplicationId);
        String tierId = addTierToApplication(apiClient, createdApplicationId);
        log.info("Tier added successfully with ID: {}", tierId);

        // Step 3: Get Tier Information
        log.info("\n========================================");
        log.info("Step 3: Get Tier Information");
        log.info("========================================");
        log.info("Retrieving tier information for tier ID: {} in application ID: {}", tierId, createdApplicationId);
        Tier tier = getApplicationTier(apiClient, createdApplicationId, tierId);
        displayTierInfo(tier, "Tier Details");

        // Step 4: Delete Tier from Application
        log.info("\n========================================");
        log.info("Step 4: Delete Tier from Application");
        log.info("========================================");
        log.info("Deleting tier with ID: {} from application with ID: {}", tierId, createdApplicationId);
        deleteTierFromApplication(apiClient, createdApplicationId, tierId);
        log.info("Tier deleted successfully");

        // Step 5: Delete Application
        log.info("\n========================================");
        log.info("Step 5: Delete Application");
        log.info("========================================");
        log.info("Deleting application with ID: {}", createdApplicationId);
        deleteApplication(apiClient, createdApplicationId);
        log.info("Application deleted successfully");

        // Step 6: List Applications
        log.info("\n========================================");
        log.info("Step 6: List Applications");
        log.info("========================================");
        log.info("Retrieving all applications to confirm deletion...");
        List<EntityId> finalApplications = listApplications(apiClient);
        displayApplicationsList(finalApplications);
        log.info("Notice: The deleted application (ID: {}) should no longer appear in the list", createdApplicationId);
    }

    /**
     * Creates a new application with a test name.
     * If an application with the same name already exists, it will be deleted first.
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
        String applicationName = "Test Tiered Application";
        request.setName(applicationName);

        try {
            return applicationsStub.addApplication(request).invoke().get();
        } catch (ExecutionException e) {
            // Check if the exception is due to an application with the same name already existing
            Throwable cause = e.getCause();
            if (cause instanceof ApiError) {
                ApiError apiError = (ApiError) cause;
                if (apiError.getCode() != null && apiError.getCode() == 400
                        && apiError.getMessage() != null
                        && apiError.getMessage().contains("already exists")) {
                    log.warn("Application with name '{}' already exists. Attempting to delete existing application...", applicationName);
                    
                    // Find and delete the existing application
                    deleteExistingApplicationByName(apiClient, applicationName);
                    
                    // Retry creating the application
                    log.info("Retrying application creation after deleting existing application...");
                    return applicationsStub.addApplication(request).invoke().get();
                }
            }
            // Re-throw if it's not the expected ApiError
            throw e;
        }
    }

    /**
     * Finds and deletes an existing application by name.
     *
     * @param apiClient the authenticated API client
     * @param applicationName the name of the application to find and delete
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static void deleteExistingApplicationByName(final ApiClient apiClient, final String applicationName)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.groups.Applications applicationsStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.groups.Applications.class);

        // List all applications to find the one with the matching name
        PagedListResponse pagedListResponse = applicationsStub.listApplications().invoke().get();
        List<EntityId> applications = pagedListResponse.getResults();

        if (applications != null) {
            for (EntityId entityId : applications) {
                try {
                    // Get the full application details to check the name
                    Application app = applicationsStub.getApplicationById(entityId.getEntityId()).invoke().get();
                    if (app != null && applicationName.equals(app.getName())) {
                        log.info("Found existing application with name '{}' (ID: {}). Deleting it...", applicationName, entityId.getEntityId());
                        applicationsStub.deleteApplication(entityId.getEntityId()).invoke().get();
                        log.info("Successfully deleted existing application with name '{}'", applicationName);
                        return;
                    }
                } catch (ExecutionException e) {
                    // If we can't get or delete this application, continue to the next one
                    log.warn("Could not process application with ID {}: {}", entityId.getEntityId(), e.getMessage());
                }
            }
        }

        log.warn("Could not find an existing application with name '{}' to delete", applicationName);
    }

    /**
     * Adds a tier to an application.
     *
     * @param apiClient the authenticated API client
     * @param applicationId the ID of the application to add the tier to
     * @return the ID of the created tier
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static String addTierToApplication(final ApiClient apiClient, final String applicationId)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.groups.applications.Tiers tiersStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.groups.applications.Tiers.class);

        // Create SearchMembershipCriteria
        SearchMembershipCriteria searchMembershipCriteria = new SearchMembershipCriteria();
        searchMembershipCriteria.setEntityType("VirtualMachine");
        searchMembershipCriteria.setFilter("security_groups.entity_id = '18230:82:604573173'");

        // Create GroupMembershipCriteria and set SearchMembershipCriteria
        GroupMembershipCriteria groupMembershipCriteria = new GroupMembershipCriteria();
        groupMembershipCriteria.setMembershipType("SearchMembershipCriteria");
        groupMembershipCriteria.setSearchMembershipCriteria(searchMembershipCriteria);

        // Create a list of GroupMembershipCriteria (TierRequest expects a List)
        List<GroupMembershipCriteria> groupMembershipCriteriaList = new ArrayList<>();
        groupMembershipCriteriaList.add(groupMembershipCriteria);

        // Create TierRequest and set required properties
        TierRequest tierRequest = new TierRequest();
        tierRequest.setName("Web Tier");
        tierRequest.setGroupMembershipCriteria(groupMembershipCriteriaList);
        
        Tier createdTier = tiersStub.addTier(applicationId, tierRequest).invoke().get();
        return createdTier.getEntityId();
    }

    /**
     * Gets a specific tier from an application.
     *
     * @param apiClient the authenticated API client
     * @param applicationId the ID of the application
     * @param tierId the ID of the tier to retrieve
     * @return Tier object containing tier information
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static Tier getApplicationTier(final ApiClient apiClient, final String applicationId, final String tierId)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.groups.applications.Tiers tiersStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.groups.applications.Tiers.class);

        return tiersStub.getApplicationTier(applicationId, tierId).invoke().get();
    }

    /**
     * Deletes a tier from an application.
     *
     * @param apiClient the authenticated API client
     * @param applicationId the ID of the application
     * @param tierId the ID of the tier to delete
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static void deleteTierFromApplication(final ApiClient apiClient, final String applicationId, final String tierId)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.groups.applications.Tiers tiersStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.groups.applications.Tiers.class);

        tiersStub.deleteTier(applicationId, tierId).invoke().get();
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
     * Displays detailed tier information in a formatted manner.
     *
     * @param tier the Tier object containing detailed tier information
     * @param title the title to display for this tier information
     */
    private static void displayTierInfo(final Tier tier, final String title) {
        log.info("========================================");
        log.info("{}", title);
        log.info("========================================");

        if (tier != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nFull Tier Details: ").append(tier);
            log.info(sb.toString());
        } else {
            log.warn("No tier information received");
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
