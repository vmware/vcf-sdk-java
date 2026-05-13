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

import com.vmware.sdk.ops.logs.api.v2.agent.Secrets;
import com.vmware.sdk.ops.logs.api.v2.agent.secrets.Exchange;
import com.vmware.sdk.ops.logs.api.v2.agent.secrets.Revoke;
import com.vmware.sdk.ops.logs.model.AgentAuthenticationRequest;
import com.vmware.sdk.ops.logs.model.AgentAuthenticationResponse;
import com.vmware.sdk.ops.logs.model.AgentSecretCreateRequest;
import com.vmware.sdk.ops.logs.model.AgentSecretCreateResponse;
import com.vmware.sdk.ops.logs.model.AgentSecretRevokeResponse;

import com.vmware.sdk.samples.ops.logs.util.ApiClientUtil;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.vapi.bindings.CompletionStageFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentSecretExample {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentSecretExample.class.getName());
    
    private ApiClientUtil clientUtil;
    private Secrets agentSecretsService;
    private Exchange agentSecretsExchangeService;
    private Revoke agentSecretsRevokeService;
    private String secret;
    private String secretName;

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
        AgentSecretExample example = new AgentSecretExample();
        try {
            SampleCommandLineParser.load(AgentSecretExample.class, args);
            example.initialize();
            example.runAllAgentSecretExamples();
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
        agentSecretsService = clientUtil.getV2Factory().agent().secretsService();
        agentSecretsExchangeService = clientUtil.getV2Factory().agent().secrets().exchangeService();
        agentSecretsRevokeService = clientUtil.getV2Factory().agent().secrets().revokeService();
        LOGGER.info("Agent groups service initialized successfully");
    }
    
    /**
     * Run all agent group examples.
     */
    public void runAllAgentSecretExamples() {
        try {
            // Basic agent secret operations
            createSecret();
            exchangeSecrets();
            revokeSecret();
        } catch (Exception e) {
            LOGGER.error("Error running agent group examples", e);
        }
    }

    public void createSecret() {
        LOGGER.info("=== Create Secret Example ===");

        try {
            AgentSecretCreateRequest createRequest = new AgentSecretCreateRequest.Builder()
                    //        When the secret name is not set, it is generated
                    //        .setName("Agent_Secret")
                    .build();
            // Execute the request
            CompletionStageFuture<AgentSecretCreateResponse> future =
                    agentSecretsService.createAgentSecret(createRequest).invoke();
            AgentSecretCreateResponse sec = future.get();

            LOGGER.info("Successfully acquired agent secret:");
            LOGGER.info("Name: {}", sec.getName());
            LOGGER.info("Secret: {}", sec.getSecret());
            secretName = sec.getName();
            secret = sec.getSecret();

        } catch (Exception e) {
            LOGGER.error("Failed to acquire secret", e);
        }
    }

    public void exchangeSecrets() {
        LOGGER.info("=== Exchange Secret Example ===");

        try {
            AgentAuthenticationRequest authRequest = new AgentAuthenticationRequest.Builder()
                    .setSecret(secret)
                    .build();
            // Execute the request
            CompletionStageFuture<AgentAuthenticationResponse> future =
                    agentSecretsExchangeService.createAgentSession(authRequest).invoke();
            AgentAuthenticationResponse auth = future.get();

            LOGGER.info("Successfully exchanged agent secret:");
            LOGGER.info("Name: {}", auth.getName());
            LOGGER.info("New Secret: {}", auth.getNewSecret());

        } catch (Exception e) {
            LOGGER.error("Failed to exchange secret", e);
        }
    }

    public void revokeSecret() {
        LOGGER.info("=== Delete Secret Example ===");

        try {
            // Execute the request
            CompletionStageFuture<AgentSecretRevokeResponse> future =
                    agentSecretsRevokeService.revokeAgentSecret(secretName).invoke();
            AgentSecretRevokeResponse sec = future.get();

            LOGGER.info("Successfully revoked agent secret:");
            LOGGER.info("Name: {}", sec.getName());
        } catch (Exception e) {
            LOGGER.error("Failed to acquire secret", e);
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
