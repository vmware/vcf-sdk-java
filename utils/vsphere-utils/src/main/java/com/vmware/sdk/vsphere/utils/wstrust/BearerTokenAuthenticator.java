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

import static com.vmware.sdk.ssoclient.utils.WsTrustAuthenticator.acquireBearerTokenForRegularUser;

import java.time.Duration;
import java.util.Objects;

import org.w3c.dom.Element;

import com.vmware.sdk.ssoclient.utils.WsTrustAuthenticator;
import com.vmware.sdk.utils.wsdl.PortConfigurer;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VimPortType;

/** Uses username and password of a "regular" / "domain" STS user to acquire a Bearer token. */
public class BearerTokenAuthenticator extends AbstractAuthenticator {

    private final String username;
    private final String password;
    private final Duration tokenLifetime;

    /**
     * @param serverAddress vCenter FQDN or IP address
     * @param port the remote server port (443 typically)
     * @param portConfigurer port configurer for the respective STS service
     * @param username the username to use for authentication
     * @param password the password to use for authentication
     * @param tokenLifetime an optional token lifetime validity; {@link WsTrustAuthenticator#DEFAULT_TOKEN_LIFETIME} if
     *     omitted
     */
    public BearerTokenAuthenticator(
            String serverAddress,
            int port,
            PortConfigurer portConfigurer,
            String username,
            String password,
            Duration tokenLifetime) {
        super(serverAddress, port, portConfigurer);
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        this.username = username;
        this.password = password;
        this.tokenLifetime = tokenLifetime;
    }

    /**
     * Upon successful authentication, the method returns the newly acquired SAML token.
     *
     * <p>This token can be used in combination with {@link VimPortType#loginByToken(ManagedObjectReference, String)}.
     */
    public Element login() {
        return acquireBearerTokenForRegularUser(serverAddress, port, portConfigurer, username, password, tokenLifetime);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Duration getTokenLifetime() {
        return tokenLifetime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BearerTokenAuthenticator that = (BearerTokenAuthenticator) o;
        return Objects.equals(portConfigurer, that.portConfigurer)
                && Objects.equals(username, that.username)
                && Objects.equals(password, that.password)
                && Objects.equals(tokenLifetime, that.tokenLifetime);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(portConfigurer);
        result = 31 * result + Objects.hashCode(username);
        result = 31 * result + Objects.hashCode(password);
        result = 31 * result + Objects.hashCode(tokenLifetime);
        return result;
    }

    @Override
    public String toString() {
        return "BearerTokenAuthenticator{" + "stsPortConfigurer="
                + portConfigurer + ", username='"
                + username + '\'' + ", password='(censored)'"
                + ", tokenLifetime="
                + tokenLifetime + '}';
    }
}
