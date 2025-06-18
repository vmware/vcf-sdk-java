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

import static com.vmware.sdk.ssoclient.utils.WsTrustAuthenticator.acquireHokToken;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Objects;

import org.w3c.dom.Element;

import com.vmware.sdk.ssoclient.utils.WsTrustAuthenticator;
import com.vmware.sdk.utils.wsdl.PortConfigurer;

/** Uses an en existing SAML token to acquire a new one. */
public class HokTokenForTokenAuthenticator extends AbstractHokTokenAuthenticator {

    private final Element token;

    /**
     * @param serverAddress vCenter FQDN or IP address
     * @param port the remote server port (443 typically)
     * @param stsPortConfigurer port configurer for the respective STS service
     * @param token the existing SAML token
     * @param privateKey private key used to sign the authentication request
     * @param certificate certificate used to sign the authentication request
     * @param tokenLifetime an optional token lifetime validity; {@link WsTrustAuthenticator#DEFAULT_TOKEN_LIFETIME} if
     *     omitted
     */
    public HokTokenForTokenAuthenticator(
            String serverAddress,
            int port,
            PortConfigurer stsPortConfigurer,
            Element token,
            PrivateKey privateKey,
            X509Certificate certificate,
            Duration tokenLifetime) {
        super(serverAddress, port, stsPortConfigurer, privateKey, certificate, tokenLifetime);
        Objects.requireNonNull(token);
        this.token = token;
    }

    /** {@inheritDoc} */
    public Element login() {
        return acquireHokToken(serverAddress, port, portConfigurer, token, privateKey, certificate, tokenLifetime);
    }

    public Element getToken() {
        return token;
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

        HokTokenForTokenAuthenticator that = (HokTokenForTokenAuthenticator) o;
        return Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(token);
        return result;
    }

    @Override
    public String toString() {
        return "ExistingHokTokenAuthenticator{" + "token="
                + token + ", stsPortConfigurer="
                + portConfigurer + ", privateKey=(censored)"
                + ", certificate="
                + certificate + ", tokenLifetime="
                + tokenLifetime + '}';
    }
}
