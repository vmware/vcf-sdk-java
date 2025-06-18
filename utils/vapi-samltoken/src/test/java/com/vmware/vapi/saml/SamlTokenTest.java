/*
 * ******************************************************************
 * Copyright (c) 2010-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.saml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.parsers.ParserConfigurationException;

import jakarta.xml.bind.JAXBContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.vmware.vapi.internal.saml.Constants;
import com.vmware.vapi.internal.saml.SamlTokenImpl;
import com.vmware.vapi.internal.saml.exception.ParserException;
import com.vmware.vapi.saml.Advice.AdviceAttribute;
import com.vmware.vapi.saml.exception.InvalidSignatureException;
import com.vmware.vapi.saml.exception.InvalidTokenException;
import com.vmware.vapi.saml.util.KeyStoreData;
import com.vmware.vapi.saml.util.exception.SsoKeyStoreOperationException;

/** Test various SAML token creation scenarios */
public class SamlTokenTest {

    private static final String EMAIL_ADDRESS_FORMAT =
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress";
    private static final String SUBJECT_NAME_ID_OF_TOKEN_WITH_RECIPIENT_AND_IN_RESPONSE_TO = "AdminEmail@example.com";
    private static final String UPN_FORMAT = "http://schemas.xmlsoap.org/claims/UPN";
    private static final String ENTITY_FORMAT = "urn:oasis:names:tc:SAML:2.0:nameid-format:entity";
    private static final String ISSUER = "https://GStaykov-Dev.vmware.com:8444/STS";
    private static final String UPN_SUBJECT = "HoKUser@example.com";
    private static final String SAML_TOKEN_DIR = "/saml_token/";
    private static final int INVALID_TOKEN_COUNT = 13;

    @Test
    public void createValidToken()
            throws IOException, ParserException, SsoKeyStoreOperationException, InvalidTokenException {
        String samlXml = TestTokenUtil.getValidSamlTokenString();

        KeyStoreData keystore = TestTokenUtil.loadDefaultKeystore();
        SamlToken token = DefaultTokenFactory.createToken(samlXml, keystore.getCertificate());

        assertBasicFieldValid(token);
    }

    @Test
    public void createValidTokenWithGroups() throws IOException, SsoKeyStoreOperationException, InvalidTokenException {
        String samlXml = TestTokenUtil.loadStreamContent(
                this.getClass().getResourceAsStream(SAML_TOKEN_DIR + "saml_token_valid_groups.xml"));

        KeyStoreData keystore = TestTokenUtil.loadDefaultKeystore();
        SamlToken token = DefaultTokenFactory.createToken(samlXml, keystore.getCertificate());

        assertBasicFieldValid(token);
        assertEquals(3, token.getGroupList().size());
    }

    @Test
    public void createTokenWithInvalidSignature()
            throws IOException, SsoKeyStoreOperationException, InvalidTokenException {
        KeyStoreData keystore = TestTokenUtil.loadDefaultKeystore();

        String validTokenXml = TestTokenUtil.loadStreamContent(
                this.getClass().getResourceAsStream(SAML_TOKEN_DIR + "saml_token_valid.xml"));

        String invalidSignatureSamlXml = putWrongCharInSignature(validTokenXml);

        try {
            DefaultTokenFactory.createToken(invalidSignatureSamlXml, keystore.getCertificate());
            Assertions.fail();
        } catch (InvalidSignatureException e) {
            // expected
        }
    }

    @Test
    public void createValidTokenWhitespaceInConfirmation()
            throws IOException, ParserException, SsoKeyStoreOperationException, InvalidTokenException {
        String samlXml = TestTokenUtil.loadStreamContent(
                this.getClass().getResourceAsStream(SAML_TOKEN_DIR + "saml_token_valid_whitespace.xml"));

        KeyStoreData keystore = TestTokenUtil.loadDefaultKeystore();
        SamlToken token = DefaultTokenFactory.createToken(samlXml, keystore.getCertificate());

        assertBasicFieldValid(token);
    }

