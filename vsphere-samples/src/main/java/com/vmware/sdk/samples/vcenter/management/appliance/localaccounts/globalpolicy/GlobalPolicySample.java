/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.appliance.localaccounts.globalpolicy;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static java.util.Objects.requireNonNullElse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appliance.local_accounts.Policy;
import com.vmware.appliance.local_accounts.PolicyTypes;
import com.vmware.appliance.local_accounts.PolicyTypes.Info;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;

/** Demonstrates set and get Global policy values. */
public class GlobalPolicySample {
    private static final Logger log = LoggerFactory.getLogger(GlobalPolicySample.class);
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

    /** OPTIONAL: Max days to be set to localaccounts globalpolicy. */
    public static Long maxDays = null;
    /** OPTIONAL: Min days to be set to localaccounts globalpolicy. */
    public static Long minDays = null;
    /** OPTIONAL: Warn days to be set to localaccounts globalpolicy. */
    public static Long warnDays = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(GlobalPolicySample.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Policy localAccountsPolicy = client.createStub(Policy.class);

            Info policyParamInfo = new Info();
            policyParamInfo.setMaxDays(requireNonNullElse(maxDays, 0L));
            policyParamInfo.setMinDays(requireNonNullElse(minDays, 0L));
            policyParamInfo.setWarnDays(requireNonNullElse(warnDays, 0L));

            log.info(
                    "Setting values now as per passed parameters maxDays:{}, minDays:{}, warnDays:{}",
                    maxDays,
                    minDays,
                    warnDays);
            localAccountsPolicy.set(policyParamInfo);

            PolicyTypes.Info localAccountsPolicyInfo = localAccountsPolicy.get();
            log.info("Values which are set are displayed below after get call:");
            log.info("Maximum number of days between password change:{}", localAccountsPolicyInfo.getMaxDays());
            log.info("Minimum number of days between password change:{}", localAccountsPolicyInfo.getMinDays());
            log.info("Number of days of warning before password expires:{}", localAccountsPolicyInfo.getWarnDays());
        }
    }
}
