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

import static com.vmware.vapi.core.ExecutionContext.SecurityContext.AUTHENTICATION_SCHEME_ID;
import static com.vmware.vapi.security.StdSecuritySchemes.SAML_BEARER_TOKEN;
import static com.vmware.vapi.security.StdSecuritySchemes.SAML_TOKEN;

import java.security.PrivateKey;
import java.util.Objects;

import com.vmware.vapi.core.ExecutionContext.SecurityContext;
import com.vmware.vapi.internal.util.Validate;
import com.vmware.vapi.saml.SamlToken;
import com.vmware.vapi.security.StdSecuritySchemes;

/**
 * This class represents a helper structure that contains all data that can be extracted from the
 * {@link SecurityContext} related to SAML authentication type.
 */
public final class SamlAuthenticationData {

    private final SamlToken token;
    private final String tokenXml;
    private final PrivateKey key;

    private SamlAuthenticationData(SamlToken token, PrivateKey key) {
        Objects.requireNonNull(token);
        this.token = token;
        this.key = key;
        this.tokenXml = token.toXml();
    }

    private SamlAuthenticationData(String tokenXml, PrivateKey key) {
        Objects.requireNonNull(tokenXml);
        this.token = null;
        this.key = key;
        this.tokenXml = tokenXml;
    }

    /**
     * @return the SAML token used for authentication. Cannot be null. Please use {@link #getSamlTokenXml()} to obtain
     *     the SAML token content for embedding in requests
     * @throws IllegalStateException when an instance has been initialized with the token XML text and has no
     *     {@link SamlToken} object
     */
    public SamlToken getSamlToken() {
        if (token == null) {
            throw new IllegalStateException("No parsed token data provided");
        }
        return token;
    }

    /** @return the SAML token az XML test used for authentication. Cannot be null. */
    public String getSamlTokenXml() {
        return tokenXml;
    }

    /** @return the private key used to sign the request. Can be null. */
    public PrivateKey getPrivateKey() {
        return key;
    }

    /**
     * Creates a new instance of this structure by parsing a {@link SecurityContext}.
     *
     * @param ctx the security context that will be parsed. Cannot be null. The authentication scheme
     *     ({@link SecurityContext#AUTHENTICATION_SCHEME_ID} should be {@link StdSecuritySchemes#SAML_TOKEN} or
     *     {@link StdSecuritySchemes#SAML_BEARER_TOKEN}.
     * @return the parsed context
     */
    public static final SamlAuthenticationData createInstance(SecurityContext ctx) {
        Validate.notNull(ctx);
        Validate.isTrue(ctx.getProperty(AUTHENTICATION_SCHEME_ID).equals(SAML_TOKEN)
                || ctx.getProperty(AUTHENTICATION_SCHEME_ID).equals(SAML_BEARER_TOKEN));

        Object tokenObject = ctx.getProperty(SamlTokenSecurityContext.SAML_TOKEN_ID);
        boolean isSamlToken = tokenObject instanceof SamlToken;
        boolean isString = tokenObject instanceof String;
        if (!isSamlToken && !isString) {
            throw new IllegalStateException("Incorrectly constructed security context");
        }

        Object privateKeyObject = ctx.getProperty(SamlTokenSecurityContext.PRIVATE_KEY_ID);
        if (privateKeyObject != null && !(privateKeyObject instanceof PrivateKey)) {
            throw new IllegalStateException("Unknown object under the private key id");
        }

        if (isSamlToken) {
            return new SamlAuthenticationData((SamlToken) tokenObject, (PrivateKey) privateKeyObject);
        } else {
            return new SamlAuthenticationData((String) tokenObject, (PrivateKey) privateKeyObject);
        }
    }
}
