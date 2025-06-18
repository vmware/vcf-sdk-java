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

import java.security.SignatureException;

import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;

/**
 * Interface used for signing the SOAP requests
 *
 * @author Ecosystem Engineering
 */
public interface WsSecuritySignature {

    /**
     * Signs the SOAP Message.
     *
     * @param message the message to be signed
     * @return the signed Message
     * @throws SignatureException if signing fails
     * @throws SOAPException if message is invalid (for example has a wrong header)
     */
    SOAPMessage sign(SOAPMessage message) throws SignatureException, SOAPException;
}
