/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.ha;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.management.ha.helpers.SpecHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.vcha.Cluster;
import com.vmware.vcenter.vcha.CredentialsSpec;
import com.vmware.vcenter.vcha.cluster.Active;
import com.vmware.vcenter.vcha.cluster.Mode;

/**
 * Demonstrates listing active node information, vCenter HA cluster information and vCenter HA cluster mode.
 *
 * <ul>
 *   <li>Step 1: List active node information
 *   <li>Step 2: List vCenter HA cluster information
 *   <li>Step 3: List vCenter HA cluster mode
 * </ul>
 *
 * <p>Sample Prerequisites: The sample requires a configured vCenter HA cluster
 */
public class VchaClient {
    private static final Logger log = LoggerFactory.getLogger(VchaClient.class);
    /** REQUIRED: vCenter FQDN or IP address. */
    public static String serverAddress = "vcenter1.mycompany.com";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";
    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (do not leave it empty on production environments).
     */
    public static String trustStorePath = null;

    /** REQUIRED: SSL Certificate of Management vCenter Server. */
    public static String vcSpecActiveLocationSSLCertificate = "vcSpecActiveLocationSSLCertificate";
    /** OPTIONAL: Hostname of the Management vCenter Server. Leave blank if it's a self-managed VC. */
    public static String vcSpecActiveLocationHostname = null;
    /** OPTIONAL: Username to login to the Management vCenter Server. Leave blank if it's a self-managed VC. */
    public static String vcSpecActiveLocationUsername = null;
    /** OPTIONAL: Password to login to the Management vCenter Server. Leave blank if it's a self-managed VC. */
    public static String vcSpecActiveLocationPassword = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VchaClient.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Cluster clusterService = client.createStub(Cluster.class);
            Active activeService = client.createStub(Active.class);
            Mode modeService = client.createStub(Mode.class);

            CredentialsSpec mgmtVcCredentialsSpec = SpecHelper.createCredentialsSpec(
                    vcSpecActiveLocationHostname,
                    vcSpecActiveLocationUsername,
                    vcSpecActiveLocationPassword,
                    vcSpecActiveLocationSSLCertificate);

            // List active node info, vCenter HA cluster info and cluster mode
            log.info("===== ACTIVE NODE INFO: {}\n", activeService.get(mgmtVcCredentialsSpec, false));
            log.info("===== CLUSTER INFO {}\n", clusterService.get(mgmtVcCredentialsSpec, false));
            log.info("===== CLUSTER MODE {}\n", modeService.get());
        }
    }
}
