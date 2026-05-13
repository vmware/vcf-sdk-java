/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.ops.api.client.Client;
import com.vmware.ops.api.model.auth.UsernamePassword;
import com.vmware.sdk.samples.ops.config.client.BaseClientConfig;
import com.vmware.sdk.samples.ops.helpers.ClientUtils;
import com.vmware.sdk.samples.ops.helpers.ConfigValidator;

/**
 * Base client example which uses a JKS truststore while building a client.
 */
public abstract class SampleBaseWithTrustStore {
    protected static final ObjectMapper mapper = new ObjectMapper();
    protected final BaseClientConfig baseClientConfig;
    private final Client client;

    public SampleBaseWithTrustStore(String clientConfigFile) throws IOException {
        baseClientConfig = mapper.readValue(new File(clientConfigFile), BaseClientConfig.class);
        ConfigValidator.validate(baseClientConfig);

        String truststorePath = baseClientConfig.getTruststorePath();
        File testTrustStore = new File(truststorePath);
        if (!testTrustStore.exists()) {
            throw new FileNotFoundException("Truststore file not found: " + truststorePath);
        }
        this.client = Client.ClientConfig.builder()
                .tokenAuth(() -> new UsernamePassword(baseClientConfig.getUsername(), baseClientConfig.getPassword()))
                .useJson()
                .serverUrl(ClientUtils.constructServerURL(baseClientConfig.getHost()))
                .verify(baseClientConfig.getVerify())
                .ignoreHostName(baseClientConfig.isIgnoreHostName())
                .withTrustStore(truststorePath)
                .withTrustStorePassword(baseClientConfig.getTrustStorePassword())
                .withTrustStoreType(baseClientConfig.getTruststoreType())
                .build()
                .newClient();
    }

    protected Client getClient() {
        return client;
    }

    public abstract void run();
}
