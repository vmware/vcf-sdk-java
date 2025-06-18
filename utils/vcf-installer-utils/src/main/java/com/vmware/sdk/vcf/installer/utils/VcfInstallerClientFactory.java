/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.vcf.installer.utils;

import static com.vmware.sdk.utils.ssl.vapi.HttpConfigHelper.DEFAULT_PORT;
import static com.vmware.sdk.utils.ssl.vapi.HttpConfigHelper.createDefaultHttpConfiguration;
import static com.vmware.vapi.client.Configuration.HTTP_CONFIG_CFG;
import static com.vmware.vapi.client.Configuration.STUB_CONFIG_CFG;
import static com.vmware.vapi.internal.protocol.RestProtocol.REST_REQUEST_AUTHENTICATOR_CFG;

import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.utils.ssl.vapi.HttpConfigHelper;
import com.vmware.sdk.vcf.installer.model.TokenCreationSpec;
import com.vmware.sdk.vcf.installer.model.TokenPair;
import com.vmware.sdk.vcf.installer.v1.Tokens;
import com.vmware.sdk.vcf.installer.v1.tokens.access_token.Refresh;
import com.vmware.vapi.bindings.Service;
import com.vmware.vapi.bindings.StubConfiguration;
import com.vmware.vapi.client.ApiClient;
import com.vmware.vapi.client.ApiClients;
import com.vmware.vapi.client.Configuration;
import com.vmware.vapi.internal.protocol.client.rest.authn.BasicAuthenticationAppender;
import com.vmware.vapi.protocol.HttpConfiguration;

/** A factory that produces {@link ApiClient} instances used for requests to a VCF Installer appliance. */
public class VcfInstallerClientFactory {

    private static final Logger log = LoggerFactory.getLogger(VcfInstallerClientFactory.class);

    /** Default username for VCF Installer. */
    private static final String DEFAULT_USERNAME = "admin@local";

    /**
     * Creates a new {@link ApiClient} which can be used to produce properly configured, authenticated, ready-to-use
     * {@link Service}s. Authenticates by with the {@value DEFAULT_USERNAME} as user. TCP port is set to
     * {@link HttpConfigHelper#DEFAULT_PORT}. Uses the default http configuration.
     *
     * @param hostAddress VCF Installer's FQDN or IP address.
     * @param password password for the {@value DEFAULT_USERNAME}.
     * @param keyStore trust store in case the client's certificate is not issued by a well-known CA
     * @return a client which can be used to create API stubs.
     * @see HttpConfigHelper#createDefaultHttpConfiguration(KeyStore)
     */
    public ApiClient createClient(String hostAddress, String password, KeyStore keyStore) {
        return createClient(hostAddress, DEFAULT_PORT, password, keyStore);
    }

    /**
     * Creates a new {@link ApiClient} which can be used to produce properly configured, authenticated, ready-to-use
     * {@link Service}s. Authenticates by with the {@value DEFAULT_USERNAME} as user. Uses the default http
     * configuration.
     *
     * @param hostAddress VCF Installer's FQDN or IP address.
     * @param port VCF Installer's TCP port for API requests.
     * @param password password for the {@value DEFAULT_USERNAME}.
     * @param keyStore trust store in case the client's certificate is not issued by a well-known CA
     * @return a client which can be used to create API stubs.
     * @see HttpConfigHelper#createDefaultHttpConfiguration(KeyStore)
     */
    public ApiClient createClient(String hostAddress, int port, String password, KeyStore keyStore) {
        return createClient(
                hostAddress,
                port,
                password,
                createDefaultHttpConfiguration(keyStore).getConfig());
    }

    /**
     * Creates a new {@link ApiClient} which can be used to produce properly configured, authenticated, ready-to-use
     * {@link Service}s. Authenticates by with the {@value DEFAULT_USERNAME} as user. TCP port is set to
     * {@link HttpConfigHelper#DEFAULT_PORT}. Uses the default http configuration.
     *
     * @param hostAddress VCF Installer's FQDN or IP address.
     * @param password password for the {@value DEFAULT_USERNAME}.
     * @param disableHostnameVerification indicates whether the client should skip hostname verification during the TLS
     *     handshake. If {@code null}, the behavior depends on whether a trust store is given (if the {@code keyStore}
     *     is empty, the connection will be untrusted and hostname verification will be disabled).
     * @param keyStore the trust store to use if the client's certificate is not issued by a well-known CA. If
     *     {@code null}, the default Java truststore is used. If empty, the connection is untrusted.
     * @return a client which can be used to create API stubs.
     * @see HttpConfigHelper#createDefaultHttpConfiguration(Boolean, KeyStore)
     */
    public ApiClient createClient(
            String hostAddress, String password, Boolean disableHostnameVerification, KeyStore keyStore) {
        return createClient(hostAddress, DEFAULT_PORT, password, disableHostnameVerification, keyStore);
    }

    /**
     * Creates a new {@link ApiClient} which can be used to produce properly configured, authenticated, ready-to-use
     * {@link Service}s. Authenticates by with the {@value DEFAULT_USERNAME} as user. Uses the default http
     * configuration.
     *
     * @param hostAddress VCF Installer's FQDN or IP address.
     * @param port VCF Installer's TCP port for API requests.
     * @param password password for the {@value DEFAULT_USERNAME}.
     * @param disableHostnameVerification should the client skip hostname verification when establishing the TLS
     *     connection
     * @param keyStore trust store in case the client's certificate is not issued by a well-known CA
     * @return a client which can be used to create API stubs.
     * @see HttpConfigHelper#createDefaultHttpConfiguration(Boolean, KeyStore)
     */
    public ApiClient createClient(
            String hostAddress, int port, String password, Boolean disableHostnameVerification, KeyStore keyStore) {
        // TODO: Add proxy capabilities.
        return createClient(
                hostAddress,
                port,
                password,
                createDefaultHttpConfiguration(disableHostnameVerification, keyStore)
                        .getConfig());
    }

