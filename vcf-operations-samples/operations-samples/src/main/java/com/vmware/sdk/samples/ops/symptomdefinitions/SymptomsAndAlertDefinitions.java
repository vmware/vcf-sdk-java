/*
 * ******************************************************************
 * Copyright (c) 2025-2026 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.symptomdefinitions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.ops.api.client.controllers.AlertDefinitionsClient;
import com.vmware.ops.api.client.controllers.PolicyClient;
import com.vmware.ops.api.client.controllers.SymptomDefinitionsClient;
import com.vmware.ops.api.model.alertdefinition.AlertDefinition;
import com.vmware.ops.api.model.alertdefinition.AlertDefinitionImpact;
import com.vmware.ops.api.model.alertdefinition.AlertDefinitionState;
import com.vmware.ops.api.model.alertdefinition.ImpactType;
import com.vmware.ops.api.model.alertdefinition.SymptomSet;
import com.vmware.ops.api.model.common.CompareOperator;
import com.vmware.ops.api.model.common.Criticality;
import com.vmware.ops.api.model.common.types.RelationshipType;
import com.vmware.ops.api.model.policy.PolicySummary;
import com.vmware.ops.api.model.symptomdefinition.AggregationType;
import com.vmware.ops.api.model.symptomdefinition.HTCondition;
import com.vmware.ops.api.model.symptomdefinition.SymptomDefinition;
import com.vmware.ops.api.model.symptomdefinition.SymptomDefinition.SymptomDefinitions;
import com.vmware.ops.api.model.symptomdefinition.SymptomDefinitionQuery;
import com.vmware.ops.api.model.symptomdefinition.SymptomState;
import com.vmware.sdk.samples.ops.SampleBase;
import com.vmware.sdk.samples.ops.config.alertdefinitions.AlertDefinitionConfig;
import com.vmware.sdk.samples.ops.config.symptomdefintions.SymptomDefinitionConfig;

/**
 * Example that illustrates the use of API client bindings to perform CRUD
 * operations on Symptom Definitions and Alert Definitions
 */
public class SymptomsAndAlertDefinitions extends SampleBase {

    private static final Logger logger = LoggerFactory.getLogger(SymptomsAndAlertDefinitions.class);

    private final AlertDefinitionsClient alertDefinitionsClient;
    private final SymptomDefinitionsClient symptomDefinitionsClient;
    private final PolicyClient policyClient;

    private final SymptomDefinitionConfig symptomDefinitionConfig;
    private final AlertDefinitionConfig alertDefinitionConfig;

    public SymptomsAndAlertDefinitions(String clientConfigFile, String sampleConfigFile) throws IOException {
        super(clientConfigFile);

        alertDefinitionConfig = mapper.readValue(new File(sampleConfigFile), AlertDefinitionConfig.class);
        symptomDefinitionConfig = alertDefinitionConfig.getSymptomDefinitionConfig();

        this.alertDefinitionsClient = getClient().alertDefinitionsClient();
        this.symptomDefinitionsClient = getClient().symptomDefinitionsClient();
        this.policyClient = getClient().policyClient();
    }

    @Override
    public void run() {
        logger.info("Starting Symptom and Alert Definitions Example...");

        // --- Symptom Definition CRUD Operations ---
        logger.info("--- Performing Symptom Definition CRUD operations ---");
        String symptomId1 = createSymptom();
        getSymptomById(symptomId1);
        getSymptomByName(symptomDefinitionConfig.getName());
        updateSymptom(symptomId1);

        // --- Alert Definition CRUD Operations ---
        logger.info("--- Performing Alert Definition CRUD operations ---");
        // Create an alert definition linked to symptomId1
        String alertId1 = createAlertDefinition(symptomId1);
        getAlertDefinitionById(alertId1);
        updateAlertDefinition(alertId1);

        UUID policyId = getDefaultPolicyId();

        // --- Alert Definition Policy Operations ---
        logger.info("--- Demonstrating Alert Definition Policy Management ---");

        enableAlertDefinitionInPolicy(alertId1, policyId);

        // --- Final Cleanup ---
        logger.info("--- Performing final cleanup ---");
        disableAlertDefinitionInPolicy(alertId1, policyId);
        deleteAlertDefinition(alertId1);
        deleteSymptom(symptomId1);

        logger.info("Example finished successfully.");
    }

