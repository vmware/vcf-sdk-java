/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.utils.ssl;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link HostnameVerifier} implementation which does not perform any validations.
 *
 * <p>This implementation is only suitable for testing purposes in a controlled environment. <b>Production code must NOT
 * use this {@link HostnameVerifier} because it poses security risks!</b>
 */
public class InsecureHostnameVerifier implements HostnameVerifier {

    private static final Logger log = LoggerFactory.getLogger(InsecureHostnameVerifier.class);

    @Override
    public boolean verify(String hostname, SSLSession session) {
        log.warn(
                "Cannot validate the identity of {} due to the hostname verification being disabled. "
                        + "You are susceptible to man-in-the-middle attacks!",
                hostname);

        return true;
    }
}
