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

import static com.vmware.sdk.utils.ssl.vapi.HttpConfigHelper.createDefaultHttpConfiguration;
import static com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer.DEFAULT_CONNECT_TIMEOUT_MS;
import static com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer.DEFAULT_PORT;
import static com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer.DEFAULT_READ_TIMEOUT_MS;
import static com.vmware.sdk.vsphere.utils.VimClient.createVimPort;
import static com.vmware.sdk.vsphere.utils.VimClient.getVimServiceInstanceRef;
import static com.vmware.sdk.vsphere.utils.VsphereCookieHelper.configureOutgoingCookie;
import static com.vmware.sdk.vsphere.utils.VsphereCookieHelper.extractSessionId;
import static com.vmware.vapi.cis.authn.SecurityContextFactory.createSamlSecurityContext;
import static com.vmware.vapi.cis.authn.SecurityContextFactory.createUserPassSecurityContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import jakarta.xml.ws.BindingProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.vmware.cis.Session;
import com.vmware.pbm.PbmPortType;
import com.vmware.sdk.ssoclient.utils.SoapUtils;
import com.vmware.sdk.ssoclient.utils.soaphandlers.HeaderHandlerResolver;
import com.vmware.sdk.ssoclient.utils.soaphandlers.SamlTokenHandler;
import com.vmware.sdk.ssoclient.utils.soaphandlers.TimeStampHandler;
import com.vmware.sdk.ssoclient.utils.soaphandlers.WsSecuritySignatureAssertionHandler;
import com.vmware.sdk.utils.ssl.vapi.HttpConfigHelper;
import com.vmware.sdk.utils.wsdl.PortConfigurer;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;
import com.vmware.sdk.vsphere.utils.wstrust.AbstractHokTokenAuthenticator;
import com.vmware.sdk.vsphere.utils.wstrust.BearerTokenAuthenticator;
import com.vmware.sdk.vsphere.utils.wstrust.HokTokenAuthenticator;
import com.vmware.sdk.vsphere.utils.wstrust.HokTokenForTokenAuthenticator;
import com.vmware.vapi.bindings.Service;
import com.vmware.vapi.bindings.StubConfiguration;
import com.vmware.vapi.bindings.StubCreator;
import com.vmware.vapi.bindings.StubFactory;
import com.vmware.vapi.cis.authn.ProtocolFactory;
import com.vmware.vapi.core.ApiProvider;
import com.vmware.vapi.core.ExecutionContext.SecurityContext;
import com.vmware.vapi.protocol.HttpConfiguration;
import com.vmware.vapi.protocol.ProtocolConnection;
import com.vmware.vapi.saml.DefaultTokenFactory;
import com.vmware.vapi.saml.SamlToken;
import com.vmware.vapi.saml.exception.InvalidTokenException;
import com.vmware.vim.sms.SmsPortType;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;
import com.vmware.vsan.sdk.VsanhealthPortType;
import com.vmware.vslm.VslmPortType;

/**
 * A factory that produces {@link VcenterClient} instances.
 *
 * <p>The most common configuration of the underlying HTTP client can be specified using the factory constructors - see
 * {@link SimpleHttpConfigurer} for typical default values.
 *
 * <p>Advanced configuration can be specified by using {@link VcenterClientFactory#VcenterClientFactory(String, int,
 * PortConfigurer, HttpConfiguration)}.
 *
 * <p>vCenter versions prior to 8.0.2 require the API client to authenticate twice and to maintain two different
 * sessions. The first authentication can be performed by invoking one of the login() methods in {@link VimPortType}.
 * The second authentication can be performed by creating a {@link SecurityContext} and invoking
 * {@link Session#create()}.
 *
 * <p>vCenter versions 8.0.3-or-later make it possible to authenticate once and use the same session for both API
 * endpoints.
 *
 * <p>This factory hides the authentication complexity.
 *
 * <p>This factory is NOT going to take of the session health checking - i.e. long-running applications should
 * periodically "ping" the session(s) by invoking "cheap" APIs, or alternatively they should acquire new clients in
 * order to re-authenticate. Failing to do so may result in failing API invocations because of expired sessions.
 */