    public void enableAlertDefinitionInPolicy(String alertId, UUID policyId) {
        logger.info("Enabling alert definition '{}' in policy '{}'", alertId, policyId);
        alertDefinitionsClient.enableAlertDefinitionInPolicy(alertId, new UUID[] {policyId});
        logger.info("Alert definition enabled in policy successfully.");
    }

    public void disableAlertDefinitionInPolicy(String alertId, UUID policyId) {
        logger.info("Disabling alert definition '{}' in policy '{}'", alertId, policyId);
        alertDefinitionsClient.disableAlertDefinitionInPolicies(alertId, new UUID[] {policyId});
        logger.info("Alert definition disabled in policy successfully.");
    }

    public String createSymptom() {
        logger.info("Creating a new symptom definition named: '{}'", symptomDefinitionConfig.getName());
        SymptomDefinition symptom = new SymptomDefinition();
        symptom.setName(symptomDefinitionConfig.getName());
        symptom.setAdapterKindKey(symptomDefinitionConfig.getAdapterKindKey());
        symptom.setResourceKindKey(symptomDefinitionConfig.getResourceKindKey());
        symptom.setWaitCycles(symptomDefinitionConfig.getWaitCycle());
        symptom.setCancelCycles(symptomDefinitionConfig.getCancelCycle());

        SymptomState symptomState = new SymptomState();
        symptomState.setSeverity(Criticality.valueOf(symptomDefinitionConfig.getSeverity()));

        HTCondition condition = new HTCondition();
        condition.setKey(symptomDefinitionConfig.getKey());
        condition.setOperator(CompareOperator.GT_EQ);
        condition.setValue(symptomDefinitionConfig.getValue());
        condition.setInstanced(
                symptomDefinitionConfig.isInstance()); // Applies to the resource itself, not specific instances

        symptomState.setCondition(condition);
        symptom.setState(symptomState);

        SymptomDefinition createdSymptom = symptomDefinitionsClient.createSymptomDefinition(symptom);
        logger.info("Symptom definition created successfully with ID: {}", createdSymptom.getId());
        return createdSymptom.getId();
    }

    public void updateSymptom(String id) {
        logger.info("Updating symptom definition with ID: {}", id);
        SymptomDefinition symptom = getSymptomById(id);
        symptom.setWaitCycles(10); // Example update: change wait cycles
        symptomDefinitionsClient.updateSymptomDefinition(symptom);
        logger.info("Symptom definition '{}' updated (WaitCycles changed to 10).", id);
    }

    public void updateAlertDefinition(String id) {
        logger.info("Updating alert definition with ID: {}", id);
        AlertDefinition alert = getAlertDefinitionById(id);
        alert.setWaitCycles(10); // Example update: change wait cycles
        alertDefinitionsClient.updateAlertDefinition(alert);
        logger.info("Alert definition '{}' updated (WaitCycles changed to 10).", id);
    }

    public SymptomDefinition getSymptomById(String id) {
        logger.info("Getting symptom definition by ID: {}", id);
        SymptomDefinition symptom = symptomDefinitionsClient.getSymptomDefinitionByKey(id);
        logger.info(
                "Successfully looked up symptom definition with ID: {} and name: {}",
                symptom.getId(),
                symptom.getName());
        return symptom;
    }

    public SymptomDefinitions getSymptomByName(String name) {
        logger.info("Querying symptom definitions by name: '{}'", name);
        SymptomDefinitionQuery symptomDefinitionQuery = new SymptomDefinitionQuery();
        symptomDefinitionQuery.setName(name);
        SymptomDefinitions symptoms = symptomDefinitionsClient.querySymptomDefinitions(symptomDefinitionQuery, null);
        logger.info(
                "Number of symptoms found with name '{}': {}",
                name,
                symptoms.getSymptomDefinitions().size());
        if (!symptoms.getSymptomDefinitions().isEmpty()) {
            symptoms.getSymptomDefinitions()
                    .forEach(s -> logger.info("  - Found symptom: {} (ID: {})", s.getName(), s.getId()));
        }
        return symptoms;
    }

