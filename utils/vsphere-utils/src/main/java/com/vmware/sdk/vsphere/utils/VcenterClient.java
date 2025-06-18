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

import static com.vmware.sdk.vsphere.utils.VcenterClientFactory.createPbmUrl;
import static com.vmware.sdk.vsphere.utils.VcenterClientFactory.createSmsUrl;
import static com.vmware.sdk.vsphere.utils.VcenterClientFactory.createVslmUrl;
import static com.vmware.sdk.vsphere.utils.VsphereCookieHelper.configureOutgoingSoapCookieHeader;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Objects;

import jakarta.xml.ws.BindingProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.cis.Session;
import com.vmware.pbm.PbmPortType;
import com.vmware.pbm.PbmService;
import com.vmware.pbm.PbmServiceInstanceContent;
import com.vmware.sdk.utils.wsdl.PortConfigurer;
import com.vmware.vapi.bindings.Service;
import com.vmware.vapi.bindings.StubConfiguration;
import com.vmware.vapi.bindings.StubConfigurationBase;
import com.vmware.vapi.bindings.StubCreator;
import com.vmware.vapi.security.SessionSecurityContext;
import com.vmware.vim.sms.SmsPortType;
import com.vmware.vim.sms.SmsService;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vsan.sdk.VsanhealthPortType;
import com.vmware.vslm.VslmPortType;
import com.vmware.vslm.VslmService;
import com.vmware.vslm.VslmServiceInstanceContent;

/**
 * This vCenter client can be used to access the SOAP-based APIs ({@link #getVimPort()}) as well as the vCenter REST
 * APIs ({@link #createStub(Class)}).
 *
 * <p>The various stubs, provided by the methods below, are going to be authenticated as long as the provided
 * {@link #vimSessionProvider} and {@link #vapiSessionProvider} provide a valid session.
 */
public class VcenterClient extends VimClient implements StubCreator, Closeable {

    private static final Logger log = LoggerFactory.getLogger(VcenterClient.class);

    protected final SessionIdProvider vapiSessionProvider;
    protected final StubCreator stubCreator;
    protected volatile VslmServiceInstanceContent vslmServiceInstanceContent;
    protected volatile PbmServiceInstanceContent pbmServiceInstanceContent;

    public VcenterClient(
            String serverAddress,
            int port,
            PortConfigurer portConfigurer,
            SessionIdProvider vimSessionProvider,
            StubCreator stubCreator,
            SessionIdProvider vapiSessionProvider) {
        super(serverAddress, port, portConfigurer, vimSessionProvider);
        Objects.requireNonNull(stubCreator);
        Objects.requireNonNull(vapiSessionProvider);

        this.vapiSessionProvider = vapiSessionProvider;
        this.stubCreator = stubCreator;
    }

    /** @return fresh VSLM port, which is fully configured and authenticated. */
    public VslmPortType getVslmPort() {
        VslmService vslmService = new VslmService();
        VslmPortType vslmPort = vslmService.getVslmPort();

        this.portConfigurer.configure((BindingProvider) vslmPort, createVslmUrl(serverAddress, this.port));

        String sessionId = new String(vimSessionProvider.get());
        configureOutgoingSoapCookieHeader((BindingProvider) vslmPort, sessionId);

        return vslmPort;
    }

    /** @return fresh vSAN port for accessing vSAN APIs on vCenter. The port is fully configured and authenticated. */
    public VsanhealthPortType getVsanPort() {
        return getVsanPort(VcenterClientFactory::createVsanVcenterUrl);
    }

    /** @return a fresh PBM port, which is fully configured and authenticated. */
    public PbmPortType getPbmPort() {
        PbmService pbmService = new PbmService();
        PbmPortType pbmPort = pbmService.getPbmPort();

        this.portConfigurer.configure((BindingProvider) pbmPort, createPbmUrl(serverAddress, this.port));

        String sessionId = new String(vimSessionProvider.get());
        configureOutgoingSoapCookieHeader((BindingProvider) pbmPort, sessionId);

        return pbmPort;
    }

