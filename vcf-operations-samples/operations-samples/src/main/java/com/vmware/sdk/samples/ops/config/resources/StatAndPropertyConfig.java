/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.config.resources;

import jakarta.validation.constraints.NotBlank;

public class StatAndPropertyConfig {

    @NotBlank
    private String statKey;

    @NotBlank
    private String propertyKey;

    private int numberOfStatAndPropertyToPush = 5;

    public String getStatKey() {
        return statKey;
    }

    public void setStatKey(String statKey) {
        this.statKey = statKey;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public void setPropertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    public int getNumberOfStatAndPropertyToPush() {
        return numberOfStatAndPropertyToPush;
    }

    public void setNumberOfStatAndPropertyToPush(int numberOfStatAndPropertyToPush) {
        this.numberOfStatAndPropertyToPush = numberOfStatAndPropertyToPush;
    }
}
