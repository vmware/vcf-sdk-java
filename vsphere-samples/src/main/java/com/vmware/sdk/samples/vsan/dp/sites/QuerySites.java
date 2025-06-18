/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vsan.dp.sites;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.createTrustManagerFromFile;
import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.sdk.utils.ssl.vapi.HttpConfigHelper.createDefaultHttpConfiguration;

import java.security.KeyStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;
import com.vmware.sdk.vsphere.utils.vsan.dp.SnapshotServiceClient;
import com.vmware.snapservice.Sites;
import com.vmware.snapservice.SitesTypes;
import com.vmware.vapi.protocol.HttpConfiguration;

/**
 * Demonstrates querying sites.
 *
 * <p>Sample Prerequisites: vCenter 8.0.3+
 */
public class QuerySites {
    private static final Logger log = LoggerFactory.getLogger(QuerySites.class);

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

    /** OPTIONAL: The site ID to query. */
    public static String siteId;

    public static void main(String[] args) {
        SampleCommandLineParser.load(QuerySites.class, args);

        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);
        SimpleHttpConfigurer portConfigurer = new SimpleHttpConfigurer(createTrustManagerFromFile(trustStorePath));
        HttpConfiguration httpConfiguration =
                createDefaultHttpConfiguration(keyStore).getConfig();

        SnapshotServiceClient snapshotServiceClient = new SnapshotServiceClient(
                snapServiceAddress, httpConfiguration, serverAddress, portConfigurer, username, password);

        if (siteId == null || siteId.isEmpty()) {
            SitesTypes.ListResult listResult = list(snapshotServiceClient);
            log.info("Got {} items for list: {}", listResult.getItems().size(), listResult);
        } else {
            SitesTypes.Info siteInfo = get(snapshotServiceClient, siteId);
            log.info("Get site info for siteId {}: {}", siteId, siteInfo);
        }
    }

    public static SitesTypes.ListResult list(SnapshotServiceClient snapshotServiceClient) {
        Sites sites = snapshotServiceClient.createStub(Sites.class);

        return sites.list(null);
    }

    public static SitesTypes.Info get(SnapshotServiceClient snapshotServiceClient, String siteId) {
        Sites sites = snapshotServiceClient.createStub(Sites.class);

        return sites.get(siteId);
    }
}