    /** @return fresh SMS port, which is fully configured and authenticated. */
    public SmsPortType getSmsPort() {

        SmsService smsService = new SmsService();
        SmsPortType pbmPort = smsService.getSmsPort();

        this.portConfigurer.configure((BindingProvider) pbmPort, createSmsUrl(serverAddress, this.port));

        String sessionId = new String(vimSessionProvider.get());
        configureOutgoingSoapCookieHeader((BindingProvider) pbmPort, sessionId);

        return pbmPort;
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
    public VslmServiceInstanceContent getVslmServiceInstanceContent() {
        VslmServiceInstanceContent serviceContent = this.vslmServiceInstanceContent;

        // technically speaking this null check is racey - there's no locking, however this is intentional because:
        // - fetching the ServiceContent is a cheap operation (if we ignore the networking overhead)
        // - the ServiceContent is "static" i.e. it doesn't change and once retrieved, it can be re-used
        // - the "serviceContent" is volatile which, in this particular case, provides read & write mem barrier
        // - in the worse case scenario, if 2 threads call this method, they will both fetch & update the reference
        //  but that's not a problem and is a small cost to pay, compared to the alternative - which is to lock
        if (serviceContent == null) {
            try {
                serviceContent = this.getVslmPort().retrieveContent(getVslmServiceInstanceRef());
                this.vslmServiceInstanceContent = serviceContent;
                return serviceContent;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return serviceContent;
    }

    /**
     * Returns the {@link PbmServiceInstanceContent} that contains the various Managed Object References for the various
     * services and manager entities.
     *
     * <p>The server response is going to be cached on first use and subsequent method invocations will simply return
     * the cached value.
     *
     * @return the {@link PbmServiceInstanceContent}
     */
    public PbmServiceInstanceContent getPbmServiceInstanceContent() {
        PbmServiceInstanceContent serviceContent = this.pbmServiceInstanceContent;

        // technically speaking this null check is racey - there's no locking, however this is intentional because:
        // - fetching the ServiceContent is a cheap operation (if we ignore the networking overhead)
        // - the ServiceContent is "static" i.e. it doesn't change and once retrieved, it can be re-used
        // - the "serviceContent" is volatile which, in this particular case, provides read & write mem barrier
        // - in the worse case scenario, if 2 threads call this method, they will both fetch & update the reference
        //  but that's not a problem and is a small cost to pay, compared to the alternative - which is to lock
        if (serviceContent == null) {
            try {
                serviceContent = this.getPbmPort().pbmRetrieveServiceContent(getPbmServiceInstanceRef());
                this.pbmServiceInstanceContent = serviceContent;
                return serviceContent;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return serviceContent;
    }

    /** @return the {@link SessionIdProvider} used to configure {@link VimPortType}s. */
    public SessionIdProvider getVimSessionProvider() {
        return vimSessionProvider;
    }

    /**
     * @return the {@link SessionIdProvider} used to configure stubs for the vCenter REST APIs
     *     ({@link #createStub(Class)}).
     */
    public SessionIdProvider getVapiSessionProvider() {
        return vapiSessionProvider;
    }

    /**
     * Creates a stub for the specified interface.
     *
     * @param vapiIface <code>Class</code> representing a vAPI interface. not null
     * @return a stub instance for the specified vAPI interface
     */
    @Override
    public <T extends Service> T createStub(Class<T> vapiIface) {
        char[] sessionId = vapiSessionProvider.get();
        StubConfiguration stubConfig = new StubConfiguration(new SessionSecurityContext(sessionId));

        return this.createStub(vapiIface, stubConfig);
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

    /** Invalidates the remote sessions. */
    @Override
    public void close() {
        super.close();

        // In order to handle the case where vCenter is older than 8.0.3,
        // we'll need to destroy the SOAP session and also the "vAPI" session.
        boolean requiresDoubleLogout = !Arrays.equals(vimSessionProvider.get(), vapiSessionProvider.get());
        if (requiresDoubleLogout) {
            try {
                this.createStub(Session.class).delete();
                log.trace("Successfully destroyed the vAPI session");
            } catch (Exception e) {
                log.warn("Could not destroy the vAPI session", e);
            }
        } else {
            log.trace("The vAPI session and the SOAP session are the same, skipping the vAPI session cleanup");
        }
    }

    /** @return new VslmServiceInstance {@link ManagedObjectReference} with pre-populated type and value. */
    public static ManagedObjectReference getVslmServiceInstanceRef() {
        ManagedObjectReference ref = new ManagedObjectReference();

        ref.setType("VslmServiceInstance");
        ref.setValue("ServiceInstance");

        return ref;
    }

    /** @return new bmServiceInstance {@link ManagedObjectReference} with pre-populated type and value. */
    public static ManagedObjectReference getPbmServiceInstanceRef() {
        ManagedObjectReference ref = new ManagedObjectReference();

        ref.setType("PbmServiceInstance");
        ref.setValue("ServiceInstance");

        return ref;
    }

    /** @return new SmsServiceInstance {@link ManagedObjectReference} with pre-populated type and value. */
    public static ManagedObjectReference getSmsServiceInstanceRef() {
        ManagedObjectReference ref = new ManagedObjectReference();

        ref.setType("SmsServiceInstance");
        ref.setValue("ServiceInstance");

        return ref;
    }
}