    /**
     * Creates a new {@link ApiClient} which can be used to produce properly configured, authenticated, ready-to-use
     * {@link Service}s. Authenticates by with the {@value DEFAULT_USERNAME} as user. TCP port is set to
     * {@link HttpConfigHelper#DEFAULT_PORT}.
     *
     * @param hostAddress VCF Installer's FQDN or IP address.
     * @param password password for the {@value DEFAULT_USERNAME}.
     * @param httpConfig configuration for vApi stubs.
     * @return a client which can be used to create API stubs.
     */
    public ApiClient createClient(String hostAddress, String password, HttpConfiguration httpConfig) {
        return createClient(hostAddress, DEFAULT_PORT, password, httpConfig);
    }

    /**
     * Creates a new {@link ApiClient} which can be used to produce properly configured, authenticated, ready-to-use
     * {@link Service}s. Authenticates by with the {@value DEFAULT_USERNAME} as user.
     *
     * @param hostAddress VCF Installer's FQDN or IP address.
     * @param port VCF Installer's TCP port for API requests.
     * @param password password for the {@value DEFAULT_USERNAME}.
     * @param httpConfig configuration for vApi stubs.
     * @return a client which can be used to create API stubs.
     */
    public ApiClient createClient(String hostAddress, int port, String password, HttpConfiguration httpConfig) {
        return createClient(hostAddress, port, DEFAULT_USERNAME, password, httpConfig);
    }

    /**
     * Creates a new {@link ApiClient} which can be used to produce properly configured, authenticated, ready-to-use
     * {@link Service}s.
     *
     * @param hostAddress VCF Installer's FQDN or IP address.
     * @param port VCF Installer's TCP port for API requests.
     * @param username VCF Installer username.
     * @param password password for the username.
     * @param httpConfig configuration for vApi stubs.
     * @return a client which can be used to create API stubs.
     */
    public ApiClient createClient(
            String hostAddress, int port, String username, String password, HttpConfiguration httpConfig) {
        TokenPair tokenPair;
        try {
            tokenPair = getTokenPair(hostAddress, port, username, password, httpConfig);
        } catch (Exception e) {
            log.error("Could not acquire token pair from VCF Installer", e);
            throw new RuntimeException(e);
        }

        var supplier = createTokenSupplier(hostAddress, port, httpConfig, tokenPair);

        char[] accessToken = tokenPair.getAccessToken().toCharArray();
        Configuration configuration = new Configuration.Builder()
                .registerHttpConfiguration(httpConfig)
                .configureBearerAuthentication(accessToken, supplier)
                .build();

        return ApiClients.newRestClient(createUrl(hostAddress, port), configuration);
    }

    private static Supplier<CompletionStage<char[]>> createTokenSupplier(
            String hostAddress, int port, HttpConfiguration httpConfig, TokenPair tokenPair) {
        var client = getBasicAuthApiClient(hostAddress, port, httpConfig);

        var refreshToken = tokenPair.getRefreshToken().getId();
        var refresh = client.createStub(Refresh.class);
        return () -> refresh.refreshAccessToken(refreshToken).invoke().thenApply(String::toCharArray);
    }

    /**
     * Creates a basic client used for authentication in {@link #getTokenPair(String, int, String, String,
     * HttpConfiguration)}.
     *
     * @param hostAddress VCF Installer's FQDN or IP address.
     * @param port VCF Installer's TCP port for API requests.
     * @param httpConfig configuration for vApi stubs.
     */
    private static ApiClient getBasicAuthApiClient(String hostAddress, int port, HttpConfiguration httpConfig) {
        Configuration configuration = new Configuration.Builder()
                .register(HTTP_CONFIG_CFG, httpConfig)
                .register(STUB_CONFIG_CFG, new StubConfiguration())
                .register(REST_REQUEST_AUTHENTICATOR_CFG, new BasicAuthenticationAppender())
                .build();

        return ApiClients.newRestClient(createUrl(hostAddress, port), configuration);
    }

    /**
     * Provides a {@link TokenPair} after authentication to VCF Installer with username and password.
     *
     * @param hostAddress VCF Installer's FQDN or IP address.
     * @param port VCF Installer's TCP port for API requests.
     * @param username VCF Installer username.
     * @param password password for the username.
     * @param httpConfig configuration for vApi stubs.
     * @return Pair of access and refresh tokens.
     */
    private static TokenPair getTokenPair(
            String hostAddress, int port, String username, String password, HttpConfiguration httpConfig)
            throws Exception {
        try (ApiClient basicAuthClient = getBasicAuthApiClient(hostAddress, port, httpConfig)) {
            Tokens tokens = basicAuthClient.createStub(Tokens.class);
            TokenCreationSpec tokenCreationSpec = new TokenCreationSpec.Builder()
                    .setUsername(username)
                    .setPassword(password)
                    .build();
            return tokens.createToken(tokenCreationSpec).invoke().get();
        }
    }

    /**
     * Constructs a URL that is set to the {@link ApiClient}.
     *
     * @param hostAddress VCF Installer's FQDN or IP address.
     * @param port VCF Installer's TCP port for API requests.
     */
    private static String createUrl(String hostAddress, int port) {
        try {
            return new URIBuilder()
                    .setScheme("https")
                    .setHost(hostAddress)
                    .setPort(port)
                    .build()
                    .toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
