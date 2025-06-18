/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.vcf.installer.utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.vcf.installer.model.SddcSubTask;
import com.vmware.sdk.vcf.installer.model.SddcTask;
import com.vmware.sdk.vcf.installer.model.Validation;
import com.vmware.sdk.vcf.installer.model.ValidationCheck;
import com.vmware.sdk.vcf.installer.v1.Sddcs;
import com.vmware.sdk.vcf.installer.v1.sddcs.Validations;
import com.vmware.vapi.bindings.Service;

/** Util class used for task monitoring. */
public class SddcTaskUtil {
    private static final Logger log = LoggerFactory.getLogger(SddcTaskUtil.class);

    /** VCF Task Statuses. */
    public static final String VALIDATION_COMPLETED = "COMPLETED"; // Validation.executionStatus success

    public static final String VALIDATION_FAILED =
            "FAILED"; // both Validation.executionStatus and Validation.resultStatus failed
    public static final String VALIDATION_RESULT_SUCCEEDED = "SUCCEEDED"; // Validation.resultStatus success
    public static final String VALIDATION_RESULT_WARNING = "WARNING"; // Validation.resultStatus warning
    public static final String VALIDATION_RESULT_UNKNOWN = "UNKNOWN"; // Validation.resultStatus unknown
    public static final String DEPLOYMENT_COMPLETED_WITH_SUCCESS = "COMPLETED_WITH_SUCCESS"; // SddcTask success
    public static final String DEPLOYMENT_COMPLETED_WITH_FAILURE = "COMPLETED_WITH_FAILURE"; // SddcTask failure
    public static final String DEPLOYMENT_ALREADY_EXISTS = "DEPLOYMENT_ALREADY_EXISTS";
    public static final String IN_PROGRESS = "IN_PROGRESS"; // both Validation & SddcTask

    /** Time in milliseconds to wait between calls to VCF Installer Appliance for validation of SDDC Spec tasks. */
    private static final long VALIDATION_TIME_BETWEEN_POLLS_MS =
            Duration.ofSeconds(10).toMillis();

    /** Time in milliseconds after which a validation task is no longer polled and a timeout exception is thrown. */
    private static final long VALIDATION_TASK_TIMEOUT_MS =
            Duration.ofMinutes(10).toMillis();

    /** Time in milliseconds to wait between calls to VCF Installer Appliance for SDDC Manager deployment tasks. */
    private static final long SDDCM_DEPLOY_TIME_BETWEEN_POLLS_MS =
            Duration.ofSeconds(60).toMillis();

    /** Time in milliseconds after which a sddc deploy task is no longer polled and a timeout exception is thrown. */
    private static final long SDDCM_DEPLOY_TASK_TIMEOUT_MS =
            Duration.ofHours(10).toMillis();

