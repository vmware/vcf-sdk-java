/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vsan.dp.cluster_pairs;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.createTrustManagerFromFile;
import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.sdk.utils.ssl.vapi.HttpConfigHelper.createDefaultHttpConfiguration;

import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;
import com.vmware.sdk.vsphere.utils.vsan.dp.SnapshotServiceClient;
import com.vmware.snapservice.ClusterPairs;
import com.vmware.snapservice.ClusterPairsTypes;
import com.vmware.snapservice.Tasks;
import com.vmware.snapservice.tasks.Info;
import com.vmware.snapservice.tasks.Status;
import com.vmware.vapi.protocol.HttpConfiguration;

/**
 * Demonstrates prechecking a cluster pair.
 *
 * <p>Sample Prerequisites: vCenter 8.0.3+
 */
public class CreatePrecheckClusterPairs {
    private static final Logger log = LoggerFactory.getLogger(CreatePrecheckClusterPairs.class);

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

    /** REQUIRED: The local cluster ID. */
    public static String localClusterId = "local-cluster-id";
    /** REQUIRED: The peer cluster ID. */
    public static String peerSiteId = "peer-site-id";
    /** REQUIRED: The peer cluster ID. */
    public static String peerClusterId = "peer-cluster-id";

    public static void main(String[] args) throws InterruptedException {
        SampleCommandLineParser.load(CreatePrecheckClusterPairs.class, args);

        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);
        SimpleHttpConfigurer portConfigurer = new SimpleHttpConfigurer(createTrustManagerFromFile(trustStorePath));
        HttpConfiguration httpConfiguration =
                createDefaultHttpConfiguration(keyStore).getConfig();

        SnapshotServiceClient snapshotServiceClient = new SnapshotServiceClient(
                snapServiceAddress, httpConfiguration, serverAddress, portConfigurer, username, password);

        String taskId = createPrecheckTask(snapshotServiceClient, localClusterId, peerSiteId, peerClusterId);
        log.info("taskId: {}", taskId);

        waitForSnapshotServiceTask(snapshotServiceClient, taskId);
    }

    public static String createPrecheckTask(
            SnapshotServiceClient snapshotServiceClient,
            String localClusterId,
            String peerSiteId,
            String peerClusterId) {
        ClusterPairs clusterPairs = snapshotServiceClient.createStub(ClusterPairs.class);

        ClusterPairsTypes.CreateSpec spec = new ClusterPairsTypes.CreateSpec();

        ClusterPairsTypes.LocalClusterMemberSpec localClusterMemberSpec =
                new ClusterPairsTypes.LocalClusterMemberSpec();
        localClusterMemberSpec.setCluster(localClusterId);
        spec.setLocalCluster(localClusterMemberSpec);

        ClusterPairsTypes.PeerClusterMemberSpec peerClusterMemberSpec = new ClusterPairsTypes.PeerClusterMemberSpec();
        peerClusterMemberSpec.setSite(peerSiteId);
        peerClusterMemberSpec.setCluster(peerClusterId);
        spec.setPeerCluster(peerClusterMemberSpec);

        return clusterPairs.createPrecheck_Task(spec);
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
