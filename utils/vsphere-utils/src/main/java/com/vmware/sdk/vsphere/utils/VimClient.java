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

import static com.vmware.sdk.vsphere.utils.VcenterClientFactory.createVimUrl;
import static com.vmware.sdk.vsphere.utils.VsphereCookieHelper.configureOutgoingCookie;

import java.io.Closeable;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import jakarta.xml.ws.BindingProvider;

import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.utils.wsdl.PortConfigurer;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vsan.sdk.VsanhealthPortType;
import com.vmware.vsan.sdk.VsanhealthService;

/**
 * A VIM client can be used to access the WSDL-based APIs of an ESXi or vCenter server.
 *
 * @see ESXiClientFactory
 * @see VcenterClientFactory
 */
public abstract class VimClient implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(VimClient.class);

    protected final String serverAddress;
    protected final int port;
    protected final PortConfigurer portConfigurer;
    protected final SessionIdProvider vimSessionProvider;
    protected volatile ServiceContent vimServiceContent;

    public VimClient(
            String serverAddress, int port, PortConfigurer portConfigurer, SessionIdProvider vimSessionProvider) {
        Objects.requireNonNull(serverAddress);
        Objects.requireNonNull(portConfigurer);
        Objects.requireNonNull(vimSessionProvider);

        this.serverAddress = serverAddress;
        this.port = port;
        this.portConfigurer = portConfigurer;
        this.vimSessionProvider = vimSessionProvider;
    }

    /** @return fresh VIM port, which is fully configured and authenticated. */
    public VimPortType getVimPort() {
        VimPortType port = createVimPort(null);

        this.portConfigurer.configure((BindingProvider) port, createVimUrl(serverAddress, this.port));

        String sessionId = new String(vimSessionProvider.get());
        configureOutgoingCookie((BindingProvider) port, sessionId);

        return port;
    }

    protected VsanhealthPortType getVsanPort(BiFunction<String, Integer, URI> uriGenerator) {
        VsanhealthService vsanService = new VsanhealthService();
        VsanhealthPortType vsanPort = vsanService.getVsanhealthPort();

        this.portConfigurer.configure((BindingProvider) vsanPort, uriGenerator.apply(serverAddress, port));

        String sessionId = new String(vimSessionProvider.get());
        configureOutgoingCookie((BindingProvider) vsanPort, sessionId);

        return vsanPort;
    }

    /**
     * Returns the {@link ServiceContent} that contains the various Managed Object References for the various services
     * and manager entities.
     *
     * <p>The server response is going to be cached on first use and subsequent method invocations will simply return
     * the cached value.
     *
     * @return the {@link ServiceContent}
     */
    public ServiceContent getVimServiceContent() {
        ServiceContent serviceContent = this.vimServiceContent;

        // technically speaking this null check is racey - there's no locking, however this is intentional because:
        // - fetching the ServiceContent is a cheap operation (if we ignore the networking overhead)
        // - the ServiceContent is "static" i.e. it doesn't change and once retrieved, it can be re-used
        // - the "serviceContent" is volatile which, in this particular case, provides read & write mem barrier
        // - in the worse case scenario, if 2 threads call this method, they will both fetch & update the reference
        //  but that's not a problem and is a small cost to pay, compared to the alternative - which is to lock
        if (serviceContent == null) {
            try {
                serviceContent = this.getVimPort().retrieveServiceContent(getVimServiceInstanceRef());
                this.vimServiceContent = serviceContent;
                return serviceContent;
            } catch (RuntimeFaultFaultMsg e) {
                throw new RuntimeException(e);
            }
        }

        return serviceContent;
    }

    /** Invalidates the remote sessions. */
    @Override
    public void close() {
        try {
            this.getVimPort().logout(this.getVimServiceContent().getSessionManager());
            log.debug("Successfully destroyed the SOAP session");
        } catch (Exception e) {
            log.warn("Could not destroy the SOAP session", e);
        }
    }

    /**
     * Creates the VIMPort via the JAX-WS Proxy Factory Bean. Initializes jaxb.additionalContextClasses with all
     * bindings from com.vmware.vim25.
     *
     * @param features A list of {@link org.apache.cxf.feature.Feature} to configure on the proxy.
     * @return returns VimPortType with pre-populated type and value.
     */
    public static VimPortType createVimPort(List<? extends Feature> features) {
        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        Map<String, Object> properties = new HashMap<>();
        properties.put(
                "jaxb.additionalContextClasses", com.vmware.sdk.vsphere.client.bindings.Vim25Classes.getClasses());
        factoryBean.setProperties(properties);
        if (features != null) {
            factoryBean.setFeatures(features);
        }
        return factoryBean.create(VimPortType.class);
    }

    /** @return new ServiceInstance {@link ManagedObjectReference} with pre-populated type and value. */
    public static ManagedObjectReference getVimServiceInstanceRef() {
        ManagedObjectReference ref = new ManagedObjectReference();

        ref.setType("ServiceInstance");
        ref.setValue("ServiceInstance");

        return ref;
    }
}