    @Test
    public void createValidTokenFromDom()
            throws IOException, SAXException, ParserConfigurationException, ParserException, InvalidTokenException,
                    SsoKeyStoreOperationException {

        final KeyStoreData keystore = TestTokenUtil.loadDefaultKeystore();

        Element tokenRoot = TestTokenUtil.getValidSamlTokenElement();
        SamlToken token = DefaultTokenFactory.createTokenFromDom(tokenRoot, keystore.getCertificate());
        assertNotNull(token);

        Element tokenRootDuplicate = TestTokenUtil.parseXml(token.toXml());
        SamlToken tokenDuplicate =
                DefaultTokenFactory.createTokenFromDom(tokenRootDuplicate, keystore.getCertificate());
        assertNotNull(tokenDuplicate);

        assertEquals(tokenDuplicate, token);
    }

    @Test
    public void createTokenFromParsedTokenXml()
            throws ParserException, SsoKeyStoreOperationException, InvalidTokenException {

        Element tokenElement = TestTokenUtil.getValidSamlTokenElement();
        KeyStoreData keystore = TestTokenUtil.loadDefaultKeystore();
        SamlToken token = DefaultTokenFactory.createTokenFromDom(tokenElement, keystore.getCertificate());

        assertNotNull(token);

        SamlToken dupToken = DefaultTokenFactory.createToken(token.toXml(), keystore.getCertificate());

        assertNotNull(dupToken);
        assertEquals(token.getSubject(), dupToken.getSubject());
    }

    @Test
    public void createInvalidToken() throws IOException, SsoKeyStoreOperationException, URISyntaxException {
        KeyStoreData keystore = new KeyStoreData(
                this.getClass()
                        .getResource("/" + TestTokenUtil.TEST_KEYSTORE_FILENAME)
                        .toURI()
                        .getPath(),
                TestTokenUtil.TEST_KEYSTORE_KEY.toCharArray(),
                TestTokenUtil.TEST_KEYSTORE_CERT_ALIAS);

        for (int currentToken = 1; currentToken < INVALID_TOKEN_COUNT + 1; currentToken++) {
            String samlXml = TestTokenUtil.loadStreamContent(this.getClass()
                    .getResourceAsStream(SAML_TOKEN_DIR + "saml_token_invalid_" + currentToken + ".xml"));

            try {
                DefaultTokenFactory.createToken(samlXml, keystore.getCertificate());
                Assertions.fail();
            } catch (InvalidTokenException e) {
                // expected
            }
        }
    }

    @Test
    public void createTokenWithInvalidString() throws SsoKeyStoreOperationException, InvalidTokenException {
        assertThrows(IllegalArgumentException.class, () -> {
            KeyStoreData keystore = new KeyStoreData(
                    this.getClass()
                            .getResource("/" + TestTokenUtil.TEST_KEYSTORE_FILENAME)
                            .toURI()
                            .getPath(),
                    TestTokenUtil.TEST_KEYSTORE_KEY.toCharArray(),
                    TestTokenUtil.TEST_KEYSTORE_CERT_ALIAS);

            DefaultTokenFactory.createToken(null, keystore.getCertificate());
        });
    }

    @Test
    public void createTokenWithNoTrustedCertificates() throws InvalidTokenException, ParserException {
        assertThrows(IllegalArgumentException.class, () -> {
            DefaultTokenFactory.createToken(TestTokenUtil.getValidSamlTokenString(), (X509Certificate[]) null);
        });
    }

    @Test
    public void createTokenWithNullTrustedCertificate() throws InvalidTokenException, ParserException {
        assertThrows(IllegalArgumentException.class, () -> {
            DefaultTokenFactory.createToken(TestTokenUtil.getValidSamlTokenString(), (X509Certificate) null);
        });
    }

    @Test
    public void serializeToXml()
            throws IOException, ParserException, SsoKeyStoreOperationException, InvalidTokenException {
        String samlXml = TestTokenUtil.getValidSamlTokenString();
        SamlToken token = getToken(samlXml);

        String serializedToken = token.toXml();
        assertFalse(serializedToken.contains("<?xml"));
        assertTrue(serializedToken.contains(token.getSubject().getName()));
    }

    // the next two test-cases include validation of inResponseTo and Recipient
    // attributes in the subjectConfirmationData
    @Test
    public void checkSubjectInEmailFormat()
            throws IOException, ParserException, SsoKeyStoreOperationException, InvalidTokenException {
        String tokenWithRecipient_ResponseTo_SubjectInEmailFormat =
                TestTokenUtil.getValidSamlTokenString_RecipientInSubject();
        // tokenWithRecipient_ResponseTo_SubjectInEmailFormat - this string equals
        // to the one in the file
        SamlToken token = getToken(tokenWithRecipient_ResponseTo_SubjectInEmailFormat);
        assertEquals(
                SUBJECT_NAME_ID_OF_TOKEN_WITH_RECIPIENT_AND_IN_RESPONSE_TO,
                token.getSubjectNameId().getValue());
        assertEquals(EMAIL_ADDRESS_FORMAT, token.getSubjectNameId().getFormat());
        assertNull(token.getSubject());
    }

