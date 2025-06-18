/*
 * ******************************************************************
 * Copyright (c) 2012-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.saml;

/**
 * Instances of this object will contain the subject value and the format in which it is. It represents the NameId
 * section in the Subject of each token.
 */
public final class SubjectNameId {

    // SAML 2.0:
    // if no Format value is provided, then the value
    // urn:oasis:names:tc:SAML:1.0:nameid-format:unspecified
    private static final String _defaultFormat = "urn:oasis:names:tc:SAML:1.0:nameid-format:unspecified";

    private final String value;
    private final String format;

    /**
     * @param value is the token subject name id. Cannot be null.
     * @param format is the corresponding format of subject's name id. Cannot be null.
     */
    public SubjectNameId(String value, String format) {
        if (value == null) {
            throw new IllegalArgumentException("Subject name id cannot be null");
        }

        if ((format == null) || (format.isEmpty())) {
            format = _defaultFormat;
        }

        this.value = value;
        this.format = format;
    }

    /** @return the token subject. Cannot be null. */
    public String getValue() {
        return value;
    }

    /** @return the format of the token subject. Cannot be null. */
    public String getFormat() {
        return format;
    }

    @Override
    public String toString() {
        return String.format("SubjectNameId [value=%s, format=%s]", value, format);
    }
}
