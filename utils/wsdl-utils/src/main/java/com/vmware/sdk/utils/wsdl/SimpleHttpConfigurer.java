/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.utils.wsdl;

import static org.apache.cxf.frontend.ClientProxy.getClient;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.TrustManager;

import jakarta.xml.ws.BindingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.https.httpclient.DefaultHostnameVerifier;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Port configurers are classes which configure the underlying HTTP client of a SOAP-based ({@link BindingProvider})
 * service.
 *
 * <p>This class is not portable across JAX-WS implementations. It contains Apache CXF-specific code.
 */
public class SimpleHttpConfigurer implements PortConfigurer {

    private static final Logger log = LoggerFactory.getLogger(SimpleHttpConfigurer.class);

    private static final String DEFAULT_USER_AGENT = createDefaultUserAgent();

    /** Default value for {@link #connectTimeout} if it's not explicitly specified. */
    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 30_000;

    /** Default value for {@link #readTimeout} if it's not explicitly specified. */
    public static final int DEFAULT_READ_TIMEOUT_MS = 60_000;

    /** The default port typically SOAP services can be found. */
    public static final int DEFAULT_PORT = 443;

    /** Default value {@link #hostnameVerifier} if not specified - performs strict validations. */
    public static final HostnameVerifier DEFAULT_HOSTNAME_VERIFIER = new DefaultHostnameVerifier();

    protected final int connectTimeout;
    protected final int readTimeout;
    protected final HostnameVerifier hostnameVerifier;
    protected final TrustManager[] trustManagers;

    /**
     * Calls {@link SimpleHttpConfigurer#SimpleHttpConfigurer(int, int, HostnameVerifier, TrustManager[])} using
     * defaults such as {@link #DEFAULT_CONNECT_TIMEOUT_MS}, {@link #DEFAULT_READ_TIMEOUT_MS}, and
     * {@link #DEFAULT_HOSTNAME_VERIFIER}.
     *
     * @param trustManager trust manager used during the TLS handshake to verify the server identity
     */
    public SimpleHttpConfigurer(TrustManager trustManager) {
        this(DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS, DEFAULT_HOSTNAME_VERIFIER, new TrustManager[] {
            trustManager
        });
    }

    /**
     * Calls {@link SimpleHttpConfigurer#SimpleHttpConfigurer(int, int, HostnameVerifier, TrustManager[])} using
     * defaults such as {@link #DEFAULT_CONNECT_TIMEOUT_MS} and {@link #DEFAULT_READ_TIMEOUT_MS}.
     *
     * @param hostnameVerifier hostname verifier used during TLS handshake to verify the server identity
     * @param trustManager trust manager used during the TLS handshake to verify the server identity
     */
    public SimpleHttpConfigurer(HostnameVerifier hostnameVerifier, TrustManager trustManager) {
        this(DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS, hostnameVerifier, new TrustManager[] {trustManager});
    }

