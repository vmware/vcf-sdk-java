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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;
import com.vmware.sdk.vsphere.utils.vsan.dp.SnapshotServiceClient;
import com.vmware.snapservice.ProtectionGroupInfo;
import com.vmware.snapservice.clusters.ProtectionGroups;
import com.vmware.snapservice.clusters.ProtectionGroupsTypes;
import com.vmware.vapi.protocol.HttpConfiguration;

/**
 * Demonstrates querying a protection group.
 *
 * <p>Sample Prerequisites: vCenter 8.0.3+
 */
public class QueryProtectionGroups {
    private static final Logger log = LoggerFactory.getLogger(QueryProtectionGroups.class);

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
    /** OPTIONAL: Protection group ID to perform action on. */
    public static String pgId;

    public static void main(String[] args) {
        SampleCommandLineParser.load(QueryProtectionGroups.class, args);

        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);
        SimpleHttpConfigurer portConfigurer = new SimpleHttpConfigurer(createTrustManagerFromFile(trustStorePath));
        HttpConfiguration httpConfiguration =
                createDefaultHttpConfiguration(keyStore).getConfig();

        SnapshotServiceClient snapshotServiceClient = new SnapshotServiceClient(
                snapServiceAddress, httpConfiguration, serverAddress, portConfigurer, username, password);

        if (pgId == null) {
            ProtectionGroupInfo info = get(snapshotServiceClient, clusterId, pgId);
            log.info("Get protection group for id {}: {}", pgId, info);
        } else {
            log.info("List of protection groups:");
            ProtectionGroupsTypes.ListResult listResult = list(snapshotServiceClient, clusterId);
            log.info("Task item size: {}, items: {}", listResult.getItems().size(), listResult.getItems());
        }
    }

    public static ProtectionGroupsTypes.ListResult list(SnapshotServiceClient snapshotServiceClient, String clusterId) {
        ProtectionGroups protectionGroups = snapshotServiceClient.createStub(ProtectionGroups.class);
        return protectionGroups.list(clusterId, null);
    }

    public static ProtectionGroupInfo get(SnapshotServiceClient snapshotServiceClient, String clusterId, String pgId) {
        ProtectionGroups protectionGroups = snapshotServiceClient.createStub(ProtectionGroups.class);
        return protectionGroups.get(clusterId, pgId);
    }
}
