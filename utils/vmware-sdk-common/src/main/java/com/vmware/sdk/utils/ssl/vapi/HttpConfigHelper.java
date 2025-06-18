/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.utils.ssl.vapi;

import java.security.KeyStore;
import java.security.KeyStoreException;

import com.vmware.vapi.protocol.HttpConfiguration;

/** This class provides code that makes it easier to create http configurations for SDK clients. */
public class HttpConfigHelper {

    /** Default value for a client's connect timeout if it's not explicitly specified. */
    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 30_000;

    /** Default value for a client's read timeout if it's not explicitly specified. */
    public static final int DEFAULT_READ_TIMEOUT_MS = 60_000;

    /** The default port typically SOAP services can be found. */
    public static final int DEFAULT_PORT = 443;

    /**
     * Creates default http configuration used in SDK clients with hostname verification,
     * {@value DEFAULT_CONNECT_TIMEOUT_MS} for the client's connect timeout, {@value DEFAULT_READ_TIMEOUT_MS} for the
     * client's read timeout and {@value DEFAULT_PORT} for the client's port.
     *
     * @param keyStore trust store in case the client's certificate is not issued by a well-known CA
     * @return Builder for the http configuration.
     * @see HttpConfiguration.Builder
     */
    public static HttpConfiguration.Builder createDefaultHttpConfiguration(KeyStore keyStore) {
        return createDefaultHttpConfiguration(null, keyStore);
    }

    /**
     * Creates default http configuration used in SDK clients with {@value DEFAULT_CONNECT_TIMEOUT_MS} for the client's
     * connect timeout, {@value DEFAULT_READ_TIMEOUT_MS} for the client's read timeout and {@value DEFAULT_PORT} for the
     * client's port.
     *
     * @param disableHostnameVerification indicates whether the client should skip hostname verification during the TLS
     *     handshake. If {@code null}, the behavior depends on whether a trust store is given (if the {@code keyStore}
     *     is empty, the connection will be untrusted and hostname verification will be disabled).
     * @param keyStore the trust store to use if the client's certificate is not issued by a well-known CA. If
     *     {@code null}, the default Java truststore is used. If empty, the connection is untrusted.
     * @return Builder for the http configuration.
     * @see HttpConfiguration.Builder
     */
    public static HttpConfiguration.Builder createDefaultHttpConfiguration(
            Boolean disableHostnameVerification, KeyStore keyStore) {
        return createDefaultHttpConfiguration(
                DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS, disableHostnameVerification, keyStore);
    }

    /**
     * Creates default http configuration used in SDK clients.
     *
     * @param connectTimeoutMs connect timeout for the underlying HTTP clients
     * @param receiveTimeoutMs read timeout for the underlying HTTP clients
     * @param disableHostnameVerification indicates whether the client should skip hostname verification during the TLS
     *     handshake. If {@code null}, the behavior depends on whether a trust store is given (if the {@code keyStore}
     *     is empty, the connection will be untrusted and hostname verification will be disabled).
     * @param keyStore the trust store to use if the client's certificate is not issued by a well-known CA. If
     *     {@code null}, the default Java truststore is used. If empty, the connection is untrusted.
     * @return Builder for the http configuration.
     * @see HttpConfiguration.Builder
     */
    public static HttpConfiguration.Builder createDefaultHttpConfiguration(
            int connectTimeoutMs, int receiveTimeoutMs, Boolean disableHostnameVerification, KeyStore keyStore) {

        HttpConfiguration.SslConfiguration.Builder sslConfigBuilder = new HttpConfiguration.SslConfiguration.Builder();

        try {
            boolean insecure = keyStore != null && !keyStore.aliases().hasMoreElements();
            if (disableHostnameVerification == null) {
                disableHostnameVerification = insecure;
            }
            if (disableHostnameVerification) {
                sslConfigBuilder.disableHostnameVerification();
            }
            if (insecure) {
                sslConfigBuilder.disableCertificateValidation();
            } else {
                sslConfigBuilder.setTrustStore(keyStore);
            }
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }

        HttpConfiguration.Builder httpConfigBuilder = new HttpConfiguration.Builder();

        httpConfigBuilder.setSslConfiguration(sslConfigBuilder.getConfig());
        httpConfigBuilder.setConnectTimeout(connectTimeoutMs);
        httpConfigBuilder.setSoTimeout(receiveTimeoutMs);

        return httpConfigBuilder;
    }
}
