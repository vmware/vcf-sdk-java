/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.nsx.client;

import static com.vmware.vapi.client.Configuration.HTTP_CONFIG_CFG;
import static com.vmware.vapi.client.Configuration.STUB_CONFIG_CFG;
import static com.vmware.vapi.internal.protocol.RestProtocol.REST_REQUEST_AUTHENTICATOR_CFG;

import java.time.Duration;

import com.vmware.vapi.bindings.StubConfiguration;
import com.vmware.vapi.client.ApiClient;
import com.vmware.vapi.client.ApiClients;
import com.vmware.vapi.client.Configuration;
import com.vmware.vapi.internal.protocol.client.rest.authn.BasicAuthenticationAppender;
import com.vmware.vapi.protocol.HttpConfiguration;
import com.vmware.vapi.security.UserPassSecurityContext;

public class ApiClientHelper {
    public static final int RESPONSE_TIMEOUT = (int) Duration.ofSeconds(180).toMillis();
    public static final int CONNECT_TIMEOUT = (int) Duration.ofSeconds(180).toMillis();
    public static final String BASE_URL = "https://%s:443";

    public static final HttpConfiguration.SslConfiguration sslConfiguration =
            new HttpConfiguration.SslConfiguration.Builder()
                    .disableCertificateValidation()
                    .disableHostnameVerification()
                    .getConfig();

    public static final HttpConfiguration httpConfiguration = new HttpConfiguration.Builder()
            .setSoTimeout(RESPONSE_TIMEOUT)
            .setConnectTimeout(CONNECT_TIMEOUT)
            .setSslConfiguration(sslConfiguration)
            .getConfig();

    public static ApiClient getBasicAuthApiClient(String host, String username, String password) {
        Configuration configuration = new Configuration.Builder()
                .register(HTTP_CONFIG_CFG, httpConfiguration)
                .register(REST_REQUEST_AUTHENTICATOR_CFG, new BasicAuthenticationAppender())
                .register(
                        STUB_CONFIG_CFG,
                        new StubConfiguration(new UserPassSecurityContext(username, password.toCharArray())))
                .build();
        return ApiClients.newRestClient(String.format(BASE_URL, host), configuration);
    }
}
