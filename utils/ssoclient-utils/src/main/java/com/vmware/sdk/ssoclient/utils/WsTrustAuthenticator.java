/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.ssoclient.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.xml.ws.BindingProvider;

import org.oasis_open.docs.ws_sx.ws_trust._200512.LifetimeType;
import org.oasis_open.docs.ws_sx.ws_trust._200512.RenewTargetType;
import org.oasis_open.docs.ws_sx.ws_trust._200512.RenewingType;
import org.oasis_open.docs.ws_sx.ws_trust._200512.RequestSecurityTokenResponseType;
import org.oasis_open.docs.ws_sx.ws_trust._200512.RequestSecurityTokenType;
import org.oasis_open.docs.ws_sx.ws_trust._200512.StatusType;
import org.oasis_open.docs.ws_sx.ws_trust._200512.ValidateTargetType;
import org.oasis_open.docs.ws_sx.ws_trust._200512.wsdl.STSService;
import org.oasis_open.docs.ws_sx.ws_trust._200512.wsdl.STSServicePortType;
import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_utility_1_0.AttributedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.vmware.sdk.ssoclient.utils.soaphandlers.HeaderHandlerResolver;
import com.vmware.sdk.ssoclient.utils.soaphandlers.SamlTokenExtractionHandler;
import com.vmware.sdk.ssoclient.utils.soaphandlers.SamlTokenHandler;
import com.vmware.sdk.ssoclient.utils.soaphandlers.TimeStampHandler;
import com.vmware.sdk.ssoclient.utils.soaphandlers.UserCredentialHandler;
import com.vmware.sdk.ssoclient.utils.soaphandlers.WsSecuritySignatureAssertionHandler;
import com.vmware.sdk.ssoclient.utils.soaphandlers.WsSecurityUserCertificateSignatureHandler;
import com.vmware.sdk.utils.wsdl.PortConfigurer;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;

/** This class implements various authentication methods provided by the STS Service. */
public class WsTrustAuthenticator {

    public static final String URN_OASIS_NAMES_TC_SAML_2_0_ASSERTION = "urn:oasis:names:tc:SAML:2.0:assertion";
    public static final String RENEW_REQUEST_TYPE = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Renew";
    public static final String BEARER_REQUEST_TYPE = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer";
    public static final String STATUS_TOKEN_TYPE = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTR/Status";
    public static final String VALIDATE_REQUEST_TYPE = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Validate";
    public static final String VALID_TOKEN_STATUS = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/status/valid";
    public static final String ISSUE_REQUEST_TYPE = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue";
    public static final String PUBLIC_KEY_TYPE = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey";
    public static final String SIGNATURE_ALGORITHM = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    public static final Duration DEFAULT_TOKEN_LIFETIME = Duration.ofMinutes(30);
    private static final Logger log = LoggerFactory.getLogger(WsTrustAuthenticator.class);

    /**
     * Acquires a SAML Holder-of-Key token using an SSO user, password and the user's private key and certificate.
     *
     * <p>To generate keypair, the jdk-provided keytool can be used. For example:
     *
     * <blockquote>
     *
     * <pre>
     *     keytool -genkey -keysize 4096 -keyalg RSA -alias someAlias -keystore keystore.p12 -storepass somepass -validity 365 -dname 'CN=example.com, OU=ExampleUnit, O=ExampleOrg'
     * </pre>
     *
     * </blockquote>
     *
     * @param serverAddress vCenter FQDN or IP address
     * @param port the port of the remote server (typically 443)
     * @param portConfigurer configurer used to configure {@link STSServicePortType} instances
     * @param username the SSO username who's being authenticated
     * @param password the password of the username who's being authenticated
     * @param key the private key of the user
     * @param certificate the public certificate of the user
     * @param tokenLifetime how long should the token be valid; {@link #DEFAULT_TOKEN_LIFETIME} will be used if omitted
     * @return A Holder-Of-Key token
     */
    public static Element acquireHokTokenWithUserCredentials(
            String serverAddress,
            int port,
            PortConfigurer portConfigurer,
            String username,
            String password,
            PrivateKey key,
            X509Certificate certificate,
            Duration tokenLifetime) {

        log.debug("Acquiring HoK token for {} from {}", username, serverAddress);

        HeaderHandlerResolver headerResolver = createHeaderResolver();

        /*
         * For this specific case we need the following header elements wrapped
         * in the security tag.
         *
         * 1. Timestamp containing the request's creation and expiry time
         *
         * 2. UsernameToken containing the username/password
         *
         * Once the above headers are added we need to sign the SOAP message
         * using the private key. The certificate is embedded when sending the issue() request.
         */

        headerResolver.addHandler(new UserCredentialHandler(username, password));
        headerResolver.addHandler(new WsSecurityUserCertificateSignatureHandler(key, certificate));
        SamlTokenExtractionHandler sbHandler = new SamlTokenExtractionHandler();
        headerResolver.addHandler(sbHandler);

        STSService stsService = createSTSService(headerResolver);

        STSServicePortType stsPort = stsService.getSTSServicePort();
        portConfigurer.configure((BindingProvider) stsPort, createStsUrl(serverAddress, port));

        RequestSecurityTokenType tokenRequest =
                createHoKSecurityTokenRequest(tokenLifetime, true, createRenewingType(true));
        stsPort.issue(tokenRequest);

        return sbHandler.getToken();
    }

