/*
 * ******************************************************************
 * Copyright (c) 2025-2026 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * The term "Broadcom" refers to Broadcom Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.logs;

import com.vmware.sdk.ops.logs.api.v2.logs.Forwarders;
import com.vmware.sdk.ops.logs.model.LogForwarder;
import com.vmware.sdk.samples.ops.logs.util.ApiClientUtil;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.vapi.bindings.CompletionStageFuture;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogForwarderExample {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LogForwarderExample.class.getName());

    private ApiClientUtil clientUtil;
    private Forwarders forwardersService;

    /** REQUIRED: VCF Log Management FQDN or IP address. */
    public static String logsHost = "<LOGS-ADDRESS>";
    /** REQUIRED: VCF Log Management Port. */
    public static String logsPort = "<port>";
    /** REQUIRED: VCF Operations FQDN or IP address. */
    public static String opsHost = "<VROPS-SUITE-VM-IP-ADDRESS>";
    /** REQUIRED: VCF Operations Username. */
    public static String username = "<username>";
    /** REQUIRED: VCF Operations Password. */
    public static String password = "<password>";

    public static void main(String[] args) {
        LogForwarderExample example = new LogForwarderExample();
        try {
            SampleCommandLineParser.load(LogForwarderExample.class, args);
            example.initialize();
            example.runAllForwarderExamples();
        } catch (Exception e) {
            LOGGER.error("Log forwarder example failed", e);
        } finally {
            example.cleanup();
        }
    }
    
    /**
     * Initialize the client and forwarders service.
     */
    public void initialize() throws Exception {
        LOGGER.info("Initializing example...");
        clientUtil = new ApiClientUtil();
        clientUtil.initializeClient(opsHost, username, password, logsHost, logsPort);
        forwardersService = clientUtil.getV2Factory().logs().forwardersService();
        LOGGER.info("Forwarders service initialized successfully");
    }
    
    /**
     * Run all log forwarder examples.
     */
    public void runAllForwarderExamples() {
        try {
            // Basic forwarder operations
            getAllForwarders();
            String forwarderId = createForwarder();
            
            if (forwarderId != null) {
                getForwarderById(forwarderId);
                updateForwarder(forwarderId);
                deleteForwarder(forwarderId);
            }
        } catch (Exception e) {
            LOGGER.error("Error running forwarder examples", e);
        }
    }
    
    /**
     * Example 1: Retrieve all log forwarders.
     */
    public void getAllForwarders() {
        LOGGER.info("=== Get All Log Forwarders Example ===");
        
        try {
            // Execute the request
            CompletionStageFuture<List<LogForwarder>> future = forwardersService.getAllLogForwarders().invoke();
            List<LogForwarder> forwarders = future.get();
            
            LOGGER.info("Retrieved {} log forwarders", forwarders.size());
            
            // Display forwarder information
            for (LogForwarder forwarder : forwarders) {
                LOGGER.info("Forwarder ID: {}", forwarder.getId());
                LOGGER.info("Forwarder Name: {}", forwarder.getName());
                LOGGER.info("Host: {}", forwarder.getHost() + ":" + forwarder.getPort());
                LOGGER.info("Protocol: {}", forwarder.getProtocol());
                LOGGER.info("Transport: {}", forwarder.getTransportProtocol());
                LOGGER.info("SSL Enabled: {}", forwarder.getSslEnabled());
                LOGGER.info("Enabled: {}", forwarder.getEnabled());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve all forwarders", e);
        }
    }
    
    /**
     * Example 2: Create a new forwarder.
     */
    public String createForwarder() {
        LOGGER.info("=== Create Forwarder Example ===");
        
        try {
            // Create a new syslog forwarder
            LogForwarder newForwarder = new LogForwarder();
            newForwarder.setName("Log Forwarder");
            newForwarder.setHost("syslog.example.com");
            newForwarder.setPort(514L);
            newForwarder.setProtocol(LogForwarder.PROTOCOL_SYSLOG);
            newForwarder.setTransportProtocol(LogForwarder.TRANSPORT_PROTOCOL_TCP);
            newForwarder.setSslEnabled(false);
            newForwarder.setEnabled(true);
            newForwarder.setWorkerCount(2L);
            newForwarder.setConnectionRefreshInterval(300L); // 5 minutes
            newForwarder.setForwardComplementaryFields(true);
            
            // Add tags
            Map<String, String> tags = new HashMap<>();
            tags.put("environment", "production");
            tags.put("team", "infrastructure");
            tags.put("purpose", "log-aggregation");
            newForwarder.setTags(tags);
            
            // Execute the creation request
            CompletionStageFuture<LogForwarder> future = forwardersService.createLogForwarder(newForwarder).invoke();
            LogForwarder createdForwarder = future.get();
            
            LOGGER.info("Successfully created log forwarder:");
            LOGGER.info("Forwarder ID: {}", createdForwarder.getId());
            LOGGER.info("Forwarder Name: {}", createdForwarder.getName());
            LOGGER.info("Host: {}", createdForwarder.getHost() + ":" + createdForwarder.getPort());
            LOGGER.info("Protocol: {}", createdForwarder.getProtocol());
            
            return createdForwarder.getId();
            
        } catch (Exception e) {
            LOGGER.error("Failed to create syslog forwarder", e);
            return null;
        }
    }

    /**
     * Example 3: Get forwarder by ID
     */
    public void getForwarderById(String forwarderId) {
        LOGGER.info("=== Get Forwarder By ID Example ===");
        
        try {
            // Execute the request
            CompletionStageFuture<LogForwarder> future = forwardersService.getLogForwarderById(forwarderId).invoke();
            LogForwarder forwarder = future.get();
            
            LOGGER.info("Retrieved log forwarder:");
            LOGGER.info("Forwarder ID: {}", forwarder.getId());
            LOGGER.info("Name: {}", forwarder.getName());
            LOGGER.info("Host: {}", forwarder.getHost());
            LOGGER.info("Port: {}", forwarder.getPort());
            LOGGER.info("Protocol: {}", forwarder.getProtocol());
            LOGGER.info("Transport Protocol: {}", forwarder.getTransportProtocol());
            LOGGER.info("SSL Enabled: {}", forwarder.getSslEnabled());
            LOGGER.info("Enabled: {}", forwarder.getEnabled());
            LOGGER.info("Worker Count: {}", forwarder.getWorkerCount());
            LOGGER.info("Connection Refresh Interval: {}", forwarder.getConnectionRefreshInterval());
            LOGGER.info("Forward Complementary Fields: {}", forwarder.getForwardComplementaryFields());
            LOGGER.info("Tags: {}", forwarder.getTags());
            
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve forwarder by ID", e);
        }
    }
    
    /**
     * Example 4: Update an existing forwarder.
     */
    public void updateForwarder(String forwarderId) {
        LOGGER.info("=== Update Forwarder Example ===");
        
        try {
            // First, get the existing forwarder
            CompletionStageFuture<LogForwarder> getFuture = forwardersService.getLogForwarderById(forwarderId).invoke();
            LogForwarder existingForwarder = getFuture.get();
            
            // Create update request based on existing forwarder
            LogForwarder updateRequest = new LogForwarder();
            updateRequest.setName(existingForwarder.getName());
            updateRequest.setHost(existingForwarder.getHost() + " - Updated");
            updateRequest.setPort(existingForwarder.getPort());
            updateRequest.setProtocol(existingForwarder.getProtocol());
            updateRequest.setTransportProtocol(existingForwarder.getTransportProtocol());
            updateRequest.setSslEnabled(existingForwarder.getSslEnabled());
            updateRequest.setEnabled(existingForwarder.getEnabled());
            updateRequest.setWorkerCount(existingForwarder.getWorkerCount() + 1); // Increase worker count
            updateRequest.setConnectionRefreshInterval(existingForwarder.getConnectionRefreshInterval());
            updateRequest.setForwardComplementaryFields(existingForwarder.getForwardComplementaryFields());

            // Update tags
            Map<String, String> updatedTags = new HashMap<>(existingForwarder.getTags());
            updatedTags.put("updated", "true");
            updatedTags.put("update-time", String.valueOf(System.currentTimeMillis()));
            updateRequest.setTags(updatedTags);
            
            // Execute the update request
            CompletionStageFuture<LogForwarder> updateFuture =
                    forwardersService.updateLogForwarder(forwarderId, updateRequest).invoke();
            LogForwarder updatedForwarder = updateFuture.get();
            
            LOGGER.info("Successfully updated log forwarder:");
            LOGGER.info("Forwarder ID: {}", updatedForwarder.getId());
            LOGGER.info("Updated Name: {}", updatedForwarder.getName());
            LOGGER.info("Updated Host: {}", updatedForwarder.getHost());
            LOGGER.info("Updated Worker Count: {}", updatedForwarder.getWorkerCount());
            LOGGER.info("Updated Tags: {}", updatedForwarder.getTags());
            
        } catch (Exception e) {
            LOGGER.error("Failed to update forwarder", e);
        }
    }
    
    /**
     * Example 5: Delete a forwarder.
     */
    public void deleteForwarder(String forwarderId) {
        LOGGER.info("=== Delete Forwarder Example ===");
        
        try {
            // Execute the delete request
            CompletionStageFuture<Void> future = forwardersService.deleteLogForwarder(forwarderId).invoke();
            future.get(); // Wait for completion
            
            LOGGER.info("Successfully deleted log forwarder with ID: {}", forwarderId);
            
            // Verify deletion by trying to retrieve the forwarder
            try {
                CompletionStageFuture<LogForwarder> verifyFuture =
                        forwardersService.getLogForwarderById(forwarderId).invoke();
                LogForwarder forwarder = verifyFuture.get();
                
                if (forwarder == null) {
                    LOGGER.info("Confirmed: Forwarder has been deleted");
                } else {
                    LOGGER.warn("Forwarder still exists after deletion attempt");
                }
            } catch (Exception verifyException) {
                // Expected if forwarder is truly deleted
                LOGGER.info("Confirmed: Forwarder not found (expected after deletion)");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to delete forwarder", e);
        }
    }
    
    /**
     * Clean up resources.
     */
    public void cleanup() {
        if (clientUtil != null) {
            clientUtil.cleanup();
        }
    }
}
