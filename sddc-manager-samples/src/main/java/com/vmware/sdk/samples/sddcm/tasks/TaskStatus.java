/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.sddcm.tasks;

/** Constants to define the task status. */
public enum TaskStatus {
    PENDING("PENDING"),
    IN_PROGRESS("IN PROGRESS"),
    SUCCESSFUL("SUCCESSFUL"),
    FAILED("FAILED"),
    UNKNOWN("UNKNOWN"),
    SKIPPED("SKIPPED"),
    CANCELLED("CANCELLED"),
    SCHEDULED("SCHEDULED");

    private String taskStatus;

    TaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }

    public String getStatus() {
        return this.taskStatus;
    }
}