    /**
     * Acquires a SAML Holder-of-Key token using an existing SAML token and the user's private key and certificate.
     *
     * <p>To generate keypair, the jdk-provided keytool can be used. For example:
     *
     * <blockquote>
     *
     * <pre>
     *     keytool -genkey -keysize 4096 -keyalg RSA -alias someAlias -keystore keystore.p12 -storepass somepass -validity 365 -dname 'CN=example.com, OU=ExampleUnit, O=ExampleOrg'
     * </pre>
     *
     * </blockquote>
     *
     * @param serverAddress vCenter FQDN or IP address
     * @param port the port of the remote server (typically 443)
     * @param portConfigurer configurer used to configure the port on the correct URL
     * @param token an already existing HoK token
     * @param key the private of the user
     * @param certificate the certificate of the user
     * @param tokenLifetime how long should the token be valid; {@link #DEFAULT_TOKEN_LIFETIME} will be used if omitted
     * @return A Holder-Of-Key token
     */
    public static Element acquireHokToken(
            String serverAddress,
            int port,
            PortConfigurer portConfigurer,
            Element token,
            PrivateKey key,
            X509Certificate certificate,
            Duration tokenLifetime) {

        log.debug("Acquiring a new HoK token using an already existing token from {}", serverAddress);

        HeaderHandlerResolver headerResolver = createHeaderResolver();

        /*
         * For this specific case we need the following header elements wrapped
         * in the security tag.
         *
         * 1. Timestamp containing the request's creation and expiry time
         *
         * 2. Holder-Of-Key token to be used for issuing the new token
         *
         * Once the above headers are added we need to sign the SOAP message
         * using the combination of private key, certificate of the user or
         * solution and the Holder-Of-Key token by adding a Signature element to
         * the security header
         */

        headerResolver.addHandler(new SamlTokenHandler(token));
        SamlTokenExtractionHandler sbHandler = new SamlTokenExtractionHandler();
        headerResolver.addHandler(sbHandler);
        headerResolver.addHandler(
                new WsSecuritySignatureAssertionHandler(key, certificate, SoapUtils.getNodeProperty(token, "ID")));

        STSService stsService = createSTSService(headerResolver);

        STSServicePortType stsPort = stsService.getSTSServicePort();
        portConfigurer.configure((BindingProvider) stsPort, createStsUrl(serverAddress, port));

        RequestSecurityTokenType tokenType =
                createHoKSecurityTokenRequest(tokenLifetime, true, createRenewingType(true));

        stsPort.issue(tokenType);

        return sbHandler.getToken();
    }

    /**
     * Renews an SAML existing token and returns the newly issued new token.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @param port the port of the remote server (typically 443)
     * @param portConfigurer configurer used to configure {@link STSServicePortType} instances
     * @param token the already existing HoK token
     * @param key the private key of the user
     * @param certificate the certificate of the user
     * @param tokenLifetime how long should the new token be valid
     * @return The newly issued Holder-Of-Key token
     */
    public static Element renewToken(
            String serverAddress,
            int port,
            PortConfigurer portConfigurer,
            Element token,
            PrivateKey key,
            X509Certificate certificate,
            Duration tokenLifetime) {

        log.debug("Renewing an existing HoK token from {}", serverAddress);

        HeaderHandlerResolver headerResolver = createHeaderResolver();

        /*
         * For this specific case we need the following header elements wrapped
         * in the security tag.
         *
         * 1. Timestamp containing the request's creation and expiry time
         *
         * Once the above headers are added we need to sign the SOAP message
         * using the combination of private key, certificate of the user or
         * solution by adding a Signature element to the security header
         */

        headerResolver.addHandler(new WsSecurityUserCertificateSignatureHandler(key, certificate));
        SamlTokenExtractionHandler sbHandler = new SamlTokenExtractionHandler();
        headerResolver.addHandler(sbHandler);

        STSService stsService = createSTSService(headerResolver);

        STSServicePortType stsPort = stsService.getSTSServicePort();
        portConfigurer.configure((BindingProvider) stsPort, createStsUrl(serverAddress, port));

        RequestSecurityTokenType tokenType = new RequestSecurityTokenType();

        /*
         * For this request we need at least the following element in the
         * RequestSecurityTokenType set
         *
         * 1. Lifetime - represented by LifetimeType which specifies the
         * lifetime for the token to be issued. In this case this will represent
         * the extended validity period for the token after renewal
         *
         * 2. Tokentype - "urn:oasis:names:tc:SAML:2.0:assertion", which is the
         * class that models the requested token
         *
         * 3. RequestType -
         * "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Renew", as we want
         * to get a token renewed
         *
         * 4. RenewTarget - represented by RenewTargetType which contains the
         * Holder-Of-Key SAML token to be renewed
         */
        tokenType.setLifetime(createLifetime(tokenLifetime));
        tokenType.setTokenType(URN_OASIS_NAMES_TC_SAML_2_0_ASSERTION);
        tokenType.setRequestType(RENEW_REQUEST_TYPE);

        RenewTargetType renewTarget = new RenewTargetType();
        renewTarget.setAny(token);
        tokenType.setRenewTarget(renewTarget);

        stsPort.renew(tokenType);

        return sbHandler.getToken();
    }

