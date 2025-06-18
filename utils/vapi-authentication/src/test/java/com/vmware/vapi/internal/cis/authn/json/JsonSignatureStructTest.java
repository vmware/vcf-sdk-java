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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.vmware.vapi.cis.authn.json.SamlTokenUtil;
import com.vmware.vapi.dsig.json.SignatureException;
import com.vmware.vapi.internal.dsig.json.Verifier;
import com.vmware.vapi.internal.security.SecurityContextConstants;
import com.vmware.vapi.saml.SamlToken;
import com.vmware.vapi.saml.exception.InvalidTokenException;

public class JsonSignatureStructTest {

    private static String SAMPLE_SIG_VALUE;
    private static SamlToken SAMPLE_TOKEN;

    @BeforeAll
    public static void testSetup() throws InvalidTokenException, IOException, URISyntaxException {
        SAMPLE_TOKEN = SamlTokenUtil.loadSampleToken();
        SAMPLE_SIG_VALUE = SamlTokenUtil.readFromFile("/sample-signature.data");
    }

    @Test
    public void testParsingValidSignature() {
        JsonSignatureStruct sig = JsonSignatureStruct.parseJsonSignatureStruct(
                getSignature(getValidAlg(), SAMPLE_SIG_VALUE, SAMPLE_TOKEN.toXml()),
                SamlTokenUtil.getTrustedRoots(),
                Verifier.DEFAULT_CLOCK_TOLERANCE_SEC);
        validateDefaultSigValues(sig);
    }

    @Test
    public void testInvalidAlg() {
        assertThrows(SignatureException.class, () -> {
            JsonSignatureStruct.parseJsonSignatureStruct(
                    getSignature(null, SAMPLE_SIG_VALUE, SAMPLE_TOKEN.toXml()),
                    SamlTokenUtil.getTrustedRoots(),
                    Verifier.DEFAULT_CLOCK_TOLERANCE_SEC);
        });
    }

    @Test
    public void testInvalidSigValue() {
        assertThrows(SignatureException.class, () -> {
            JsonSignatureStruct.parseJsonSignatureStruct(
                    getSignature(getValidAlg(), null, SAMPLE_TOKEN.toXml()),
                    SamlTokenUtil.getTrustedRoots(),
                    Verifier.DEFAULT_CLOCK_TOLERANCE_SEC);
        });
    }

    @Test
    public void testNullSamlToken() {
        assertThrows(SignatureException.class, () -> {
            JsonSignatureStruct.parseJsonSignatureStruct(
                    getSignature(getValidAlg(), SAMPLE_SIG_VALUE, null),
                    SamlTokenUtil.getTrustedRoots(),
                    Verifier.DEFAULT_CLOCK_TOLERANCE_SEC);
        });
    }

    @Test
    public void testInvalidSamlToken() {
        assertThrows(SignatureException.class, () -> {
            JsonSignatureStruct.parseJsonSignatureStruct(
                    getSignature(
                            getValidAlg(),
                            SAMPLE_SIG_VALUE,
                            SAMPLE_TOKEN.toXml().substring(4)),
                    SamlTokenUtil.getTrustedRoots(),
                    Verifier.DEFAULT_CLOCK_TOLERANCE_SEC);
        });
    }

    @Test
    public void testInvalidSamlTokenObject() {
        assertThrows(SignatureException.class, () -> {
            JsonSignatureStruct.parseJsonSignatureStruct(
                    getSignature(getValidAlg(), SAMPLE_SIG_VALUE, SAMPLE_TOKEN),
                    SamlTokenUtil.getTrustedRoots(),
                    Verifier.DEFAULT_CLOCK_TOLERANCE_SEC);
        });
    }

    @Test
    public void testInvalidAlgObject() {
        assertThrows(SignatureException.class, () -> {
            JsonSignatureStruct.parseJsonSignatureStruct(
                    getSignature(new Object(), SAMPLE_SIG_VALUE, SAMPLE_TOKEN.toXml()),
                    SamlTokenUtil.getTrustedRoots(),
                    Verifier.DEFAULT_CLOCK_TOLERANCE_SEC);
        });
    }

    @Test
    public void testInvalidSigObject() {
        assertThrows(SignatureException.class, () -> {
            JsonSignatureStruct.parseJsonSignatureStruct(
                    getSignature(getValidAlg(), new Object(), SAMPLE_TOKEN.toXml()),
                    SamlTokenUtil.getTrustedRoots(),
                    Verifier.DEFAULT_CLOCK_TOLERANCE_SEC);
        });
    }

    @Test
    public void testInvalidClockTolerance() {
        assertThrows(IllegalArgumentException.class, () -> {
            JsonSignatureStruct.parseJsonSignatureStruct(
                    getSignature(getValidAlg(), SAMPLE_SIG_VALUE, SAMPLE_TOKEN.toXml()),
                    SamlTokenUtil.getTrustedRoots(),
                    -10);
        });
    }

    private void validateDefaultSigValues(JsonSignatureStruct signature) {
        assertEquals(JsonSignatureAlgorithm.getDefault().name(), signature.getAlg());
        assertEquals(SAMPLE_SIG_VALUE, signature.getSigValue());
        assertEquals(SAMPLE_TOKEN, signature.getSamlToken());
    }

    private String getValidAlg() {
        return JsonSignatureAlgorithm.getDefault().name();
    }

    private Map<String, Object> getSignature(Object alg, Object sigValue, Object token) {
        Map<String, Object> signature = new HashMap<String, Object>();
        signature.put(SecurityContextConstants.SIGNATURE_ALGORITHM_KEY, alg);
        signature.put(SecurityContextConstants.SIG_VALUE_KEY, sigValue);
        signature.put(SecurityContextConstants.SAML_TOKEN_KEY, token);
        return signature;
    }
}