    /**
     * Gets the validation task.
     *
     * @param validations Invokable Validations Stub used for polling VCF Installer Appliance.
     * @param validationTaskId ID of the validation task.
     * @return the validation task.
     */
    public static Validation getValidation(Validations validations, String validationTaskId) {
        try {
            return validations.getSddcSpecValidation(validationTaskId).invoke().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Waits for validation task to finish. Returns true if the task is completed successfully.
     *
     * @param validations Invokable Validations Stub used for polling VCF Installer Appliance.
     * @param validationTaskId ID of the validation task.
     * @return true if the task is completed successfully.
     */
    public static boolean waitForValidationTask(Validations validations, String validationTaskId) {
        return waitForValidationTask(
                validations, validationTaskId, VALIDATION_TIME_BETWEEN_POLLS_MS, VALIDATION_TASK_TIMEOUT_MS);
    }

    /**
     * Waits for validation task to finish. Returns true if the task is completed successfully.
     *
     * @param validations Invokable Validations Stub used for polling VCF Installer Appliance.
     * @param validationTaskId ID of the validation task.
     * @param timeBetweenPollMs Time in milliseconds to wait between calls to VCF Installer Appliance.
     * @param taskWaitTimeoutMs Time in milliseconds after which a validation task is no longer polled and a timeout
     *     exception is thrown.
     * @return true if the task is completed successfully.
     */
    public static boolean waitForValidationTask(
            Validations validations, String validationTaskId, long timeBetweenPollMs, long taskWaitTimeoutMs) {
        log.info("Waiting for validation task '{}' to finish", validationTaskId);
        return waitForTask(validations, validationTaskId, timeBetweenPollMs, taskWaitTimeoutMs, (invokable, taskId) -> {
            Validation validation = getValidation((Validations) invokable, validationTaskId);
            String execStatus = validation.getExecutionStatus();
            if (!IN_PROGRESS.equals(execStatus)) {
                logValidationResult(validation);
            }
            return execStatus;
        });
    }

    /**
     * Waits for validation task to finish. Then throws an exception if either the task execution or the validation
     * result is not successful.
     *
     * @param validations Invokable Validations Stub used for polling VCF Installer Appliance.
     * @param validationTaskId ID of the validation task.
     * @throws RuntimeException if task fails or validation is not successful
     */
    public static void waitForValidationTaskAndFailOnError(Validations validations, String validationTaskId) {
        if (!waitForValidationTask(validations, validationTaskId)) {
            throw new RuntimeException("Validation task failed.");
        }
        Validation validation = getValidation(validations, validationTaskId);
        if (!VALIDATION_RESULT_SUCCEEDED.equals(validation.getResultStatus())
                && !VALIDATION_RESULT_WARNING.equals(validation.getResultStatus())) {
            throw new RuntimeException(
                    "Validation of SddcSpec failed. Execution status: " + validation.getResultStatus());
        }
    }

    /**
     * Waits for sddc manager deployment task to finish. Then throws an exception if either the task execution is not
     * successful.
     *
     * @param sddc Invokable Sddcs Stub used for polling VCF Installer Appliance.
     * @param sddcDeploymentTaskId ID of the sddc task.
     * @throws RuntimeException if task fails
     */
    public static void waitForSddcDeploymentTaskAndFailOnError(Sddcs sddc, String sddcDeploymentTaskId) {
        if (!waitForSddcDeploymentTask(sddc, sddcDeploymentTaskId)) {
            throw new RuntimeException("Sddc deployment task failed.");
        }
    }

    /**
     * Waits for sddc manager deployment task to finish. Returns true if the task is completed successfully. Time
     * between polls is set to 10 seconds in milliseconds. Task timeout is set to 10 hours in milliseconds.
     *
     * @param sddc Invokable Sddcs Stub used for polling VCF Installer Appliance.
     * @param sddcDeploymentTaskId ID of the sddc task.
     * @return true if the task is completed successfully.
     */
    public static boolean waitForSddcDeploymentTask(Sddcs sddc, String sddcDeploymentTaskId) {
        return waitForSddcDeploymentTask(
                sddc, sddcDeploymentTaskId, SDDCM_DEPLOY_TIME_BETWEEN_POLLS_MS, SDDCM_DEPLOY_TASK_TIMEOUT_MS);
    }

    /**
     * Waits for sddc manager deployment task to finish. Returns true if the task is completed successfully.
     *
     * @param sddc Invokable Sddcs Stub used for polling VCF Installer Appliance.
     * @param sddcDeploymentTaskId ID of the sddc task.
     * @param timeBetweenPollMs Time in milliseconds to wait between calls to VCF Installer Appliance.
     * @param taskWaitTimeoutMs Time in milliseconds after which a sddc task is no longer polled and a timeout exception
     *     is thrown.
     * @return true if the task is completed successfully.
     */
    public static boolean waitForSddcDeploymentTask(
            Sddcs sddc, String sddcDeploymentTaskId, long timeBetweenPollMs, long taskWaitTimeoutMs) {
        log.info("Waiting for sddc task '{}' to finish", sddcDeploymentTaskId);
        return waitForTask(sddc, sddcDeploymentTaskId, timeBetweenPollMs, taskWaitTimeoutMs, (invokable, taskId) -> {
            try {
                SddcTask sddcTask =
                        ((Sddcs) invokable).getSddcTaskByID(taskId).invoke().get();
                String taskStatus = sddcTask.getStatus();
                if (!IN_PROGRESS.equals(taskStatus)) {
                    if (DEPLOYMENT_COMPLETED_WITH_SUCCESS.equals(taskStatus)) {
                        log.info("Sddc Deployment Task '{}' has completed successfully", taskId);
                    } else if (taskStatus != null && taskStatus.contains(DEPLOYMENT_COMPLETED_WITH_FAILURE)) {
                        log.error("Sddc Deployment Task '{}' has failed. Execution status: {}", taskId, taskStatus);
                        List<SddcSubTask> subTasks = sddcTask.getSddcSubTasks();
                        for (SddcSubTask subTask : subTasks) {
                            if (subTask.getStatus().contains(DEPLOYMENT_COMPLETED_WITH_FAILURE)) {
                                log.error(
                                        "Sddc Deployment SubTask '{}' has failed with the following errors:\n\t{}",
                                        subTask.getName(),
                                        subTask.getErrors());
                            }
                        }
                    }
                }
                return taskStatus;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static boolean waitForTask(
            Service client,
            String taskId,
            long timeBetweenPollMs,
            long taskWaitTimeoutMs,
            BiFunction<Service, String, String> function) {
        long startTime = System.currentTimeMillis();
        long now = startTime;

        while (now < startTime + taskWaitTimeoutMs) {
            now = System.currentTimeMillis();
            try {
                String taskStatus = function.apply(client, taskId);
                if (IN_PROGRESS.equals(taskStatus)) {
                    log.info(
                            "Status for task '{}' is still in progress. Will check again in {} seconds.",
                            taskId,
                            Duration.ofMillis(timeBetweenPollMs).getSeconds());
                    Thread.sleep(timeBetweenPollMs);
                } else {
                    log.info("Task status is: {}", taskStatus);
                    return VALIDATION_COMPLETED.equals(taskStatus)
                            || DEPLOYMENT_COMPLETED_WITH_SUCCESS.equals(taskStatus);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException(String.format(
                "Timed out for task '%s' after %s seconds",
                taskId, Duration.ofMillis(taskWaitTimeoutMs).getSeconds()));
    }

    private static void logValidationResult(Validation validation) {
        List<ValidationCheck> validationChecks = validation.getValidationChecks();
        List<ValidationCheck> failures = new ArrayList<>();
        for (ValidationCheck valCheck : validationChecks) {
            if (!VALIDATION_RESULT_UNKNOWN.equals(valCheck.getResultStatus())) {
                log.info("Validation Check Result: {}", valCheck);
            }
            if (VALIDATION_FAILED.equals(valCheck.getResultStatus())) {
                failures.add(valCheck);
            }
        }
        for (ValidationCheck failedCheck : failures) {
            log.error("Failed Validation Check: {}", failedCheck);
        }
    }
}