    /**
     * Acquires a SAML Bearer token using the provided SSO username and password.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @param port the port of the remote server (typically 443)
     * @param portConfigurer configurer used to configure {@link STSServicePortType} instances
     * @param username the SSO username who's being authenticated
     * @param password the password of the username who's being authenticated
     * @param tokenLifetime how long should the token be valid; {@link #DEFAULT_TOKEN_LIFETIME} will be used if omitted
     * @return A Holder-Of-Key token
     */
    public static Element acquireBearerTokenForRegularUser(
            String serverAddress,
            int port,
            PortConfigurer portConfigurer,
            String username,
            String password,
            Duration tokenLifetime) {

        log.debug("Acquiring Bearer token for {} from {}", username, serverAddress);

        HeaderHandlerResolver headerResolver = createHeaderResolver();

        /*
         * For this specific case we need the following header elements wrapped
         * in the security tag.
         *
         * 1. Timestamp containing the request's creation and expiry time
         *
         * 2. UsernameToken containing the username/password
         */

        headerResolver.addHandler(new UserCredentialHandler(username, password));
        SamlTokenExtractionHandler sbHandler = new SamlTokenExtractionHandler();
        headerResolver.addHandler(sbHandler);

        STSService stsService = createSTSService(headerResolver);

        STSServicePortType stsPort = stsService.getSTSServicePort();
        portConfigurer.configure((BindingProvider) stsPort, createStsUrl(serverAddress, port));

        RequestSecurityTokenType tokenType =
                createHoKSecurityTokenRequest(tokenLifetime, true, createRenewingType(false));

        // override key type because createHoKSecurityTokenRequest is generally used for HoK
        tokenType.setKeyType(BEARER_REQUEST_TYPE);

        stsPort.issue(tokenType);

        return sbHandler.getToken();
    }

    /**
     * Performs an "online" token validation by calling {@link STSServicePortType#validate(RequestSecurityTokenType)} to
     * determine whether the given token is valid or not.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @param port the port of the remote server (typically 443)
     * @param stsPortConfigurer configurer used to configure {@link STSServicePortType} instances
     * @param token An existing token to be verified
     * @return true is valid, false otherwise
     */
    public static boolean isTokenValid(
            String serverAddress, int port, PortConfigurer stsPortConfigurer, Element token) {

        HeaderHandlerResolver headerResolver = createHeaderResolver();

        STSService stsService = createSTSService(headerResolver);

        STSServicePortType stsPort = stsService.getSTSServicePort();
        stsPortConfigurer.configure((BindingProvider) stsPort, createStsUrl(serverAddress, port));

        RequestSecurityTokenType tokenType = new RequestSecurityTokenType();

        /*
         * For this request we need at least the following element in the
         * RequestSecurityTokenType set
         *
         * 1. Tokentype -
         * "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTR/Status", which
         * is the class that models token status
         *
         * 2. RequestType -
         * "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Validate", as we
         * want to get a token validated
         *
         * 3. ValidateTarget - represented by ValidateTargetType which contains
         * the SAML token to be validated
         */
        tokenType.setTokenType(STATUS_TOKEN_TYPE);
        tokenType.setRequestType(VALIDATE_REQUEST_TYPE);

        ValidateTargetType value = new ValidateTargetType();

        value.setAny(token);
        tokenType.setValidateTarget(value);

        /*
         * Invoke the "validate" method on the STSService object to validate the
         * token from SSO Server
         */
        RequestSecurityTokenResponseType statusResponse = stsPort.validate(tokenType);

        /* handle the response - extract the SAML token status */
        StatusType rstResponse = statusResponse.getStatus();

        /*
         * There are only two possible values for the status code
         * "http://docs.oasis-open.org/ws-sx/ws-trust/200512/status/valid" for
         * valid token
         * "http://docs.oasis-open.org/ws-sx/ws-trust/200512/status/invalid" for
         * invalid token
         */
        String tokenStatus = rstResponse.getCode();
        return tokenStatus.equalsIgnoreCase(VALID_TOKEN_STATUS);
    }

