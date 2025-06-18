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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vapi.saml.XmlParserFactory;

/**
 * XML Parser factory that:
 *
 * <ul>
 *   <li>disables validation
 *   <li>doesn't load external DTDs
 *   <li>resolves all external XML entities to empty string
 *   <li>supports standard XML entities and local "string-substition" XML entities
 * </ul>
 */
public class SecureXmlParserFactory implements XmlParserFactory {

    private final Logger log = LoggerFactory.getLogger(SecureXmlParserFactory.class);

    @Override
    public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        /*
         * IMPORTANT: We disable validation because it might bring synthetic
         * ("non-specified", i.e. generated from the Schema's default values)
         * attributes into the DOM tree; they'll become "specified" after copying
         * and will most probably break the signature.
         */
        dbf.setValidating(false);

        /*
         * Optional features, recommended by security team. The feature handling
         * depends on what XML parser is on the classpath. Successfully setting
         * the features is not as essential, as dbf.validating = false and the
         * custom entity resolver.
         */
        trySetFeature(dbf, "http://xml.org/sax/features/validation", false);
        trySetFeature(dbf, XMLConstants.FEATURE_SECURE_PROCESSING, true);

        // configuration for Xerces XML parser; has not effect if using JDK parser
        trySetFeature(dbf, "http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        trySetFeature(dbf, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        trySetFeature(dbf, "http://apache.org/xml/features/disallow-doctype-decl", true);

        return dbf.newDocumentBuilder();
    }

    private void trySetFeature(DocumentBuilderFactory dbf, String featureKey, boolean value) {
        try {
            dbf.setFeature(featureKey, value);
        } catch (ParserConfigurationException e) {
            // Note that this may happen on every token parse.
            if (log.isDebugEnabled()) {
                log.debug(
                        "Couldn't apply feature {} to DocumentBuilderFactory {}. Can be safely ignored.",
                        featureKey,
                        dbf.getClass().getName());
            }
        }
    }
}
