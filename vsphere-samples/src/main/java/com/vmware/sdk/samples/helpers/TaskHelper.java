/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */
package com.vmware.sdk.samples.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.vsphere.utils.vsan.dp.SnapshotServiceClient;
import com.vmware.snapservice.Tasks;
import com.vmware.snapservice.tasks.Info;
import com.vmware.snapservice.tasks.Status;

public class TaskHelper {
    private static final Logger log = LoggerFactory.getLogger(TaskHelper.class);

    public static void waitForSnapshotServiceTask(SnapshotServiceClient snapshotServiceClient, String ssTaskId)
            throws InterruptedException {
        Tasks tasks = snapshotServiceClient.createStub(Tasks.class);
        while (true) {
            Info taskInfo = tasks.get(ssTaskId);

            if (taskInfo.getStatus() == Status.SUCCEEDED) {
                log.info("# Task {} succeeds: {}", ssTaskId, taskInfo);
                return;
            } else if (taskInfo.getStatus() == Status.FAILED) {
                log.error("# Task {} failed.", taskInfo.getDescription().getId());
                log.error("Error: {}", taskInfo.getError().getMessage());
                return;
            } else {
                log.info(
                        "# Task {} progress: {}",
                        taskInfo.getDescription().getId(),
                        taskInfo.getProgress().getCompleted());
                java.util.concurrent.TimeUnit.SECONDS.sleep(5);
            }
        }
    }
}

