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

import com.vmware.sdk.ops.networks.model.BaseVirtualMachine;
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
 * Demonstrates how to manage VMs (Virtual Machines) in VCF Operations for networks.
 *
 * <p>This sample shows how to:
 * <ol>
 *   <li>Create an authenticated API client using VcfOpsNetworksClientFactory
 *   <li>List all VMs
 *   <li>Get a specific VM by ID
 * </ol>
 *
 * <p>Sample Prerequisites:
 *
 * <ul>
 *   <li>VCF Operations for networks instance running and accessible
 *   <li>Valid credentials (username/password) with appropriate permissions
 *   <li>Network connectivity to the VCF Operations for networks host
 *   <li>Java 11 or later installed
 *   <li>For the "get" operation, a valid VM entity ID (obtained from the "list" operation)
 * </ul>
 */
public class Vms {
    private static final Logger log = LoggerFactory.getLogger(Vms.class);

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

    /** OPTIONAL: VM entity ID for get operation. */
    public static String entityId = null;

    /**
     * Main method to execute the Vms sample.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        try {
            // Parse command-line arguments
            SampleCommandLineParser.load(Vms.class, args);
            initUnsetOptionalParameters();

            log.info("=== VCF Operations for networks - VMs Sample ===");
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

            log.info("=== VMs sample completed successfully ===");

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
     * Performs the list VMs operation.
     *
     * @param apiClient the authenticated API client
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static void performListOperation(ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        log.info("Retrieving all VMs...");
        final List<EntityIdWithTime> entityIdWithTimes = listVms(apiClient);
        displayVmsList(entityIdWithTimes);
    }

    /**
     * Performs the get VM operation.
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

        log.info("Retrieving VM with ID: {}", entityId);
        final BaseVirtualMachine vm = getVm(apiClient, entityId);
        displayVmInfo(vm, "VM Details");
    }

    /**
     * Retrieves all VMs from the server.
     *
     * @param apiClient the authenticated API client
     * @return List of EntityIdWithTime objects containing VM identifiers
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static List<EntityIdWithTime> listVms(final ApiClient apiClient)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.entities.Vms vmsStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.entities.Vms.class);

        return vmsStub.listVms().invoke().get().getResults();
    }

    /**
     * Retrieves a specific VM by ID.
     *
     * @param apiClient the authenticated API client
     * @param entityId the ID of the VM to retrieve
     * @return Vms object containing detailed VM information
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the operation is interrupted
     */
    private static BaseVirtualMachine getVm(final ApiClient apiClient, final String entityId)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.ops.networks.entities.Vms vmsStub =
                apiClient.createStub(com.vmware.sdk.ops.networks.entities.Vms.class);

        return vmsStub.getVm(entityId).invoke().get();
    }

    /**
     * Displays list of VMs in a formatted manner.
     * Shows total count and details for each VM including entity ID and type.
     *
     * @param vmList the list of EntityIdWithTime objects containing VM identifiers
     */
    private static void displayVmsList(final List<EntityIdWithTime> vmList) {
        log.info("========================================");
        log.info("VMs List:");
        log.info("========================================");

        if (vmList != null && !vmList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nTotal VMs: ").append(vmList.size());
            sb.append("\n");
            
            int index = 1;
            for (EntityIdWithTime entity : vmList) {
                sb.append("\nVM #").append(index);
                sb.append("\n  Entity: ").append(entity);
                sb.append("\n");
                index++;
            }
            
            log.info(sb.toString());
        } else {
            log.warn("No VMs information received");
        }

        log.info("========================================");
    }

    /**
     * Displays detailed VM information in a formatted manner.
     *
     * @param vms the Vms object containing detailed VM information
     * @param title the title to display for this VM information
     */
    private static void displayVmInfo(final BaseVirtualMachine vms, final String title) {
        log.info("========================================");
        log.info("{}", title);
        log.info("========================================");

        if (vms != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nFull VM Details: ").append(vms);
            log.info(sb.toString());
        } else {
            log.warn("No VM information received");
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
