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
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;
import com.vmware.sdk.vsphere.utils.vsan.dp.SnapshotServiceClient;
import com.vmware.snapservice.DailyRetention;
import com.vmware.snapservice.LogicalOperator;
import com.vmware.snapservice.LongTermRetention;
import com.vmware.snapservice.ProtectionGroupSpec;
import com.vmware.snapservice.ReplicationPolicy;
import com.vmware.snapservice.RetentionPeriod;
import com.vmware.snapservice.RetentionPolicy;
import com.vmware.snapservice.ShortTermRetention;
import com.vmware.snapservice.SnapshotPolicy;
import com.vmware.snapservice.SnapshotSchedule;
import com.vmware.snapservice.TagRule;
import com.vmware.snapservice.TargetEntities;
import com.vmware.snapservice.Tasks;
import com.vmware.snapservice.TimePeriod;
import com.vmware.snapservice.TimeUnit;
import com.vmware.snapservice.clusters.ProtectionGroups;
import com.vmware.snapservice.tasks.Info;
import com.vmware.snapservice.tasks.Status;
import com.vmware.vapi.protocol.HttpConfiguration;

/**
 * Demonstrates creating a protection group using Tag Rules.
 *
 * <p>Sample Prerequisites: vCenter 9.0.0+
 */
public class CreateProtectionGroupTag {
    private static final Logger log = LoggerFactory.getLogger(CreateProtectionGroupTag.class);

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

    /** REQUIRED: Cluster MoRef ID where the protection group locates. */
    public static String clusterId = "domain-c1";
    /** REQUIRED: Cluster pair ID where the protection group locates. */
    public static String clusterPairId = "cluster-pair-id";
    /** REQUIRED: Comma separated list of Tag IDs. */
    public static String tags = "id1,id2";

    public static void main(String[] args) throws InterruptedException {
        SampleCommandLineParser.load(CreateProtectionGroupTag.class, args);

        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);
        SimpleHttpConfigurer portConfigurer = new SimpleHttpConfigurer(createTrustManagerFromFile(trustStorePath));
        HttpConfiguration httpConfiguration =
                createDefaultHttpConfiguration(keyStore).getConfig();

        SnapshotServiceClient snapshotServiceClient = new SnapshotServiceClient(
                snapServiceAddress, httpConfiguration, serverAddress, portConfigurer, username, password);

        String taskId = createTask(snapshotServiceClient, clusterId, clusterPairId, tags);
        log.info("taskId: {}", taskId);
        waitForSnapshotServiceTask(snapshotServiceClient, taskId);
    }

    public static String createTask(
            SnapshotServiceClient snapshotServiceClient, String clusterId, String clusterPairId, String tagIds) {
        // Build snapshot policy
        SnapshotSchedule.Builder scheduleBldr = new SnapshotSchedule.Builder(TimeUnit.MINUTE, 30);
        RetentionPeriod.Builder retentionBldr = new RetentionPeriod.Builder(TimeUnit.HOUR, 6);
        SnapshotPolicy.Builder snapshotPolicyBldr =
                new SnapshotPolicy.Builder("policy1", scheduleBldr.build(), retentionBldr.build());

        // Build replication policy
        TimePeriod.Builder tpBldr = new TimePeriod.Builder(TimeUnit.MINUTE, 5);
        ShortTermRetention.Builder stBldr = new ShortTermRetention.Builder(5);
        RetentionPolicy.Builder rpBldr = new RetentionPolicy.Builder(stBldr.build());
        LongTermRetention.Builder longTermRetentionConfigBldr = new LongTermRetention.Builder();
        DailyRetention dailyRetention = new DailyRetention();
        dailyRetention.setRetention(new TimePeriod.Builder(TimeUnit.DAY, 1).build());
        rpBldr.setLongTerm(longTermRetentionConfigBldr.setDaily(dailyRetention).build());

        ReplicationPolicy.Builder replicationBldr = new ReplicationPolicy.Builder(tpBldr.build(), rpBldr.build());
        replicationBldr.setClusterPair(clusterPairId);

        // Build target entities with Tag Rules
        TargetEntities.Builder targetBldr = new TargetEntities.Builder();
        
        List<String> tagsList = Arrays.stream(tagIds.split(",")).collect(Collectors.toList());
        TagRule.Builder tagRuleBldr = new TagRule.Builder(tagsList, LogicalOperator.OR);
        
        targetBldr.setTagRules(List.of(tagRuleBldr.build()));

        // Build protection group spec
        ProtectionGroupSpec.Builder pgBldr = new ProtectionGroupSpec.Builder("pg-tag", targetBldr.build());
        pgBldr.setSnapshotPolicies(List.of(snapshotPolicyBldr.build()));
        pgBldr.setReplicationPolicies(List.of(replicationBldr.build()));

        ProtectionGroups protectionGroups = snapshotServiceClient.createStub(ProtectionGroups.class);
        log.info("Creating protection group with spec: {}", pgBldr.build());
        return protectionGroups.create_Task(clusterId, pgBldr.build());
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
                java.util.concurrent.TimeUnit.SECONDS.sleep(5);
            }
        }
    }
}
