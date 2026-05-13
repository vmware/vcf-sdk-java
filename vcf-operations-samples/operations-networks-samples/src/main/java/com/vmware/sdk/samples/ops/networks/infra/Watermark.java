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

import com.vmware.sdk.ops.networks.model.VCFWatermarkConfiguration;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.ops.networks.utils.VcfOpsNetworksClientFactory;
import com.vmware.vapi.client.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;
import java.util.concurrent.ExecutionException;

/**
 * Demonstrates a complete workflow for managing VCF Watermark in VCF Operations for networks.
 *
 * <p>This sample demonstrates a complete workflow that:
 * <ol>
 *   <li>Creates an authenticated API client using VcfOpsNetworksClientFactory
 *   <li>Gets the current VCF Watermark (initial state)
 *   <li>Saves a new VCF Watermark configuration
 *   <li>Gets the VCF Watermark again (demonstrating the saved configuration)
 *   <li>Updates the VCF Watermark configuration
 *   <li>Gets the VCF Watermark again (demonstrating the updated configuration)
 *   <li>Deletes the VCF Watermark
 *   <li>Gets the VCF Watermark one final time (confirming deletion)
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
public class Watermark {
    private static final Logger log = LoggerFactory.getLogger(Watermark.class);

    /** REQUIRED: VCF Operations for networks host address or FQDN. */
    public static String hostName = "hostName";

    /** REQUIRED: Username for authentication. */
    public static String username = "username";

    /** REQUIRED: Password for authentication. */
    public static String password = "password";

    /** OPTIONAL: Trust store path for SSL/TLS certificate validation. */
    public static String trustStorePath = null;

    /**
     * Main method to execute the Watermark workflow sample.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        try {
            // Parse command-line arguments
            SampleCommandLineParser.load(Watermark.class, args);

            log.info("=== VCF Operations for networks - VCF Watermark Workflow Sample ===");
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

            log.info("=== VCF Watermark workflow sample completed successfully ===");

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
     * Executes the complete workflow for managing VCF Watermark.
     * This workflow demonstrates the full lifecycle: get -> save -> get -> update -> get -> delete -> get
     *
     * @param apiClient the authenticated API client
     * @throws ExecutionException if any API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static void executeWorkflow(ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        // Step 1: Get current VCF Watermark (initial state)
        log.info("\n========================================");
        log.info("Step 1: Get VCF Watermark (Initial State)");
        log.info("========================================");
        log.info("Retrieving current VCF Watermark configuration...");
        VCFWatermarkConfiguration initialWatermark = null;
        try {
            initialWatermark = getVCFWatermark(apiClient);
            displayWatermarkInfo(initialWatermark, "Current VCF Watermark");
        } catch (ExecutionException e) {
            if (e.getMessage() != null && (e.getMessage().contains("404") || e.getMessage().contains("Not Found"))) {
                log.info("No VCF Watermark found in the system (this is expected if no watermark has been configured)");
                log.info("Proceeding to create a new watermark...");
            } else {
                throw e;
            }
        }

        // Step 2: Save a new VCF Watermark
        log.info("\n========================================");
        log.info("Step 2: Save VCF Watermark");
        log.info("========================================");
        log.info("Saving new VCF Watermark configuration...");
        VCFWatermarkConfiguration newWatermark = new VCFWatermarkConfiguration();
        newWatermark.setManagedby("abc.com");
        newWatermark.setVersion("vcf-4.0");
        newWatermark.setLogtoken("VCF");
        newWatermark.setDeployedby("VMware Cloud Foundation");
        newWatermark.setInstanceid("UUID-1234");
        
        saveVCFWatermark(apiClient, newWatermark);
        log.info("VCF Watermark saved successfully");

        // Step 3: Get VCF Watermark again (after save)
        log.info("\n========================================");
        log.info("Step 3: Get VCF Watermark (After Save)");
        log.info("========================================");
        log.info("Retrieving VCF Watermark to verify save operation...");
        VCFWatermarkConfiguration savedWatermark = getVCFWatermark(apiClient);
        displayWatermarkInfo(savedWatermark, "Saved VCF Watermark");
        log.info("Notice: The saved watermark configuration should now be active");

        // Step 4: Update VCF Watermark
        log.info("\n========================================");
        log.info("Step 4: Update VCF Watermark");
        log.info("========================================");
        log.info("Updating VCF Watermark configuration...");
        VCFWatermarkConfiguration watermarkToUpdate = getVCFWatermark(apiClient);
        watermarkToUpdate.setInstanceid("UUID-5678");
        
        VCFWatermarkConfiguration updatedWatermark = updateVCFWatermark(apiClient, watermarkToUpdate);
        displayWatermarkInfo(updatedWatermark, "Updated VCF Watermark");
        log.info("VCF Watermark updated successfully");

        // Step 5: Get VCF Watermark again (after update)
        log.info("\n========================================");
        log.info("Step 5: Get VCF Watermark (After Update)");
        log.info("========================================");
        log.info("Retrieving VCF Watermark to verify update operation...");
        VCFWatermarkConfiguration watermarkAfterUpdate = getVCFWatermark(apiClient);
        displayWatermarkInfo(watermarkAfterUpdate, "VCF Watermark After Update");
        log.info("Notice: The updated watermark configuration should now be active");

        // Step 6: Delete VCF Watermark
        log.info("\n========================================");
        log.info("Step 6: Delete VCF Watermark");
        log.info("========================================");
        log.info("Deleting VCF Watermark...");
        deleteVCFWatermark(apiClient);
        log.info("VCF Watermark deleted successfully");

        // Step 7: Get VCF Watermark one final time (after deletion)
        log.info("\n========================================");
        log.info("Step 7: Get VCF Watermark (After Deletion)");
        log.info("========================================");
        log.info("Retrieving VCF Watermark to confirm deletion...");
        try {
            VCFWatermarkConfiguration watermarkAfterDelete = getVCFWatermark(apiClient);
            displayWatermarkInfo(watermarkAfterDelete, "VCF Watermark After Deletion");
            log.info("Notice: The watermark still exists in the system");
        } catch (ExecutionException e) {
            if (e.getMessage() != null && (e.getMessage().contains("404") || e.getMessage().contains("Not Found"))) {
                log.info("VCF Watermark successfully deleted - no watermark found (404 Not Found)");
                log.info("This confirms that the deletion operation completed successfully");
            } else {
                log.warn("Error retrieving watermark after deletion: {}", e.getMessage());
                log.info("This may indicate the watermark was deleted or an error occurred");
            }
        } catch (Exception e) {
            log.warn("Unexpected error retrieving watermark after deletion: {}", e.getMessage());
            log.info("This may indicate the watermark was deleted or an error occurred");
        }
    }


    /**
     * Retrieves VCF Watermark information from the server.
     *
     * @param apiClient the authenticated API client
     * @return VCFWatermark containing watermark information
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static VCFWatermarkConfiguration getVCFWatermark(final ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.infra.Watermark watermarkStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.infra.Watermark.class);

        return watermarkStub.getVCFWatermark().invoke().get();
    }

    /**
     * Updates VCF Watermark on the server.
     *
     * @param apiClient the authenticated API client
     * @param watermark the VCFWatermark object with updated values
     * @return VCFWatermark containing updated watermark information
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static VCFWatermarkConfiguration updateVCFWatermark(final ApiClient apiClient, final VCFWatermarkConfiguration watermark)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.infra.Watermark watermarkStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.infra.Watermark.class);

        return watermarkStub.updateVCFWatermark(watermark).invoke().get();
    }

    /**
     * Saves VCF Watermark on the server.
     *
     * @param apiClient the authenticated API client
     * @param watermark the VCFWatermark object to save
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static void saveVCFWatermark(final ApiClient apiClient, final VCFWatermarkConfiguration watermark)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.infra.Watermark watermarkStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.infra.Watermark.class);

        watermarkStub.saveVCFWatermark(watermark).invoke().get();
    }

    /**
     * Deletes VCF Watermark from the server.
     *
     * @param apiClient the authenticated API client
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static void deleteVCFWatermark(final ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.infra.Watermark watermarkStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.infra.Watermark.class);

        watermarkStub.deleteVCFWatermark().invoke().get();
    }

    /**
     * Displays VCF Watermark information in a formatted manner.
     *
     * @param watermark the VCFWatermark object containing watermark information
     * @param title the title to display for this watermark information
     */
    private static void displayWatermarkInfo(final VCFWatermarkConfiguration watermark, final String title) {
        log.info("========================================");
        log.info("{}", title);
        log.info("========================================");

        if (watermark != null) {
            // Print full watermark details
            StringBuilder sb = new StringBuilder();
            sb.append("\nFull Watermark Details: ").append(watermark);
            log.info(sb.toString());
        } else {
            log.warn("No watermark information received");
        }

        log.info("========================================");
    }
}
