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
 * Base client example which uses a certificate with a .PEM extension while building a client.
 */
public abstract class SampleBaseWithCertificate {

    protected static final ObjectMapper mapper = new ObjectMapper();
    protected final BaseClientConfig baseClientConfig;
    private final Client client;

    public SampleBaseWithCertificate(String clientConfigFile) throws IOException {
        baseClientConfig = mapper.readValue(new File(clientConfigFile), BaseClientConfig.class);
        ConfigValidator.validate(baseClientConfig);

        String certificatePath = baseClientConfig.getCertificatePath();
        File certificateFile = new File(certificatePath);
        if (!certificateFile.exists()) {
            throw new FileNotFoundException("Certificate file not found: " + certificatePath);
        }
        this.client = Client.ClientConfig.builder()
                .tokenAuth(() -> new UsernamePassword(baseClientConfig.getUsername(), baseClientConfig.getPassword()))
                .useJson()
                .serverUrl(ClientUtils.constructServerURL(baseClientConfig.getHost()))
                .verify(baseClientConfig.getVerify())
                .ignoreHostName(baseClientConfig.isIgnoreHostName())
                .withCertificates(certificatePath)
                .build()
                .newClient();
    }

    protected Client getClient() {
        return client;
    }

    public abstract void run();
}
