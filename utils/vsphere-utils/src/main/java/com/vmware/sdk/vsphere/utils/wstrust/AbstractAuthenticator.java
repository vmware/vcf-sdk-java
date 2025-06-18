/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.vsphere.utils.wstrust;

import java.util.Objects;

import org.oasis_open.docs.ws_sx.ws_trust._200512.wsdl.STSServicePortType;
import org.w3c.dom.Element;

import com.vmware.sdk.utils.wsdl.PortConfigurer;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VimPortType;

/** Authenticators know how to talk to the STS service ({@link STSServicePortType}) - how to acquire SAML tokens. */
public abstract class AbstractAuthenticator {

    /** The FQDN or IP address of the STS server (i.e. the vCenter server). */
    protected final String serverAddress;

    /** The https port of the STS server (typically 443). */
    protected final int port;

    /** Port configurer that will be used to prepare a {@link STSServicePortType}. */
    protected final PortConfigurer portConfigurer;

    /**
     * @param serverAddress vCenter FQDN or IP address
     * @param port the remote server port (443 typically)
     * @param portConfigurer port configurer used to prepare a {@link STSServicePortType}
     */
    public AbstractAuthenticator(String serverAddress, int port, PortConfigurer portConfigurer) {
        Objects.requireNonNull(serverAddress);
        Objects.requireNonNull(portConfigurer);
        this.portConfigurer = portConfigurer;
        this.serverAddress = serverAddress;
        this.port = port;
    }

    /**
     * Upon successful authentication, the method returns the newly acquired SAML token.
     *
     * <p>This token can be used in combination with {@link VimPortType#loginByToken(ManagedObjectReference, String)}.
     *
     * @return the SAML token
     */
    public abstract Element login();

    public String getServerAddress() {
        return serverAddress;
    }

    public int getPort() {
        return port;
    }

    public PortConfigurer getPortConfigurer() {
        return portConfigurer;
    }
}
