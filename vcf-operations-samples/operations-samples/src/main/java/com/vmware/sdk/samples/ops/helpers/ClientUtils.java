/*
 * ******************************************************************
 * Copyright (c) 2025-2026 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.helpers;

import java.util.UUID;

import com.vmware.sdk.samples.ops.config.credentials.CredentialConfig;

public class ClientUtils {

    private static final String API_PREFIX = "suite-api";

    public static String constructServerURL(String host) {
        return String.format("https://%s/%s", host, API_PREFIX);
    }

    public static void populateCredentialName(CredentialConfig credentialConfig) {
        if (credentialConfig.getName() == null || credentialConfig.getName().isBlank()) {
            // If the user does not provide a name, then create the credential
            // with a randomly generated name each time
            credentialConfig.setName("credential_" + UUID.randomUUID());
        }
    }
}
