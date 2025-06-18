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

import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vapi.internal.saml.ValidateUtil;
import com.vmware.vapi.saml.exception.SsoException;
import com.vmware.vapi.saml.exception.SsoRuntimeException;

/**
 * Simplification of the {@link java.util.ResourceBundle} class for the purpose of i18n of the STS client error messages
 *
 * <p>Thread-safe.
 */
public class BundleMessageSource {

    public static enum Key {
        MALFORMED_SAML_TOKEN("malformedSamlToken"),
        INVALID_SAML_TOKEN("invalidSamlToken"),

        // types specific to wst client
        INVALID_CREDENTIALS("invalidCredentials"),
        INVALID_REQUESTED_TIME_RANGE("invalidRequestedTimeRange"),
        RENEW_NEEDED("renewNeeded"),
        POTENITAL_TAMPERING_OF_REQUEST("potenitalTamperingOfRequest"),
        REQUEST_EXPIRED("requestExpired"),
        SIGNATURE_VALIDATION_FAULT("signatureValidationFault"),
        UNEXPECTED_SERVER_ERROR("unexpectedServerError"),
        TIME_SYNCHRONIZATION_ERROR("timeSynchronizationError"),
        UNEXPECTED_RESPONSE_FORMAT("unexpectedResponseFormat"),
        INTERNAL_CLIENT_ERROR("internalClientError"),
        BAD_SERVER_SSL_CERTIFICATE("serverSslCertficateIsNotTrusted"),
        FAILED_TO_CONNECT_TO_SERVER("failedToConnectToServer"),
        ACCOUNT_LOCKED("accountLocked"),
        EXPIRED_PASSWORD("expiredPassword"),
        REQUEST_AFFECTED_BY_TIME_SKEW("requestAffectedByTimeSkew"),
        INVALID_REQUEST("invalidRequest"),

        // Types of internal client error
        PARSER_ERROR("serializationError"),
        FAILED_TO_SIGN_REQUEST("failedToSignRequest");

        private final String _bundleKey;

        private Key(String bundleKey) {
            _bundleKey = bundleKey;
        }
    }

    private static final String BASE_NAME = "com.vmware.vapi.internal.saml.messages.Messages";
    private final Logger log = LoggerFactory.getLogger(BundleMessageSource.class);

    private final ResourceBundle _messages;

    /**
     * Creates a new message resource, based on the com.vmware.vim.sso.client.impl.
     * BundleMessageSource.messages.Messages_locale resource file.
     *
     * @param locale required, can be {@link Locale#getDefault()}
     * @see ResourceBundle#getBundle(String)
     */
    public BundleMessageSource(Locale locale) {
        _messages = ResourceBundle.getBundle(BASE_NAME, locale);
    }

    /**
     * @param key type of message needed, required
     * @return a non-null, possibly empty, message in the locale of this message source
     */
    public String get(Key key) {
        ValidateUtil.validateNotNull(key, "key");
        return _messages.getString(key._bundleKey);
    }

    /**
     * Creates a localized message for this exception.
     *
     * <p>Localizable are exceptions with e.getKey != null
     *
     * @param e exception, required
     * @return a localized message, if exception is localizable; e.getMessage otherwise
     */
    public String createMessage(SsoException e) {
        return createMessage(e.getMessageKey(), e.getMessageDetails(), e.getMessage());
    }

    /**
     * Creates a localized message for this exception.
     *
     * <p>Localizable are exceptions with e.getKey != null
     *
     * @param e exception, required
     * @return a localized message, if exception is localizable; e.getMessage otherwise
     */
    public String createMessage(SsoRuntimeException e) {
        return createMessage(e.getMessageKey(), e.getMessageDetails(), e.getMessage());
    }

    /**
     * Creates a localized message
     *
     * <p>The method uses String.format using the localized message, corresponding to the key, as pattern and the
     * {@code details} objects as parameters.
     *
     * @param messageKey the locale-neutral message key, optional
     * @param details , parameters to be filled in the pattern that corresponds to the message key, optional
     * @param fallbackMessage fallback to use if the message key is null
     * @return a message, not null if at least one of messageKey and fallbackMessage are not null
     */
    String createMessage(Key messageKey, Object[] details, String fallbackMessage) {
        if (messageKey == null) {
            return fallbackMessage;
        }
        Object[] localizedDetails = new Object[details != null ? details.length : 0];
        for (int i = 0; i < localizedDetails.length; ++i) {
            if (details[i] instanceof Key) {
                localizedDetails[i] = get((Key) details[i]);
            } else {
                localizedDetails[i] = details[i];
            }
        }
        try {
            return String.format(get(messageKey), localizedDetails);
        } catch (IllegalFormatException e) {
            // in case of bad placeholders
            String roughMessage = get(messageKey) + " " + Arrays.asList(localizedDetails);
            log.warn("Failed to create valid message for localized pattern&params: {} ", roughMessage);
            return roughMessage;
        }
    }

    /** @return the locale of this message source */
    public Locale getLocale() {
        return _messages.getLocale();
    }
}
