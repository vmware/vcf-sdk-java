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
import com.vmware.ops.api.client.controllers.VcfIntegrationsClient;
import com.vmware.ops.api.client.exceptions.ClientSideException;
import com.vmware.ops.api.model.adapter.VCFDomain;
import com.vmware.ops.api.model.adapter.VCFDomainSummary;
import com.vmware.ops.api.model.adapter.VCFIntegration;
import com.vmware.ops.api.model.common.CertificatesErrorMessage;
import com.vmware.ops.api.model.common.ErrorMessage;
import com.vmware.ops.api.model.common.IntegrationCertificates;
import com.vmware.ops.api.model.common.NameValuePair;
import com.vmware.ops.api.model.credential.CredentialInstance;
import com.vmware.sdk.samples.ops.SampleBase;
import com.vmware.sdk.samples.ops.config.credentials.CredentialConfig;
import com.vmware.sdk.samples.ops.config.integrations.VCFIntegrationConfig;
import com.vmware.sdk.samples.ops.helpers.ClientUtils;
import com.vmware.sdk.samples.ops.helpers.ConfigValidator;

public class VCFIntegrations extends SampleBase {
    private static final Logger logger = LoggerFactory.getLogger(VCFIntegrations.class);

    private final VcfIntegrationsClient vcfIntegrationsClient;
    private final AdapterInstancesClient adapterInstancesClient;
    private final CredentialsClient credentialsClient;

    private final VCFIntegrationConfig vcfIntegrationConfig;
    private final CredentialConfig credentialConfig;

    public boolean deleteResources = true;
    public boolean deleteChildResources = true;
    public boolean forceDelete = false;

    public VCFIntegrations(String clientConfigFile, String sampleConfigClient) throws IOException {
        super(clientConfigFile);
        logger.info("--- VCFIntegrations Constructor Started ---");
        logger.info("Client configuration file: {}", clientConfigFile);
        logger.info("Sample configuration client file: {}", sampleConfigClient);

        vcfIntegrationConfig = mapper.readValue(new File(sampleConfigClient), VCFIntegrationConfig.class);
        ConfigValidator.validate(vcfIntegrationConfig);
        logger.info("VCF Integration config loaded successfully from {}", sampleConfigClient);

        credentialConfig = vcfIntegrationConfig.getCredentialConfig();
        ClientUtils.populateCredentialName(credentialConfig);

        logger.info("Credential config extracted from VCF Integration config.");

        this.vcfIntegrationsClient = getClient().vcfIntegrationsClient();
        logger.info("VcfIntegrationsClient initialized.");

        this.adapterInstancesClient = getClient().adapterInstancesClient();
        logger.info("AdapterInstancesClient initialized.");

        this.credentialsClient = getClient().credentialsClient();
        logger.info("CredentialsClient initialized.");
        logger.info("--- VCFIntegrations Constructor Finished ---");
    }

