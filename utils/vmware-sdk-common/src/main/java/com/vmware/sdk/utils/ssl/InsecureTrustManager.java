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

import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link X509TrustManager} implementation which does not perform any certificate or trust chain validations.
 *
 * <p>This implementation is only suitable for testing purposes in a controlled environment. <b>Production code must NOT
 * use this {@link X509TrustManager} because it poses security risks!</b>
 */
public class InsecureTrustManager implements X509TrustManager {

    private static final Logger log = LoggerFactory.getLogger(InsecureTrustManager.class);

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
        warn();
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
        warn();
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    private static void warn() {
        log.warn("Skipped the validation of a certificate chain due to "
                + "configuration policy. Your connection is not secure!");
    }
}
