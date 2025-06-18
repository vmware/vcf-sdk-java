/*
 * ******************************************************************
 * Copyright (c) 2010-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.internal.saml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.vmware.vapi.internal.saml.exception.ParserException;
import com.vmware.vapi.saml.PrincipalId;

/** Unit test for {@link PrincipalIdParser} class */
public class PrincipalIdParserTest {

    private static final String DOMAIN = "eng.vmware.com";
    private static final String NAME = "jdoe";
    private static final char SEPARATOR = '@';
    private static final String VMWAREID_DOMAIN = "vmwareid";

    @Test
    public void testParseNullStringValue() throws ParserException {
        assertThrows(IllegalArgumentException.class, () -> {
            PrincipalIdParser.parseUpn(null);
        });
    }

    @Test
    public void testParseEmptyStringValue() throws ParserException {
        assertThrows(IllegalArgumentException.class, () -> {
            PrincipalIdParser.parseUpn("");
        });
    }

    @Test
    public void testParseWithManySeparators() throws ParserException {
        assertThrows(ParserException.class, () -> {
            String strValue = String.format("%s%2$c%s%2$c", NAME, SEPARATOR, DOMAIN);
            PrincipalIdParser.parseUpn(strValue);
        });
    }

    @Test
    public void testParseVmwareidWithManySeparatorsIncludingDomain() throws ParserException {
        String email = String.format("%s%c%s", NAME, SEPARATOR, DOMAIN);
        String vmwareidUpn = String.format("%s%c%s", email, SEPARATOR, VMWAREID_DOMAIN);
        PrincipalId result = PrincipalIdParser.parseUpn(vmwareidUpn);
        assertEquals(new PrincipalId(email, VMWAREID_DOMAIN), result);
    }

    @Test
    public void testParse() throws ParserException {
        String strValue = String.format("%s%c%s", NAME, SEPARATOR, DOMAIN);

        PrincipalId result = PrincipalIdParser.parseUpn(strValue);

        assertEquals(new PrincipalId(NAME, DOMAIN), result);
    }

    @Test
    public void testParseNoDomain() throws ParserException {
        assertThrows(ParserException.class, () -> {
            PrincipalIdParser.parseUpn(String.format("%s%c", NAME, SEPARATOR));
        });
    }

    @Test
    public void testParseNoName() throws ParserException {
        assertThrows(ParserException.class, () -> {
            PrincipalIdParser.parseUpn(String.format("%c%s", SEPARATOR, DOMAIN));
        });
    }

    @Test
    public void testParseNoSeparator() throws ParserException {
        assertThrows(ParserException.class, () -> {
            PrincipalIdParser.parseUpn(NAME + DOMAIN);
        });
    }
}
