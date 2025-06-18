/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vsan.dp.clusters.virtual_machines;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.createTrustManagerFromFile;
import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.sdk.utils.ssl.vapi.HttpConfigHelper.createDefaultHttpConfiguration;

import java.security.KeyStore;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;
import com.vmware.sdk.vsphere.utils.vsan.dp.SnapshotServiceClient;
import com.vmware.snapservice.clusters.VirtualMachines;
import com.vmware.snapservice.clusters.VirtualMachinesTypes;
import com.vmware.vapi.protocol.HttpConfiguration;

/**
 * Demonstrates querying virtual machines.
 *
 * <p>Sample Prerequisites: vCenter 8.0.3+
 */
public class QueryVirtualMachines {
    private static final Logger log = LoggerFactory.getLogger(QueryVirtualMachines.class);

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

    /** REQUIRED: Cluster MoRef ID where the virtual machines locate. */
    public static String clusterId = "domain-c1";
    /** OPTIONAL: Virtual machine IDs to query. */
    public static String vmIds = "vm-1";

    public static void main(String[] args) {
        SampleCommandLineParser.load(QueryVirtualMachines.class, args);

        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);
        SimpleHttpConfigurer portConfigurer = new SimpleHttpConfigurer(createTrustManagerFromFile(trustStorePath));
        HttpConfiguration httpConfiguration =
                createDefaultHttpConfiguration(keyStore).getConfig();

        SnapshotServiceClient snapshotServiceClient = new SnapshotServiceClient(
                snapServiceAddress, httpConfiguration, serverAddress, portConfigurer, username, password);

        VirtualMachinesTypes.FilterSpec filter = new VirtualMachinesTypes.FilterSpec();
        filter.setVms(Arrays.stream(vmIds.split(",")).collect(Collectors.toSet()));
        VirtualMachinesTypes.ListResult listResult = list(snapshotServiceClient, clusterId, filter);
        log.info(
                "Virtual machine item size: {}, items: {}",
                listResult.getItems().size(),
                listResult.getItems());
    }

    public static VirtualMachinesTypes.ListResult list(
            SnapshotServiceClient snapshotServiceClient, String clusterId, VirtualMachinesTypes.FilterSpec filter) {
        VirtualMachines virtualMachines = snapshotServiceClient.createStub(VirtualMachines.class);
        return virtualMachines.list(clusterId, filter);
    }
}
