/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.config.integrations;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.vmware.sdk.samples.ops.config.credentials.CredentialConfig;

public class VCFIntegrationConfig {
    private String name = "Adapter-Instance";
    private String description = "Adapter instance for illustrative purposes";
    private String collectorId = "1";

    @NotNull
    @Valid
    private CredentialConfig credentialConfig;

    @NotNull
    private Map<String, String> identifiers;

    private boolean force = false;
    private boolean forceManagementOwnership = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCollectorId() {
        return collectorId;
    }

    public void setCollectorId(String collectorId) {
        this.collectorId = collectorId;
    }

    public CredentialConfig getCredentialConfig() {
        return credentialConfig;
    }

    public void setCredentialConfig(CredentialConfig credentialConfig) {
        this.credentialConfig = credentialConfig;
    }

    public Map<String, String> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Map<String, String> identifiers) {
        this.identifiers = identifiers;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public boolean isForceManagementOwnership() {
        return forceManagementOwnership;
    }

    public void setForceManagementOwnership(boolean forceManagementOwnership) {
        this.forceManagementOwnership = forceManagementOwnership;
    }
}
