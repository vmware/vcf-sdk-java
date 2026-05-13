/*
 * ******************************************************************
 * Copyright (c) 2025-2026 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.config.credentials;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CredentialConfig {
    private String name;

    @NotBlank
    private String adapterKindKey;

    @NotBlank
    private String credentialKindKey;

    @NotNull
    private Map<String, String> fields;

    public String getAdapterKindKey() {
        return adapterKindKey;
    }

    public void setAdapterKindKey(String adapterKindKey) {
        this.adapterKindKey = adapterKindKey;
    }

    public String getCredentialKindKey() {
        return credentialKindKey;
    }

    public void setCredentialKindKey(String credentialKindKey) {
        this.credentialKindKey = credentialKindKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }
}
