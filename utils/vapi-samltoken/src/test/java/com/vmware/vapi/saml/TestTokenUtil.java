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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URISyntaxException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.vmware.vapi.internal.saml.exception.ParserException;
import com.vmware.vapi.saml.util.KeyStoreData;
import com.vmware.vapi.saml.util.exception.SsoKeyStoreOperationException;

/** Utility class for containing common test helper methods */
public class TestTokenUtil {

    private static final String SAML_TOKEN_RECIPIENT_IN_SUBJECT_XML = "saml_token_recipient_in_subject.xml";
    private static final String SAML_TOKEN_VALID_XML = "saml_token_valid.xml";
    private static final String ANOTHER_SAML_TOKEN_VALID_XML = "saml_token_valid2.xml";
    public static final String TEST_KEYSTORE_FILENAME = "sso_test.jks";
    public static final String TEST_KEYSTORE_KEY = "vmware";
    public static final String TEST_KEYSTORE_CERT_ALIAS = "vmware";
    public static final String TEST_KEYSTORE_PRIV_KEY_PASSWORD = "vmware";
    public static final String SAML_TOKEN_DIR = "/saml_token/";

    /**
     * Loads a file into string
     *
     * @return String content of the file
     */
    public static String loadStreamContent(InputStream stream) throws IOException {
        StringBuilder content = new StringBuilder();

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        try {
            char[] buff = new char[1024];
            int i = 0;
            while ((i = reader.read(buff)) != -1) {
                content.append(buff, 0, i);
            }
        } finally {
            reader.close();
        }

        return content.toString();
    }

    /**
     * Loads the default keystore for the test cases
     *
     * @return KeyStoreData
     */
    public static KeyStoreData loadDefaultKeystore() throws SsoKeyStoreOperationException {

        try {
            return new KeyStoreData(
                    TestTokenUtil.class
                            .getResource("/" + TestTokenUtil.TEST_KEYSTORE_FILENAME)
                            .toURI()
                            .getPath(),
                    TestTokenUtil.TEST_KEYSTORE_KEY.toCharArray(),
                    TestTokenUtil.TEST_KEYSTORE_CERT_ALIAS);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /** Loads a valid token string. */
    public static String getValidSamlTokenString() throws ParserException {
        return getValidSamlTokenStringImpl(SAML_TOKEN_VALID_XML);
    }

    /** Loads a valid token string with recipient in the subject. */
    public static String getValidSamlTokenString_RecipientInSubject() throws ParserException {
        return getValidSamlTokenStringImpl(SAML_TOKEN_RECIPIENT_IN_SUBJECT_XML);
    }

    /** Loads another valid token string. */
    public static String getAnotherValidSamlTokenString() throws ParserException {
        return getValidSamlTokenStringImpl(ANOTHER_SAML_TOKEN_VALID_XML);
    }

    /** Load a valid token DOM element. */
    public static Element getValidSamlTokenElement() throws ParserException {
        return parseXml(getValidSamlTokenString());
    }

    /** Load another valid token DOM element. */
    public static Element getAnotherValidSamlTokenElement() throws ParserException {
        return parseXml(getAnotherValidSamlTokenString());
    }

    /**
     * Parses arbitrary xml
     *
     * @return non null DOM-tree root element
     */
    public static Element parseXml(String token) throws ParserException {

        XmlParserFactory parserFactory = XmlParserFactory.Factory.createSecureXmlParserFactory();

        try {
            DocumentBuilder docBuilder = parserFactory.newDocumentBuilder();

            InputSource src = new InputSource(new StringReader(token));
            return docBuilder.parse(src).getDocumentElement();

        } catch (ParserConfigurationException e) {
            throw new ParserException("Internal creating XML parser", e);

        } catch (SAXException e) {
            throw new ParserException("Error parsing token XML", e);

        } catch (IOException e) {
            throw new ParserException("Unexpected error reading from in-memory stream", e);
        }
    }

    private static String getValidSamlTokenStringImpl(String tokenFileName) throws ParserException {
        try {
            return TestTokenUtil.loadStreamContent(
                    TestTokenUtil.class.getResourceAsStream(TestTokenUtil.SAML_TOKEN_DIR + tokenFileName));

        } catch (IOException e) {
            throw new ParserException("SamlToken cannot be read", e);
        }
    }
}