public class VcenterClientFactory extends VimClientFactory {

    private static final Logger log = LoggerFactory.getLogger(VcenterClientFactory.class);

    /** Http configuration for vAPI stubs. */
    protected final HttpConfiguration vApiHttpConfiguration;

    /**
     * Creates a new factory that'll produce clients with default connection configuration.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @see SimpleHttpConfigurer
     * @see HttpConfigHelper#createDefaultHttpConfiguration(int, int, Boolean, KeyStore)
     */
    public VcenterClientFactory(String serverAddress) {
        this(serverAddress, null);
    }

    /**
     * Creates a new factory that'll produce clients with default connection configuration.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @param trustStore user-supplied trust store in case vCenter's certificate is not issued by a well-known CA; for
     *     insecure connections this should be an empty keystore; <code>null</code> means "use JVM's default CA
     *     truststore"
     * @see SimpleHttpConfigurer
     * @see HttpConfigHelper#createDefaultHttpConfiguration(int, int, Boolean, KeyStore)
     */
    public VcenterClientFactory(String serverAddress, KeyStore trustStore) {
        this(serverAddress, null, trustStore);
    }

    /**
     * Creates a new factory that'll produce clients with default connection configuration.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @param verifyHostname whether to perform hostname verification during the TLS handshake; for insecure this must
     *     be set to <code>false</code>. If null, its value will depend on the presence of the truststore.
     * @param trustStore user-supplied trust store in case vCenter's certificate is not issued by a well-known CA; for
     *     insecure connections this should be an empty keystore; <code>null</code> means "use JVM's default CA
     *     truststore"
     * @see SimpleHttpConfigurer
     * @see HttpConfigHelper#createDefaultHttpConfiguration(int, int, Boolean, KeyStore)
     */
    public VcenterClientFactory(String serverAddress, Boolean verifyHostname, KeyStore trustStore) {
        this(
                serverAddress,
                DEFAULT_PORT,
                DEFAULT_CONNECT_TIMEOUT_MS,
                DEFAULT_READ_TIMEOUT_MS,
                verifyHostname,
                trustStore);
    }

    /**
     * Creates a new factory that'll produce clients with user-provided connection configuration.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @param port the HTTPS port for API requests
     * @param connectTimeoutMs read timeout for the underlying HTTP clients
     * @param readTimeoutMs connect timeout for the underlying HTTP clients
     * @param verifyHostname whether to perform hostname verification during the TLS handshake; for insecure this must
     *     be set to <code>false</code>
     * @param trustStore user-supplied trust store in case vCenter's certificate is not issued by a well-known CA; for
     *     insecure connections this should be an empty keystore; <code>null</code> means "use JVM's default CA
     *     truststore"
     * @see SimpleHttpConfigurer
     * @see HttpConfigHelper#createDefaultHttpConfiguration(int, int, Boolean, KeyStore)
     */
    public VcenterClientFactory(
            String serverAddress,
            int port,
            int connectTimeoutMs,
            int readTimeoutMs,
            Boolean verifyHostname,
            KeyStore trustStore) {
        super(
                serverAddress,
                port,
                createDefaultPortConfigurer(connectTimeoutMs, readTimeoutMs, verifyHostname, trustStore));
        this.vApiHttpConfiguration = createDefaultHttpConfiguration(
                        connectTimeoutMs, readTimeoutMs, verifyHostname, trustStore)
                .getConfig();
    }

    /**
     * Creates a new factory that'll produce clients with user-provided connection configuration.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @param port the HTTPS port for API requests
     * @param connectTimeoutMs read timeout for the underlying HTTP clients
     * @param readTimeoutMs connect timeout for the underlying HTTP clients
     * @param trustStore user-supplied trust store in case vCenter's certificate is not issued by a well-known CA; for
     *     insecure connections this should be an empty keystore; <code>null</code> means "use JVM's default CA
     *     truststore"
     * @see SimpleHttpConfigurer
     * @see HttpConfigHelper#createDefaultHttpConfiguration(int, int, Boolean, KeyStore)
     */
    public VcenterClientFactory(
            String serverAddress, int port, int connectTimeoutMs, int readTimeoutMs, KeyStore trustStore) {
        this(serverAddress, port, connectTimeoutMs, readTimeoutMs, null, trustStore);
    }

