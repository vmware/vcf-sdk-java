/*
 * ******************************************************************
 * Copyright (c) 2012-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.internal.cis.authn.json;

import java.security.cert.X509Certificate;
import java.util.Map;

import com.vmware.vapi.Message;
import com.vmware.vapi.MessageFactory;
import com.vmware.vapi.dsig.json.SignatureException;
import com.vmware.vapi.internal.security.SecurityContextConstants;
import com.vmware.vapi.internal.security.SecurityUtil;
import com.vmware.vapi.internal.util.Validate;
import com.vmware.vapi.saml.DefaultTokenFactory;
import com.vmware.vapi.saml.SamlToken;
import com.vmware.vapi.saml.exception.InvalidTokenException;

/** This class represents a JSON signature. */
public final class JsonSignatureStruct {

    private static final Message VERIFY_ERROR = MessageFactory.getMessage("vapi.signature.verify");
    private static final DefaultTokenFactory TOKEN_FACTORY = new DefaultTokenFactory();

    private final String sigValue;
    private final SamlToken samlToken;
    private final String alg;

    /**
     * Creates a new JSON signature structure
     *
     * @param sigValue the value of the signature over the signed data. should be base64 encoded. cannot be null.
     * @param samlToken the SAML token containing the certificate that match the private key used to sign the data.
     *     cannot be null.
     * @param alg the algorithm used for signing the data. cannot be null.
     */
    public JsonSignatureStruct(String sigValue, SamlToken samlToken, String alg) {
        Validate.notNull(sigValue);
        Validate.notNull(samlToken);
        Validate.notNull(alg);

        this.sigValue = sigValue;
        this.samlToken = samlToken;
        this.alg = alg;
    }

    /** @return the base64 encoded signature value. cannot be null. */
    public String getSigValue() {
        return sigValue;
    }

    /**
     * @return the SAML token that contains the certificate corresponding to the private key used to sign the data.
     *     cannot be null.
     */
    public SamlToken getSamlToken() {
        return samlToken;
    }

    /** @return the algorithm used to sign the data. cannot be null. */
    public String getAlg() {
        return alg;
    }

    /**
     * Parses a JSON signature structure string.
     *
     * @param jsonSignatureStruct cannot be null.
     * @param trustedRoots the trusted root certificates of the SSO server, used to sign the SAML token in the JSON
     *     signature structure. cannot be empty.
     * @param clockToleranceSec the allowed time discrepancy between the client and the server. must not be negative.
     * @return the parsed {@link JsonSignatureStruct}
     */
    public static JsonSignatureStruct parseJsonSignatureStruct(
            Map<String, Object> jsonSignatureStruct, X509Certificate[] trustedRoots, long clockToleranceSec) {

        Validate.notNull(jsonSignatureStruct);
        Validate.notEmpty(trustedRoots);
        Validate.isTrue(clockToleranceSec > -1);

        String sigValue =
                SecurityUtil.narrowType(jsonSignatureStruct.get(SecurityContextConstants.SIG_VALUE_KEY), String.class);
        String samlToken =
                SecurityUtil.narrowType(jsonSignatureStruct.get(SecurityContextConstants.SAML_TOKEN_KEY), String.class);
        String signingAlgorithm = SecurityUtil.narrowType(
                jsonSignatureStruct.get(SecurityContextConstants.SIGNATURE_ALGORITHM_KEY), String.class);

        if (sigValue != null && samlToken != null && signingAlgorithm != null) {
            SamlToken parsedToken;
            try {
                synchronized (TOKEN_FACTORY) {
                    parsedToken = TOKEN_FACTORY.parseToken(samlToken, trustedRoots, clockToleranceSec);
                }
            } catch (InvalidTokenException e) {
                throw new SignatureException(VERIFY_ERROR, e);
            }
            return new JsonSignatureStruct(sigValue, parsedToken, signingAlgorithm);
        }

        throw new SignatureException(VERIFY_ERROR);
    }
}
