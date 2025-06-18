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

import static com.vmware.sdk.ssoclient.utils.WsTrustAuthenticator.acquireHokTokenWithUserCredentials;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Objects;

import org.w3c.dom.Element;

import com.vmware.sdk.ssoclient.utils.WsTrustAuthenticator;
import com.vmware.sdk.utils.wsdl.PortConfigurer;

/** Uses username and password of a "regular" / "domain" STS user to acquire a HoK token. */
public class HokTokenAuthenticator extends AbstractHokTokenAuthenticator {

    private final String username;
    private final String password;

    /**
     * @param serverAddress vCenter FQDN or IP address
     * @param port the remote server port (443 typically)
     * @param stsPortConfigurer port configurer for the respective STS service
     * @param username the username to use for authentication
     * @param password the password to use for authentication
     * @param privateKey private key used to sign the authentication request
     * @param certificate certificate used to sign the authentication request
     * @param tokenLifetime an optional token lifetime validity; {@link WsTrustAuthenticator#DEFAULT_TOKEN_LIFETIME} if
     *     omitted
     */
    public HokTokenAuthenticator(
            String serverAddress,
            int port,
            PortConfigurer stsPortConfigurer,
            String username,
            String password,
            PrivateKey privateKey,
            X509Certificate certificate,
            Duration tokenLifetime) {
        super(serverAddress, port, stsPortConfigurer, privateKey, certificate, tokenLifetime);
        this.username = username;
        this.password = password;
    }

    /** {@inheritDoc} */
    public Element login() {
        return acquireHokTokenWithUserCredentials(
                serverAddress, port, portConfigurer, username, password, privateKey, certificate, tokenLifetime);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        HokTokenAuthenticator that = (HokTokenAuthenticator) o;
        return Objects.equals(username, that.username) && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(username);
        result = 31 * result + Objects.hashCode(password);
        return result;
    }

    @Override
    public String toString() {
        return "DomainUserAuthenticator{" + "username='"
                + username + '\'' + ", password='(censored)'"
                + ", stsPortConfigurer="
                + portConfigurer + ", privateKey=(censored)"
                + ", certificate="
                + certificate + ", tokenLifetime="
                + tokenLifetime + '}';
    }
}