    @Override
    public void run() {
        logger.info("LOG: --- VCFIntegrations Run Started ---");

        logger.info("LOG: Step 1: Calling createCredential()...");
        UUID credentialId = createCredential();
        logger.info("LOG: Step 1 Finished. Credential created with ID: {}", credentialId);

        logger.info("LOG: Step 2: Calling testAndCreateVCFIntegration(credentialId)...");
        UUID createdVCFIntegrationId = testAndCreateVCFIntegration(credentialId);
        logger.info("LOG: Step 2 Finished. VCF Integration created with ID: {}", createdVCFIntegrationId);

        logger.info("LOG: Step 3: Calling getVCFIntegrationById(createdVCFIntegrationId)...");
        VCFIntegration vcfIntegrationById = getVCFIntegrationById(createdVCFIntegrationId);
        logger.info(
                "LOG: Step 3 Finished. Retrieved VCF Integration: {} (ID: {})",
                vcfIntegrationById.getName(),
                createdVCFIntegrationId);

        logger.info(
                "LOG: Step 4: Calling adapterInstancesClient.startMonitoringResourcesOfAdapterInstance(createdVCFIntegrationId)...");
        // vcf integration will not start automatically, remember to start it via api call
        startMonitoringResourcesOfAdapterInstance(createdVCFIntegrationId);
        logger.info("LOG: Step 4 Finished. Monitoring started for Adapter Instance ID: {}", createdVCFIntegrationId);

        logger.info("LOG: Step 5: Calling updateVCFIntegration(vcfIntegrationById)...");

        // Updating name and description of our created VCF integration
        vcfIntegrationById.setName("Updated VCFIntegration name");
        vcfIntegrationById.setDescription("Updated VCFIntegration description");
        vcfIntegrationById.setCollectorGroupId(null);
        updateVCFIntegration(vcfIntegrationById);

        logger.info("LOG: Step 5 Finished. VCF Integration {} updated.", createdVCFIntegrationId);

        logger.info("LOG: Step 6: Calling getListOfVCFIntegrationDomains(createdVCFIntegrationId)...");
        VCFDomainSummary.VCFDomainSummaries listOfVCFIntegrationDomains =
                getListOfVCFIntegrationDomains(createdVCFIntegrationId);
        UUID domainId;
        if (listOfVCFIntegrationDomains != null
                && listOfVCFIntegrationDomains.getConfiguredDomains() != null
                && !listOfVCFIntegrationDomains.getConfiguredDomains().isEmpty()) {
            domainId = listOfVCFIntegrationDomains.getConfiguredDomains().get(0).getDomainId();
            logger.info(
                    "LOG: Step 6 Finished. Found at least one configured domain. Using the first one with ID: {}",
                    domainId);

            logger.info("LOG: Step 7: Calling getVCFIntegrationDomainByIds(createdVCFIntegrationId, domainId)...");
            VCFDomain domain = getVCFIntegrationDomainByIds(createdVCFIntegrationId, domainId);
            logger.info("LOG: Step 7 Finished. Retrieved VCF Domain: (ID: {})", domainId);

            logger.info("LOG: Step 8: Calling testAndUpdateVCFDomain(domain, domainId, createdVCFIntegrationId)...");

            // Certain fields, such as those in standalone vCenter integration, cannot be changed for vCenter and NSX
            // domains. However, identifiers and the collector can still be modified
            // Additionally, vSAN and SDMP can also be enabled from here.
            domain.getVcfDomainVcenter().setCollectorId("1");
            domain.getVcfDomainVcenter().setCollectorGroupId(null);
            domain.getVcfDomainVcenter().getSdmpConfig().setEnabled(true);
            testAndUpdateVCFDomain(domain, domainId, createdVCFIntegrationId);

            logger.info("LOG: Step 8 Finished. VCF Domain {} updated and tested.", domainId);

            logger.info("LOG: Step 9: Calling deleteVCFDomainByIds(createdVCFIntegrationId, domainId)...");
            deleteVCFDomainByIds(createdVCFIntegrationId, domainId);
            logger.info("LOG: Step 9 Finished. VCF Domain {} deleted.", domainId);
        } else {
            logger.info(
                    "LOG: Step 6 Finished. No configured VCF Domains found for integration {} to process steps 7-9.",
                    createdVCFIntegrationId);
        }
        logger.info("LOG: Step 10: Calling deleteVCFIntegrationById(createdVCFIntegrationId)...");
        deleteVCFIntegrationById(createdVCFIntegrationId);
        logger.info("LOG: Step 10 Finished. VCF Integration {} deleted.", createdVCFIntegrationId);

        logger.info("Sleep for 2 minutes to allow the VCF integration to be deleted");
        try {
            Thread.sleep(2 * 60 * 1000);
        } catch (InterruptedException e) {
            logger.error("Thread interrupted", e);
        }

        logger.info("LOG: Step 11: Calling deleteCredentialInstance(credentialId)...");
        deleteCredentialInstance(credentialId);
        logger.info("LOG: Step 11 Finished. Credential Instance {} deleted.", credentialId);

        logger.info("LOG: --- VCFIntegrations Run Complete ---");
    }

    public void deleteVCFDomainByIds(UUID integrationId, UUID domainId) {
        // We need both integration and domain id's for domain removal
        vcfIntegrationsClient.deleteVcfDomain(integrationId, domainId, this.deleteResources, this.forceDelete);
    }

