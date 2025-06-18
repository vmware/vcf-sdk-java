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

import static com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer.DEFAULT_HOSTNAME_VERIFIER;
import static com.vmware.sdk.vsphere.utils.VcenterClientFactory.createVimUrl;
import static com.vmware.sdk.vsphere.utils.VimClient.createVimPort;
import static com.vmware.sdk.vsphere.utils.VimClient.getVimServiceInstanceRef;
import static com.vmware.sdk.vsphere.utils.VsphereCookieHelper.extractSessionId;

import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Objects;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.TrustManager;

import jakarta.xml.ws.BindingProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.utils.ssl.InsecureHostnameVerifier;
import com.vmware.sdk.utils.ssl.InsecureTrustManager;
import com.vmware.sdk.utils.ssl.TlsHelper;
import com.vmware.sdk.utils.wsdl.PortConfigurer;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/**
 * This is the base class for handling code related to VIM client (be it for vCenter or ESXi) initialization.
 *
 * @see ESXiClientFactory
 * @see VcenterClientFactory
 */
abstract class VimClientFactory {

    private static final Logger log = LoggerFactory.getLogger(VimClientFactory.class);

    /** vCenter FQDN or IP address. */
    protected final String serverAddress;

    /** vCenter's https port (typically 443). */
    protected final int port;

    /** A port configurer that knows how to configure SOAP ports. */
    protected final PortConfigurer portConfigurer;

    public VimClientFactory(String serverAddress, int port, PortConfigurer portConfigurer) {
        Objects.requireNonNull(serverAddress);
        Objects.requireNonNull(portConfigurer);
        this.serverAddress = serverAddress;
        this.port = port;
        this.portConfigurer = portConfigurer;
    }

    protected VimClient createClient(String username, String password, String locale) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);

        VimPortType vimPort = createVimPort(null);

        portConfigurer.configure((BindingProvider) vimPort, createVimUrl(serverAddress, port));

        ServiceContent serviceContent;
        try {
            serviceContent = vimPort.retrieveServiceContent(getVimServiceInstanceRef());
            vimPort.login(serviceContent.getSessionManager(), username, password, locale);
        } catch (Exception e) {
            log.error("The login authentication failed.", e);
            throw new RuntimeException(e);
        }

        String sessionId = extractSessionId(vimPort);
        SessionIdProvider vimSessionProvider = sessionId::toCharArray;

        return createClient(username, password, locale, vimPort, serviceContent, vimSessionProvider);
    }

    /**
     * A factory method which can be used to create a type of {@link VimClient}.
     *
     * <p>The provided credentials have been already used to {@link VimPortType#login(ManagedObjectReference, String,
     * String, String)}.
     *
     * @param username the username with which the vim session was created
     * @param password the password with which the vim session was created
     * @param locale the locale with which the vim session was created
     * @param vimPort the port used to create the vim session
     * @param serviceContent the server-provided service content
     * @param vimSessionProvider session provider that captures the created session
     * @return a specific {@link VimClient} implementation
     */
    protected abstract VimClient createClient(
            String username,
            String password,
            String locale,
            VimPortType vimPort,
            ServiceContent serviceContent,
            SessionIdProvider vimSessionProvider);

    protected static SimpleHttpConfigurer createDefaultPortConfigurer(
            int connectTimeoutMs, int readTimeoutMs, Boolean verifyHostname, KeyStore trustStore) {

        TrustManager[] trustManagers;
        try {
            boolean insecure = trustStore != null && !trustStore.aliases().hasMoreElements();
            if (verifyHostname == null) {
                verifyHostname = !insecure;
            }
            if (insecure) {
                trustManagers = new TrustManager[] {new InsecureTrustManager()};
            } else {
                trustManagers = TlsHelper.createTrustManagers(trustStore);
            }
        } catch (KeyStoreException e) {
            log.error("Could not retrieve the keystore aliases - it hasn't been initialized.");
            throw new RuntimeException(e);
        }
        HostnameVerifier hostnameVerifier = verifyHostname ? DEFAULT_HOSTNAME_VERIFIER : new InsecureHostnameVerifier();
        return new SimpleHttpConfigurer(connectTimeoutMs, readTimeoutMs, hostnameVerifier, trustManagers);
    }

    protected static URI createUrl(String serverAddress, String path, int port) {
        try {
            return new URI("https", null, serverAddress, port, path, null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
