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
import com.vmware.vcenter.certificate_management.vcenter.SigningCertificate;
import com.vmware.vcenter.certificate_management.vcenter.SigningCertificateTypes;

/**
 * Sample code to get the Signing Certificate for the vCenter Server. This will enable users to view the certificate
 * actively used to sign tokens and certificates used for token signature verification.
 */
public class GetSigningCertificate {
    private static final Logger log = LoggerFactory.getLogger(GetSigningCertificate.class);
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

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(GetSigningCertificate.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            SigningCertificate certService = client.createStub(SigningCertificate.class);

            SigningCertificateTypes.Info certInfo = certService.get();
            if (certInfo == null) {
                log.error("Signing certificates not found on this vCenter");
            }
            log.info("vCenter signing certificate \n{}", certInfo);
        }
    }
}
