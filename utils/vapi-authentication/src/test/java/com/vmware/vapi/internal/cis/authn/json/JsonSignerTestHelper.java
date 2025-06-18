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

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import com.vmware.vapi.cis.authn.json.SamlTokenUtil;
import com.vmware.vapi.dsig.json.DefaultStsTrustChain;
import com.vmware.vapi.internal.dsig.json.KeyStoreHelper;
import com.vmware.vapi.internal.protocol.common.json.JsonSecurityContextSerializer;
import com.vmware.vapi.internal.security.SecurityContextConstants;
import com.vmware.vapi.saml.SamlToken;

public class JsonSignerTestHelper {

    public static final DefaultStsTrustChain STS_TRUSTCHAIN =
            loadFromKeystore(SamlTokenUtil.SAML_KEYSTORE_JKS, null, SamlTokenUtil.SIGNING_CERT_ALIAS);

    private final String payload;
    private final SamlToken samlToken;
    private JsonSecurityContextSerializer serializer = new JsonSecurityContextSerializer();

    public JsonSignerTestHelper() {
        try {
            payload = SamlTokenUtil.readFromFile("/testMessage.json");
            samlToken = SamlTokenUtil.loadSampleToken();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] serializeSecurityContext(String request, Map<String, Object> signature) {
        Map<String, Object> secCtx = new HashMap<String, Object>();
        secCtx.put(SecurityContextConstants.SIGNATURE_KEY, signature);
        return serializer.serializeSecurityContext(secCtx, request.getBytes(StandardCharsets.UTF_8));
    }

    public Map<String, Object> serializeSignature(String signatureValue, String alg) {
        Map<String, Object> signature = new HashMap<String, Object>();
        signature.put(SecurityContextConstants.SIG_VALUE_KEY, signatureValue);
        signature.put(SecurityContextConstants.SAML_TOKEN_KEY, getSamlToken().toXml());
        signature.put(SecurityContextConstants.SIGNATURE_ALGORITHM_KEY, alg);
        return signature;
    }

    public String getPayload() {
        return payload;
    }

    public SamlToken getSamlToken() {
        return samlToken;
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
}
