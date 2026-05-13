/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.networks.info;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import com.vmware.sdk.ops.networks.model.VersionResponse;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.vapi.client.ApiClient;
import com.vmware.sdk.ops.networks.utils.VcfOpsNetworksClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;
import java.util.concurrent.ExecutionException;

/**
 * Demonstrates how to retrieve version information from VCF Operations for networks.
 *
 * <p>This sample shows how to:
 * <ol>
 *   <li>Create an authenticated API client using VcfOpsNetworksClientFactory
 *   <li>Call the Version API to get version information
 *   <li>Display the version details
 * </ol>
 */
public class Version {
    private static final Logger log = LoggerFactory.getLogger(Version.class);

    /** REQUIRED: VCF Operations for networks host address or FQDN. */
    public static String hostName = "hostName";

    /** REQUIRED: Username for authentication. */
    public static String username = "username";

    /** REQUIRED: Password for authentication. */
    public static String password = "password";

    /** OPTIONAL: Trust store path for SSL/TLS certificate validation. */
    public static String trustStorePath = null;
    /**
     * Main method to execute the Version sample.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        try {
            // Parse command-line arguments
            SampleCommandLineParser.load(Version.class, args);

            log.info("=== VCF Operations for networks - Version Sample ===");
            log.info("Host: {}", hostName);
            log.info("Username: {}", username);

            // Create API client using VcfOpsNetworksClientFactory
            log.info("Creating API client...");
            KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);
            VcfOpsNetworksClientFactory factory = new VcfOpsNetworksClientFactory();
            ApiClient apiClient = factory.createClient(hostName, username, password, keyStore);
            log.info("API client created successfully");

            // Get version information
            log.info("Retrieving version information...");
            VersionResponse versionResponse = getVersion(apiClient);

            // Display version information
            displayVersionInfo(versionResponse);

            log.info("=== Version sample completed successfully ===");

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
     * Retrieves version information from the server.
     *
     * @param apiClient the authenticated API client
     * @return VersionResponse containing version details
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static VersionResponse getVersion(ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        // Create Version stub
        com.vmware.sdk.ops.networks.info.Version versionStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.info.Version.class);

        // Call getVersion API
        return versionStub.getVersion().invoke().get();
    }

    /**
     * Displays version information in a formatted manner.
     *
     * @param versionResponse the version response object
     */
    private static void displayVersionInfo(VersionResponse versionResponse) {
        log.info("========================================");
        log.info("Version Information:");
        log.info("========================================");

        if (versionResponse != null) {
            if (versionResponse.getApiVersion() != null) {
                log.info("API Version: {}", versionResponse.getApiVersion());
            }
            
            // Print full response object which contains all available fields
            log.info("\nVersion Response: {}", versionResponse);
        } else {
            log.warn("No version information received");
        }

        log.info("========================================\n");
    }
}
