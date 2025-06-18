/*
 * ******************************************************************
 * Copyright (c) 2010-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.saml.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vapi.saml.util.exception.SsoKeyStoreOperationException;

/** KeyStoreData class represents a certificate/private key for a given alias from java keystore file. */
public class KeyStoreData {

    private final String _keystorePath;
    private final String _certAlias;
    private final KeyStore _keyStore;
    private final String LOAD_ERROR_MSG;
    private static final Logger log = LoggerFactory.getLogger(KeyStoreData.class);

    /**
     * Creates a KeyStoreData object.
     *
     * @param keystorePath The path to the java keystore file
     * @param storePass The password for the keystore file
     * @param certAlias The alias of the certificate that should be loaded
     */
    public KeyStoreData(String keystorePath, char[] storePass, String certAlias) throws SsoKeyStoreOperationException {

        if (log.isDebugEnabled()) {
            log.debug("Loading keystore: {}", keystorePath);
        }

        _keystorePath = keystorePath;
        _certAlias = certAlias;

        LOAD_ERROR_MSG = "Error while trying to load certificate entry " + _certAlias + " from " + _keystorePath;

        FileInputStream fis = null;
        try {
            _keyStore = KeyStore.getInstance("JKS");
            fis = new FileInputStream(keystorePath);
            _keyStore.load(fis, storePass);
        } catch (KeyStoreException e) {
            logErrorMessage(e);
            throw new SsoKeyStoreOperationException(LOAD_ERROR_MSG, e);
        } catch (NoSuchAlgorithmException e) {
            logErrorMessage(e);
            ;
            throw new SsoKeyStoreOperationException(LOAD_ERROR_MSG, e);
        } catch (CertificateException e) {
            logErrorMessage(e);
            throw new SsoKeyStoreOperationException(LOAD_ERROR_MSG, e);
        } catch (FileNotFoundException e) {
            logErrorMessage(e);
            throw new SsoKeyStoreOperationException(LOAD_ERROR_MSG, e);
        } catch (IOException e) {
            logErrorMessage(e);
            throw new SsoKeyStoreOperationException(LOAD_ERROR_MSG, e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    logErrorMessage(e);
                }
            }
        }
    }

    /** @return the private key for the specified certificate alias */
    public Key getPrivateKey(char[] keyPass) throws SsoKeyStoreOperationException {
        PrivateKeyEntry keyEntry;
        try {
            // TODO : Check the alias exists
            keyEntry = (PrivateKeyEntry) _keyStore.getEntry(_certAlias, new KeyStore.PasswordProtection(keyPass));
        } catch (NoSuchAlgorithmException e) {
            logErrorMessage(e);
            throw new SsoKeyStoreOperationException(LOAD_ERROR_MSG, e);
        } catch (UnrecoverableEntryException e) {
            logErrorMessage(e);
            throw new SsoKeyStoreOperationException(LOAD_ERROR_MSG, e);
        } catch (KeyStoreException e) {
            logErrorMessage(e);
            throw new SsoKeyStoreOperationException(LOAD_ERROR_MSG, e);
        }

        return keyEntry.getPrivateKey();
    }

    private void logErrorMessage(Throwable t) {
        log.error("Error while trying to load certificate entry {} from {}", _certAlias, _keystorePath, t);
    }

    /** @return the certificate for the specified certificate alias */
    public X509Certificate getCertificate() throws SsoKeyStoreOperationException {
        try {
            // TODO: check the alias exists and that it corresponds to an X.509
            // Certificate
            return (X509Certificate) _keyStore.getCertificate(_certAlias);
        } catch (KeyStoreException e) {
            logErrorMessage(e);
            throw new SsoKeyStoreOperationException(LOAD_ERROR_MSG, e);
        }
    }
}
