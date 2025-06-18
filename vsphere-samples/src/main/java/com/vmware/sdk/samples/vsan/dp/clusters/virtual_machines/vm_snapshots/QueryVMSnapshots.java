/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vsan.dp.clusters.virtual_machines.vm_snapshots;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.createTrustManagerFromFile;
import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.sdk.utils.ssl.vapi.HttpConfigHelper.createDefaultHttpConfiguration;

import java.security.KeyStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;
import com.vmware.sdk.vsphere.utils.vsan.dp.SnapshotServiceClient;
import com.vmware.snapservice.clusters.virtual_machines.Snapshots;
import com.vmware.snapservice.clusters.virtual_machines.SnapshotsTypes;
import com.vmware.vapi.protocol.HttpConfiguration;

/**
 * Demonstrates querying virtual machine snapshots.
 *
 * <p>Sample Prerequisites: vCenter 8.0.3+
 */
public class QueryVMSnapshots {
    private static final Logger log = LoggerFactory.getLogger(QueryVMSnapshots.class);

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
    /** REQUIRED: Virtual machine MoRef ID where the snapshots locate. */
    public static String vmId = "vm-1";

    public static void main(String[] args) {
        SampleCommandLineParser.load(QueryVMSnapshots.class, args);

        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);
        SimpleHttpConfigurer portConfigurer = new SimpleHttpConfigurer(createTrustManagerFromFile(trustStorePath));
        HttpConfiguration httpConfiguration =
                createDefaultHttpConfiguration(keyStore).getConfig();

        SnapshotServiceClient snapshotServiceClient = new SnapshotServiceClient(
                snapServiceAddress, httpConfiguration, serverAddress, portConfigurer, username, password);

        SnapshotsTypes.ListResult listResult = list(snapshotServiceClient, clusterId, vmId, null);
        log.info("Vm snapshots item size: {}, items: {}", listResult.getItems().size(), listResult.getItems());
    }

    public static SnapshotsTypes.ListResult list(
            SnapshotServiceClient snapshotServiceClient,
            String clusterId,
            String vmId,
            SnapshotsTypes.FilterSpec filter) {
        Snapshots snapshots = snapshotServiceClient.createStub(Snapshots.class);
        return snapshots.list(clusterId, vmId, filter);
    }
}
