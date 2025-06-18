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

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Objects;

import org.w3c.dom.Element;

import com.vmware.sdk.ssoclient.utils.WsTrustAuthenticator;
import com.vmware.sdk.utils.wsdl.PortConfigurer;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VimPortType;

/**
 * Base class for STS authentications that rely on "holding" a private key and certificate.
 *
 * <p>Consult the vSphere documentation for full list of requirements and supported options.
 *
 * @see WsTrustAuthenticator
 */
public abstract class AbstractHokTokenAuthenticator extends AbstractAuthenticator {

    protected final PrivateKey privateKey;
    protected final X509Certificate certificate;
    protected final Duration tokenLifetime;

    protected AbstractHokTokenAuthenticator(
            String serverAddress,
            int port,
            PortConfigurer portConfigurer,
            PrivateKey privateKey,
            X509Certificate certificate,
            Duration tokenLifetime) {
        super(serverAddress, port, portConfigurer);
        Objects.requireNonNull(portConfigurer);
        Objects.requireNonNull(privateKey);
        Objects.requireNonNull(certificate);
        this.privateKey = privateKey;
        this.certificate = certificate;
        this.tokenLifetime = tokenLifetime;
    }

    /**
     * Upon successful authentication, the method returns the newly acquired SAML token.
     *
     * <p>This token can be used in combination with {@link VimPortType#loginByToken(ManagedObjectReference, String)}.
     */
    public abstract Element login();

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public X509Certificate getCertificate() {
        return certificate;
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

        AbstractHokTokenAuthenticator that = (AbstractHokTokenAuthenticator) o;
        return Objects.equals(portConfigurer, that.portConfigurer)
                && Objects.equals(privateKey, that.privateKey)
                && Objects.equals(certificate, that.certificate)
                && Objects.equals(tokenLifetime, that.tokenLifetime);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(portConfigurer);
        result = 31 * result + Objects.hashCode(privateKey);
        result = 31 * result + Objects.hashCode(certificate);
        result = 31 * result + Objects.hashCode(tokenLifetime);
        return result;
    }

    @Override
    public String toString() {
        return "HokTokenAuthenticator{" + "stsPortConfigurer="
                + portConfigurer + ", privateKey=(censored)"
                + ", certificate="
                + certificate + ", tokenLifetime="
                + tokenLifetime + '}';
    }
}
