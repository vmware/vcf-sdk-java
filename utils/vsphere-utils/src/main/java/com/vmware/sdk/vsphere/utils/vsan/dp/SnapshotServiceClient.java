/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.vsphere.utils.vsan.dp;

import static com.vmware.sdk.ssoclient.utils.WsTrustAuthenticator.acquireBearerTokenForRegularUser;

import java.time.Duration;
import java.util.Objects;

import org.w3c.dom.Element;

import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;
import com.vmware.vapi.bindings.Service;
import com.vmware.vapi.bindings.StubConfiguration;
import com.vmware.vapi.bindings.StubConfigurationBase;
import com.vmware.vapi.bindings.StubCreator;
import com.vmware.vapi.bindings.StubFactory;
import com.vmware.vapi.cis.authn.ProtocolFactory;
import com.vmware.vapi.cis.authn.SecurityContextFactory;
import com.vmware.vapi.core.ApiProvider;
import com.vmware.vapi.core.ExecutionContext;
import com.vmware.vapi.protocol.HttpConfiguration;
import com.vmware.vapi.protocol.ProtocolConnection;
import com.vmware.vapi.saml.DefaultTokenFactory;
import com.vmware.vapi.saml.SamlToken;
import com.vmware.vapi.saml.exception.InvalidTokenException;

/**
 * The Snapshot Service client can be used to create stubs with which the API can be accessed.
 *
 * <p>This class issues a vSphere Bearer SAML token and uses it to configure the security context of the user-requested
 * stubs.
 */
public class SnapshotServiceClient implements StubCreator {

    // In 80u3, the JSON-RPC endpoint is /api
    // Since 9.0, it is changed to /snapservice
    private static final String JSON_RPC_ENDPOINT = "/snapservice";

    protected final StubCreator stubCreator;
    protected final StubConfiguration ssStubConfig;

    /**
     * Constructs a client which can be used to obtain Snap Service stubs.
     *
     * @param ssServer Snap Service FQDN or IP address
     * @param httpConfiguration stub configuration
     * @param vcServer vCenter FQDN or IP address - used to acquire a SAML token in order to authenticate Snap Service
     *     API calls
     * @param portConfigurer port configurer used to talk to the STS service in order to acquire the SAML token
     * @param username the SSO username with which the SAML token will be acquired
     * @param password the password for the SSO user
     */
    public SnapshotServiceClient(
            String ssServer,
            HttpConfiguration httpConfiguration,
            String vcServer,
            SimpleHttpConfigurer portConfigurer,
            String username,
            String password) {
        this(ssServer, httpConfiguration, vcServer, 443, portConfigurer, username, password, null);
    }

    /**
     * Constructs a client which can be used to obtain Snap Service stubs.
     *
     * @param ssServer Snap Service FQDN or IP address
     * @param httpConfiguration stub configuration
     * @param vcServer vCenter FQDN or IP address - used to acquire a SAML token in order to authenticate Snap Service
     *     API calls
     * @param vcPort the vCenter https port (443 usually)
     * @param portConfigurer port configurer used to talk to the STS service in order to acquire the SAML token
     * @param username the SSO username with which the SAML token will be acquired
     * @param password the password for the SSO user
     * @param tokenLifetime how long should the token be valid for (30 min if omitted)
     */
    public SnapshotServiceClient(
            String ssServer,
            HttpConfiguration httpConfiguration,
            String vcServer,
            int vcPort,
            SimpleHttpConfigurer portConfigurer,
            String username,
            String password,
            Duration tokenLifetime) {

        Objects.requireNonNull(vcServer);
        Objects.requireNonNull(portConfigurer);
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        Objects.requireNonNull(httpConfiguration);

        this.stubCreator = createApiStubFactory(ssServer, httpConfiguration);

        Element tokenElement =
                acquireBearerTokenForRegularUser(vcServer, vcPort, portConfigurer, username, password, tokenLifetime);

        SamlToken samlBearerToken;
        try {
            samlBearerToken = DefaultTokenFactory.createTokenFromDom(tokenElement);
        } catch (InvalidTokenException e) {
            throw new RuntimeException(e);
        }

        // Create a SAML security context using SAML bearer token
        ExecutionContext.SecurityContext samlSecurityContext =
                SecurityContextFactory.createSamlSecurityContext(samlBearerToken, null);

        // Create a stub configuration with SAML security context
        this.ssStubConfig = new StubConfiguration(samlSecurityContext);
    }

    /**
     * Creates a stub for the specified interface.
     *
     * @param vapiIface <code>Class</code> representing a vAPI interface; must not be <code>null</code>
     * @return a stub instance for the specified vAPI interface
     */
    @Override
    public <T extends Service> T createStub(Class<T> vapiIface) {
        return createStub(vapiIface, this.ssStubConfig);
    }

    /**
     * Creates a stub for the specified interface.
     *
     * @param vapiIface <code>Class</code> representing a vAPI interface; must not be <code>null</code>
     * @param config the stub's additional configuration
     * @return a stub instance for the specified vAPI interface
     */
    @Override
    public <T extends Service> T createStub(Class<T> vapiIface, StubConfigurationBase config) {
        return this.stubCreator.createStub(vapiIface, config);
    }

    /**
     * Connects to the server using https protocol and returns the factory instance that can be used for creating the
     * client side stubs.
     *
     * @param server hostname or ip address of the server
     * @param httpConfig HTTP configuration settings to be applied for the connection to the server.
     * @return factory for the client side stubs
     */
    private static StubFactory createApiStubFactory(String server, HttpConfiguration httpConfig) {
        // Create a https connection with the vapi url
        ProtocolFactory pf = new ProtocolFactory();
        String apiUrl = "https://" + server + JSON_RPC_ENDPOINT;

        // Get a connection to the vapi url
        ProtocolConnection connection = pf.getHttpConnection(apiUrl, null, httpConfig);

        // Initialize the stub factory with the api provider
        ApiProvider provider = connection.getApiProvider();
        return new StubFactory(provider);
    }
}