    @Test
    public void checkSubjectInUPNFormat()
            throws IOException, ParserException, SsoKeyStoreOperationException, InvalidTokenException {
        String samlXml = TestTokenUtil.getValidSamlTokenString();
        SamlToken token = getToken(samlXml);

        assertEquals(UPN_SUBJECT, token.getSubjectNameId().getValue());
        assertEquals(UPN_FORMAT, token.getSubjectNameId().getFormat());
        assertNotNull(token.getSubject());
        PrincipalId subject = token.getSubject();
        assertEquals(UPN_SUBJECT, subject.getName() + "@" + subject.getDomain());
    }

    @Test
    public void checkIssuer()
            throws IOException, ParserException, SsoKeyStoreOperationException, InvalidTokenException {
        String samlXml = TestTokenUtil.getValidSamlTokenString();
        SamlToken token = getToken(samlXml);

        assertTrue((token instanceof ValidatableSamlTokenEx), "Token should implement ValidatableSamlTokenEx");
        ValidatableSamlTokenEx tokenEx = (ValidatableSamlTokenEx) token;
        assertNotNull(tokenEx.getIssuerNameId());
        assertEquals(ENTITY_FORMAT, tokenEx.getIssuerNameId().getFormat());
        assertEquals(ISSUER, tokenEx.getIssuerNameId().getValue());
    }

    @Test
    public void testAdviceFriendlyName()
            throws IOException, ParserException, SsoKeyStoreOperationException, InvalidTokenException {
        String samlXml = TestTokenUtil.loadStreamContent(
                this.getClass().getResourceAsStream(SAML_TOKEN_DIR + "saml_token_valid_adviceattr.xml"));

        KeyStoreData keystore = TestTokenUtil.loadDefaultKeystore();
        SamlToken token = DefaultTokenFactory.createToken(samlXml, keystore.getCertificate());

        assertBasicFieldValid(token);

        List<Advice> adviceList = token.getAdvice();
        assertNotNull(adviceList, "advice should be present");
        assertEquals(1, adviceList.size(), "advice should be present");
        Advice advice = adviceList.get(0);
        List<AdviceAttribute> attrs = advice.getAttributes();
        assertNotNull(attrs, "advice attribute should be present");
        assertEquals(1, attrs.size(), "advice attribute should be present");
        AdviceAttribute adviceAttribute = attrs.get(0);
        assertNotNull(adviceAttribute, "advice attribute should be present");

        final String friendlyName = "my advice attribute";
        final String name = "urn:vc:admin.users";

        assertEquals(name, adviceAttribute.getName());
        assertEquals(friendlyName, adviceAttribute.getFriendlyName());
    }

    @Test
    public void createTokenWithoutSignatureValidation() throws Exception {
        String samlXml = TestTokenUtil.getValidSamlTokenString();
        SamlToken token = new DefaultTokenFactory().parseToken(samlXml);

        assertSame(ConfirmationType.HOLDER_OF_KEY, token.getConfirmationType());
        assertBasicFieldValid(token);

        // using the static factory
        token = DefaultTokenFactory.createToken(samlXml);

        assertSame(ConfirmationType.HOLDER_OF_KEY, token.getConfirmationType());
        assertBasicFieldValid(token);
    }

    @Test
    public void createTokenWithoutSignatureValidationFromDom() throws Exception {
        Element tokenRoot = TestTokenUtil.getValidSamlTokenElement();
        SamlToken token = new DefaultTokenFactory().parseToken(tokenRoot);

        assertSame(ConfirmationType.HOLDER_OF_KEY, token.getConfirmationType());
        assertBasicFieldValid(token);

        // using the static factory
        token = DefaultTokenFactory.createTokenFromDom(tokenRoot);

        assertSame(ConfirmationType.HOLDER_OF_KEY, token.getConfirmationType());
        assertBasicFieldValid(token);
    }