    public void deleteCredentialInstance(UUID credentialId) {
        credentialsClient.deleteCredential(credentialId);
    }

    public void testAndUpdateVCFDomain(VCFDomain domain, UUID domainId, UUID integrationId) {
        // Similar to VCF, if any certificate needs to be accepted for the connection, it will be included in the error
        // result and must be added in future requests.
        VCFDomain vcfDomain = vcfIntegrationsClient.testDomain(integrationId, domainId, domain);

        vcfIntegrationsClient.updateDomainDetails(integrationId, domainId, vcfDomain, false);
    }

    public VCFDomain getVCFIntegrationDomainByIds(UUID integrationId, UUID domainId) {
        // By this call we can get more detailed information about domain, by providing its id, and integrations id
        return vcfIntegrationsClient.getDomainDetailsById(integrationId, domainId);
    }

    public VCFDomainSummary.VCFDomainSummaries getListOfVCFIntegrationDomains(UUID id) {
        // Returns a list of VCF integration domains, categorized into configured and unconfigured domains
        // The result model for each domain will include only the id and url
        return vcfIntegrationsClient.getDomainSummary(id);
    }

    public void deleteVCFIntegrationById(UUID id) {
        // Deleting created VCF integration by providing its id, and flags, that will indicate deletion of related and
        // child resources
        vcfIntegrationsClient.deleteVcfIntegration(
                id, this.deleteResources, this.deleteChildResources, this.forceDelete);
    }

    public void updateVCFIntegration(VCFIntegration integration) {
        vcfIntegrationsClient.updateVCFIntegration(integration, false);
    }

    public VCFIntegration getVCFIntegrationById(UUID id) {
        return vcfIntegrationsClient.getVCFIntegrationById(id);
    }

    public UUID testAndCreateVCFIntegration(UUID credentialId) {
        // Constructing vcf integration creation request
        VCFIntegration vcfIntegration = new VCFIntegration();
        vcfIntegration.setName(vcfIntegrationConfig.getName());
        vcfIntegration.setDescription(vcfIntegrationConfig.getDescription());
        vcfIntegration.setCollectorId(vcfIntegrationConfig.getCollectorId());

        List<NameValuePair> identifiers = new ArrayList<>();
        for (Map.Entry<String, String> entry :
                vcfIntegrationConfig.getIdentifiers().entrySet()) {
            identifiers.add(new NameValuePair(entry.getKey(), entry.getValue()));
        }

        vcfIntegration.setResourceIdentifiers(identifiers);
        vcfIntegration.setCredentialInstanceId(credentialId);

        try {
            // Testing constructed request body, to be sure everything is normal
            vcfIntegrationsClient.testVCFIntegration(vcfIntegration);
        } catch (ClientSideException clientSideException) {
            ErrorMessage errorMessage = clientSideException.getErrorMessage();
            if (errorMessage instanceof CertificatesErrorMessage) {
                CertificatesErrorMessage certificatesErrorMessage = (CertificatesErrorMessage) errorMessage;
                for (IntegrationCertificates integrationCertificates : certificatesErrorMessage.getCertificates()) {
                    vcfIntegration.getCertificates().addAll(integrationCertificates.getCertificates());
                }
            }
        }
        // In case if testVCFIntegration fails with no valid certificate's error,
        // it will provide ones for acceptance, which means that those certificates must be provided in the future
        // creation request

        VCFIntegration createdIntegration = vcfIntegrationsClient.createVCFIntegration(
                vcfIntegration, vcfIntegrationConfig.isForceManagementOwnership(), vcfIntegrationConfig.isForce());
        return createdIntegration.getId();
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

    public void startMonitoringResourcesOfAdapterInstance(UUID createdVCFIntegrationId) {
        adapterInstancesClient.startMonitoringResourcesOfAdapterInstance(createdVCFIntegrationId);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new RuntimeException(
                    "Please provide the client-config JSON file path first, followed by the vcf-integration-config JSON file path, via arguments.");
        }

        VCFIntegrations clientExample = new VCFIntegrations(args[0], args[1]);
        clientExample.run();
    }
}
