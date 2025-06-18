/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.sddcm.client;

import static com.vmware.vapi.client.Configuration.HTTP_CONFIG_CFG;
import static com.vmware.vapi.client.Configuration.STUB_CONFIG_CFG;
import static com.vmware.vapi.internal.protocol.RestProtocol.REST_REQUEST_AUTHENTICATOR_CFG;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import com.vmware.sdk.sddcm.model.TokenCreationSpec;
import com.vmware.sdk.sddcm.model.TokenPair;
import com.vmware.sdk.sddcm.v1.Tokens;
import com.vmware.vapi.bindings.StubConfiguration;
import com.vmware.vapi.client.ApiClient;
import com.vmware.vapi.client.ApiClients;
import com.vmware.vapi.client.Configuration;
import com.vmware.vapi.internal.protocol.client.rest.authn.BasicAuthenticationAppender;
import com.vmware.vapi.internal.protocol.client.rest.authn.OauthAuthenticationAppender;
import com.vmware.vapi.protocol.HttpConfiguration;
import com.vmware.vapi.security.OAuthSecurityContext;
import com.vmware.vapi.security.UserPassSecurityContext;

/**
 * This class provides utility methods for working with API clients.
 *
 * <p>It includes methods for creating API client instances and other common API client tasks.
 */
public class ApiClientUtil {
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

    /**
     * Gets the basic authentication API client instance.
     *
     * @param host server IP Address/FQDN
     * @return the OAuth API client instance
     */
    public static ApiClient getBasicAuthApiClient(String host) {
        Configuration configuration = new Configuration.Builder()
                .register(HTTP_CONFIG_CFG, httpConfiguration)
                .register(STUB_CONFIG_CFG, new StubConfiguration())
                .register(REST_REQUEST_AUTHENTICATOR_CFG, new BasicAuthenticationAppender())
                .build();
        return ApiClients.newRestClient(String.format(BASE_URL, host), configuration);
    }

    /**
     * Gets the OAuth API client instance.
     *
     * @param host server IP Address/FQDN
     * @param sddcStubConfiguration used to provide security contexts (e.g., username/password, OAuth tokens) for
     *     authentication and authorization.
     * @return ApiClient handler required by the client for invoking the API
     */
    public static ApiClient createClient(String host, StubConfiguration sddcStubConfiguration) {
        Configuration configuration = new Configuration.Builder()
                .register(HTTP_CONFIG_CFG, httpConfiguration)
                .register(STUB_CONFIG_CFG, sddcStubConfiguration)
                .register(REST_REQUEST_AUTHENTICATOR_CFG, new OauthAuthenticationAppender())
                .build();
        return ApiClients.newRestClient(String.format(BASE_URL, host), configuration);
    }

    /**
     * Gets the OAuth API client instance for cloud builder instance.
     *
     * @param host server IP Address/FQDN
     * @param username user name of the server
     * @param password password of the server
     * @return ApiClient handler required by the client for invoking the API
     */
    public static ApiClient getCloudBuilderApiClient(String host, String username, String password) {
        Configuration configuration = new Configuration.Builder()
                .register(HTTP_CONFIG_CFG, httpConfiguration)
                .register(STUB_CONFIG_CFG, getCloudBuilderStubConfiguration(username, password))
                .register(REST_REQUEST_AUTHENTICATOR_CFG, new BasicAuthenticationAppender())
                .build();
        return ApiClients.newRestClient(String.format(BASE_URL, host), configuration);
    }

    /**
     * Gets the Cloud Builder stub configuration.
     *
     * @param username user name of the server
     * @param password password of the server
     * @return StubConfiguration used to provide security contexts (e.g., username/password, OAuth tokens) for
     *     authentication and authorization.
     */
    public static StubConfiguration getCloudBuilderStubConfiguration(String username, String password) {
        return new StubConfiguration(new UserPassSecurityContext(username, password.toCharArray()));
    }

    /**
     * Gets the SDDC stub configuration.
     *
     * @param host server IP Address/FQDN
     * @param username yser name of the server
     * @param password password of the server
     * @return StubConfiguration used to provide security contexts (e.g., username/password, OAuth tokens) for
     *     authentication and authorization.
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the client is interrupted while waiting
     */
    public static StubConfiguration getSddcStubConfiguration(String host, String username, String password)
            throws ExecutionException, InterruptedException {
        return new StubConfiguration(new OAuthSecurityContext(
                getTokenPair(host, username, password).getAccessToken().toCharArray()));
    }

    /**
     * Gets a token pair for provided credentials for authorization and authentication.
     *
     * @param host server IP Address/FQDN
     * @param username yser name of the server
     * @param password password of the server
     * @return TokenPair to manage authentication and authorization for API calls
     * @throws ExecutionException if the API call fails
     * @throws InterruptedException if the client is interrupted while waiting
     */
    public static TokenPair getTokenPair(String host, String username, String password)
            throws ExecutionException, InterruptedException {
        Tokens tokens = getBasicAuthApiClient(host).createStub(Tokens.class);
        TokenCreationSpec tokenCreationSpec = new TokenCreationSpec.Builder()
                .setUsername(username)
                .setPassword(password)
                .build();
        return tokens.createToken(tokenCreationSpec).invoke().get();
    }
}
