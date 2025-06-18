/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vsan.dp.clusters.protection_groups.pg_snapshots;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.createTrustManagerFromFile;
import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.sdk.utils.ssl.vapi.HttpConfigHelper.createDefaultHttpConfiguration;

import java.security.KeyStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;
import com.vmware.sdk.vsphere.utils.vsan.dp.SnapshotServiceClient;
import com.vmware.snapservice.clusters.protection_groups.Snapshots;
import com.vmware.snapservice.clusters.protection_groups.SnapshotsTypes;
import com.vmware.vapi.protocol.HttpConfiguration;

/**
 * Demonstrates querying protection group snapshots.
 *
 * <p>Sample Prerequisites: vCenter 8.0.3+
 */
public class QueryPGSnapshots {
    private static final Logger log = LoggerFactory.getLogger(QueryPGSnapshots.class);

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

    /** REQUIRED: Cluster MoRef ID where the snapshots locate. */
    public static String clusterId = "domain-c1";
    /** REQUIRED: Protection group ID where the snapshots locate. */
    public static String pgId = "pg-1";

    public static void main(String[] args) {
        SampleCommandLineParser.load(QueryPGSnapshots.class, args);

        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);
        SimpleHttpConfigurer portConfigurer = new SimpleHttpConfigurer(createTrustManagerFromFile(trustStorePath));
        HttpConfiguration httpConfiguration =
                createDefaultHttpConfiguration(keyStore).getConfig();

        SnapshotServiceClient snapshotServiceClient = new SnapshotServiceClient(
                snapServiceAddress, httpConfiguration, serverAddress, portConfigurer, username, password);

        SnapshotsTypes.ListResult listResult =
                list(snapshotServiceClient, clusterId, pgId, new SnapshotsTypes.FilterSpec());
        log.info(
                "PG snapshot item size: {}, items: {}",
                listResult.getSnapshots().size(),
                listResult.getSnapshots());
    }

    public static SnapshotsTypes.ListResult list(
            SnapshotServiceClient snapshotServiceClient,
            String clusterId,
            String pgId,
            SnapshotsTypes.FilterSpec filter) {
        Snapshots snapshots = snapshotServiceClient.createStub(Snapshots.class);
        return snapshots.list(clusterId, pgId, filter);
    }
}
