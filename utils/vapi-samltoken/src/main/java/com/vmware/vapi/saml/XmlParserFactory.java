/*
 * ******************************************************************
 * Copyright (c) 2012-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.saml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import com.vmware.vapi.internal.saml.SecureXmlParserFactory;

/** Simplified version of {@link javax.xml.parsers.DocumentBuilderFactory} designed for use for SAML token parsing. */
public interface XmlParserFactory {

    /**
     * Creates a document builder with configuration acceptable only for securely parsing SAML tokens and related XML
     * contexts. Do not use as a general XML parser.
     *
     * @return a new document builder suitable for secure SAML token parsing, not null
     * @throws ParserConfigurationException if the document builder cannot be created
     */
    public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException;

    /** Factory for different flavor XML parsers. */
    public static class Factory {
        /**
         * Creates a XML parser factory that:
         *
         * <ul>
         *   <li>disables validation
         *   <li>doesn't load external DTDs
         *   <li>resolves all external XML entities to empty string
         *   <li>supports standard XML entities and local "string-substition" XML entities
         *   <li>is namespace aware
         *   <li>is thread-safe
         * </ul>
         *
         * @return a secure XML parser factory.
         */
        public static XmlParserFactory createSecureXmlParserFactory() {
            return new SecureXmlParserFactory();
        }
    }
}
