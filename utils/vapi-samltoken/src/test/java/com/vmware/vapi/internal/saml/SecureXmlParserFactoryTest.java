/*
 * ******************************************************************
 * Copyright (c) 2012-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.internal.saml;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class SecureXmlParserFactoryTest {

    @Test
    public void testDoctypeDeclarationDisallowed() throws ParserConfigurationException, SAXException, IOException {
        assertThrows(SAXParseException.class, () -> {
            SecureXmlParserFactory factory = new SecureXmlParserFactory();
            DocumentBuilder db = factory.newDocumentBuilder();
            db.parse(this.getClass().getResourceAsStream("/sample_xml/xml_entities.xml"));
        });
    }
}
