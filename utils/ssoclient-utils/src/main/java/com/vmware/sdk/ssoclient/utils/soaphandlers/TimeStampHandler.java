/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.ssoclient.utils.soaphandlers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_utility_1_0.AttributedDateTime;
import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_utility_1_0.ObjectFactory;
import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_utility_1_0.TimestampType;
import org.w3c.dom.Node;

import com.vmware.sdk.ssoclient.utils.Constants;
import com.vmware.sdk.ssoclient.utils.SoapUtils;

/**
 * Handler class to add the TimeStamp element inside the security header
 *
 * @author Ecosystem Engineering
 */
public class TimeStampHandler extends SSOHeaderHandler {

    public static final String GMT = "GMT";

    /**
     * Creates a datetime formatter needed for populating objects containing XML requests/responses.
     *
     * @return the date formatter
     */
    public static DateFormat createDateFormatter() {
        DateFormat dateFormat = new SimpleDateFormat(Constants.XML_DATE_FORMAT);
        // always send UTC/GMT time
        dateFormat.setTimeZone(TimeZone.getTimeZone(TimeStampHandler.GMT));
        return dateFormat;
    }

    /**
     * Creates a timestamp WS-Security element. It is needed for the STS to tell if the request is invalid due to slow
     * delivery
     *
     * @return timestamp element issued with start date = NOW and expiration date = NOW + REQUEST_VALIDITY_IN_MINUTES
     */
    private JAXBElement<TimestampType> createTimestamp() {
        ObjectFactory wssuObjFactory = new ObjectFactory();

        TimestampType timestamp = wssuObjFactory.createTimestampType();

        final long now = System.currentTimeMillis();
        Date createDate = new Date(now);
        Date expirationDate = new Date(now + TimeUnit.MINUTES.toMillis(Constants.REQUEST_VALIDITY_IN_MINUTES));

        DateFormat wssDateFormat = createDateFormatter();
        AttributedDateTime createTime = wssuObjFactory.createAttributedDateTime();
        createTime.setValue(wssDateFormat.format(createDate));

        AttributedDateTime expirationTime = wssuObjFactory.createAttributedDateTime();
        expirationTime.setValue(wssDateFormat.format(expirationDate));

        timestamp.setCreated(createTime);
        timestamp.setExpires(expirationTime);
        return wssuObjFactory.createTimestamp(timestamp);
    }

    @Override
    public boolean handleMessage(SOAPMessageContext smc) {
        if (SoapUtils.isOutgoingMessage(smc)) {
            try {
                Node securityNode = SoapUtils.getSecurityElement(SoapUtils.getSOAPHeader(smc));
                Node timeStampNode =
                        SoapUtils.marshallJaxbElement(createTimestamp()).getDocumentElement();
                securityNode.appendChild(securityNode.getOwnerDocument().importNode(timeStampNode, true));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }
}