    /**
     * Creates a new factory that'll produce clients whose underlying HTTP configuration is entirely managed by the
     * given {@link PortConfigurer} and {@link HttpConfiguration}.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @param port the HTTPS port for API requests
     * @param portConfigurer an optional port configurer; if omitted, {@link SimpleHttpConfigurer} will be used
     * @param vApiHttpConfiguration an optional configuration for vApi stubs; if omitted, default one will be used
     */
    public VcenterClientFactory(
            String serverAddress, int port, PortConfigurer portConfigurer, HttpConfiguration vApiHttpConfiguration) {
        super(serverAddress, port, portConfigurer);
        this.vApiHttpConfiguration = vApiHttpConfiguration;
    }

    /**
     * Fetches a HoK token from the STS Service, creates a new vCenter session and uses it to construct a new client
     * which can provide stubs for the various services.
     *
     * @param auth the authenticator which will do the actual authentication
     * @return a client which can be used to create API stubs
     * @see HokTokenAuthenticator
     * @see HokTokenForTokenAuthenticator
     */
    public VcenterClient createClient(AbstractHokTokenAuthenticator auth) {
        Objects.requireNonNull(auth);
        return createClient(auth.login(), auth.getPrivateKey(), auth.getCertificate());
    }

    /**
     * Fetches a Bearer token from the STS Service, creates a new vCenter session and uses it to construct a new client
     * which can provide stubs for the various services.
     *
     * @param auth the authenticator which will do the actual authentication
     * @return a client which can be used to create API stubs
     */
    public VcenterClient createClient(BearerTokenAuthenticator auth) {
        Objects.requireNonNull(auth);
        return createClient(auth.login(), null, null);
    }

    /**
     * Creates a new vCenter session and uses it to construct a new client which can provide stubs for the various
     * services.
     *
     * @param username vCenter username
     * @param password password for the username
     * @param locale an optional locale used to format information such as dates, error messages, etc.
     * @return a client which can be used to create API stubs
     */
    public VcenterClient createClient(String username, String password, String locale) {
        return (VcenterClient) super.createClient(username, password, locale);
    }

    @Override
    protected VcenterClient createClient(
            String username,
            String password,
            String locale,
            VimPortType vimPort,
            ServiceContent serviceContent,
            SessionIdProvider vimSessionProvider) {
        return createClient(
                vimPort, vimSessionProvider, () -> createUserPassSecurityContext(username, password.toCharArray()));
    }

    /**
     * Creates a new {@link VcenterClient} which can be used to produce properly configured, authenticated, ready-to-use
     * {@link VimPortType}s or {@link Service}s.
     *
     * <p>Uses the given SAML token to log into vCenter. If it's running 8.0.3-or-later, a single session is going to be
     * used for both API endpoints. Older versions require dedicated sessions which will be created here.
     *
     * @param samlToken the SAML token received after successful STS authentication
     * @param privateKey the key used to issue a HoK token or null for Bearer token
     * @param certificate the certificate used to issue a HoK token or null for Bearer token
     * @return the client
     */
    protected VcenterClient createClient(Element samlToken, PrivateKey privateKey, X509Certificate certificate) {

        SessionIdProvider vimSessionProvider = createSessionProvider(samlToken, privateKey, certificate);

        VimPortType vimPort = createVimPort(null);

        portConfigurer.configure((BindingProvider) vimPort, createVimUrl(serverAddress, port));
        configureOutgoingCookie((BindingProvider) vimPort, new String(vimSessionProvider.get()));

        return createClient(vimPort, vimSessionProvider, () -> {
            try {
                SamlToken vapiSamlToken = DefaultTokenFactory.createTokenFromDom(samlToken);
                return createSamlSecurityContext(vapiSamlToken, privateKey);
            } catch (InvalidTokenException e) {
                // this shouldn't be possible because the STS is expected to always provide a valid token
                throw new RuntimeException(e);
            }
        });
    }

