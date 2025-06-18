/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.certificates;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.certificate_management.X509CertChain;
import com.vmware.vcenter.certificate_management.vcenter.SigningCertificate;

/**
 * Sample code to refresh the Signing Certificate for the vCenter Server. Use the force option to attempt to force the
 * refresh in environments that would otherwise fail. On success, the new signing certificates will be printed.
 */
public class RefreshSigningCertificate {
    private static final Logger log = LoggerFactory.getLogger(RefreshSigningCertificate.class);
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

    /** OPTIONAL: Attempt to force refresh. Default value is false. */
    public static Boolean force = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(RefreshSigningCertificate.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            SigningCertificate certService = client.createStub(SigningCertificate.class);

            X509CertChain newCert = certService.refresh(Boolean.TRUE.equals(force));
            log.info("New vCenter signing certificate \n{}", newCert);
        }
    }
}
