/*
 * ******************************************************************
 * Copyright (c) 2010-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.saml;

import java.util.Arrays;

import com.vmware.vapi.internal.saml.ValidateUtil;

/** Uniquely identifies particular principal */
public final class PrincipalId {

    /** Principal name */
    private final String _name;

    /** Principal domain */
    private final String _domain;

    /**
     * Construct a principal identifier by domain name where he/she is located and short name which should be unique in
     * scope of the given domain
     *
     * @param name principal short name (e.g. jdoe); requires {@code not-null} and not empty string value
     * @param domain domain name or alias (e.g. vmware.com); requires {@code not-null} and not empty string value;
     */
    public PrincipalId(String name, String domain) {

        ValidateUtil.validateNotEmpty(name, "name");
        ValidateUtil.validateNotEmpty(domain, "domain");

        _name = name;
        _domain = domain;
    }

    /** @return the name; {@code not-null} and not empty string value */
    public String getName() {
        return _name;
    }

    /** @return the domain; {@code not-null} and not empty string value */
    public String getDomain() {
        return _domain;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!obj.getClass().equals(PrincipalId.class)) {
            return false;
        }

        PrincipalId other = (PrincipalId) obj;
        return _name.equals(other._name) && _domain.toLowerCase().equals(other._domain.toLowerCase());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] {_name, _domain.toLowerCase()});
    }

    @Override
    public String toString() {
        return String.format("{Name: %s, Domain: %s}", getName(), getDomain());
    }
}
