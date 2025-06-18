/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.ha.helpers;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

public class TaskHelper {
    private static final Logger log = LoggerFactory.getLogger(TaskHelper.class);

    private static final String TASK_TYPE_MO_REF = "Task";
    private static final String TASK_ID_SEPARATOR = ":";
    public static final Long TASK_SLEEP = 60L;

    /**
     * Waits for given Task to complete.
     *
     * @param vimPort vim port to access the operation
     * @param serviceContent service content used to find the task
     * @param taskID ID for the performed task
     * @return true if the task is completed
     */
    public static boolean waitForTask(VimPortType vimPort, ServiceContent serviceContent, String taskID) {
        String[] taskIDParts = taskID.split(TASK_ID_SEPARATOR);
        String finalTaskID = taskIDParts[0];

        ManagedObjectReference taskMoRef = new ManagedObjectReference();
        taskMoRef.setType(TASK_TYPE_MO_REF);
        taskMoRef.setValue(finalTaskID);

        PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);
        try {
            if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                log.info("Successfully completed task [{}]", taskMoRef.getValue());
                return true;
            } else {
                log.error("Failed to complete task [{}]", taskMoRef.getValue());
            }
        } catch (Exception e) {
            log.error("Unable to execute task [{}]", taskMoRef.getValue());
            log.error("Reason: {}", e.getLocalizedMessage());
        }
        return false;
    }

    public static void sleep(Long duration) {
        Long startTime = System.nanoTime();
        long currTime = System.nanoTime();

        while (currTime - startTime < TimeUnit.NANOSECONDS.convert(duration, TimeUnit.SECONDS)) {
            currTime = System.nanoTime();
        }
    }
}
