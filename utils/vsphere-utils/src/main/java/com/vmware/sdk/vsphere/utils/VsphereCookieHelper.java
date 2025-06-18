/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.vsphere.utils;

import static jakarta.xml.ws.handler.MessageContext.HTTP_REQUEST_HEADERS;
import static jakarta.xml.ws.handler.MessageContext.HTTP_RESPONSE_HEADERS;
import static java.util.Collections.singletonList;
import static org.apache.cxf.headers.Header.HEADER_LIST;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.ws.BindingProvider;

import org.apache.cxf.headers.Header;
import org.apache.cxf.jaxb.JAXBDataBinding;

import com.vmware.vim25.VimPortType;

public class VsphereCookieHelper {

    /** The name of the cookie from which a sessionId is going to be extracted upon successful vCenter login. */
    public static final String VMWARE_SOAP_SESSION_COOKIE = "vmware_soap_session";

    /**
     * Configures the given port to send a "vcSessionCookie" SOAP header.
     *
     * @param provider the port to configure - it could be for vslm, sms or pbm
     * @param cookie the {@link #VMWARE_SOAP_SESSION_COOKIE} value extracted after successful vCenter login
     */
    public static void configureOutgoingSoapCookieHeader(BindingProvider provider, String cookie) {
        @SuppressWarnings("unchecked")
        List<Header> outgoingSoapHeaders =
                (List<Header>) provider.getRequestContext().getOrDefault(HEADER_LIST, new ArrayList<>());
        try {
            QName name = new QName("#", "vcSessionCookie");
            outgoingSoapHeaders.add(new Header(name, cookie, new JAXBDataBinding(String.class)));
            provider.getRequestContext().put(HEADER_LIST, outgoingSoapHeaders);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static void configureOutgoingCookie(BindingProvider vimPort, String cookie) {
        // TODO: this should be moved to an authenticator of some type
        Map<String, Object> ctx = vimPort.getRequestContext();
        @SuppressWarnings("unchecked")
        Map<String, List<String>> headers =
                (Map<String, List<String>>) ctx.getOrDefault(HTTP_REQUEST_HEADERS, new HashMap<>());
        headers.put("Cookie", singletonList(new HttpCookie(VMWARE_SOAP_SESSION_COOKIE, cookie).toString()));
        ctx.put(HTTP_REQUEST_HEADERS, headers);
    }

    /**
     * This method should be invoked after successfully logging into vCenter - it examines the response headers, looks
     * for the Set-Cookie header and attempts to extract the {@link #VMWARE_SOAP_SESSION_COOKIE} cookie.
     *
     * @param vimPort the port used to perform the login
     * @return the header value
     */
    public static String extractSessionId(VimPortType vimPort) {
        return HttpCookie.parse(getCookieHeader(vimPort)).stream()
                .filter(c -> VMWARE_SOAP_SESSION_COOKIE.equals(c.getName()))
                .findFirst()
                .map(HttpCookie::getValue)
                .orElseThrow(
                        () -> new RuntimeException("Could not extract session id because the cookie was not found"));
    }

    /**
     * Inspects the response headers and returns the first Set-Cookie value.
     *
     * <p>This method assumes that there is <b>exactly</b> one Set-Cookie value. If there are 0 or more than one, throws
     * an exception.
     *
     * @param vimPort port used to login
     * @return the Set-Cookie header value
     */
    protected static String getCookieHeader(VimPortType vimPort) {
        Map<String, Object> context = ((BindingProvider) vimPort).getResponseContext();
        @SuppressWarnings("unchecked")
        Map<String, List<String>> responseHeaders = (Map<String, List<String>>) context.get(HTTP_RESPONSE_HEADERS);

        if (responseHeaders == null) {
            throw new RuntimeException(
                    "Could not extract extract a session id because the server did not provide cookies");
        }

        List<String> cookies = responseHeaders.get("Set-Cookie");
        if (cookies == null || cookies.isEmpty()) {
            throw new RuntimeException(
                    "Could not extract extract a session id because the server did not provide cookies");
        }

        if (cookies.size() > 1) {
            throw new RuntimeException(
                    "Could not extract extract a session id because the server responded with multiple Set-Cookie headers");
        }
        return cookies.get(0);
    }
}
