/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.utils.ssl;

import static com.vmware.sdk.utils.ssl.TlsHelper.createEmptyKeystore;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import com.vmware.sdk.utils.ssl.InsecureTrustManager;
import com.vmware.sdk.utils.ssl.TlsHelper;

public class SecurityHelper {

    /**
     * Tries to read the given file, parse its content as PEM or DER-encoded {@link X509Certificate}s and puts it in a
     * newly created keystore.
     *
     * <p>If {@code trustStorePath} is not provided, an empty keystore will be created which means that the TLS
     * connections, established using this keystore as trust store, are going to be insecure
     *
     * @param trustStorePath file path pointing to the certificate
     * @return newly created keystore with the certificate
     * @see TlsHelper#certificatesToKeystore(InputStream)
     */
    public static KeyStore loadKeystoreOrCreateEmpty(String trustStorePath) {
        if (trustStorePath == null || trustStorePath.isEmpty() || !Files.exists(Paths.get(trustStorePath))) {
            return createEmptyKeystore();
        }

        try (InputStream is = Files.newInputStream(Paths.get(trustStorePath))) {
            return TlsHelper.certificatesToKeystore(is);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Tries to read the given file, parse its content as PEM or DER-encoded {@link X509Certificate}s and puts them in a
     * newly created keystore.
     *
     * <p>If {@code trustStorePath} is not provided, an empty keystore will be created which means that the TLS
     * connections, established using this keystore as trust store, are going to be insecure
     *
     * @param trustStorePath file path pointing to the certificate
     * @return newly created keystore with the certificate
     * @see TlsHelper#certificatesToKeystore(InputStream)
     */
    public static TrustManager createTrustManagerFromFile(String trustStorePath) {
        KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);
        try {
            // if the keystore is not empty, it has a custom certificate in it
            if (keyStore.aliases().hasMoreElements()) {
                return TlsHelper.createTrustManagers(keyStore)[0];
            } else {
                // empty keystore => assume insecure connection
                return new InsecureTrustManager();
            }
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a {@link SocketFactory} whose trust managers are set to {@link InsecureTrustManager} meaning that the TLS
     * connections issued are going to be insecure.
     *
     * <p>This implementation is only suitable for testing purposes in a controlled environment. <b>Production code *
     * must NOT use this {@link SSLSocketFactory} because it poses security risks!</b>
     *
     * @return a {@link SocketFactory} whose trust managers are set to {@link InsecureTrustManager}
     */
    public static SSLSocketFactory createInsecureSocketFactory() {
        return createSocketFactory(null);
    }

    /**
     * Creates a {@link SocketFactory} with trust manager determined by {@code trustStorePath}.
     *
     * @param trustStorePath path to a custom PEM/DER file. If <code>null</code> insecure trust store will be used
     *     <p>This implementation is only suitable for testing purposes in a controlled environment. <b>Production code
     *     * must NOT use insecure trust manager because it poses security risks!</b>
     * @return a {@link SocketFactory} with respective trust manager
     */
    public static SSLSocketFactory createSocketFactory(String trustStorePath) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager trustManager = createTrustManagerFromFile(trustStorePath);
            sslContext.init(null, new TrustManager[] {trustManager}, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
}
