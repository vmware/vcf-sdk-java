/*
 * ******************************************************************
 * Copyright (c) 2021-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.internal.cis.authn.json;

/** The enum values represent the vAPI names of the supported signature algorithms. */
public enum JsonSignatureAlgorithm {
    RS256("SHA256withRSA"),
    RS384("SHA384withRSA"),
    RS512("SHA512withRSA"),
    ES256("SHA256withECDSA"),
    ES384("SHA384withECDSA"),
    ES512("SHA512withECDSA");

    private final String javaName;

    private JsonSignatureAlgorithm(String javaName) {
        this.javaName = javaName;
    }

    /** @return The JDK standard name of the signature algorithm */
    public String getJavaName() {
        return javaName;
    }

    /** @return RS256 - the default signature algorithm */
    public static JsonSignatureAlgorithm getDefault() {
        return RS256;
    }
}
