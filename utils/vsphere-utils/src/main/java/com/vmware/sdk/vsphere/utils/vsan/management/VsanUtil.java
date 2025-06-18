/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.vsphere.utils.vsan.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.TaskInfoState;

/** This class holds static utility methods which facilitate usage of the vSAN module of the SDK. */
public class VsanUtil {

    private static final Logger log = LoggerFactory.getLogger(VsanUtil.class);

    /**
     * This method waits the task until it's done and returns a boolean value specifying whether the task is succeeded
     * or failed.
     *
     * @param propertyCollectorHelper The helper instance for waiting on the given task
     * @param task ManagedObjectReference representing the Task.
     * @return boolean value representing the Task result.
     */
    public static boolean waitForTasks(PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference task) {
        boolean retVal = false;
        log.debug("Awaiting task {}", task.getValue());
        try {
            Object[] result = propertyCollectorHelper.awaitManagedObjectUpdates(
                    task, new String[] {"info.state", "info.error"}, new String[] {"state"}, new Object[][] {
                        new Object[] {TaskInfoState.SUCCESS, TaskInfoState.ERROR}
                    });

            if (result[0].equals(TaskInfoState.SUCCESS)) {
                log.debug("Task {} finished successfully", task.getValue());
                retVal = true;
            } else {
                log.debug("Task {} failed", task.getValue());
                retVal = false;
            }
        } catch (Exception ex) {
            log.error("Failed to wait for task.", ex);
        }
        return retVal;
    }
}