    protected VcenterClient createClient(
            VimPortType vimPort,
            SessionIdProvider vimSessionProvider,
            Supplier<SecurityContext> securityContextSupplier) {

        String apiVersion;
        try {
            apiVersion = vimPort.retrieveServiceContent(getVimServiceInstanceRef())
                    .getAbout()
                    .getApiVersion();
        } catch (Exception e) {
            log.error("Could not get retrieve the vCenter's ServiceContent.", e);
            throw new RuntimeException(e);
        }

        URI vimUrl = createVimUrl(serverAddress, port);

        log.debug("Creating vCenter client for {} (server version = {})", vimUrl, apiVersion);

        char[] vapiSessionId;
        if (is803OrLater(apiVersion)) {
            // no need to do secondary login
            vapiSessionId = vimSessionProvider.get();
        } else {
            // create a secondary session
            SecurityContext samlSecurityContext = securityContextSupplier.get();
            StubConfiguration stubConfig = new StubConfiguration(samlSecurityContext);

            StubFactory stubFactory = createStubFactory(vimUrl, vApiHttpConfiguration);
            Session session = stubFactory.createStub(Session.class, stubConfig);

            vapiSessionId = session.create();
        }

        SessionIdProvider vapiSessionProvider = () -> vapiSessionId;
        StubCreator stubCreator = createStubFactory(vimUrl, vApiHttpConfiguration);

        return new VcenterClient(
                serverAddress, port, portConfigurer, vimSessionProvider, stubCreator, vapiSessionProvider);
    }

