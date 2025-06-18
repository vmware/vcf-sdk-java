/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.utils.wsdl;

import java.net.URI;

import jakarta.xml.ws.BindingProvider;

/**
 * An interface whose responsibility is to configure a {@link BindingProvider}.
 *
 * <p>Implementations are not expected to be portable i.e. it is expected for the implementation to depend on specific
 * implementation, but this should be documented accordingly.
 */
public interface PortConfigurer {

    /**
     * Configures the given port with the already provided settings such as timeouts and TLS properties.
     *
     * @param provider the port to configure
     * @param url the address of the remote service, including its path
     */
    void configure(BindingProvider provider, URI url);
}
