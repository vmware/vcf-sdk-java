/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.config.auth;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

public class UserRoleConfig {
    private String name = "Role-Name";
    private String description = "Role-Description";

    @NotEmpty
    private Set<String> privilegeKeys;

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

    public Set<String> getPrivilegeKeys() {
        return privilegeKeys;
    }

    public void setPrivilegeKeys(Set<String> privilegeKeys) {
        this.privilegeKeys = privilegeKeys;
    }
}
