/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.ssoclient.utils.wssecurity;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.namespace.QName;

import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.vmware.sdk.ssoclient.utils.Constants;

public abstract class WsSecuritySignatureImpl implements WsSecuritySignature {

    protected final XMLSignatureFactory xmlSigFactory = XMLSignatureFactory.getInstance();
    private final PrivateKey _privateKey;
    private final X509Certificate _userCert;

    public PrivateKey getPrivateKey() {
        return _privateKey;
    }

    public X509Certificate getUserCert() {
        return _userCert;
    }

    public WsSecuritySignatureImpl(PrivateKey privateKey, X509Certificate userCert) {
        _privateKey = privateKey;
        _userCert = userCert;
    }

    @Override
    public SOAPMessage sign(SOAPMessage message) throws SignatureException, SOAPException {

        try {
            CanonicalizationMethod canonicalizationMethod = xmlSigFactory.newCanonicalizationMethod(
                    CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null);
            SignatureMethod signatureMethod = xmlSigFactory.newSignatureMethod(Constants.RSA_WITH_SHA512, null);
            ArrayList<String> refList = new ArrayList<String>();
            refList.add(createSoapBodyUuid(message));
            refList.add(createTimestampUuid(message));
            List<Reference> references = createSignatureReferences(refList);
            SignedInfo signedInfo = xmlSigFactory.newSignedInfo(canonicalizationMethod, signatureMethod, references);

            KeyInfoFactory kif = KeyInfoFactory.getInstance();
            KeyInfo ki = kif.newKeyInfo(Collections.singletonList(new DOMStructure(createKeyInfoContent(message))));

            XMLSignature signature =
                    xmlSigFactory.newXMLSignature(signedInfo, ki, null, addUseKeySignatureId(message), null);

            DOMSignContext dsc =
                    new DOMSignContext(getPrivateKey(), message.getSOAPHeader().getFirstChild());
            dsc.putNamespacePrefix(XMLSignature.XMLNS, Constants.DIGITAL_SIGNATURE_NAMESPACE_PREFIX);

            signature.sign(dsc);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return message;
    }

    protected abstract String addUseKeySignatureId(SOAPMessage message);

    protected abstract Node createKeyInfoContent(SOAPMessage message);

    protected List<Reference> createSignatureReferences(ArrayList<String> referenceIdList)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        List<Reference> result = new ArrayList<Reference>();

        for (String refId : referenceIdList) {
            if (refId == null) {
                continue;
            }

            Reference ref = xmlSigFactory.newReference(
                    "#" + refId,
                    xmlSigFactory.newDigestMethod(DigestMethod.SHA512, null),
                    Collections.singletonList(xmlSigFactory.newCanonicalizationMethod(
                            CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null)),
                    null,
                    null);

            result.add(ref);
        }

        return Collections.unmodifiableList(result);
    }

    protected String createTimestampUuid(SOAPMessage message) throws SOAPException {
        NodeList timestampList = message.getSOAPHeader()
                .getOwnerDocument()
                .getElementsByTagNameNS(Constants.WSU_NAMESPACE, Constants.WSU_TIMESTAMP_LOCAL_NAME);

        if (timestampList.getLength() > 1) {
            throw new RuntimeException("Assertion: multiple " + Constants.WSU_TIMESTAMP_LOCAL_NAME + " received.");
        }

        if (timestampList.getLength() == 1) {
            if (timestampList.item(0).getNodeType() != Node.ELEMENT_NODE) {
                throw new RuntimeException(
                        "Assertion: " + timestampList.item(0).getNodeType() + " is not expected value");
            }

            Element timestamp = (Element) timestampList.item(0);
            String timestampId = "_" + UUID.randomUUID().toString();
            timestamp.setAttributeNS(
                    Constants.WSU_NAMESPACE, timestamp.getPrefix() + ":" + Constants.WSU_ID_LOCAL_NAME, timestampId);
            return timestampId;
        }

        System.out.println("Timestamp element not found in the message");
        return null;
    }

    protected String createSoapBodyUuid(SOAPMessage message) throws SOAPException {
        String bodyId = "_" + UUID.randomUUID().toString();
        message.getSOAPBody()
                .addAttribute(
                        new QName(Constants.WSU_NAMESPACE, Constants.WSU_ID_LOCAL_NAME, Constants.WSU_PREFIX), bodyId);
        return bodyId;
    }
}
