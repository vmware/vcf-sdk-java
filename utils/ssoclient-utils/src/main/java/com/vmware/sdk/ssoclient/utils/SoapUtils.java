/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.ssoclient.utils;

import javax.xml.parsers.DocumentBuilderFactory;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0.ObjectFactory;
import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0.SecurityHeaderType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** A utility class providing various helper methods for working with SOAP messages, SOAP headers, and SAML tokens. */
public class SoapUtils {

    static final ObjectFactory wsseObjFactory = new ObjectFactory();

    /**
     * Retrieves the specified property (attribute) of the given {@link Node}, returning its {@code String} value.
     *
     * @param node the {@code Node} from which to retrieve the property
     * @param propertyName the name of the property (attribute) to retrieve
     * @return the string value of the property
     * @throws NullPointerException if the attribute does not exist or the node is {@code null}
     */
    public static String getNodeProperty(Node node, String propertyName) {
        return node.getAttributes().getNamedItem(propertyName).getNodeValue();
    }

    /**
     * Finds the Security element from the given SOAP header. If it does not exist, this method creates one, inserts it
     * into the header, and returns it. If multiple security elements are found, a {@code RuntimeException} is thrown.
     *
     * @param header the {@link SOAPHeader} from which to retrieve or create the Security element
     * @return the Security element as a {@code Node}
     * @throws RuntimeException if multiple security elements exist or an error occurs creating the element
     */
    public static Node getSecurityElement(SOAPHeader header) {
        NodeList targetElement = header.getElementsByTagNameNS(Constants.WSS_NS, Constants.SECURITY_ELEMENT_NAME);
        if (targetElement == null || targetElement.getLength() == 0) {
            JAXBElement<SecurityHeaderType> value =
                    wsseObjFactory.createSecurity(wsseObjFactory.createSecurityHeaderType());
            Node headerNode = marshallJaxbElement(value).getDocumentElement();
            return header.appendChild(header.getOwnerDocument().importNode(headerNode, true));
        } else if (targetElement.getLength() > 1) {
            throw new RuntimeException(Constants.ERR_INSERTING_SECURITY_HEADER);
        }
        return targetElement.item(0);
    }

    /**
     * Returns the {@link SOAPHeader} from the given {@link SOAPMessageContext}. If the header does not exist, a new one
     * is created and returned.
     *
     * @param smc the {@link SOAPMessageContext} from which to retrieve or create the header
     * @return the existing or newly created {@link SOAPHeader}
     * @throws SOAPException if an error occurs while retrieving or adding the header
     */
    public static SOAPHeader getSOAPHeader(SOAPMessageContext smc) throws SOAPException {
        return smc.getMessage().getSOAPPart().getEnvelope().getHeader() == null
                ? smc.getMessage().getSOAPPart().getEnvelope().addHeader()
                : smc.getMessage().getSOAPPart().getEnvelope().getHeader();
    }

    /**
     * Performs an elementary test to check if the given {@link Node} potentially represents a Holder-of-Key (HoK) SAML
     * token. Throws an {@link IllegalArgumentException} if the token structure is invalid, or a
     * {@link RuntimeException} if the node does not represent a SAML token.
     *
     * @param token the {@link Node} to check
     * @return {@code true} if it is a Holder-of-Key SAML token, otherwise {@code false}
     * @throws IllegalArgumentException if the token structure is invalid (e.g., more than one SubjectConfirmation)
     * @throws RuntimeException if the node does not represent a SAML token
     */
    public static boolean isHoKToken(Node token) {
        if (isSamlToken(token)) {
            NodeList elements = ((Element) token)
                    .getElementsByTagNameNS(
                            Constants.URN_OASIS_NAMES_TC_SAML_2_0_ASSERTION, Constants.SUBJECT_CONFIRMATION);
            if (elements.getLength() != 1) {
                throw new IllegalArgumentException(Constants.ERR_NOT_A_SAML_TOKEN);
            }
            Node value = elements.item(0).getAttributes().getNamedItem(Constants.METHOD);
            return Constants.URN_OASIS_NAMES_TC_SAML_2_0_CM_HOLDER_OF_KEY.equalsIgnoreCase(value.getNodeValue());
        }
        throw new RuntimeException("The Node does not represnt a SAML token");
    }

    /**
     * Indicates whether the {@link SOAPMessageContext} is for an outgoing (request) message.
     *
     * @param smc the {@link SOAPMessageContext} to check
     * @return {@code true} if the message is outgoing; {@code false} otherwise
     */
    public static boolean isOutgoingMessage(SOAPMessageContext smc) {
        Boolean outboundProperty = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        return outboundProperty.booleanValue();
    }

    /**
     * Performs an elementary test to check if the given {@link Node} potentially represents a SAML token. This is
     * determined by its namespace URI and local name.
     *
     * @param token the {@link Node} to check
     * @return {@code true} if the node is a SAML token; {@code false} otherwise
     */
    public static boolean isSamlToken(Node token) {
        boolean isValid = false;
        isValid = (Constants.URN_OASIS_NAMES_TC_SAML_2_0_ASSERTION.equalsIgnoreCase(token.getNamespaceURI()))
                && ("assertion".equalsIgnoreCase(token.getLocalName()));
        return isValid;
    }

    /**
     * Marshals the specified {@link JAXBElement} into a new {@link Document}.
     *
     * @param <T> the type of the JAXB element
     * @param jaxbElement the {@link JAXBElement} to marshal
     * @return a new {@link Document} containing the marshalled content
     * @throws RuntimeException if an error occurs during the marshalling process
     */
    public static final <T> Document marshallJaxbElement(JAXBElement<T> jaxbElement) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document result = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Constants.WS_1_3_TRUST_JAXB_PACKAGE + ":"
                    + Constants.WSSE_JAXB_PACKAGE + ":"
                    + Constants.WSSU_JAXB_PACKAGE);
            result = dbf.newDocumentBuilder().newDocument();
            jaxbContext.createMarshaller().marshal(jaxbElement, result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public static String samlTokenToString(Element token) {
        StringBuilder sb = new StringBuilder();
        if (isSamlToken(token)) {
            sb.append("Token details:");
            sb.append("\n");

            sb.append("\tAssertionId = ");
            sb.append(SoapUtils.getNodeProperty(token, "ID"));
            sb.append("\n");

            sb.append("\tToken type = ");
            sb.append((isHoKToken(token) ? "Holder-Of-Key" : "Bearer"));
            sb.append("\n");

            sb.append("\tIssued On = ");
            sb.append(SoapUtils.getNodeProperty(token, "IssueInstant"));
        } else {
            sb.append("Invalid token");
        }
        return sb.toString();
    }
}
