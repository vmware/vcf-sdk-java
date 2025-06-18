/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.vsphere.utils;

import static com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer.DEFAULT_CONNECT_TIMEOUT_MS;
import static com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer.DEFAULT_PORT;
import static com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer.DEFAULT_READ_TIMEOUT_MS;

import java.net.URI;
import java.security.KeyStore;

import com.vmware.sdk.utils.ssl.vapi.HttpConfigHelper;
import com.vmware.sdk.utils.wsdl.PortConfigurer;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vsan.sdk.VsanhealthPortType;

/**
 * A factory that produces {@link ESXiClient} instances.
 *
 * <p>The most common configuration of the underlying HTTP client can be specified using the factory constructors - see
 * {@link SimpleHttpConfigurer} for typical default values.
 *
 * <p>Advanced configuration can be specified by using {@link ESXiClientFactory#ESXiClientFactory(String, int,
 * PortConfigurer)}.
 *
 * <p>This factory is NOT going to take of the session health checking - i.e. long-running applications should
 * periodically "ping" the session(s) by invoking "cheap" APIs, or alternatively they should acquire new clients in
 * order to re-authenticate. Failing to do so may result in failing API invocations because of expired sessions.
 */
public class ESXiClientFactory extends VimClientFactory {

    /**
     * Creates a new factory that'll produce clients with default connection configuration.
     *
     * @param serverAddress ESXi FQDN or IP address
     * @see SimpleHttpConfigurer
     * @see HttpConfigHelper#createDefaultHttpConfiguration(int, int, Boolean, KeyStore)
     */
    public ESXiClientFactory(String serverAddress) {
        this(serverAddress, DEFAULT_PORT, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS, true, null);
    }

    /**
     * Creates a new factory that'll produce clients with default connection configuration.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @param trustStore user-supplied trust store in case vCenter's certificate is not issued by a well-known CA. If
     *     not present, the connection will be insecure.
     * @see SimpleHttpConfigurer
     * @see HttpConfigHelper#createDefaultHttpConfiguration(int, int, Boolean, KeyStore)
     */
    public ESXiClientFactory(String serverAddress, KeyStore trustStore) {
        this(serverAddress, DEFAULT_PORT, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS, trustStore);
    }

    /**
     * Creates a new factory that'll produce clients with default connection configuration.
     *
     * @param serverAddress ESXi FQDN or IP address
     * @param verifyHostname whether to perform hostname verification during the TLS handshake; for insecure this must
     *     be set to <code>false</code>. If null, its value will depend on the presence of the truststore
     * @param trustStore user-supplied trust store in case vCenter's certificate is not issued by a well-known CA; for
     *     insecure connections this should be an empty keystore; <code>null</code> means "use JVM's default CA
     *     truststore"
     * @see SimpleHttpConfigurer
     * @see HttpConfigHelper#createDefaultHttpConfiguration(int, int, Boolean, KeyStore)
     */
    public ESXiClientFactory(String serverAddress, Boolean verifyHostname, KeyStore trustStore) {
        this(
                serverAddress,
                DEFAULT_PORT,
                DEFAULT_CONNECT_TIMEOUT_MS,
                DEFAULT_READ_TIMEOUT_MS,
                verifyHostname,
                trustStore);
    }

    /**
     * Creates a new factory that'll produce clients with user-provided connection configuration.
     *
     * @param serverAddress ESXi FQDN or IP address
     * @param port the HTTPS port for API requests
     * @param connectTimeoutMs connect timeout for the underlying HTTP clients
     * @param readTimeoutMs read timeout for the underlying HTTP clients
     * @param trustStore user-supplied trust store in case vCenter's certificate is not issued by a well-known CA; for
     *     insecure connections this should be an empty keystore; <code>null</code> means "use JVM's default CA
     *     truststore"
     * @see SimpleHttpConfigurer
     * @see HttpConfigHelper#createDefaultHttpConfiguration(int, int, Boolean, KeyStore)
     */
    public ESXiClientFactory(
            String serverAddress, int port, int connectTimeoutMs, int readTimeoutMs, KeyStore trustStore) {
        this(serverAddress, port, createDefaultPortConfigurer(connectTimeoutMs, readTimeoutMs, null, trustStore));
    }
    /**
     * Creates a new factory that'll produce clients with user-provided connection configuration.
     *
     * @param serverAddress ESXi FQDN or IP address
     * @param port the HTTPS port for API requests
     * @param connectTimeoutMs connect timeout for the underlying HTTP clients
     * @param readTimeoutMs read timeout for the underlying HTTP clients
     * @param verifyHostname whether to perform hostname verification during the TLS handshake; for insecure this must
     *     be set to <code>false</code>. If null, its value will depend on the presence of the truststore.
     * @param trustStore user-supplied trust store in case vCenter's certificate is not issued by a well-known CA; for
     *     insecure connections this should be an empty keystore; <code>null</code> means "use JVM's default CA
     *     truststore"
     * @see SimpleHttpConfigurer
     * @see HttpConfigHelper#createDefaultHttpConfiguration(int, int, Boolean, KeyStore)
     */
    public ESXiClientFactory(
            String serverAddress,
            int port,
            int connectTimeoutMs,
            int readTimeoutMs,
            Boolean verifyHostname,
            KeyStore trustStore) {
        this(
                serverAddress,
                port,
                createDefaultPortConfigurer(connectTimeoutMs, readTimeoutMs, verifyHostname, trustStore));
    }

    /**
     * Creates a new factory that'll produce clients with user-provided connection configuration.
     *
     * @param serverAddress ESXi FQDN or IP address
     * @param port the HTTPS port for API requests
     * @param portConfigurer the configurer which will perform the actual configuration of the SOAP ports
     */
    public ESXiClientFactory(String serverAddress, int port, PortConfigurer portConfigurer) {
        super(serverAddress, port, portConfigurer);
    }

    /**
     * Creates a new session and uses it to construct a new client which can provide stubs for the various services.
     *
     * @param username vCenter username
     * @param password password for the username
     * @param locale an optional locale used to format information such as dates, error messages, etc.
     * @return a client which can be used to create API stubs
     */
    public ESXiClient createClient(String username, String password, String locale) {
        return (ESXiClient) super.createClient(username, password, locale);
    }

    @Override
    protected VimClient createClient(
            String username,
            String password,
            String locale,
            VimPortType vimPort,
            ServiceContent serviceContent,
            SessionIdProvider vimSessionProvider) {
        return new ESXiClient(serverAddress, port, portConfigurer, vimSessionProvider);
    }

    /**
     * Constructs a URI that should be used to configure {@link VsanhealthPortType}.
     *
     * @param serverAddress ESXi FQDN or IP address
     * @param port remote server port (typically 443)
     * @return the URI
     */
    public static URI createVsanEsxUrl(String serverAddress, int port) {
        return createUrl(serverAddress, "/vsan", port);
    }

    /**
     * Constructs a URI that should be used to configure {@link VsanhealthPortType}.
     *
     * <p>Assumes the standard {@link SimpleHttpConfigurer#DEFAULT_PORT}.
     *
     * @param serverAddress ESXi FQDN or IP address
     * @return the URI
     */
    public static URI createVsanEsxUrl(String serverAddress) {
        return createVsanEsxUrl(serverAddress, DEFAULT_PORT);
    }
}
