/*
 * ******************************************************************
 * Copyright (c) 2012-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.cis.authn;

import java.security.PrivateKey;
import java.util.Collections;
import java.util.Map;

import com.vmware.vapi.core.ExecutionContext.SecurityContext;
import com.vmware.vapi.saml.SamlToken;
import com.vmware.vapi.security.OAuthSecurityContext;
import com.vmware.vapi.security.SessionSecurityContext;
import com.vmware.vapi.security.StdSecuritySchemes;
import com.vmware.vapi.security.UserPassSecurityContext;

/**
 * This factory class have methods for creating and parsing {@link SecurityContext} for all known authentication
 * schemas.
 */
// TODO: this should go out of this package
// TODO: should provide extensibility for 3rd parties to add new
//       factory methods (for new authn scheme contexts)
public final class SecurityContextFactory {

    /**
     * Creates a SAML token security context.
     *
     * @param token the token that should be used for authentication
     * @param privateKey the private key that should be used to sign the request
     * @return the security context that describes the requested authn
     */
    public static SecurityContext createSamlSecurityContext(SamlToken token, PrivateKey privateKey) {
        return new SamlTokenSecurityContext(token, privateKey);
    }

    /**
     * Creates a SAML token security context.
     *
     * @param token XML text of the token that should be used for authentication
     * @param privateKey the private key that should be used to sign the request
     * @return the security context that describes the requested authn
     */
    public static SecurityContext createSamlSecurityCtx(String token, PrivateKey privateKey) {
        return new SamlTokenSecurityContext(token, privateKey);
    }

    /**
     * Creates a security context which represents and attempt to reuse an existing session. This is only usable with
     * session-aware services. Such attempt would succeed only if the client has already been authenticated by the
     * service and has a valid session with it.
     *
     * @param sessionId String representing a session ID
     * @return the created security context
     */
    public static SecurityContext createSessionSecurityContext(char[] sessionId) {
        return new SessionSecurityContext(sessionId);
    }

    /**
     * Creates a security context based on OAuth2 access token
     *
     * @param accessToken access token that should be used for authentication
     * @return the security context that describes the requested authentication
     */
    public static SecurityContext createOAuthSecurityContext(char[] accessToken) {
        return new OAuthSecurityContext(accessToken);
    }

    /**
     * Creates a security context suitable for user/pass authentication.
     *
     * @param user the username of the principal. cannot be null.
     * @param password the password of the principal. cannot be null.
     * @return the created security context
     */
    public static SecurityContext createUserPassSecurityContext(String user, char[] password) {
        return new UserPassSecurityContext(user, password);
    }

    /**
     * Parses a {@link SecurityContext} into a {@link UserPassSecurityContext} if all needed data is there, otherwise
     * null will be returned.
     *
     * @param ctx cannot be null
     * @return the parsed security context
     */
    public static UserPassSecurityContext parseUserPassSecurityContext(SecurityContext ctx) {
        return UserPassSecurityContext.getInstance(ctx);
    }

    /**
     * Parses a given {@link SecurityContext} into a more convenient for use structure
     *
     * @param ctx the security context that will be parsed. Cannot be null. The authentication scheme
     *     ({@link SecurityContext#AUTHENTICATION_SCHEME_ID} should be {@link StdSecuritySchemes#SAML_TOKEN} or
     *     {@link StdSecuritySchemes#SAML_BEARER_TOKEN}.
     * @return the parsed security context
     */
    public static SamlAuthenticationData parseSamlSecurityContext(SecurityContext ctx) {
        return SamlAuthenticationData.createInstance(ctx);
    }

    /**
     * Parses the {@link SecurityContext} looking for session information.
     *
     * @param ctx the security context that will be parsed. Cannot be null.
     * @return {@link SessionSecurityContext} if session is found, <code>null</code> otherwise
     */
    public static SessionSecurityContext parseSessionSecurityContext(SecurityContext ctx) {
        return SessionSecurityContext.newInstance(ctx);
    }

    /**
     * Parses the {@link SecurityContext} looking for oauth information.
     *
     * @param ctx the security context that will be parsed. Cannot be null.
     * @return {@link OAuthSecurityContext} if access token is found, <code>null</code> otherwise
     */
    public static OAuthSecurityContext parseOAuthSecurityContext(SecurityContext ctx) {
        return OAuthSecurityContext.newInstance(ctx);
    }

    /**
     * Creates security context using the provided map of properties
     *
     * @param props contains the security context properties. can be <code>null</code>
     * @return {@link SecurityContext} containing the properties passed as parameter. must not be <code>null</code>
     */
    public static SecurityContext createDefaultSecurityContext(Map<String, Object> props) {
        final Map<String, Object> scProps =
                (props != null) ? Collections.unmodifiableMap(props) : Collections.<String, Object>emptyMap();
        return new SecurityContext() {

            @Override
            public Object getProperty(String key) {
                return scProps.get(key);
            }

            @Override
            public Map<String, Object> getAllProperties() {
                return scProps;
            }
        };
    }
}
