/*
 * ******************************************************************
 * Copyright (c) 2012-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.cis.authn.json;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vapi.Message;
import com.vmware.vapi.MessageFactory;
import com.vmware.vapi.cis.authn.SamlAuthenticationData;
import com.vmware.vapi.cis.authn.SecurityContextFactory;
import com.vmware.vapi.core.ExecutionContext.SecurityContext;
import com.vmware.vapi.internal.cis.authn.Signer;
import com.vmware.vapi.internal.cis.authn.json.JsonSignatureAlgorithm;
import com.vmware.vapi.internal.cis.authn.json.JsonSignerImpl;
import com.vmware.vapi.internal.dsig.json.JsonCanonicalizer;
import com.vmware.vapi.internal.protocol.common.json.JsonSecurityContextSerializer;
import com.vmware.vapi.internal.security.SecurityContextConstants;
import com.vmware.vapi.internal.util.DateTimeConverter;
import com.vmware.vapi.internal.util.Validate;
import com.vmware.vapi.protocol.RequestProcessor;
import com.vmware.vapi.security.StdSecuritySchemes;

/** This implementation of {@link RequestProcessor} takes care for JSON request signatures */
public final class JsonSigningProcessor implements RequestProcessor {

    private static final String UTF8_CHARSET = "UTF-8";
    private static final Logger log = LoggerFactory.getLogger(JsonSigningProcessor.class);
    private static final Message DECODE_ERROR = MessageFactory.getMessage("vapi.sso.signproc.decoderequest");
    private final Signer jsonSigner;
    private final JsonSecurityContextSerializer scSerializer = new JsonSecurityContextSerializer();
    private final DateTimeConverter dateConverter = new DateTimeConverter();

    public JsonSigningProcessor() {
        jsonSigner = new JsonSignerImpl(new JsonCanonicalizer());
    }

    @Override
    public byte[] process(byte[] request, Map<String, Object> metadata, Request vapiRequest) {

        Validate.notNull(request);
        Validate.notNull(metadata);

        // TODO a more optimal usage of vapiRequest could be used here
        Object secCtx = metadata.get(RequestProcessor.SECURITY_CONTEXT_KEY);
        if (!shouldSignRequest(secCtx)) {
            return request;
        }

        return signRequest(request, (SecurityContext) secCtx);
    }

    /** @return true if the request should be signed, false otherwise */
    boolean shouldSignRequest(Object secCtx) {
        boolean shouldSign = false;

        if (secCtx != null && secCtx instanceof SecurityContext) {
            SecurityContext context = (SecurityContext) secCtx;
            if (StdSecuritySchemes.SAML_TOKEN.equals(context.getProperty(SecurityContext.AUTHENTICATION_SCHEME_ID))) {
                SamlAuthenticationData sc = SecurityContextFactory.parseSamlSecurityContext(context);
                shouldSign = sc.getPrivateKey() != null;
            }
        }

        return shouldSign;
    }

    /**
     * Signs the request and embeds the signature into it
     *
     * @param jsonRequest cannot be null
     * @param secCtx cannot be null
     * @return the signed request
     */
    private byte[] signRequest(byte[] jsonRequest, SecurityContext secCtx) {
        if (jsonRequest == null) {
            throw new IllegalArgumentException("jsonRequest is null");
        }
        if (secCtx == null) {
            throw new IllegalArgumentException("secCtx is null");
        }

        log.debug("Signing the request");

        // create the portion of the security context that should be signed
        Map<String, Object> secCtxMap = new HashMap<String, Object>();
        secCtxMap.put(SecurityContextConstants.SCHEME_ID_KEY, StdSecuritySchemes.SAML_TOKEN);
        secCtxMap.put(
                SecurityContextConstants.TIMESTAMP_KEY, createTimestamp(JsonSecurityContextSerializer.TS_DEF_OFFSET));
        // TODO signatureAlgorithm should not be hardcoded
        secCtxMap.put(
                SecurityContextConstants.SIGNATURE_ALGORITHM_KEY,
                JsonSignatureAlgorithm.getDefault().name());
        byte[] signRequest = scSerializer.serializeSecurityContext(secCtxMap, jsonRequest);

        SamlAuthenticationData sc = SecurityContextFactory.parseSamlSecurityContext(secCtx);
        String signature = jsonSigner.sign(signRequest, sc.getPrivateKey(), JsonSignatureAlgorithm.getDefault());

        // add the signature to the security context
        Map<String, String> scSigValue = new HashMap<String, String>();
        scSigValue.put(SecurityContextConstants.SIG_VALUE_KEY, signature);
        String xmlText = sc.getSamlTokenXml();
        scSigValue.put(SecurityContextConstants.SAML_TOKEN_KEY, xmlText);
        secCtxMap.put(SecurityContextConstants.SIGNATURE_KEY, scSigValue);
        return scSerializer.serializeSecurityContext(secCtxMap, jsonRequest);
    }

    /**
     * Creates a request timestamp
     *
     * @param offset the request validity window in minutes
     * @return the timestamp structure
     */
    private Map<String, String> createTimestamp(int offset) {
        // TODO is creating a calendar a slow operation?
        // TODO does the default timezone works?
        Calendar cal = Calendar.getInstance();
        Map<String, String> ts = new HashMap<String, String>();
        ts.put(SecurityContextConstants.TS_CREATED_KEY, dateConverter.toStringValue(cal));
        cal.add(Calendar.MINUTE, offset);
        ts.put(SecurityContextConstants.TS_EXPIRES_KEY, dateConverter.toStringValue(cal));
        return ts;
    }
}
