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

import com.vmware.sdk.utils.wsdl.PortConfigurer;
import com.vmware.vsan.sdk.VsanhealthPortType;

/**
 * This ESXi client can be used to access its WSDL-based APIs.
 *
 * @see #getVimPort()
 * @see #getVsanPort()
 */
public class ESXiClient extends VimClient {

    public ESXiClient(
            String serverAddress, int port, PortConfigurer portConfigurer, SessionIdProvider vimSessionProvider) {
        super(serverAddress, port, portConfigurer, vimSessionProvider);
    }

    /**
     * Creates a fresh vSAN port for accessing vSAN APIs on ESXi. The port is fully configured and authenticated.
     *
     * @return new vSAN port.
     */
    public VsanhealthPortType getVsanPort() {
        return getVsanPort(ESXiClientFactory::createVsanEsxUrl);
    }
}
