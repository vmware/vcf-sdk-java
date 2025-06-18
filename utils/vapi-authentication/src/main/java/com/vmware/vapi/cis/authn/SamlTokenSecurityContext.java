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
import java.util.HashMap;
import java.util.Map;

import com.vmware.vapi.core.ExecutionContext.SecurityContext;
import com.vmware.vapi.internal.util.Validate;
import com.vmware.vapi.saml.ConfirmationType;
import com.vmware.vapi.saml.SamlToken;
import com.vmware.vapi.security.StdSecuritySchemes;

/** This class represents the security context needed for authentication using SAML tokens. */
public final class SamlTokenSecurityContext implements SecurityContext {

    public static final String SAML_TOKEN_ID = "samlToken";
    public static final String PRIVATE_KEY_ID = "privateKey";

    private final Map<String, Object> props = new HashMap<>();

    /**
     * @param token cannot be {@code null}. will be serialized as part of the signature
     * @param privateKey if {@code null} request payload signature will not be generated. The private key will NOT be
     *     serialized in the request.
     */
    public SamlTokenSecurityContext(SamlToken token, PrivateKey privateKey) {
        Validate.notNull(token);
        if (token.getConfirmationType() == ConfirmationType.HOLDER_OF_KEY) {
            Validate.notNull(privateKey);
        }

        props.put(SAML_TOKEN_ID, token);
        props.put(PRIVATE_KEY_ID, privateKey);
        props.put(
                SecurityContext.AUTHENTICATION_SCHEME_ID,
                (privateKey != null) ? StdSecuritySchemes.SAML_TOKEN : StdSecuritySchemes.SAML_BEARER_TOKEN);
    }

    /**
     * Creates a SAML token security context from text. The text will be embedded in the request and will not be checked
     * if it represents valid SAML token with suitable attributes.
     *
     * @param token cannot be {@code null}. will be serialized as part of the signature.
     * @param privateKey if {@code null} request payload signature will not be generated. The private key will NOT be
     *     serialized in the request.
     */
    public SamlTokenSecurityContext(String token, PrivateKey privateKey) {
        Validate.notNull(token);

        props.put(SAML_TOKEN_ID, token);
        props.put(PRIVATE_KEY_ID, privateKey);
        props.put(
                SecurityContext.AUTHENTICATION_SCHEME_ID,
                (privateKey == null) ? StdSecuritySchemes.SAML_BEARER_TOKEN : StdSecuritySchemes.SAML_TOKEN);
    }

    /**
     * Internal. This constructor is used to transfer request authentication data to the service implementation.
     *
     * @param token cannot be {@code null}.
     */
    public SamlTokenSecurityContext(SamlToken token) {
        Validate.notNull(token);

        props.put(SAML_TOKEN_ID, token);
        boolean isHok = token.getConfirmationType() == ConfirmationType.HOLDER_OF_KEY;
        props.put(
                SecurityContext.AUTHENTICATION_SCHEME_ID,
                isHok ? StdSecuritySchemes.SAML_TOKEN : StdSecuritySchemes.SAML_BEARER_TOKEN);
    }

    @Override
    public Object getProperty(String key) {
        Validate.notNull(key);

        return props.get(key);
    }

    @Override
    public Map<String, Object> getAllProperties() {
        return Collections.unmodifiableMap(props);
    }
}
