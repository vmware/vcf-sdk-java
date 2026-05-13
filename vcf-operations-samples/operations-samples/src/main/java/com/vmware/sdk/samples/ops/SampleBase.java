/*
 * ******************************************************************
 * Copyright (c) 2025-2026 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.ops.api.client.Client;
import com.vmware.ops.api.client.Client.ClientConfig;
import com.vmware.ops.api.model.auth.UsernamePassword;
import com.vmware.sdk.samples.ops.config.client.BaseClientConfig;
import com.vmware.sdk.samples.ops.helpers.ClientUtils;
import com.vmware.sdk.samples.ops.helpers.ConfigValidator;

/**
 * A base example that allows for code reuse amongst all the Example classes.
 */
public abstract class SampleBase {
    protected static final ObjectMapper mapper = new ObjectMapper();
    protected final BaseClientConfig baseClientConfig;

    private final Client client;

    public SampleBase(String clientConfigFile) throws IOException {
        baseClientConfig = mapper.readValue(new File(clientConfigFile), BaseClientConfig.class);
        ConfigValidator.validate(baseClientConfig);

        this.client = ClientConfig.builder()
                .tokenAuth(() -> new UsernamePassword(baseClientConfig.getUsername(), baseClientConfig.getPassword()))
                .useJson()
                .locale(baseClientConfig.getLocale())
                .timezone(baseClientConfig.getTimezone())
                .serverUrl(ClientUtils.constructServerURL(baseClientConfig.getHost()))
                .verify(baseClientConfig.getVerify())
                .ignoreHostName(baseClientConfig.isIgnoreHostName())
                .useInternalApis(baseClientConfig.isUseInternalApis())
                .build()
                .newClient();
    }

    public Client getClient() {
        return client;
    }

    public abstract void run();
}
