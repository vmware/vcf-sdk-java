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

import com.vmware.sdk.ops.logs.api.v2.agent.Groups;
import com.vmware.sdk.ops.logs.model.AgentGroupPatchRequest;
import com.vmware.sdk.ops.logs.model.AgentGroupRequest;
import com.vmware.sdk.ops.logs.model.AgentGroupResponse;
import com.vmware.sdk.ops.logs.model.BoolQuery;
import com.vmware.sdk.ops.logs.model.Query;
import com.vmware.sdk.samples.ops.logs.util.ApiClientUtil;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.vapi.bindings.CompletionStageFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentGroupExample {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentGroupExample.class.getName());

    private ApiClientUtil clientUtil;
    private Groups agentGroupsService;

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
        AgentGroupExample example = new AgentGroupExample();
        try {
            SampleCommandLineParser.load(AgentGroupExample.class, args);
            example.initialize();
            example.runAllAgentGroupExamples();
        } catch (Exception e) {
            LOGGER.error("Agent group example failed", e);
        } finally {
            example.cleanup();
        }
    }
    
    /**
     * Initialize the client and agent groups service.
     */
    public void initialize() throws Exception {
        LOGGER.info("Initializing example...");
        clientUtil = new ApiClientUtil();
        clientUtil.initializeClient(opsHost, username, password, logsHost, logsPort);
        agentGroupsService = clientUtil.getV2Factory().agent().groupsService();
        LOGGER.info("Agent groups service initialized successfully");
    }
    
    /**
     * Run all agent group examples.
     */
    public void runAllAgentGroupExamples() {
        try {
            // Basic agent group operations
            String groupId = createAgentGroup();
            if (groupId != null) {
                getAgentGroupById(groupId);
                updateAgentGroup(groupId);
                patchAgentGroup(groupId);
                deleteAgentGroup(groupId);
            }
        } catch (Exception e) {
            LOGGER.error("Error running agent group examples", e);
        }
    }
    
    /**
     * Example 1: Create a basic agent group.
     */
    public String createAgentGroup() {
        LOGGER.info("=== Create Agent Group Example ===");
        
        try {
            Query sampleConstraint = new Query();
            sampleConstraint.setBool(new BoolQuery());

            // Create agent group request
            AgentGroupRequest newGroup = new AgentGroupRequest.Builder()
                    .setName("Agent Group")
                    .setInfo("Agent group info")
                    .setAutoUpdate(true)
                    .setConstraints(sampleConstraint)
                    .setAgentConfig("[server]\n max_disk_buffer=100\n")
                    .setMpId("mp-001")
                    .build();
            
            // Execute the creation request
            CompletionStageFuture<AgentGroupResponse> future = agentGroupsService.createAgentGroupConfig(newGroup).invoke();
            AgentGroupResponse createdGroups = future.get();

            AgentGroupResponse createdGroup = createdGroups;
            LOGGER.info("Successfully created agent group:");
            LOGGER.info("Group ID: " + createdGroup.getId());
            LOGGER.info("Group Name: " + createdGroup.getName());
            LOGGER.info("Auto Update: " + createdGroup.getAutoUpdate());

            return createdGroup.getId();

        } catch (Exception e) {
            LOGGER.error("Failed to create agent group", e);
            return null;
        }
    }
    
    /**
     * Example 2: Retrieve a specific agent group by ID.
     */
    public void getAgentGroupById(String groupId) {
        LOGGER.info("=== Get Agent Group By ID Example ===");
        
        try {
            // Execute the request
            CompletionStageFuture<AgentGroupResponse> future =
                    agentGroupsService.getAgentGroupConfigById(groupId).invoke();
            AgentGroupResponse group = future.get();

            LOGGER.info("Retrieved agent group:");
            LOGGER.info("Group ID: " + group.getId());
            LOGGER.info("Name: " + group.getName());
            LOGGER.info("Info: " + group.getInfo());
            LOGGER.info("Auto Update: " + group.getAutoUpdate());
            LOGGER.info("MP ID: " + group.getMpId());
            LOGGER.info("Agent Config: " + (group.getAgentConfig() != null ? "Present" : "Not set"));
            LOGGER.info("Constraints: " + (group.getConstraints() != null ? group.getConstraints().toString() : "None"));

            
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve agent group by ID", e);
        }
    }
    
    /**
     * Example 3: Update an existing agent group (full update).
     */
    public void updateAgentGroup(String groupId) {
        LOGGER.info("=== Update Agent Group Example ===");
        
        try {
            // First, get the existing group
            CompletionStageFuture<AgentGroupResponse> getFuture =
                    agentGroupsService.getAgentGroupConfigById(groupId).invoke();
            AgentGroupResponse existingGroup = getFuture.get();
            Query sampleConstraint = new Query();
            sampleConstraint.setBool(new BoolQuery());
            
            // Create update request
            AgentGroupRequest updateRequest = new AgentGroupRequest.Builder()
                    .setName("Web & Database Servers Group 122")
                    .setInfo("Updated agent group for web and database servers")
                    .setAutoUpdate(true)
                    .setConstraints(sampleConstraint)
                    .setAgentConfig("[server]\n max_disk_buffer=100\n")
                    .setMpId(existingGroup.getMpId())
                    .build();
            
            // Execute the update request
            CompletionStageFuture<AgentGroupResponse> updateFuture =
                    agentGroupsService.updateAgentGroupConfig(groupId, updateRequest).invoke();
            AgentGroupResponse updatedGroup = updateFuture.get();

            LOGGER.info("Successfully updated agent group:");
            LOGGER.info("Group ID: {}", updatedGroup.getId());
            LOGGER.info("Updated Name: {}", updatedGroup.getName());
            LOGGER.info("Updated Info: {}", updatedGroup.getInfo());
            
        } catch (Exception e) {
            LOGGER.error("Failed to update agent group", e);
        }
    }
    
    /**
     * Example 4: Patch an existing agent group (partial update).
     */
    public void patchAgentGroup(String groupId) {
        LOGGER.info("=== Patch Agent Group Example ===");
        
        try {
            // Create a partial update request
            AgentGroupPatchRequest patchRequest = new AgentGroupPatchRequest();
            patchRequest.setInfo("Patched info - optimized for high-performance logging");
            patchRequest.setAutoUpdate(false); // Disable auto-update temporarily
            
            // Execute the patch request
            CompletionStageFuture<AgentGroupResponse> future =
                    agentGroupsService.patchUpdateAgentGroupConfig(groupId, patchRequest).invoke();
            AgentGroupResponse patchedGroup = future.get();

            LOGGER.info("Successfully patched agent group:");
            LOGGER.info("Group ID: {}", patchedGroup.getId());
            LOGGER.info("Name: {}", patchedGroup.getName());
            LOGGER.info("Patched Info: {}", patchedGroup.getInfo());
            LOGGER.info("Auto Update: {}", patchedGroup.getAutoUpdate());
            
        } catch (Exception e) {
            LOGGER.error("Failed to patch agent group", e);
        }
    }
    
    /**
     * Example 5: Delete an agent group.
     */
    public void deleteAgentGroup(String groupId) {
        LOGGER.info("=== Delete Agent Group Example ===");
        
        try {
            // Execute the delete request
            CompletionStageFuture<Void> future = agentGroupsService.deleteAgentGroupConfig(groupId).invoke();
            future.get(); // Wait for completion
            
            LOGGER.info("Successfully deleted agent group with ID: {}", groupId);
            
            // Verify deletion by trying to retrieve the group
            try {
                CompletionStageFuture<AgentGroupResponse> verifyFuture =
                        agentGroupsService.getAgentGroupConfigById(groupId).invoke();
                AgentGroupResponse group = verifyFuture.get();
                
                if (group == null) {
                    LOGGER.info("Confirmed: Agent group has been deleted");
                } else {
                    LOGGER.warn("Agent group still exists after deletion attempt");
                }
            } catch (Exception verifyException) {
                // Expected if group is truly deleted
                LOGGER.info("Confirmed: Agent group not found (expected after deletion)");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to delete agent group", e);
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