    @Test
    public void serializeTokenWithoutSignatureValidationToXml() throws Exception {
        String samlXml = TestTokenUtil.getValidSamlTokenString();
        SamlToken token = new DefaultTokenFactory().parseToken(samlXml);

        String serializedToken = token.toXml();
        assertFalse(serializedToken.contains("<?xml"));
        assertTrue(serializedToken.contains(token.getSubject().getName()));

        // using the static factory
        token = DefaultTokenFactory.createToken(samlXml);

        serializedToken = token.toXml();
        assertFalse(serializedToken.contains("<?xml"));
        assertTrue(serializedToken.contains(token.getSubject().getName()));
    }

    @Test
    public void serializeTokenWithoutSignatureValidationFromDomToXml() throws Exception {
        Element tokenRoot = TestTokenUtil.getValidSamlTokenElement();
        SamlToken token = new DefaultTokenFactory().parseToken(tokenRoot);

        String serializedToken = token.toXml();
        assertFalse(serializedToken.contains("<?xml"));
        assertTrue(serializedToken.contains(token.getSubject().getName()));

        // using the static factory
        token = DefaultTokenFactory.createTokenFromDom(tokenRoot);

        serializedToken = token.toXml();
        assertFalse(serializedToken.contains("<?xml"));
        assertTrue(serializedToken.contains(token.getSubject().getName()));
    }

    @Test
    public void checkNoAccessForNotValidatedToken() throws Exception {
        String samlXml = TestTokenUtil.getValidSamlTokenString();
        SamlTokenImpl token = new SamlTokenImpl(samlXml, JAXBContext.newInstance(Constants.ASSERTION_JAXB_PACKAGE));

        // access to token attributes it not allowed
        try {
            token.getConfirmationType();
            Assertions.fail("");
        } catch (IllegalStateException ex) {
            // expected
        }

        token.allowTokenAccess();
        assertSame(ConfirmationType.HOLDER_OF_KEY, token.getConfirmationType());
    }

    // test thread safaty of DefaultTokenFactory.parseToken()
    private final SamlTokenFactory concurrentFactory = new DefaultTokenFactory();

    class SamlTokenCreator implements Callable<SamlToken> {
        @Override
        public SamlToken call() throws Exception {
            return concurrentFactory.parseToken(TestTokenUtil.getValidSamlTokenString());
        }
    }

    @Test
    public void concurrentCreateTokens() throws Exception {
        List<SamlTokenCreator> tasks = Collections.nCopies(50, new SamlTokenCreator());
        ExecutorService executor = Executors.newFixedThreadPool(5);

        // Execute concurrently 50 parseToken tasks in 5 threads
        List<Future<SamlToken>> results = executor.invokeAll(tasks);

        // then verify all created SamlToken instances are good
        for (Future<SamlToken> f : results) {
            // this might report ExecutionException if some of the tasks threw
            SamlToken token = f.get();

            assertSame(ConfirmationType.HOLDER_OF_KEY, token.getConfirmationType());
            assertBasicFieldValid(token);
        }
    }

    private SamlToken getToken(String samlXml)
            throws ParserException, SsoKeyStoreOperationException, InvalidTokenException {
        KeyStoreData keystore = TestTokenUtil.loadDefaultKeystore();
        SamlToken token = DefaultTokenFactory.createToken(samlXml, keystore.getCertificate());
        return token;
    }

    private void assertBasicFieldValid(SamlToken token) {
        assertNotNull(token);
        assertTrue(token.getConfirmationType().equals(ConfirmationType.HOLDER_OF_KEY), "Confirmation type");
        assertTrue(!token.isDelegable());
        assertNotNull(token.getGroupList());
        assertTrue(!token.isRenewable());
        assertNotNull(token.getSubject());
        assertNotNull(token.getExpirationTime());
        assertNotNull(token.getStartTime());
    }

    private String putWrongCharInSignature(String samlXml) {
        if (samlXml == null) {
            throw new IllegalArgumentException("samlXml cannot be null");
        }

        int charIndex = samlXml.indexOf("Q==</ds:SignatureValue>") - 1;

        String startXmlPart = samlXml.substring(0, charIndex);
        String endXmlPart = samlXml.substring(charIndex + 1);

        if (samlXml.charAt(charIndex) != '5') {
            throw new RuntimeException("Assertion - character must be 5");
        }
        return startXmlPart + "6" + endXmlPart;
    }
}
