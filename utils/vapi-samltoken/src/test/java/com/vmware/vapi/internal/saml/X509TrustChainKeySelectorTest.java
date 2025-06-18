/*
 * ******************************************************************
 * Copyright (c) 2010-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.internal.saml;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.XMLValidateContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Isolated
@Disabled("Disabled as org.jcp.xml.dsig.secureValidation still does not work on all JDK."
        + "We will convert the RSA keys to longer.")
public class X509TrustChainKeySelectorTest {

    private static final String TEST_FIXTURE_PREFIX = "cert_chain_selector/";

    private static X509Certificate CERT_ROOT_CA;
    private static X509Certificate CERT_CA1;
    private static X509Certificate CERT_CA2;
    private static X509Certificate CERT_STS;

    @BeforeAll
    public static void removeSecureValidation() {
        // only for the unit tests as the resource a shorter key is provided
        System.setProperty("org.jcp.xml.dsig.secureValidation", "false");
        Security.setProperty("jdk.certpath.disabledAlgorithms", "MD2, RSA keySize < 512");
    }

    @AfterAll
    public static void recoverSecureValidation() {
        System.setProperty("org.jcp.xml.dsig.secureValidation", "true");
        Security.setProperty("jdk.certpath.disabledAlgorithms", "");
    }

    @BeforeAll
    public static void setupSuite() throws GeneralSecurityException {
        ClassLoader loader = ClassLoader.getSystemClassLoader();

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        CERT_ROOT_CA =
                (X509Certificate) cf.generateCertificate(loader.getResourceAsStream(TEST_FIXTURE_PREFIX + "root.cert"));

        CERT_CA1 =
                (X509Certificate) cf.generateCertificate(loader.getResourceAsStream(TEST_FIXTURE_PREFIX + "ca1.cert"));

        CERT_CA2 =
                (X509Certificate) cf.generateCertificate(loader.getResourceAsStream(TEST_FIXTURE_PREFIX + "ca1.cert"));

        CERT_STS =
                (X509Certificate) cf.generateCertificate(loader.getResourceAsStream(TEST_FIXTURE_PREFIX + "sts.cert"));
    }

    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
    /*                         Construction Tests                              */
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

    @Test
    public void testCreateNoTrustedRoots() {
        assertThrows(IllegalArgumentException.class, () -> {
            new X509TrustChainKeySelector();
        });
    }

    @Test
    public void testCreateNullArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            new X509TrustChainKeySelector((X509Certificate[]) null);
        });
    }

    @Test
    public void testCreateNullCertificate() {
        assertThrows(IllegalArgumentException.class, () -> {
            new X509TrustChainKeySelector(CERT_ROOT_CA, null, CERT_STS);
        });
    }

    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
    /*                      KeyInfo Parsing Tests                              */
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

    @Test
    public void testInvalidPurpose() throws KeySelectorException {
        KeySelector ks = new X509TrustChainKeySelector(CERT_ROOT_CA);
        assertNull(ks.select(null, KeySelector.Purpose.SIGN, null, null).getKey());
    }

    @Test
    public void testParseNullKeyInfo() throws KeySelectorException {
        KeySelector ks = new X509TrustChainKeySelector(CERT_ROOT_CA, CERT_CA1);

        assertNull(ks.select(null, KeySelector.Purpose.VERIFY, null, null).getKey());
    }

    @Test
    public void testParseNoX509Data() throws Exception {
        assertNull(selectKeyFromKeyInfo("keyinfo-no-x509data"));
    }

    @Test
    public void testParseEmptyX509Data() throws Exception {
        assertNull(selectKeyFromKeyInfo("keyinfo-empty-x509data"));
    }

    @Test
    public void testParseMultiX509Data() throws Exception {
        assertNull(selectKeyFromKeyInfo("keyinfo-multi-x509data"));
    }

    @Test
    public void testParseMultiNoX509Certificates() throws Exception {
        assertNull(selectKeyFromKeyInfo("keyinfo-no-x509cert"));
    }

    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
    /*                         Validation Tests                                */
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

    @Test
    public void testValidationTrustRootCA() throws Exception {
        KeySelector ks = new X509TrustChainKeySelector(CERT_ROOT_CA);
        assertTrue(loadAndValidateSignature("ds-valid-full-ordered-chain", ks));
    }

    @Test
    public void testValidationTrustIntermediateCA() throws Exception {
        KeySelector ks = new X509TrustChainKeySelector(CERT_CA2);
        assertTrue(loadAndValidateSignature("ds-valid-full-ordered-chain", ks));
    }

    @Test
    public void testValidationTrustSigningKey() throws Exception {
        KeySelector ks = new X509TrustChainKeySelector(CERT_STS);
        assertTrue(loadAndValidateSignature("ds-valid-full-ordered-chain", ks));
    }

    @Test
    public void testValidationUnorderedChain() throws Exception {
        KeySelector ks = new X509TrustChainKeySelector(CERT_ROOT_CA);
        assertTrue(loadAndValidateSignature("ds-valid-unordered-chain", ks));
    }

    @Test
    public void testValidationChainWithoutRoot() throws Exception {
        KeySelector ks = new X509TrustChainKeySelector(CERT_ROOT_CA);
        assertTrue(loadAndValidateSignature("ds-valid-chain-noroot", ks));
    }

    @Test
    public void testValidationIncompleteChain() throws Exception {
        assertThrows(XMLSignatureException.class, () -> {
            KeySelector ks = new X509TrustChainKeySelector(CERT_ROOT_CA);
            assertTrue(loadAndValidateSignature("ds-valid-incomplete-chain", ks));
        });
    }

    @Test
    public void testValidationNoChainAndManualKey() throws Exception {
        KeySelector ks = new X509TrustChainKeySelector(CERT_STS);
        assertTrue(loadAndValidateSignature("ds-valid-nochain", ks));
    }

    @Test
    public void testValidationNoChainAndMissingKey() throws Exception {
        assertThrows(XMLSignatureException.class, () -> {
            KeySelector ks = new X509TrustChainKeySelector(CERT_ROOT_CA, CERT_CA1);
            loadAndValidateSignature("ds-valid-nochain", ks);
        });
    }

    @Test
    public void testValidationWithInvalidKey() throws Exception {
        KeySelector ks = new X509TrustChainKeySelector(CERT_ROOT_CA);
        try {
            assertFalse(loadAndValidateSignature("ds-valid-nochain", ks));
        } catch (XMLSignatureException e) {
            // thrown by Java7 XMLSignature.validate, Java6 returns false
        }
    }

    /**
     * Helper: Load the XML document from the resource with the specified name and return whether or not it's signature
     * is valid. The validation is performed with the specified KeySelector. The resources are resolved under
     * TEST_FIXTURE_PREFIX.
     */
    private boolean loadAndValidateSignature(String name, KeySelector keySelector) throws Exception {

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);

        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(getClass().getResourceAsStream("/" + TEST_FIXTURE_PREFIX + name + ".xml"));

        Element data = (Element) doc.getElementsByTagName("data").item(0);
        if (data.hasAttribute("id")) {
            data.setIdAttribute("id", true);
        }
        Element signatureElement = (Element)
                doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);

        XMLSignatureFactory dsFactory = XMLSignatureFactory.getInstance("DOM");
        XMLValidateContext ctx = new DOMValidateContext(keySelector, signatureElement);

        return dsFactory.unmarshalXMLSignature(ctx).validate(ctx);
    }

    /**
     * Helper: parse a KeyInfo element from the XML resource with the specified name, apply the
     * X509TrustChainKeySelector on it and return the selected key. The resource is resolved under the
     * TEST_FIXTURE_PREFIX.
     */
    private Key selectKeyFromKeyInfo(String name) throws Exception {

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);

        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(getClass().getResourceAsStream("/" + TEST_FIXTURE_PREFIX + name + ".xml"));

        XMLSignatureFactory dsFactory = XMLSignatureFactory.getInstance("DOM");
        KeyInfo keyInfo = dsFactory.getKeyInfoFactory().unmarshalKeyInfo(new DOMStructure(doc.getDocumentElement()));

        return new X509TrustChainKeySelector(CERT_ROOT_CA, CERT_CA1)
                .select(keyInfo, KeySelector.Purpose.VERIFY, null, null)
                .getKey();
    }
}
