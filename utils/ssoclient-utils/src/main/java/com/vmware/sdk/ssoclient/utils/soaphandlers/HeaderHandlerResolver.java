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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.handler.HandlerResolver;
import jakarta.xml.ws.handler.PortInfo;

/**
 * Reference implementation of the {@link HandlerResolver} interface with an additional method addHandler to add a new
 * {@link SSOHeaderHandler} to the chain.
 *
 * @author Ecosystem Engineering
 */
@SuppressWarnings("rawtypes")
public final class HeaderHandlerResolver implements HandlerResolver {

    private final List<Handler> handlerChain = new ArrayList<Handler>();

    @Override
    public List<Handler> getHandlerChain(PortInfo portInfo) {
        return Collections.unmodifiableList(handlerChain);
    }

    /**
     * Adds a specific {@link SSOHeaderHandler} to the handler chain
     *
     * @param ssoHandler the {@link SSOHeaderHandler} to add
     */
    public void addHandler(SSOHeaderHandler ssoHandler) {
        handlerChain.add(ssoHandler);
    }

    /** Clears the current list of {@link SSOHeaderHandler} in the handler chain */
    public void clearHandlerChain() {
        handlerChain.clear();
    }
}
