/*
 * ******************************************************************
 * Copyright (c) 2025-2026 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.credentials;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.ops.api.client.controllers.CredentialsClient;
import com.vmware.ops.api.model.common.NameValuePair;
import com.vmware.ops.api.model.credential.CredentialInstance;
import com.vmware.ops.api.model.credential.CredentialInstance.CredentialInstances;
import com.vmware.ops.api.model.credential.CredentialType;
import com.vmware.ops.api.model.credential.CredentialType.CredentialTypes;
import com.vmware.ops.api.model.credential.CredentialsQuery;
import com.vmware.sdk.samples.ops.SampleBase;
import com.vmware.sdk.samples.ops.config.credentials.CredentialConfig;
import com.vmware.sdk.samples.ops.helpers.ClientUtils;
import com.vmware.sdk.samples.ops.helpers.ConfigValidator;

/**
 * Example that illustrates the use of API client bindings to perform CRUD
 * operations for Credential Management. Also, this example
 * illustrates the ability to query the Credential Kinds (metadata)
 * available in the system.
 */
public class CredentialsManagement extends SampleBase {

    private static final Logger logger = LoggerFactory.getLogger(CredentialsManagement.class);

    private final CredentialConfig credentialConfig;
    private final CredentialsClient credentialsClient;

    public CredentialsManagement(String clientConfigFile, String sampleConfigFile) throws IOException {
        super(clientConfigFile);

        credentialConfig = mapper.readValue(new File(sampleConfigFile), CredentialConfig.class);
        ClientUtils.populateCredentialName(credentialConfig);

        ConfigValidator.validate(credentialConfig);

        credentialsClient = getClient().credentialsClient();
    }

    @Override
    public void run() {
        CredentialInstance credentialInstance = null;
        try {
            CredentialTypes credKinds = listCredentialKinds();
            for (CredentialType credType : credKinds.getCredentialTypes()) {
                logger.info("Credential Kind: {}", credType.getId());
            }

            credentialInstance = createCredentialInstance();

            credentialInstance.setName("Updated name");
            // Propagating the password field since it was returned as null
            String password = credentialConfig.getFields().get("PASSWORD");
            for (NameValuePair field : credentialInstance.getFields()) {
                if (field.getName().equals("PASSWORD")) {
                    field.setValue(password);
                }
            }
            modifyCredentialInstance(credentialInstance);

            CredentialInstances ciList = retrieveCredentialInstance(null);
            logger.info("Listing all Credential Instances...");
            logger.info(
                    "# Credential Instances = {}",
                    ciList.getCredentialInstances().size());
            CredentialInstances credentialInstances = retrieveCredentialInstance(null);

            List<UUID> uuidList = credentialInstances.getCredentialInstances().stream()
                    .map(CredentialInstance::getId)
                    .collect(Collectors.toList());

            CredentialsQuery querySpec = new CredentialsQuery();
            logger.info("Listing all Credential Instances - Empty Query Spec...");
            retrieveCredentialInstance(querySpec);

            querySpec = new CredentialsQuery();
            querySpec.setAdapterKind(new String[] {"Container"});
            logger.info("Retrieving Credential Instances for the Container Adapter Kind...");
            retrieveCredentialInstance(querySpec);

            querySpec = new CredentialsQuery();
            querySpec.setId(new UUID[] {uuidList.get(0)});
            logger.info("Retrieving Credential Instance for the specified ID");
            ciList = retrieveCredentialInstance(querySpec);
            logger.info(
                    "Credential = {}", ciList.getCredentialInstances().get(0).toString());

            logger.info("Cleaning up...");
            deleteCredentialInstance(credentialInstance.getId());

        } catch (Exception e) {
            if (credentialInstance != null) {
                logger.info("Cleaning up...");
                deleteCredentialInstance(credentialInstance.getId());
            }
            throw e;
        }
    }

    public CredentialTypes listCredentialKinds() {
        logger.info("Get all the Credential Kinds in the system...");
        return credentialsClient.listCredentialKinds();
    }

    public CredentialInstance createCredentialInstance() {
        logger.info("Creating a Credential Instance...");
        CredentialInstance ci = new CredentialInstance();
        ci.setName(credentialConfig.getName());
        ci.setCredentialKindKey(credentialConfig.getCredentialKindKey());
        ci.setAdapterKindKey(credentialConfig.getAdapterKindKey());
        List<NameValuePair> fields = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> entry : credentialConfig.getFields().entrySet()) {
            NameValuePair aField = new NameValuePair(entry.getKey(), entry.getValue());
            fields.add(aField);
        }
        ci.setFields(fields);
        return credentialsClient.createCredential(ci);
    }

    public CredentialInstances retrieveCredentialInstance(CredentialsQuery querySpec) {
        CredentialInstances ciList = credentialsClient.getCredentialInstances(querySpec);
        logger.info("# CredentialsClient = {}", ciList.getCredentialInstances().size());
        return ciList;
    }

    public CredentialInstance modifyCredentialInstance(CredentialInstance credentialInstance) {
        logger.info("Modify credential instance...");
        return credentialsClient.updateCredential(credentialInstance);
    }

    public void deleteCredentialInstance(UUID id) {
        logger.info("Delete credential instance with id: {}", id);
        credentialsClient.deleteCredential(id);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new RuntimeException(
                    "Please provide the client-config JSON file path first, followed by the credential-config JSON file path, via arguments.");
        }

        new CredentialsManagement(args[0], args[1]).run();
    }
}
