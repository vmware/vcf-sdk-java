/*
 * ******************************************************************
 * Copyright (c) 2025-2026 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.config.resources;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ResourceConfig {
    private String name = "FooBar";
    private String description = "Description";

    @NotBlank
    private String adapterKindKey;

    @NotBlank
    private String resourceKindKey;

    @NotNull
    private Map<String, String> identifiers;

    private StatAndPropertyConfig statAndPropertyConfig;

    private String pushAdapterKindKey = "SOME-PUSH-ADAPTER-KEY";

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

    public String getAdapterKindKey() {
        return adapterKindKey;
    }

    public void setAdapterKindKey(String adapterKindKey) {
        this.adapterKindKey = adapterKindKey;
    }

    public String getResourceKindKey() {
        return resourceKindKey;
    }

    public void setResourceKindKey(String resourceKindKey) {
        this.resourceKindKey = resourceKindKey;
    }

    public Map<String, String> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Map<String, String> identifiers) {
        this.identifiers = identifiers;
    }

    public StatAndPropertyConfig getStatAndPropertyConfig() {
        return statAndPropertyConfig;
    }

    public void setStatAndPropertyConfig(StatAndPropertyConfig statAndPropertyConfig) {
        this.statAndPropertyConfig = statAndPropertyConfig;
    }

    public String getPushAdapterKindKey() {
        return pushAdapterKindKey;
    }

    public void setPushAdapterKindKey(String pushAdapterKindKey) {
        this.pushAdapterKindKey = pushAdapterKindKey;
    }
}