    public String createAlertDefinition(String symptomId) {
        logger.info(
                "Creating a new alert definition named '{}' linked to symptom ID: {}",
                alertDefinitionConfig.getName(),
                symptomId);
        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setName(alertDefinitionConfig.getName());
        alertDefinition.setDescription(alertDefinitionConfig.getDescription());
        alertDefinition.setAdapterKindKey(alertDefinitionConfig.getAdapterKindKey());
        alertDefinition.setResourceKindKey(alertDefinitionConfig.getResourceKindKey());
        alertDefinition.setType(alertDefinitionConfig.getType()); // Example type ID (e.g., 'Performance')
        alertDefinition.setSubType(alertDefinitionConfig.getSubType()); // Example sub-type ID (e.g., 'CPU')
        alertDefinition.setWaitCycles(alertDefinitionConfig.getWaitCycle());
        alertDefinition.setCancelCycles(alertDefinitionConfig.getCancelCycle());

        List<AlertDefinitionState> problemStates = new ArrayList<>();
        AlertDefinitionState alertDefinitionState = new AlertDefinitionState();

        AlertDefinitionImpact impact = new AlertDefinitionImpact();
        impact.setImpactType(ImpactType.valueOf(alertDefinitionConfig.getImpactType()));
        impact.setDetail(alertDefinitionConfig.getDetail()); // Impacts the health badge

        alertDefinitionState.setImpact(impact);
        alertDefinitionState.setSeverity(
                Criticality.valueOf(alertDefinitionConfig.getSeverity())); // Severity determined automatically

        SymptomSet symptomSet = new SymptomSet();
        symptomSet.setRelation(
                RelationshipType.valueOf(alertDefinitionConfig.getRelation())); // Symptom on the same resource
        symptomSet.setSymptomDefinitionReferences(Collections.singleton(symptomId));
        symptomSet.setAggregation(AggregationType.valueOf(
                alertDefinitionConfig.getAggregation())); // All symptoms in the set must be true

        alertDefinitionState.setSymptoms(symptomSet);
        problemStates.add(alertDefinitionState);
        alertDefinition.setAlertDefinitionStates(problemStates);

        AlertDefinition createdAlertDefinition = alertDefinitionsClient.createAlertDefinition(alertDefinition);
        logger.info("Alert definition created successfully with ID: {}", createdAlertDefinition.getId());
        return createdAlertDefinition.getId();
    }

    public AlertDefinition getAlertDefinitionById(String id) {
        logger.info("Getting alert definition by ID: {}", id);
        AlertDefinition alert = alertDefinitionsClient.getAlertDefinitionById(id);
        logger.info("Successfully looked up alert definition with ID: {} and name: {}", alert.getId(), alert.getName());
        return alert;
    }

    public void deleteAlertDefinition(String alertId) {
        logger.info("Deleting alert definition with ID: {}", alertId);
        alertDefinitionsClient.deleteAlertDefinition(alertId);
        logger.info("Alert definition '{}' deleted successfully.", alertId);
    }

    public void deleteSymptom(String symptomId) {
        logger.info("Deleting symptom definition with ID: {}", symptomId);
        symptomDefinitionsClient.deleteSymptomDefinition(symptomId);
        logger.info("Symptom definition '{}' deleted successfully.", symptomId);
    }

    public UUID getDefaultPolicyId() {
        PolicySummary policy = policyClient.getPolicies(true).getPolicySummaries().stream()
                .filter(PolicySummary::isDefaultPolicy) // Prefer the default policy
                .findFirst()
                .orElse(policyClient.getPolicies(true).getPolicySummaries().get(0)); // Fallback to first if no default

        UUID policyId = policy.getId();
        logger.info("Using policy: '{}' (ID: {}) for alert definition operations.", policy.getName(), policyId);
        return policyId;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            logger.error(
                    "Please provide the client-config JSON file path first, followed by the symptom-and-alert-definition-config JSON file path, via arguments.");
            System.exit(1);
        }
        new SymptomsAndAlertDefinitions(args[0], args[1]).run();
    }
}
