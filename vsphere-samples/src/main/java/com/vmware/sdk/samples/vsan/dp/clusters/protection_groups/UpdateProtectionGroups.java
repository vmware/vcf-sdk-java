/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vsan.dp.clusters.protection_groups;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.createTrustManagerFromFile;
import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.sdk.utils.ssl.vapi.HttpConfigHelper.createDefaultHttpConfiguration;

import java.security.KeyStore;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;
import com.vmware.sdk.vsphere.utils.vsan.dp.SnapshotServiceClient;
import com.vmware.snapservice.ProtectionGroupUpdateSpec;
import com.vmware.snapservice.TargetEntities;
import com.vmware.snapservice.Tasks;
import com.vmware.snapservice.clusters.ProtectionGroups;
import com.vmware.snapservice.tasks.Info;
import com.vmware.snapservice.tasks.Status;
import com.vmware.vapi.protocol.HttpConfiguration;

/**
 * Demonstrates updating a protection group.
 *
 * <p>Sample Prerequisites: vCenter 8.0.3+
 */
public class UpdateProtectionGroups {
    private static final Logger log = LoggerFactory.getLogger(UpdateProtectionGroups.class);

    /** REQUIRED: vCenter FQDN or IP address. */
    public static String serverAddress = "vcenter1.mycompany.com";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";
    /** REQUIRED: Snapshot service FQDN or IP address. */
    public static String snapServiceAddress = "snapServiceAddress";
    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (do not leave it empty on production environments).
     */
    public static String trustStorePath = null;

    /** REQUIRED: MoRef ID of the cluster whose protection groups to inspect. */
    public static String clusterId = "domain-c1";
    /** REQUIRED: Protection group ID to perform action on. */
    public static String pgId = "pg-id";
    /** REQUIRED: Virtual machine IDs to update. */
    public static String vmIds = "vm-ids";

    public static void main(String[] args) throws InterruptedException {
        SampleCommandLineParser.load(UpdateProtectionGroups.class, args);

        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);
        SimpleHttpConfigurer portConfigurer = new SimpleHttpConfigurer(createTrustManagerFromFile(trustStorePath));
        HttpConfiguration httpConfiguration =
                createDefaultHttpConfiguration(keyStore).getConfig();

        SnapshotServiceClient snapshotServiceClient = new SnapshotServiceClient(
                snapServiceAddress, httpConfiguration, serverAddress, portConfigurer, username, password);

        TargetEntities.Builder builder = new TargetEntities.Builder();
        builder.setVms(Arrays.stream(vmIds.split(",")).collect(Collectors.toSet()));

        ProtectionGroupUpdateSpec updateSpec = new ProtectionGroupUpdateSpec();
        updateSpec.setTargetEntities(builder.build());

        String taskId = updateTask(snapshotServiceClient, clusterId, pgId, updateSpec);
        log.info("Update protection group task: {}", taskId);
        waitForSnapshotServiceTask(snapshotServiceClient, taskId);
    }

    public static String updateTask(
            SnapshotServiceClient snapshotServiceClient,
            String clusterId,
            String pgId,
            ProtectionGroupUpdateSpec updateSpec) {
        ProtectionGroups protectionGroups = snapshotServiceClient.createStub(ProtectionGroups.class);
        return protectionGroups.update_Task(clusterId, pgId, updateSpec);
    }

    private static void waitForSnapshotServiceTask(SnapshotServiceClient snapshotServiceClient, String ssTaskId)
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
                TimeUnit.SECONDS.sleep(5);
            }
        }
    }
}
