/*
 * ******************************************************************
 * Copyright (c) 2012-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.internal.cis.authn.json;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.vmware.vapi.Message;
import com.vmware.vapi.MessageFactory;
import com.vmware.vapi.dsig.json.SignatureException;
import com.vmware.vapi.dsig.json.StsTrustChain;
import com.vmware.vapi.internal.cis.authn.Signer;
import com.vmware.vapi.internal.dsig.json.Canonicalizer;
import com.vmware.vapi.internal.dsig.json.Verifier;
import com.vmware.vapi.internal.protocol.common.json.JsonSecurityContextSerializer;
import com.vmware.vapi.internal.util.Validate;

/** The JSON implementation of the {@link Signer} interface. */
public final class JsonSignerImpl implements Signer, Verifier {

    private static final Message SIGN_ERROR = MessageFactory.getMessage("vapi.signature.sign");
    private static final Message VERIFY_ERROR = MessageFactory.getMessage("vapi.signature.verify");
    private final Canonicalizer jsonCanonicalizer;
    private final StsTrustChain stsTrustChain;
    private final JsonSecurityContextSerializer deserializer = new JsonSecurityContextSerializer();

    /** @param jsonCanonicalizer cannot be null. */
    public JsonSignerImpl(Canonicalizer jsonCanonicalizer) {
        this(jsonCanonicalizer, null);
    }

    /**
     * @param jsonCanonicalizer cannot be null.
     * @param stsTrustChain StsTrustChain retriever. Can be null if the current instance will be used only as a
     *     {@link Signer} but not as a {@link Verifier}.
     */
    public JsonSignerImpl(Canonicalizer jsonCanonicalizer, StsTrustChain stsTrustChain) {
        Validate.notNull(jsonCanonicalizer);

        this.jsonCanonicalizer = jsonCanonicalizer;
        this.stsTrustChain = stsTrustChain;
    }

    @Override
    public String sign(byte[] jsonMessage, PrivateKey privateKey, JsonSignatureAlgorithm alg) {
        Validate.notNull(jsonMessage);
        Validate.notNull(privateKey);

        String signatureValue;
        try {
            signatureValue = signInternal(jsonCanonicalizer.asCanonical(jsonMessage), privateKey, alg);
        } catch (InvalidKeyException e) {
            throw new SignatureException(SIGN_ERROR, e);
        } catch (NoSuchAlgorithmException e) {
            throw new SignatureException(SIGN_ERROR, e);
        } catch (java.security.SignatureException e) {
            throw new SignatureException(SIGN_ERROR, e);
        }

        return signatureValue;
    }

    @Override
    public boolean verifySignature(byte[] jsonMessage, Map<String, Object> signature, long clockToleranceSec) {

        Validate.notNull(jsonMessage);
        Validate.notNull(signature);
        Validate.isTrue(clockToleranceSec > -1);
        if (stsTrustChain == null) {
            throw new IllegalStateException("STS trust chain retriever not set");
        }

        // the signature should be removed from the message before validation
        byte[] message = deserializer.removeSignature(jsonMessage);

        try {
            JsonSignatureStruct jsonSignature = JsonSignatureStruct.parseJsonSignatureStruct(
                    signature, stsTrustChain.getStsTrustChain(), clockToleranceSec);

            return verify(
                    jsonSignature.getSamlToken().getConfirmationCertificate(),
                    jsonCanonicalizer.asCanonical(message),
                    jsonSignature.getSigValue(),
                    jsonSignature.getAlg());
        } catch (InvalidKeyException e) {
            throw new SignatureException(VERIFY_ERROR, e);
        } catch (NoSuchAlgorithmException e) {
            throw new SignatureException(VERIFY_ERROR, e);
        } catch (java.security.SignatureException e) {
            throw new SignatureException(VERIFY_ERROR, e);
        }
    }

    /**
     * Signs the payload
     *
     * @param payload The data to be signed
     * @param privateKey The private key to sign the payload with
     * @param alg The algorithm used to sign the payload
     */
    private String signInternal(byte[] payload, PrivateKey privateKey, JsonSignatureAlgorithm alg)
            throws NoSuchAlgorithmException, InvalidKeyException, java.security.SignatureException {
        if (payload == null) {
            throw new IllegalArgumentException("Payload cannot be null");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }

        Signature rsa = Signature.getInstance(alg.getJavaName());
        rsa.initSign(privateKey);
        rsa.update(payload);

        return Base64.encodeBase64String(rsa.sign());
    }

    /**
     * Verifies the payload signature
     *
     * @param alg The signature algorithm used
     */
    private boolean verify(X509Certificate certificate, byte[] jsonMessage, String b64Signature, String alg)
            throws NoSuchAlgorithmException, InvalidKeyException, java.security.SignatureException {
        if (certificate == null) {
            throw new IllegalArgumentException("Certificate cannot be null");
        }
        if (jsonMessage == null) {
            throw new IllegalArgumentException("Json message cannot be null");
        }
        if (b64Signature == null) {
            throw new IllegalArgumentException("Base64 signature cannot be null");
        }
        JsonSignatureAlgorithm signAlg;
        try {
            signAlg = JsonSignatureAlgorithm.valueOf(alg);
        } catch (IllegalArgumentException e) {
            throw new SignatureException(MessageFactory.getMessage("vapi.signature.unknowndsigalg", alg));
        }
        Signature signature = Signature.getInstance(signAlg.getJavaName());
        signature.initVerify(certificate);
        signature.update(jsonMessage);

        return signature.verify(Base64.decodeBase64(b64Signature));
    }
}
