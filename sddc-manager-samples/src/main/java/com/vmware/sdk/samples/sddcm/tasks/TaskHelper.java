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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.sddcm.utils.SddcUtil;
import com.vmware.sdk.sddcm.model.Task;
import com.vmware.sdk.sddcm.v1.Tasks;

/** Helper class to keep track of the task/task list, user can configure the retry count and retry wait time. */
public class TaskHelper {
    private static final Logger log = LoggerFactory.getLogger(TaskHelper.class);
    private int retryWaitTimeInSec;
    private final int retryCount;

    private static final Integer TASK_WAIT_TIME = 15;

    private static final Integer DEFAULT_TASK_POLL_TIME_IN_SECONDS = 60;

    private static final Integer RETRY_COUNT = 3;

    private static final Integer RETRY_WAIT_TIME_IN_SECONDS = 180;

    public TaskHelper() {
        this.retryCount = RETRY_COUNT;
        this.retryWaitTimeInSec = RETRY_WAIT_TIME_IN_SECONDS;
    }

    public TaskHelper(int retryCount) {
        this.retryCount = retryCount;
    }

    public TaskHelper(int retryCount, int retryWaitTimeInSec) {
        this.retryCount = retryCount;
        this.retryWaitTimeInSec = retryWaitTimeInSec;
    }

    /**
     * Method to monitor the task status.
     *
     * @param task to monitor
     * @param taskService required to know the status of the task
     * @return boolean status based on whether task is success(true)/failed(false)
     * @throws Exception if any error occurs during monitoring of the task
     */
    public boolean monitorTask(Task task, Tasks taskService) throws Exception {
        return monitorTaskList(List.of(task), taskService);
    }

    /**
     * Method to monitor the tasks status.
     *
     * @param taskList to monitor
     * @param taskService required to know the status of the task
     * @return boolean status based on whether task is success(true)/failed(false)
     * @throws Exception if any error occurs during monitoring of the task
     */
    public boolean monitorTaskList(List<Task> taskList, Tasks taskService) throws Exception {
        log.info("Task Monitoring Started");
        Map<String, Integer> retryMap = new HashMap<>();

        log.info("Waiting for {} seconds before fetching task details", TASK_WAIT_TIME);
        Thread.sleep(TASK_WAIT_TIME * 1000);
        boolean flag = taskList.isEmpty();
        while (!flag) {
            for (Task task : taskList) {
                task = taskService.getTask(task.getId()).invoke().get();
                log.debug("Task Status : {}", task.getStatus());
                if (task.getStatus().equalsIgnoreCase(TaskStatus.IN_PROGRESS.getStatus())) {
                    log.info("Task Name:- {}, Id:- {}, Status:- {}", task.getName(), task.getId(), task.getStatus());
                } else if (task.getStatus().equalsIgnoreCase(TaskStatus.FAILED.getStatus())) {
                    log.error("Task Name:- {}, Id:- {}, Status:- {}", task.getName(), task.getId(), task.getStatus());

                    // Retry logic for parallel tasks
                    String id = task.getId();
                    retryMap.put(id, retryMap.containsKey(id) ? retryMap.get(id) + 1 : 1);
                    Integer count = retryMap.get(id);
                    if (count <= retryCount) {
                        log.info("Waiting for {} seconds before retrying failed task", retryWaitTimeInSec);
                        Thread.sleep(retryWaitTimeInSec * 1000L);
                        log.info(
                                "Retrying task (Attempt:- {}/{}), Task name:- {}, Id:- {}",
                                count,
                                retryCount,
                                task.getName(),
                                id);
                        taskService.retryTask(id).invoke().get();
                    } else {
                        return false;
                    }
                }
            }
            for (Task task : taskList) {
                task = taskService.getTask(task.getId()).invoke().get();
                if (task.getStatus().equalsIgnoreCase(TaskStatus.IN_PROGRESS.getStatus())
                        || task.getStatus().equalsIgnoreCase(TaskStatus.PENDING.getStatus())) {
                    flag = false;
                    break;
                } else {
                    flag = true;
                }
            }
            log.info("Wait for {} seconds before next polling", DEFAULT_TASK_POLL_TIME_IN_SECONDS);
            Thread.sleep(DEFAULT_TASK_POLL_TIME_IN_SECONDS * 1000);
        }
        return true;
    }

    /**
     * Method to monitor the long-running tasks.
     *
     * @param taskList to monitor
     * @param sddcManagerHostname SDDC host FQDN/IpAddress
     * @param sddcManagerSsoUserName SDDC SSO user name
     * @param sddcManagerSsoPassword SDDC SSO password
     * @param taskPollTime Task poll time in seconds to monitor task at given interval
     * @return boolean status based on whether task is success(true)/failed(false)
     * @throws Exception if any error occurs during monitoring of the task
     */
    public boolean monitorTasks(
            List<Task> taskList,
            String sddcManagerHostname,
            String sddcManagerSsoUserName,
            String sddcManagerSsoPassword,
            Integer taskPollTime)
            throws Exception {
        log.info("Task monitoring started");
        Map<String, Integer> retryMap = new HashMap<>();

        log.info("Waiting for {} seconds before fetching task details.", TASK_WAIT_TIME);
        Thread.sleep(TASK_WAIT_TIME * 1000);
        boolean flag = taskList.isEmpty();
        while (!flag) {
            try (SddcUtil.SddcFactory factory =
                    new SddcUtil.SddcFactory(sddcManagerHostname, sddcManagerSsoUserName, sddcManagerSsoPassword)) {
                Tasks taskService = factory.getV1Factory().tasksService();
                for (Task task : taskList) {
                    task = taskService.getTask(task.getId()).invoke().get();
                    log.debug("Task status : {}", task.getStatus());
                    if (task.getStatus().equalsIgnoreCase(TaskStatus.IN_PROGRESS.getStatus())) {
                        log.info(
                                "Task name:- {}, Id:- {}, Status:- {}", task.getName(), task.getId(), task.getStatus());
                    } else if (task.getStatus().equalsIgnoreCase(TaskStatus.FAILED.getStatus())) {
                        log.error(
                                "Task name:- {}, Id:- {}, Status:- {}", task.getName(), task.getId(), task.getStatus());
                        // Retry logic for parallel tasks
                        String id = task.getId();
                        retryMap.put(id, retryMap.containsKey(id) ? retryMap.get(id) + 1 : 1);
                        Integer count = retryMap.get(id);
                        if (count <= retryCount) {
                            log.info("Waiting for {} seconds before retrying failed task.", retryWaitTimeInSec);
                            Thread.sleep(retryWaitTimeInSec * 1000L);
                            log.info(
                                    "Retrying task (Attempt:- {}/{}), Task name:- {}, Id:- {}",
                                    count,
                                    retryCount,
                                    task.getName(),
                                    id);
                            taskService.retryTask(id).invoke().get();
                        } else {
                            return false;
                        }
                    }
                }
                for (Task task : taskList) {
                    task = taskService.getTask(task.getId()).invoke().get();
                    log.debug("Task status : {} for id : {}", task.getStatus(), task.getId());
                    if (task.getStatus().equalsIgnoreCase(TaskStatus.IN_PROGRESS.getStatus())
                            || task.getStatus().equalsIgnoreCase(TaskStatus.PENDING.getStatus())) {
                        flag = false;
                        break;
                    } else {
                        flag = true;
                    }
                }
                log.info("Wait for {} seconds before next polling.", taskPollTime);
                Thread.sleep(taskPollTime * 1000);
            }
        }
        return true;
    }
}