    /**
     * Constructs a URL that should be used to configure {@link STSServicePortType}.
     *
     * <p>Assumes the standard {@link SimpleHttpConfigurer#DEFAULT_PORT}.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @return the URL
     */
    public static URI createStsUrl(String serverAddress) {
        return createStsUrl(serverAddress, SimpleHttpConfigurer.DEFAULT_PORT);
    }

    /**
     * Constructs a URL that should be used to configure {@link STSServicePortType}.
     *
     * @param serverAddress vCenter FQDN or IP address
     * @param port remote server port (typically 443)
     * @return the URL
     */
    public static URI createStsUrl(String serverAddress, int port) {
        try {
            return new URI("https", null, serverAddress, port, "/sts/STSService", null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static RequestSecurityTokenType createHoKSecurityTokenRequest(
            Duration tokenLifetime, boolean delegatable, RenewingType renewingType) {
        /*
         * Construct the SOAP body for the request. RequestSecurityTokenType is
         * the parameter type that is passed to the "acquire" method. However,
         * based on what kind of token (bearer or holder-of-key type) and by
         * what means (aka username/password, certificate, or existing token) we
         * want to acquire the token, different elements need to be populated
         */
        RequestSecurityTokenType tokenType = new RequestSecurityTokenType();

        /*
         * For this request we need at least the following element in the
         * RequestSecurityTokenType set
         *
         * 1. Lifetime - represented by LifetimeType which specifies the
         * lifetime for the token to be issued
         *
         * 2. Tokentype - "urn:oasis:names:tc:SAML:2.0:assertion", which is the
         * class that models the requested token
         *
         * 3. RequestType -
         * "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue", as we want
         * to get a token issued
         *
         * 4. KeyType -
         * "http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey",
         * representing the holder-of-key kind of key the token will have. There
         * are two options namely bearer and holder-of-key
         *
         * 5. SignatureAlgorithm -
         * "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", representing the
         * algorithm used for generating signature
         *
         * 6. Renewing - represented by the RenewingType which specifies whether
         * the token is renewable or not
         */
        tokenType.setTokenType(URN_OASIS_NAMES_TC_SAML_2_0_ASSERTION);
        tokenType.setRequestType(ISSUE_REQUEST_TYPE);
        tokenType.setLifetime(createLifetime(tokenLifetime));
        tokenType.setKeyType(PUBLIC_KEY_TYPE);
        tokenType.setSignatureAlgorithm(SIGNATURE_ALGORITHM);
        tokenType.setDelegatable(delegatable);
        tokenType.setRenewing(renewingType);

        return tokenType;
    }

    private static LifetimeType createLifetime(Duration tokenLifetime) {
        LifetimeType lifetime = new LifetimeType();

        DatatypeFactory dtFactory;
        try {
            dtFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        XMLGregorianCalendar xmlCalendar = dtFactory.newXMLGregorianCalendar(cal);
        AttributedDateTime created = new AttributedDateTime();
        created.setValue(xmlCalendar.toXMLFormat());

        AttributedDateTime expires = new AttributedDateTime();
        if (tokenLifetime == null) {
            tokenLifetime = DEFAULT_TOKEN_LIFETIME;
        }
        xmlCalendar.add(dtFactory.newDuration(tokenLifetime.toMillis()));
        expires.setValue(xmlCalendar.toXMLFormat());

        lifetime.setCreated(created);
        lifetime.setExpires(expires);

        return lifetime;
    }

    private static RenewingType createRenewingType(boolean allow) {
        RenewingType renewing = new RenewingType();
        renewing.setAllow(allow);
        renewing.setOK(false); // WS-Trust Profile: MUST be set to false

        return renewing;
    }

    private static HeaderHandlerResolver createHeaderResolver() {
        /*
         * Instantiating the HeaderHandlerResolver. This is required to provide
         * the capability of modifying the SOAP headers and the SOAP message in
         * general for various requests via the different handlers. For
         * different kinds of requests to SSO server one needs to follow the
         * WS-Trust guidelines to provide the required SOAP message structure.
         */
        HeaderHandlerResolver headerResolver = new HeaderHandlerResolver();

        headerResolver.addHandler(new TimeStampHandler());

        return headerResolver;
    }

    private static STSService createSTSService(HeaderHandlerResolver resolver) {
        STSService stsService = new STSService();

        stsService.setHandlerResolver(resolver);

        return stsService;
    }
}
