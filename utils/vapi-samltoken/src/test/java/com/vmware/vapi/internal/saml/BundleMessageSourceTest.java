/*
 * ******************************************************************
 * Copyright (c) 2012-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.internal.saml;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.vmware.vapi.saml.BundleMessageSource;
import com.vmware.vapi.saml.BundleMessageSource.Key;
import com.vmware.vapi.saml.exception.InvalidTimingException;
import com.vmware.vapi.saml.exception.InvalidTokenException;
import com.vmware.vapi.saml.exception.MalformedTokenException;

public class BundleMessageSourceTest {

    @Test
    public void testAllStandardMessages() {
        Locale missing = Locale.CANADA;
        // We use the armenian locale for partial l10n test, so we are
        // sure that such translation doesn't exist on the main classpath
        Locale partial = Locale.forLanguageTag("am");
        Collection<Locale> locales = Arrays.asList(Locale.getDefault(), missing, partial);
        for (Locale locale : locales) {
            BundleMessageSource bundle = new BundleMessageSource(locale);
            for (Key messageKey : Key.values()) {
                assertNotNull(bundle.get(messageKey));
            }
        }
    }

    @Test
    public void testCreateMessageForException() {
        InvalidTokenException exception = new InvalidTimingException("", Key.INVALID_SAML_TOKEN, null, 111);
        BundleMessageSource messageSource = new BundleMessageSource(Locale.getDefault());
        String msg = messageSource.createMessage(exception);
        assertTrue(msg.contains("111"));
    }

    @Test
    public void testCreateMessageWithLocalizableCause() {
        InvalidTokenException exception =
                new MalformedTokenException("", Key.INVALID_SAML_TOKEN, null, Key.MALFORMED_SAML_TOKEN);
        BundleMessageSource messageSource = new BundleMessageSource(Locale.getDefault());
        String msg = messageSource.createMessage(exception);
        assertTrue(msg.contains(messageSource.get(Key.MALFORMED_SAML_TOKEN)));
    }

    @Test
    public void testDifferentMessagesForDifferentLocales() {
        BundleMessageSource defMessages = new BundleMessageSource(Locale.getDefault());
        BundleMessageSource otherLangMessages = new BundleMessageSource(Locale.forLanguageTag("am"));
        assertTrue(!defMessages.get(Key.MALFORMED_SAML_TOKEN).equals(otherLangMessages.get(Key.MALFORMED_SAML_TOKEN)));
    }
}
