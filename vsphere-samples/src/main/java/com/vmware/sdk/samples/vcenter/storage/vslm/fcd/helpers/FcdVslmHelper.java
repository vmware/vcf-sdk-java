/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.storage.vslm.fcd.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vslm.RuntimeFaultFaultMsg;
import com.vmware.vslm.VslmPortType;
import com.vmware.vslm.VslmTaskInfo;
import com.vmware.vslm.VslmTaskInfoState;

/**
 * This helper class can be used to fetch vslm port which can in turn be used to invoke any API exposed from vslm
 * endpoint.
 */
public class FcdVslmHelper {
    private static final Logger log = LoggerFactory.getLogger(FcdVslmHelper.class);

    private final VslmPortType vslmPort;
    private static final int VSLM_TASK_SLEEP_INTERVAL = 5000;

    public FcdVslmHelper(VslmPortType vslmPort) {
        this.vslmPort = vslmPort;
    }

    /**
     * Wait for a vslm task to complete.
     *
     * @param taskMoRef task to wait for
     * @return true if task succeeded, false otherwise
     * @throws RuntimeFaultFaultMsg if any error while checking the task
     * @throws java.lang.InterruptedException if the current thread was interrupted while waiting
     */
    public boolean waitForTask(ManagedObjectReference taskMoRef) throws RuntimeFaultFaultMsg, InterruptedException {
        boolean taskSucceeded = false;
        int maxWaitForUpdateAttempts = 12;

        VslmTaskInfo vslmTaskInfo = vslmPort.vslmQueryInfo(taskMoRef);
        log.info("Task {} started at time {}", vslmTaskInfo.getKey(), vslmTaskInfo.getStartTime());

        int attempt = 0;
        while (attempt < maxWaitForUpdateAttempts) {
            vslmTaskInfo = vslmPort.vslmQueryInfo(taskMoRef);
            if (vslmTaskInfo.getState().value().equals(VslmTaskInfoState.SUCCESS.value())) {
                taskSucceeded = true;
                break;
            } else if (vslmTaskInfo.getState().value().equals(VslmTaskInfoState.ERROR.value())) {
                break;
            } else {
                ++attempt;
                log.info(
                        "Vslm task status is: {}, Waiting for {} milliseconds for task to complete...",
                        vslmTaskInfo.getState(),
                        VSLM_TASK_SLEEP_INTERVAL);
                Thread.sleep(VSLM_TASK_SLEEP_INTERVAL);
            }
        }
        if (!taskSucceeded
                && (vslmTaskInfo.getState().value().equals(VslmTaskInfoState.RUNNING.value())
                        || vslmTaskInfo.getState().value().equals(VslmTaskInfoState.QUEUED.value()))) {
            int waitTime = VSLM_TASK_SLEEP_INTERVAL * attempt / 1000;
            log.info(
                    "VslmTaskInfo is still in {} state even after waiting for {} secs",
                    vslmTaskInfo.getState(),
                    waitTime);
        }
        return taskSucceeded;
    }
}