    protected StubFactory createStubFactory(URI baseUrl, HttpConfiguration httpConfig) {
        URI vapiUrl;
        try {
            vapiUrl = new URI("https", null, baseUrl.getHost(), baseUrl.getPort(), "/api", null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        ProtocolFactory pf = new ProtocolFactory();
        ProtocolConnection connection = pf.getHttpConnection(vapiUrl.toString(), null, httpConfig);

        ApiProvider provider = connection.getApiProvider();
        return new StubFactory(provider);
    }

    /**
     * Uses the provided SAML token to log into vCenter via {@link VimPortType#loginByToken(ManagedObjectReference,
     * String)}. Stores the received session id in a {@link SessionIdProvider} upon successful login.
     *
     * @param samlToken the SAML token received after successful STS authentication
     * @param privateKey the key used to issue a HoK token or null for Bearer token
     * @param certificate the certificate used to issue a HoK token or null for Bearer token
     * @see VsphereCookieHelper#configureOutgoingCookie(BindingProvider, String)
     * @return {@link SessionIdProvider} which can be used to configure new {@link VimPortType} instances
     */
    private SessionIdProvider createSessionProvider(
            Element samlToken, PrivateKey privateKey, X509Certificate certificate) {

        return () -> {
            VimService vimService = new VimService();
            HeaderHandlerResolver handlerResolver = new HeaderHandlerResolver();
            handlerResolver.addHandler(new TimeStampHandler());
            handlerResolver.addHandler(new SamlTokenHandler(samlToken));
            if (privateKey != null && certificate != null) {
                handlerResolver.addHandler(new WsSecuritySignatureAssertionHandler(
                        privateKey, certificate, SoapUtils.getNodeProperty(samlToken, "ID")));
            }

            vimService.setHandlerResolver(handlerResolver);

            VimPortType vimPort = vimService.getVimPort();
            portConfigurer.configure((BindingProvider) vimPort, createVimUrl(serverAddress, port));

            try {
                ServiceContent serviceContent = vimPort.retrieveServiceContent(getVimServiceInstanceRef());
                vimPort.loginByToken(serviceContent.getSessionManager(), null);
            } catch (Exception e) {
                log.error("Could not authenticate to vCenter.", e);
                throw new RuntimeException(e);
            }
            return extractSessionId(vimPort).toCharArray();
        };
    }

    /**
     * Determines whether the given version is 8.0.3-or-later.
     *
     * <p>The version can be retrieved via:
     *
     * <blockquote>
     *
     * <pre>
     *     String version = vimPort.retrieveServiceContent(getServiceInstanceRef()).getAbout().getApiVersion();
     * </pre>
     *
     * </blockquote>
     *
     * @param version the API version string
     * @see VimPortType#retrieveServiceContent(ManagedObjectReference)
     * @see VimClient#getVimServiceInstanceRef()
     * @return true if vCenter is 8.0.3-or-later (and supports single session), otherwise false
     */
    public static boolean is803OrLater(String version) {
        List<String> tokens = new ArrayList<>(Arrays.asList(version.split("\\.")));
        while (tokens.size() < 3) {
            tokens.add("0");
        }

        int major = Integer.parseInt(tokens.get(0));
        int minor = Integer.parseInt(tokens.get(1));
        int patch = Integer.parseInt(tokens.get(2));

        // 6.x.x, 7.x.x -> false
        if (major < 8) {
            return false;
        }

        // 9.x.x -> true
        if (major >= 9) {
            return true;
        }

        // 8.1.x -> true
        if (minor >= 1) {
            return true;
        } else {
            // check if x in 8.0.x is 3-or-later
            return patch >= 3;
        }
    }

    /**
     * Constructs a URI that should be used to configure {@link VimPortType}.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @param port remote server port (typically 443)
     * @return the URI
     */
    public static URI createVimUrl(String serverAddress, int port) {
        return createUrl(serverAddress, "/sdk", port);
    }

    /**
     * Constructs a URI that should be used to configure {@link VimPortType}.
     *
     * <p>Assumes the standard {@link SimpleHttpConfigurer#DEFAULT_PORT}.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @return the URI
     */
    public static URI createVimUrl(String serverAddress) {
        return createUrl(serverAddress, "/sdk", DEFAULT_PORT);
    }

    /**
     * Constructs a URI that should be used to configure {@link PbmPortType}.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @param port remote server port (typically 443)
     * @return the URI
     */
    public static URI createPbmUrl(String serverAddress, int port) {
        return createUrl(serverAddress, "/pbm", port);
    }

    /**
     * Constructs a URI that should be used to configure {@link PbmPortType}.
     *
     * <p>Assumes the standard {@link SimpleHttpConfigurer#DEFAULT_PORT}.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @return the URI
     */
    public static URI createPbmUrl(String serverAddress) {
        return createUrl(serverAddress, "/pbm", DEFAULT_PORT);
    }

    /**
     * Constructs a URI that should be used to configure {@link SmsPortType}.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @param port remote server port (typically 443)
     * @return the URI
     */
    public static URI createSmsUrl(String serverAddress, int port) {
        return createUrl(serverAddress, "/sms/sdk", port);
    }

    /**
     * Constructs a URI that should be used to configure {@link SmsPortType}.
     *
     * <p>Assumes the standard {@link SimpleHttpConfigurer#DEFAULT_PORT}.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @return the URI
     */
    public static URI createSmsUrl(String serverAddress) {
        return createUrl(serverAddress, "/sms/sdk", DEFAULT_PORT);
    }

    /**
     * Constructs a URI that should be used to configure {@link VslmPortType}.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @param port remote server port (typically 443)
     * @return the URI
     */
    public static URI createVslmUrl(String serverAddress, int port) {
        return createUrl(serverAddress, "/vslm/sdk", port);
    }

    /**
     * Constructs a URI that should be used to configure {@link VslmPortType}.
     *
     * <p>Assumes the standard {@link SimpleHttpConfigurer#DEFAULT_PORT}.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @return the URI
     */
    public static URI createVslmUrl(String serverAddress) {
        return createUrl(serverAddress, "/vslm/sdk", DEFAULT_PORT);
    }

    /**
     * Constructs a URI that should be used to configure {@link VsanhealthPortType}.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @param port remote server port (typically 443)
     * @return the URI
     */
    public static URI createVsanVcenterUrl(String serverAddress, int port) {
        return createUrl(serverAddress, "/vsanHealth", port);
    }

    /**
     * Constructs a URI that should be used to configure {@link VsanhealthPortType}.
     *
     * <p>Assumes the standard {@link SimpleHttpConfigurer#DEFAULT_PORT}.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @return the URI
     */
    public static URI createVsanVcenterUrl(String serverAddress) {
        return createUrl(serverAddress, "/vsanHealth", DEFAULT_PORT);
    }
}
