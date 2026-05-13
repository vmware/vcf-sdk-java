/*
 * ******************************************************************
 * Copyright (c) 2025-2026 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.integrations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.ops.api.client.controllers.AdapterInstancesClient;
import com.vmware.ops.api.client.controllers.CredentialsClient;
import com.vmware.ops.api.client.controllers.VcenterIntegrationsClient;
import com.vmware.ops.api.client.exceptions.ClientSideException;
import com.vmware.ops.api.model.adapter.VcenterIntegration;
import com.vmware.ops.api.model.common.CertificatesErrorMessage;
import com.vmware.ops.api.model.common.ErrorMessage;
import com.vmware.ops.api.model.common.NameValuePair;
import com.vmware.ops.api.model.credential.CredentialInstance;
import com.vmware.sdk.samples.ops.SampleBase;
import com.vmware.sdk.samples.ops.config.credentials.CredentialConfig;
import com.vmware.sdk.samples.ops.config.integrations.VcenterIntegrationConfig;
import com.vmware.sdk.samples.ops.helpers.ClientUtils;
import com.vmware.sdk.samples.ops.helpers.ConfigValidator;

public class VcenterIntegrations extends SampleBase {
    private static final Logger logger = LoggerFactory.getLogger(VcenterIntegrations.class);

    private final VcenterIntegrationsClient vcenterIntegrationsClient;
    private final AdapterInstancesClient adapterInstancesClient;
    private final CredentialsClient credentialsClient;

    private final VcenterIntegrationConfig vcenterIntegrationConfig;
    private final CredentialConfig credentialConfig;

    public boolean deleteResources = true;
    public boolean deleteChildResources = true;
    public boolean forceDelete = false;

    public VcenterIntegrations(String clientConfigFile, String sampleConfigClient) throws IOException {
        super(clientConfigFile);
        logger.info("Initializing VcenterIntegrations...");
        logger.info("Loading sample config from: {}", sampleConfigClient);
        vcenterIntegrationConfig = mapper.readValue(new File(sampleConfigClient), VcenterIntegrationConfig.class);
        ConfigValidator.validate(vcenterIntegrationConfig);
        credentialConfig = vcenterIntegrationConfig.getCredentialConfig();
        ClientUtils.populateCredentialName(credentialConfig);

        this.vcenterIntegrationsClient = getClient().vcenterIntegrationsClient();
        this.adapterInstancesClient = getClient().adapterInstancesClient();
        this.credentialsClient = getClient().credentialsClient();
        logger.info("Clients initialized successfully.");
    }

    @Override
    public void run() {
        logger.info("--- Starting VcenterIntegrationExample run() ---");
        UUID credentialId = createCredential();
        logger.info("Created Credential with ID: {}", credentialId);

        UUID createdVcenterIntegrationId = testAndCreateVcenterIntegration(credentialId);
        logger.info("Created vCenter Integration with ID: {}", createdVcenterIntegrationId);

        VcenterIntegration vcenterIntegrationById = getVcenterIntegrationById(createdVcenterIntegrationId);
        logger.info(
                "Retrieved vCenter Integration by ID. Name: {}, ID: {}",
                vcenterIntegrationById.getName(),
                vcenterIntegrationById.getId());

        logger.info("Starting monitoring resources for Adapter Instance with ID: {}", createdVcenterIntegrationId);
        // vCenter integration will not start automatically, remember to start it via api call
        startMonitoringResourcesOfAdapterInstance(createdVcenterIntegrationId);
        logger.info("Monitoring started for vCenter Integration.");

        VcenterIntegration updatedVcenterIntegration = updateVcenterIntegration(vcenterIntegrationById);
        logger.info(
                "Updated vCenter Integration. New Name: {}, ID: {}",
                updatedVcenterIntegration.getName(),
                updatedVcenterIntegration.getId());

        deleteVcenterIntegration(updatedVcenterIntegration.getId());
        logger.info("Deleted vCenter Integration with ID: {}", updatedVcenterIntegration.getId());

        logger.info("Sleep for 2 minutes to allow the vCenter integration to be deleted");
        try {
            Thread.sleep(2 * 60 * 1000);
        } catch (InterruptedException e) {
            logger.error("Thread interrupted", e);
        }

        deleteCredentialInstance(credentialId);
        logger.info("Deleted Credential with ID: {}", credentialId);
        logger.info("--- VcenterIntegrationExample run() completed ---");
    }

    public UUID testAndCreateVcenterIntegration(UUID credentialId) {
        logger.info("Entering testAndCreateVcenterIntegration...");
        // Constructing request for vCenter integration creation
        VcenterIntegration request = new VcenterIntegration();
        request.setName(vcenterIntegrationConfig.getName());
        request.setDescription(vcenterIntegrationConfig.getDescription());
        request.setCollectorId(vcenterIntegrationConfig.getCollectorId());
        logger.info("Constructing vCenter Integration request. Name: {}", request.getName());

        // Constructing and setting required identifiers
        List<NameValuePair> identifiersList = new ArrayList<>();
        for (Map.Entry<String, String> entry :
                vcenterIntegrationConfig.getIdentifiers().entrySet()) {
            identifiersList.add(new NameValuePair(entry.getKey(), entry.getValue()));
        }
        request.setResourceIdentifiers(identifiersList);
        request.setCredentialInstanceId(credentialId);
        logger.info("Set Resource Identifiers and Credential ID: {}", credentialId);

        // Construction and setting vSAN and Service Discovery adapters info (also enable-ing them for provided vCenter
        // integration, after its creation)
        VcenterIntegration.VsanConfig vsanConfig = new VcenterIntegration.VsanConfig();
        vsanConfig.setEnabled(vcenterIntegrationConfig.isEnableVSAN());
        vsanConfig.setSmartDataCollectionEnabled(vcenterIntegrationConfig.isSmartDataCollection());
        request.setVsanConfig(vsanConfig);
        logger.info(
                "vSAN config enabled: {}, SmartDataCollection: {}",
                vsanConfig.isEnabled(),
                vsanConfig.isSmartDataCollectionEnabled());

        VcenterIntegration.SdmpConfig sdmpConfig = new VcenterIntegration.SdmpConfig();
        sdmpConfig.setEnabled(vcenterIntegrationConfig.isEnableSDMP());
        sdmpConfig.setApplicationDiscoveryEnabled(vcenterIntegrationConfig.isEnableAutoDiscovery());
        request.setSdmpConfig(sdmpConfig);
        logger.info(
                "SDMP config enabled: {}, ApplicationDiscovery: {}",
                sdmpConfig.isEnabled(),
                sdmpConfig.isApplicationDiscoveryEnabled());

        try {
            // Testing vCenter integration request to be sure everything is normal
            logger.info("Testing vCenter integration request...");
            VcenterIntegration testedVcenterIntegrationRequest =
                    vcenterIntegrationsClient.testVcenterIntegration(request);
            logger.info(
                    "vCenter Integration test successful. Test result ID: {}", testedVcenterIntegrationRequest.getId());
        } catch (ClientSideException clientSideException) {
            ErrorMessage errorMessage = clientSideException.getErrorMessage();
            if (errorMessage instanceof CertificatesErrorMessage) {
                CertificatesErrorMessage certificatesErrorMessage = (CertificatesErrorMessage) errorMessage;
                request.setCertificates(
                        certificatesErrorMessage.getCertificates().get(0).getCertificates());
            }
        }

        // In case if testVcenterIntegration fails with no valid certificate's error,
        // it will provide ones for acceptance, which means that those certificates must be provided in the future
        // creation request
        logger.info("Creating vCenter integration...");
        VcenterIntegration result = vcenterIntegrationsClient.createVcenterIntegration(
                request, vcenterIntegrationConfig.isForce(), vcenterIntegrationConfig.isForceManagementOwnership());
        logger.info("vCenter Integration created with ID: {}, Name: {}", result.getId(), result.getName());
        return result.getId();
    }

    public VcenterIntegration getVcenterIntegrationById(UUID id) {
        logger.info("Getting vCenter Integration by ID: {}", id);
        VcenterIntegration integration = vcenterIntegrationsClient.getVcenterIntegrationById(id);
        logger.info("Successfully retrieved vCenter Integration. Name: {}", integration.getName());
        return integration;
    }

    public VcenterIntegration updateVcenterIntegration(VcenterIntegration vcenterIntegration) {
        logger.info("Updating vCenter Integration with ID: {}", vcenterIntegration.getId());
        String oldName = vcenterIntegration.getName();
        String newName = "UpdatedTestVcenterIntegration-"
                + UUID.randomUUID().toString(); // Add a unique suffix for better identification
        // Updating name and description of our created vCenter integration
        vcenterIntegration.setName(newName);
        vcenterIntegration.setDescription("Updated vCenter integration description");
        vcenterIntegration.setCollectorGroupId(null);
        logger.info("Old Name: {}, New Name: {}", oldName, vcenterIntegration.getName());
        VcenterIntegration updated = vcenterIntegrationsClient.updateVcenterIntegration(vcenterIntegration, false);
        logger.info("vCenter Integration updated successfully. Updated Name: {}", updated.getName());
        return updated;
    }

    public void deleteVcenterIntegration(UUID id) {
        logger.info("Deleting vCenter Integration with ID: {}", id);
        // Deleting created vCenter integration by providing its id, and flags, that will indicate deletion of related
        // and child resources
        vcenterIntegrationsClient.deleteVcenterIntegration(
                id, this.deleteResources, this.deleteChildResources, this.forceDelete);
        logger.info("vCenter Integration with ID {} deleted successfully.", id);
    }

    public void deleteCredentialInstance(UUID credentialId) {
        logger.info("Deleting Credential with ID: {}", credentialId);
        credentialsClient.deleteCredential(credentialId);
        logger.info("Credential with ID {} deleted successfully.", credentialId);
    }

    public void startMonitoringResourcesOfAdapterInstance(UUID createdVcenterIntegrationId) {
        adapterInstancesClient.startMonitoringResourcesOfAdapterInstance(createdVcenterIntegrationId);
    }

    public UUID createCredential() {
        logger.info("Creating Credential...");
        CredentialInstance credentialInstance = new CredentialInstance();
        credentialInstance.setName(credentialConfig.getName());
        credentialInstance.setCredentialKindKey(credentialConfig.getCredentialKindKey());
        credentialInstance.setAdapterKindKey(credentialConfig.getAdapterKindKey());
        logger.info(
                "Credential Name: {}, Kind Key: {}",
                credentialInstance.getName(),
                credentialInstance.getCredentialKindKey());
        List<NameValuePair> fields = new ArrayList<>();
        for (Map.Entry<String, String> entry : credentialConfig.getFields().entrySet()) {
            String valueDisplay = entry.getKey().equalsIgnoreCase("PASSWORD") ? "********" : entry.getValue();
            fields.add(new NameValuePair(entry.getKey(), entry.getValue()));
            logger.info("Adding credential field: {} = {}", entry.getKey(), valueDisplay);
        }
        credentialInstance.setFields(fields);
        CredentialInstance createdCredential = credentialsClient.createCredential(credentialInstance);
        logger.info("Credential created with ID: {}, Name: {}", createdCredential.getId(), createdCredential.getName());
        return createdCredential.getId();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new RuntimeException(
                    "Please provide the client-config JSON file path first, followed by the vcenter-integration-config JSON file path, via arguments.");
        }

        VcenterIntegrations clientExample = new VcenterIntegrations(args[0], args[1]);
        clientExample.run();
    }
}
