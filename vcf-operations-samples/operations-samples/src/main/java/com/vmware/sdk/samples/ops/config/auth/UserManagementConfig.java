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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class UserManagementConfig {

    @NotNull
    @Valid
    private UserConfig userConfig;

    @NotNull
    @Valid
    private UserGroupConfig userGroupConfig;

    @NotNull
    @Valid
    private UserRoleConfig userRoleConfig;

    public UserConfig getUserConfig() {
        return userConfig;
    }

    public void setUserConfig(UserConfig userConfig) {
        this.userConfig = userConfig;
    }

    public UserGroupConfig getUserGroupConfig() {
        return userGroupConfig;
    }

    public void setUserGroupConfig(UserGroupConfig userGroupConfig) {
        this.userGroupConfig = userGroupConfig;
    }

    public UserRoleConfig getUserRoleConfig() {
        return userRoleConfig;
    }

    public void setUserRoleConfig(UserRoleConfig userRoleConfig) {
        this.userRoleConfig = userRoleConfig;
    }
}
