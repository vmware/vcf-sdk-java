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

import com.vmware.vapi.internal.saml.ValidateUtil;

/**
 * Instances of this object will contain the issuer value and the format in which it is. It represents the NameId
 * section in the Issuer of each token.
 */
public final class IssuerNameId {
    // SAML 2.0:
    // Overriding the usual rule for this element's type,
    // if no Format value is provided with this element, then the
    // value urn:oasis:names:tc:SAML:2.0:nameid-format:entity is in effect
    private static final String _defaultFormat = "urn:oasis:names:tc:SAML:2.0:nameid-format:entity";

    private final String _value;
    private final String _format;

    /**
     * @param value is the token issuer name id. Cannot be null/empty.
     * @param format is the corresponding format of issuer's name id.
     */
    public IssuerNameId(String value, String format) {
        ValidateUtil.validateNotEmpty(value, value);

        if ((format == null) || (format.isEmpty())) {
            // SAML 2.0:
            // Overriding the usual rule for this element's type,
            // if no Format value is provided with this element, then the
            // value urn:oasis:names:tc:SAML:2.0:nameid-format:entity is in effect
            format = _defaultFormat;
        }

        this._value = value;
        this._format = format;
    }

    /** @return the token Issuer. Cannot be null. */
    public String getValue() {
        return this._value;
    }

    /** @return the format of the token Issuer. Cannot be null. */
    public String getFormat() {
        return this._format;
    }

    @Override
    public String toString() {
        return String.format("IssuerNameId [value=%s, format=%s]", this._value, this._format);
    }
}
