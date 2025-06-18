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

import static com.vmware.vapi.internal.cis.authn.json.JsonSignerTestHelper.STS_TRUSTCHAIN;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.vmware.vapi.cis.authn.json.SamlTokenUtil;
import com.vmware.vapi.dsig.json.SignatureException;
import com.vmware.vapi.internal.dsig.json.JsonCanonicalizer;
import com.vmware.vapi.internal.dsig.json.Verifier;

public class JsonSignerImplTest {
    private final JsonSignerTestHelper testHelper = new JsonSignerTestHelper();

    private final JsonSignerImpl jsonVerifierImpl = new JsonSignerImpl(new JsonCanonicalizer(), STS_TRUSTCHAIN);
    private final JsonSignerImpl jsonSignerImpl = new JsonSignerImpl(new JsonCanonicalizer());
    private final String payload = testHelper.getPayload();
    private final byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
    private JsonSignatureAlgorithm defaultAlg = JsonSignatureAlgorithm.getDefault();

    @Test
    public void testInvalidSignaturePayload() {
        String sigValue = jsonSignerImpl.sign(payloadBytes, SamlTokenUtil.loadTokenPrivateKey(), defaultAlg);
        Map<String, Object> signature = testHelper.serializeSignature(sigValue, defaultAlg.name());
        assertFalse(jsonVerifierImpl.verifySignature(
                testHelper.serializeSecurityContext(payload.replace('a', 'b'), signature),
                signature,
                Verifier.DEFAULT_CLOCK_TOLERANCE_SEC));
    }

    @Test
    public void testInvalidSignatureAlg() {
        Map<String, Object> signature = testHelper.serializeSignature("aa", "RS-no-such-alg");
        try {
            jsonVerifierImpl.verifySignature(
                    testHelper.serializeSecurityContext(payload, signature),
                    signature,
                    Verifier.DEFAULT_CLOCK_TOLERANCE_SEC);
        } catch (SignatureException e) {
            assertTrue(e.getMessage().contains("RS-no-such-alg is not supported"));
        }
    }

    @Test
    public void testInvalidSignatureValue() {
        assertThrows(SignatureException.class, () -> {
            String defaultAlg = JsonSignatureAlgorithm.getDefault().toString();
            Map<String, Object> signature = testHelper.serializeSignature("aa", defaultAlg);
            assertFalse(jsonVerifierImpl.verifySignature(
                    testHelper.serializeSecurityContext(payload, signature),
                    signature,
                    Verifier.DEFAULT_CLOCK_TOLERANCE_SEC));
        });
    }

    @Test
    public void testSignerVerify() {
        assertThrows(IllegalStateException.class, () -> {
            jsonSignerImpl.verifySignature(
                    new byte[0], new HashMap<String, Object>(), Verifier.DEFAULT_CLOCK_TOLERANCE_SEC);
        });
    }

    @Test
    public void testNullCanonicalizer() {
        assertThrows(IllegalArgumentException.class, () -> {
            new JsonSignerImpl(null, null);
        });
    }

    @Test
    public void testNullStsChainVerify() {
        assertThrows(IllegalStateException.class, () -> {
            JsonSignerImpl signer = new JsonSignerImpl(new JsonCanonicalizer(), null);
            String sigValue = signer.sign(payloadBytes, SamlTokenUtil.loadTokenPrivateKey(), defaultAlg);
            Map<String, Object> signature = testHelper.serializeSignature(sigValue, defaultAlg.name());
            signer.verifySignature(payloadBytes, signature, Verifier.DEFAULT_CLOCK_TOLERANCE_SEC);
        });
    }

    @Test
    public void testSignNullMsg() {
        assertThrows(IllegalArgumentException.class, () -> {
            jsonSignerImpl.sign(null, SamlTokenUtil.loadTokenPrivateKey(), defaultAlg);
        });
    }

    @Test
    public void testSignNullPrivateKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            jsonSignerImpl.sign(payloadBytes, null, defaultAlg);
        });
    }

    @Test
    public void testVerifyNullMsg() {
        assertThrows(IllegalArgumentException.class, () -> {
            jsonVerifierImpl.verifySignature(null, new HashMap<String, Object>(), Verifier.DEFAULT_CLOCK_TOLERANCE_SEC);
        });
    }

    @Test
    public void testVerifyNullSignature() {
        assertThrows(IllegalArgumentException.class, () -> {
            jsonVerifierImpl.verifySignature(new byte[0], null, Verifier.DEFAULT_CLOCK_TOLERANCE_SEC);
        });
    }

    @Test
    public void testVerifyInvalidClockTol() {
        assertThrows(IllegalArgumentException.class, () -> {
            jsonVerifierImpl.verifySignature(new byte[0], new HashMap<String, Object>(), -6);
        });
    }
}