    /**
     * @param connectTimeout how many milliseconds to wait when establishing the TCP connection
     * @param readTimeout how many milliseconds to wait when reading from the underlying socket
     * @param hostnameVerifier hostname verifier used during TLS handshake to verify the server identity
     * @param trustManagers trust managers used during the TLS handshake to verify the server identity
     */
    public SimpleHttpConfigurer(
            int connectTimeout, int readTimeout, HostnameVerifier hostnameVerifier, TrustManager[] trustManagers) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.hostnameVerifier = hostnameVerifier;
        this.trustManagers = trustManagers;
    }

    /**
     * Configures the given port with the already provided settings such as timeouts and TLS properties.
     *
     * @param provider the port to configure
     * @param url the address of the remote service, including its path
     */
    @Override
    public void configure(BindingProvider provider, URI url) {

        log.trace(
                "Configuring {} to use {}, connectTimeout={}, readTimeout={}",
                provider,
                url,
                connectTimeout,
                readTimeout);

        Map<String, Object> reqContext = provider.getRequestContext();
        reqContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url.toString());
        reqContext.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

        Client client = getClient(provider);
        Bus bus = client.getBus();

        List<Feature> features = new ArrayList<>(bus.getFeatures());

        boolean loggingFeatureEnabled = features.stream().anyMatch(f -> f instanceof LoggingFeature);

        if (!loggingFeatureEnabled) {
            LoggingFeature loggingFeature = new LoggingFeature();
            loggingFeature.setPrettyLogging(true);

            features.add(loggingFeature);
            bus.setFeatures(features);
        }

        // Disable registration of org.apache.cxf.jaxb.io.DataReaderImpl.WSUIDValidationHandler
        // instance to UnmarshallerImpl.setEventHandler(ValidationEventHandler).
        //
        // This is done for a bug patch according to the source code comments, but has the side
        // effect of changing the UnmarshallerImpl behavior in face of mismatches of the XML
        // elements names received in the SOAP response and what is expected according to the
        // WSDL file.
        //
        // The new behavior is to throw an exception and cut processing the SOAP response, while the
        // old (and preferred behavior) is to just skip the unknown XML element and unmarshal the
        // rest of the response. This is how the vSAN Management SDK behaved in 8.0.3 and previous
        // releases, based on the JAX-WS RI 2.x stack there.
        bus.setProperty("set-jaxb-validation-event-handler", Boolean.FALSE);

        HTTPConduit http = (HTTPConduit) client.getConduit();

        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setConnectionTimeout(connectTimeout);
        httpClientPolicy.setReceiveTimeout(readTimeout);
        httpClientPolicy.setBrowserType(DEFAULT_USER_AGENT);
        http.setClient(httpClientPolicy);

        TLSClientParameters tlsClientParameters = new TLSClientParameters();
        tlsClientParameters.setHostnameVerifier(hostnameVerifier);
        tlsClientParameters.setTrustManagers(trustManagers);
        http.setTlsClientParameters(tlsClientParameters);
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public TrustManager[] getTrustManagers() {
        return trustManagers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SimpleHttpConfigurer that = (SimpleHttpConfigurer) o;
        return connectTimeout == that.connectTimeout
                && readTimeout == that.readTimeout
                && Objects.equals(hostnameVerifier, that.hostnameVerifier)
                && Arrays.equals(trustManagers, that.trustManagers);
    }

    @Override
    public int hashCode() {
        int result = connectTimeout;
        result = 31 * result + readTimeout;
        result = 31 * result + Objects.hashCode(hostnameVerifier);
        result = 31 * result + Arrays.hashCode(trustManagers);
        return result;
    }

    @Override
    public String toString() {
        return "PortConfigurer{" + "connectTimeout="
                + connectTimeout + ", readTimeout="
                + readTimeout + ", hostnameVerifier="
                + hostnameVerifier + ", trustManagers="
                + Arrays.toString(trustManagers) + '}';
    }

    /**
     * Produces a User-Agent string that looks like this:
     *
     * <pre>SDK/9.0.0.0 Apache-CXF/4.0.5 Java/11.0.24+8 (Linux; 6.10.8-arch1-1; amd64)</pre>
     *
     * <p>The SDK version is read from MANIFEST.MF.
     *
     * <p>The CXF version is taken from {@link Version#getCurrentVersion()}.
     */
    private static String createDefaultUserAgent() {
        StringBuilder sb = new StringBuilder();

        String sdkVersion = "<unknown version>";
        InputStream is = SimpleHttpConfigurer.class.getResourceAsStream("/vcf_sdk_version.properties");
        if (is != null) {
            try (is) {
                Properties props = new Properties();
                props.load(is);
                sdkVersion = props.getProperty("version");
            } catch (Exception e) {
                log.trace("Could not determine the SDK version", e);
            }
        }

        sb.append("SDK/");
        sb.append(sdkVersion);

        sb.append(" ");
        sb.append("Apache-CXF/");
        sb.append(Version.getCurrentVersion());

        sb.append(" ");
        sb.append("Java/");
        sb.append(Runtime.version());

        sb.append(" (");
        sb.append(System.getProperty("os.name"));
        sb.append(";");
        sb.append(" ");
        sb.append(System.getProperty("os.version"));
        sb.append(";");
        sb.append(" ");
        sb.append(System.getProperty("os.arch"));
        sb.append(")");

        return sb.toString();
    }
}
