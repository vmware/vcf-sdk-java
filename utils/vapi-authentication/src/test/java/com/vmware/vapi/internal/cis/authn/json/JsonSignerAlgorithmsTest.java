/*
 * ******************************************************************
 * Copyright (c) 2021-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.internal.cis.authn.json;

import static com.vmware.vapi.cis.authn.json.SamlTokenUtil.SIGNING_CERT_ALIAS;
import static com.vmware.vapi.cis.authn.json.SamlTokenUtil.SIGNING_KEY_ALIAS;
import static com.vmware.vapi.internal.cis.authn.json.JsonSignatureAlgorithm.RS256;
import static com.vmware.vapi.internal.cis.authn.json.JsonSignatureAlgorithm.RS384;
import static com.vmware.vapi.internal.cis.authn.json.JsonSignatureAlgorithm.RS512;
import static com.vmware.vapi.internal.dsig.json.Verifier.DEFAULT_CLOCK_TOLERANCE_SEC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.vmware.vapi.cis.authn.json.SamlTokenUtil;
import com.vmware.vapi.dsig.json.DefaultStsTrustChain;
import com.vmware.vapi.internal.dsig.json.JsonCanonicalizer;
import com.vmware.vapi.internal.dsig.json.KeyStoreHelper;
import com.vmware.vapi.saml.exception.InvalidTokenException;

/** Unit tests that check the signature verification with different supported algorithms. */
public class JsonSignerAlgorithmsTest {

    private final JsonSignerImpl jsonSignerImpl = new JsonSignerImpl(new JsonCanonicalizer());
    private final JsonSignerTestHelper testHelper = new JsonSignerTestHelper();

    private JsonSignerImpl createVerifier(String certificateAlias) {
        DefaultStsTrustChain trustChain = loadFromKeystore(SamlTokenUtil.SAML_KEYSTORE_JKS, null, certificateAlias);
        return new JsonSignerImpl(new JsonCanonicalizer(), trustChain);
    }

    @Test
    void testSignVerify() throws IOException, InvalidTokenException {
        signVerify(RS256, SIGNING_KEY_ALIAS, SIGNING_CERT_ALIAS);
        signVerify(RS384, SIGNING_KEY_ALIAS, SIGNING_CERT_ALIAS);
        signVerify(RS512, SIGNING_KEY_ALIAS, SIGNING_CERT_ALIAS);
    }

    public static DefaultStsTrustChain loadFromKeystore(String fileName, String password, String certAlias) {
        KeyStoreHelper keyStore = null;
        try {
            keyStore = new KeyStoreHelper(
                    DefaultStsTrustChain.class.getResource(fileName).toURI().getPath(),
                    password == null ? new char[0] : password.toCharArray());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return DefaultStsTrustChain.createInstance(new X509Certificate[] {keyStore.getCertificate(certAlias)});
    }

    public void signVerify(JsonSignatureAlgorithm algorithm, String pkAlias, String certAlias)
            throws IOException, InvalidTokenException {
        PrivateKey privateKey = SamlTokenUtil.loadPrivateKey(pkAlias);
        JsonSignerImpl verifier = createVerifier(certAlias);

        // sign the canonical form
        String sigValue =
                jsonSignerImpl.sign(testHelper.getPayload().getBytes(StandardCharsets.UTF_8), privateKey, algorithm);
        assertNotNull(sigValue);
        Map<String, Object> signature = testHelper.serializeSignature(sigValue, algorithm.name());
        JsonSignatureStruct sig = JsonSignatureStruct.parseJsonSignatureStruct(
                signature, SamlTokenUtil.getTrustedRoots(), DEFAULT_CLOCK_TOLERANCE_SEC);
        assertNotNull(sig.getAlg());
        assertEquals(algorithm.name(), sig.getAlg());
        assertNotNull(sig.getSigValue());
        assertNotNull(sig.getSamlToken());

        // verify signature
        assertTrue(verifier.verifySignature(
                testHelper.serializeSecurityContext(testHelper.getPayload(), signature),
                signature,
                DEFAULT_CLOCK_TOLERANCE_SEC));
    }
}
