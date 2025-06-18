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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;
import com.vmware.sdk.vsphere.utils.vsan.dp.SnapshotServiceClient;
import com.vmware.snapservice.ClusterPairs;
import com.vmware.snapservice.ClusterPairsTypes;
import com.vmware.vapi.protocol.HttpConfiguration;

/**
 * Demonstrates getting a cluster pair.
 *
 * <p>Sample Prerequisites: vCenter 8.0.3+
 */
public class GetClusterPairs {
    private static final Logger log = LoggerFactory.getLogger(GetClusterPairs.class);

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

    /** REQUIRED: The cluster pairs ID to query. */
    public static String clusterPairId = "cluster-pair-id";

    public static void main(String[] args) {
        SampleCommandLineParser.load(GetClusterPairs.class, args);

        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);
        SimpleHttpConfigurer portConfigurer = new SimpleHttpConfigurer(createTrustManagerFromFile(trustStorePath));
        HttpConfiguration httpConfiguration =
                createDefaultHttpConfiguration(keyStore).getConfig();

        SnapshotServiceClient snapshotServiceClient = new SnapshotServiceClient(
                snapServiceAddress, httpConfiguration, serverAddress, portConfigurer, username, password);

        log.info("Get cluster pairs for id: {}", clusterPairId);
        ClusterPairsTypes.Info info = get(snapshotServiceClient, clusterPairId);
        log.info("Got cluster pairs info: {}", info);
    }

    public static ClusterPairsTypes.Info get(SnapshotServiceClient snapshotServiceClient, String clusterPairId) {
        ClusterPairs clusterPairs = snapshotServiceClient.createStub(ClusterPairs.class);
        return clusterPairs.get(clusterPairId);
    }
}
