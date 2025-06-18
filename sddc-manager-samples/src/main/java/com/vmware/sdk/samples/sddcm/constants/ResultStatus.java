/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.sddcm.constants;

public enum ResultStatus {
    SUCCEEDED("SUCCEEDED"),
    FAILED("FAILED"),
    UNKNOWN("UNKNOWN");

    private String resultStatus;

    ResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getStatus() {
        return this.resultStatus;
    }
}
