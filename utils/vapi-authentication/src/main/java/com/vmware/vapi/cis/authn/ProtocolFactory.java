/*
 * ******************************************************************
 * Copyright (c) 2012-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.cis.authn;

import java.security.KeyStore;
import java.util.Arrays;

import com.vmware.vapi.cis.authn.json.BearerTokenProcessor;
import com.vmware.vapi.cis.authn.json.JsonSigningProcessor;
import com.vmware.vapi.dsig.json.JsonOAuthProcessor;
import com.vmware.vapi.dsig.json.JsonSessionProcessor;
import com.vmware.vapi.dsig.json.JsonUserPassProcessor;
import com.vmware.vapi.internal.util.Validate;
import com.vmware.vapi.protocol.ClientConfiguration;
import com.vmware.vapi.protocol.ClientConfiguration.Builder;
import com.vmware.vapi.protocol.HttpConfiguration;
import com.vmware.vapi.protocol.HttpConfiguration.SslConfiguration;
import com.vmware.vapi.protocol.JsonProtocolConnectionFactory;
import com.vmware.vapi.protocol.ProtocolConnection;
import com.vmware.vapi.protocol.ProtocolConnectionFactory;

/**
 * Factory for creating {@link ProtocolConnection} instances which uses JSON for messaging protocol.
 *
 * <p>This factory configures the created connections for authentication support. More precisely all
 * {@link ProtocolConnection}s created by this factory are configured with the following authentication processors:
 *
 * <ul>
 *   <li>{@link JsonSigningProcessor}
 *   <li>{@link BearerTokenProcessor}
 *   <li>{@link JsonSessionProcessor}
 *   <li>{@link JsonUserPassProcessor}
 *   <li>{@link JsonOAuthProcessor}
 * </ul>
 */
public final class ProtocolFactory implements ProtocolConnectionFactory {

    private final ProtocolConnectionFactory connectionFactory;

    public enum Protocol {
        http
    }

    /**
     * Default constructor.
     *
     * <p>Uses {@link JsonProtocolConnectionFactory} to actually create the connections.
     */
    public ProtocolFactory() {
        connectionFactory = new JsonProtocolConnectionFactory();
    }

    /**
     * Constructor.
     *
     * <p>Allows injections of <code>ProtocolConnectionFactory</code> which is used to actually create the <code>
     * ProtocolConnection</code>.
     *
     * @param connectionFactory factory to create connections; must not be <code>null</code>
     */
    public ProtocolFactory(ProtocolConnectionFactory connectionFactory) {
        Validate.notNull(connectionFactory);
        this.connectionFactory = connectionFactory;
    }

    @Override
    public ProtocolConnection getConnection(String type, String args, KeyStore trustStore) {
        if (type.equals(Protocol.http.toString())) {
            SslConfiguration sslConfig = new SslConfiguration.Builder(trustStore).getConfig();
            return getHttpConnection(
                    args,
                    null,
                    new HttpConfiguration.Builder()
                            .setSslConfiguration(sslConfig)
                            .getConfig());
        } else {
            return null;
        }
    }

    @Override
    public ProtocolConnection getHttpConnection(
            String uri, ClientConfiguration clientConfig, HttpConfiguration httpConfig) {
        clientConfig = enhanceClientConfig(clientConfig);
        return connectionFactory.getHttpConnection(uri, clientConfig, httpConfig);
    }

    /**
     * Adds the default authentication processors while preserving the given configuration.
     *
     * @param clientConfig can be <code>null</code>
     * @return the client config with the default authn processors. not <code>null</code>
     */
    private ClientConfiguration enhanceClientConfig(ClientConfiguration clientConfig) {
        Builder builder = new Builder(clientConfig);
        if (clientConfig == null || builder.getRequestProcessors() == null) {
            builder.setRequestProcessors(Arrays.asList(
                    new JsonSigningProcessor(),
                    new BearerTokenProcessor(),
                    new JsonSessionProcessor(),
                    new JsonUserPassProcessor(),
                    new JsonOAuthProcessor()));
        }
        return builder.getConfig();
    }

    @Override
    public ProtocolConnection getInsecureConnection(String type, String args) {
        if (type.equals(Protocol.http.toString())) {
            return getHttpConnection(args, null, null);
        } else {
            return null;
        }
    }
}
