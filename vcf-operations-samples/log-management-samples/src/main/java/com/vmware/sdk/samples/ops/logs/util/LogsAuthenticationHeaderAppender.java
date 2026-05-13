/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.logs.util;

import com.vmware.vapi.core.ExecutionContext;
import com.vmware.vapi.data.DataValue;
import com.vmware.vapi.internal.protocol.client.rest.authn.HeaderAuthenticationAppenderBase;
import com.vmware.vapi.internal.protocol.client.rpc.HttpRequest;

public class LogsAuthenticationHeaderAppender extends HeaderAuthenticationAppenderBase {

    public static final String AUTHORIZATION_HEADER = "X-JWT-Token";
    String logsToken;

    public LogsAuthenticationHeaderAppender() {}

    public HttpRequest handle(String serviceId, String operationId, HttpRequest request, DataValue params, ExecutionContext context) {

        return addAuthorizationHeader(logsToken, request);

    }

    private static HttpRequest addAuthorizationHeader(String token, HttpRequest request) {
        request.addHeader(AUTHORIZATION_HEADER, token);
        return request;
    }

    @Override
    protected String getHeaderName() {
        return AUTHORIZATION_HEADER;
    }

    @Override
    protected String getHeaderValue(String token) {
        return logsToken;
    }

    public void setToken(String token) {
        logsToken = token;
    }
}
