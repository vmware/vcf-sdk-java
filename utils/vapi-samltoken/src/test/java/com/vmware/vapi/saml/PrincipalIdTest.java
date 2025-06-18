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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;
import java.util.Locale.Category;

import org.junit.jupiter.api.Test;

public class PrincipalIdTest {

    private static final String NAME = "test";
    private static final String DOMAIN = "eng.vmware.com";

    // uses LATIN SMALL LETTER DOTLESS I - \u0131
    private static String TURKISH_LOWER_I_DOMAIN = "doma\u0131n";
    // uses LATIN CAPITAL LETTER I WITH DOT ABOVE - \u0130
    private static String TURKISH_UPPER_I_DOMAIN = "DOMA\u0130N";

    @Test
    public void testCreateNullDomain() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PrincipalId(NAME, null);
        });
    }

    @Test
    public void testCreateNoDomain() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PrincipalId(NAME, "");
        });
    }

    @Test
    public void testCreateNullName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PrincipalId(null, DOMAIN);
        });
    }

    @Test
    public void testCreateNoName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PrincipalId("", DOMAIN);
        });
    }

    @Test
    public void testEquality() {
        PrincipalId user1 = new PrincipalId("u1", "d1");
        PrincipalId user2 = new PrincipalId("u1", "d1");

        assertSymmetricEquality(true, user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());

        PrincipalId localUser1 = new PrincipalId("local1", DOMAIN);
        PrincipalId localUser2 = new PrincipalId(localUser1.getName(), DOMAIN);
        assertSymmetricEquality(true, localUser1, localUser2);
        assertEquals(localUser1.hashCode(), localUser2.hashCode());
    }

    @Test
    public void testEqualityNegative() {
        PrincipalId user1 = new PrincipalId("u1", "d1");
        PrincipalId user2 = new PrincipalId("u1", "d2");
        PrincipalId user3 = new PrincipalId("u2", "d1");

        assertSymmetricEquality(false, user1, user2);
        assertSymmetricEquality(false, user1, user3);
        assertSymmetricEquality(false, user2, user3);

        PrincipalId localUser1 = new PrincipalId("local1", DOMAIN);
        PrincipalId localUser2 = new PrincipalId("local2", DOMAIN);
        PrincipalId localUser1a = new PrincipalId(localUser1.getName(), DOMAIN);

        assertSymmetricEquality(false, localUser1, localUser2);
        assertSymmetricEquality(true, localUser1, localUser1a);
    }

    // Before reading the next test case, please
    //
    // see http://www.i18nguy.com/unicode/turkish-i18n.html
    //
    // might also see https://bugzilla.eng.vmware.com/show_bug.cgi?id=1374580
    @Test
    public void testEquals_EnglishLocale() {
        Locale defLocale = null;
        try {
            defLocale = Locale.getDefault(Category.FORMAT);
            Locale.setDefault(Locale.ENGLISH);

            PrincipalId p1 = new PrincipalId(NAME, "domain");
            PrincipalId p2 = new PrincipalId(NAME, "domain");
            PrincipalId p3 = new PrincipalId(NAME, "DOMAIN");

            assertEquals(p1, p2);
            assertEquals(p1.hashCode(), p2.hashCode());

            // the lower-case one on the left hand side
            assertEquals(p1, p3);
            assertEquals(p1.hashCode(), p3.hashCode());

            // the lower-case one on the right hand side
            assertEquals(p3, p1);
            assertEquals(p3.hashCode(), p1.hashCode());

            // fyi: there is a regression in jdk-1.7.(_56, _75] which breaks the
            //      asserts which follow below; if you see such test failures make
            //      sure you use jdk outside of this range
            //
            // for the curious, the regression is that the upper case dotted turkish
            // i char should map to 2 lower case chars, an ordinary english i and a
            // special dot char; however in the specified range the second char is
            // not present
            assertFalse(p1.equals(new PrincipalId(NAME, TURKISH_UPPER_I_DOMAIN)));
            assertFalse(p1.equals(new PrincipalId(NAME, TURKISH_LOWER_I_DOMAIN)));
            assertFalse(p3.equals(new PrincipalId(NAME, TURKISH_UPPER_I_DOMAIN)));
            assertFalse(p3.equals(new PrincipalId(NAME, TURKISH_LOWER_I_DOMAIN)));
            assertFalse(new PrincipalId(NAME, TURKISH_LOWER_I_DOMAIN)
                    .equals(new PrincipalId(NAME, TURKISH_UPPER_I_DOMAIN)));

        } finally {
            // put it back in order not to interfere with other tests
            Locale.setDefault(defLocale);
        }
    }

    // Before reading the next test case, please
    //
    // see http://www.i18nguy.com/unicode/turkish-i18n.html
    //
    // might also see https://bugzilla.eng.vmware.com/show_bug.cgi?id=1374580
    @Test
    public void testEquals_TurkishLocale() {
        Locale defLocale = null;
        try {
            defLocale = Locale.getDefault(Category.FORMAT);
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            // the first turkish "i" - following 2 are equal
            PrincipalId p1 = new PrincipalId(NAME, "domain");
            PrincipalId p2 = new PrincipalId(NAME, TURKISH_UPPER_I_DOMAIN);

            assertEquals(p1, p1);
            assertEquals(p1.hashCode(), p1.hashCode());

            assertEquals(p1, p2);
            assertEquals(p1.hashCode(), p2.hashCode());

            assertEquals(p2, p1);
            assertEquals(p2.hashCode(), p1.hashCode());

            assertFalse(p1.equals(new PrincipalId(NAME, "DOMAIN")));
            assertFalse(p2.equals(new PrincipalId(NAME, "DOMAIN")));

            // use second turkish "i" - following 2 are equal
            PrincipalId p3 = new PrincipalId(NAME, TURKISH_LOWER_I_DOMAIN);
            PrincipalId p4 = new PrincipalId(NAME, "DOMAIN");

            assertEquals(p3, p3);
            assertEquals(p3.hashCode(), p3.hashCode());

            assertEquals(p3, p4);
            assertEquals(p3.hashCode(), p4.hashCode());

            assertEquals(p4, p3);
            assertEquals(p4.hashCode(), p3.hashCode());

            assertFalse(p3.equals(new PrincipalId(NAME, "domain")));
            assertFalse(p4.equals(new PrincipalId(NAME, "domain")));

            assertFalse(p1.equals(p3));
            assertFalse(p3.equals(p1));
            assertFalse(p2.equals(p4));
            assertFalse(p4.equals(p2));
        } finally {
            // put it back in order not to interfere with other tests
            Locale.setDefault(defLocale);
        }
    }

    private static void assertSymmetricEquality(boolean expectedEquality, Object a, Object b) {
        if (a == null) {
            throw new IllegalArgumentException("A cannot be null");
        }
        if (b == null) {
            throw new IllegalArgumentException("B cannot be null");
        }

        assertSame(expectedEquality, a.equals(b));
        assertSame(expectedEquality, b.equals(a));
    }
}
