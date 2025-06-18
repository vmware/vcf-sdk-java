/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.utils.ssl;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The following class provides utility code that makes it easy to configure vSphere and other SDK clients. */
public class TlsHelper {

    private static final Logger log = LoggerFactory.getLogger(TlsHelper.class);

    /**
     * Uses the provided keystore and {@link TrustManagerFactory#getDefaultAlgorithm()} to create trust managers which
     * later on should be used during the TLS handshake when establishing a connection to remote service.
     *
     * @param keyStore the keystore that contains custom certificates (not part of the JRE-provided keystore); if the
     *     keystore is empty, the TLS connection is going to be insecure!
     * @return trust managers that can be used to configure sdk clients
     */
    public static TrustManager[] createTrustManagers(KeyStore keyStore) {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            return tmf.getTrustManagers();
        } catch (Exception e) {
            log.error("Failed to create trust manager", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates an empty {@link KeyStore}.
     *
     * <p>If used to create a {@link TrustManagerFactory}, the TLS connections are going to be insecure!
     *
     * @return an empty {@link KeyStore}
     */
    public static KeyStore createEmptyKeystore() {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);

            return keyStore;
        } catch (Exception e) {
            // should not be possible
            throw new RuntimeException(e);
        }
    }

    /**
     * Parses the given InputStream as PEM or DER-encoded file as one or more {@link X509Certificate}s and puts them in
     * a newly created keystore.
     *
     * <p>This method should typically be used when the remote server is using a certificate which is signed by a CA
     * which is not included in the JRE default truststore.
     *
     * @param inputStream the PEM/DER-encoded certificate
     * @return newly created keystore with the certificate
     * @see X509Certificate
     * @see CertificateFactory#generateCertificate(InputStream)
     * @see #createTrustManagers(KeyStore)
     */
    public static KeyStore certificatesToKeystore(InputStream inputStream) {
        try {

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);

            int i = 0;
            var certificates = CertificateFactory.getInstance("X.509").generateCertificates(inputStream);

            for (var certificate : certificates) {
                keyStore.setCertificateEntry("certificate" + i++, certificate);
            }

            return keyStore;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
