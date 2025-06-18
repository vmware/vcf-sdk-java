/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.saml;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Security;
import java.security.cert.X509Certificate;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.vmware.vapi.internal.saml.Constants;
import com.vmware.vapi.internal.saml.SamlTokenImpl;
import com.vmware.vapi.saml.exception.InvalidTimingException;
import com.vmware.vapi.saml.util.KeyStoreData;
import com.vmware.vapi.saml.util.exception.SsoKeyStoreOperationException;

public class ADFSTokenTest {
    private static final String ADFS_CERT_ALIAS = "ADFS";
    private static final String ADFS_CERT_PWD = "adfskeystore";
    private static final String ADFS_CERT_JKS = "ADFSKeyStore.jks";
    private static final String SAML_TOKEN_DIR = "/saml_token/";
    private static final String ADFS_SAML_TOKEN_FILE = "adfs_token.xml";
    private static JAXBContext _JAXBContext;

    // saml token is a saved afdfs xml; it's expiration is in the past -
    // we still want to validate token for test purposes
    private static int CLOCK_TOLERANCE_20YRS = 60 * 60 * 24 * 365 * 20;

    static {
        // Apparently this test has side effects over X509TrustChainKeySelectorTest
        // Without this hack, running ADFSTokenTest before X509TrustChainKeySelectorTest
        // will fail some tests on X509TrustChainKeySelectorTest
        Security.setProperty("jdk.certpath.disabledAlgorithms", "MD2, RSA keySize < 512");
    }

    @BeforeAll
    public static void prep() throws JAXBException {
        _JAXBContext = JAXBContext.newInstance(Constants.ASSERTION_JAXB_PACKAGE);
    }

    @Test
    public void test() {
        URL keyStoreResource = ADFSTokenTest.class.getResource("/" + ADFSTokenTest.ADFS_CERT_JKS);
        Assertions.assertNotNull(keyStoreResource, "Should be able to get resource stream from " + ADFS_CERT_JKS);

        KeyStoreData keyStore = null;
        try {
            keyStore = new KeyStoreData(
                    keyStoreResource.toURI().getPath(),
                    ADFSTokenTest.ADFS_CERT_PWD.toCharArray(),
                    ADFSTokenTest.ADFS_CERT_ALIAS);
        } catch (URISyntaxException | SsoKeyStoreOperationException e1) {
            e1.printStackTrace();
            Assertions.fail("Should be able to get adfs keystore. Error: " + e1.getMessage());
        }

        String adfsTokenXml = null;
        try {
            adfsTokenXml = TestTokenUtil.loadStreamContent(
                    this.getClass().getResourceAsStream(SAML_TOKEN_DIR + ADFS_SAML_TOKEN_FILE));
        } catch (IOException e1) {
            e1.printStackTrace();
            Assertions.fail("Should be able to get adfs token from file. Error: " + e1.getMessage());
        }
        Assertions.assertNotNull(adfsTokenXml, "Should be able to get resource stream from " + ADFS_SAML_TOKEN_FILE);

        try {
            SamlTokenImpl samlTokenImpl = new SamlTokenImpl(adfsTokenXml, _JAXBContext, true);
            try {
                samlTokenImpl.validate(new X509Certificate[] {keyStore.getCertificate()}, CLOCK_TOLERANCE_20YRS);
            } catch (InvalidTimingException ex) {
                // expected as this token can be expired - it is a saved token ...
            }
            Assertions.assertNotNull(samlTokenImpl.getSubjectNameId(), "Token subject should be non-null");
            Assertions.assertEquals(
                    samlTokenImpl.getSubjectNameId().getFormat(),
                    "http://schemas.xmlsoap.org/claims/UPN",
                    "Token subject name format should be http://schemas.xmlsoap.org/claims/UPN");
            Assertions.assertEquals(
                    samlTokenImpl.getSubjectNameId().getValue(),
                    "ExternalIdpTest@acme.vmware.com",
                    "Token subject should be ExternalIdpTest@acme.vmware.com");
        } catch (Exception ex) {
            ex.printStackTrace();
            Assertions.fail(ex.getClass().getName() + ": " + ex.getMessage());
        }
    }
}
