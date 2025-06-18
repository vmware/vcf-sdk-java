/*
 * ******************************************************************
 * Copyright (c) 2014-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.cis.authn.json;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import com.vmware.vapi.internal.cis.authn.json.JsonSignerImplTest;
import com.vmware.vapi.internal.dsig.json.KeyStoreHelper;
import com.vmware.vapi.saml.DefaultTokenFactory;
import com.vmware.vapi.saml.SamlToken;
import com.vmware.vapi.saml.exception.InvalidTokenException;

public class SamlTokenUtil {

    public static final String TEST_MSG_JSON = "/testMessage.json";
    public static final String SAML_KEYSTORE_JKS = "/saml-keystore.jks";
    public static final String SIGNING_KEY_ALIAS = "hok";
    public static final String SIGNING_CERT_ALIAS = "sts";
    private static final String SAML_TOKEN_FILE = "/samlToken.xml";
    private static final KeyStoreHelper KEYSTORE = getKeystoreHelper();

    static KeyStoreHelper getKeystoreHelper() {
        try {
            return new KeyStoreHelper(
                    JsonSignerImplTest.class
                            .getResource(SAML_KEYSTORE_JKS)
                            .toURI()
                            .getPath(),
                    new char[] {});
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static final X509Certificate[] TOKEN_TRUSTED_ROOTS =
            new X509Certificate[] {KEYSTORE.getCertificate(SIGNING_CERT_ALIAS)};

    public static final X509Certificate[] getTrustedRoots() {
        return Arrays.copyOf(TOKEN_TRUSTED_ROOTS, TOKEN_TRUSTED_ROOTS.length);
    }

    public static String readFromFile(String fileName) throws IOException, URISyntaxException {
        String filePath = SamlTokenUtil.class.getResource(fileName).toURI().getPath();
        StringBuilder result = new StringBuilder();
        FileReader fileReader = new FileReader(filePath, StandardCharsets.UTF_8);
        try {
            BufferedReader reader = new BufferedReader(fileReader);
            try {
                char[] buff = new char[1024];
                int i = 0;
                while ((i = reader.read(buff)) != -1) {
                    result.append(buff, 0, i);
                }
            } finally {
                reader.close();
            }
        } finally {
            fileReader.close();
        }

        return result.toString();
    }

    public static SamlToken loadSampleToken() throws IOException, InvalidTokenException, URISyntaxException {
        X509Certificate certificate = KEYSTORE.getCertificate(SIGNING_CERT_ALIAS);
        return new DefaultTokenFactory().parseToken(loadSampleTokenXml(), certificate);
    }

    public static String loadSampleTokenXml() throws IOException, URISyntaxException {
        return readFromFile(SAML_TOKEN_FILE);
    }

    public static byte[] loadTestMsg() throws IOException, URISyntaxException {
        return loadTestMsgText().getBytes("UTF-8");
    }

    public static String loadTestMsgText() throws IOException, URISyntaxException {
        return readFromFile(TEST_MSG_JSON);
    }

    public static PrivateKey loadTokenPrivateKey() {
        return loadPrivateKey(SIGNING_KEY_ALIAS);
    }

    public static PrivateKey loadPrivateKey(String alias) {
        return KEYSTORE.getPrivateKey(alias, new char[] {});
    }
}
