/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.helpers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this utility class to load the private key and corresponding certificate chain from either a java keystore or
 * individual files.
 *
 * <p><b>Note: </b>This utility class is simply provided here for convenience sake. Users are free to use any other
 * mechanism of loading the private key and certificate in java and use it.
 */
public class SecurityUtil {

    private static final Logger log = LoggerFactory.getLogger(SecurityUtil.class);

    private PrivateKey privateKey;
    private X509Certificate userCert;

    /**
     * Loads the keys from the keystore.
     *
     * <p>Users can generate their own pair of private key and certificate using the keytool utility shipped with the
     * jdk. Sample usage of the keytool to generate a pair would be as follows:
     *
     * <pre>
     * <b>
     *  &gt; keytool -genkey -keyalg RSA -alias sample -keystore sampleKeystore.jks -storepass sample
     *  What is your first and last name?
     *    [Unknown]:  *.vmware.com
     *  What is the name of your organizational unit?
     *    [Unknown]:  Ecosystem Engineering
     *  What is the name of your organization?
     *    [Unknown]:  VMware, Inc.
     *  What is the name of your City or Locality?
     *    [Unknown]:  Palo Alto
     *  What is the name of your State or Province?
     *    [Unknown]:  California
     *  What is the two-letter country code for this unit?
     *    [Unknown]:  US
     *  Is CN=*.vmware.com, OU=Ecosystem Engineering, O="VMware, Inc.", L=Palo Alto, ST=
     *  California, C=US correct?
     *    [no]:  yes
     *
     *  Enter key password for &lt;sample&gt;
     *          (RETURN if same as keystore password):
     * </b>
     * </pre>
     *
     * @param keyStorePath path to the keystore
     * @param keyStorePassword keystore password
     * @param userAlias alias that was used at the time of key generation
     * @return the {@link SecurityUtil} with the loaded keys
     */
    public static SecurityUtil loadFromKeystore(String keyStorePath, String keyStorePassword, String userAlias) {
        try {
            return new SecurityUtil().loadKeystore(keyStorePath, keyStorePassword, userAlias);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Load the pre-generated private keys, and the certificate from individual files.
     *
     * @return the pre-generated key/cert pair
     */
    public static SecurityUtil loadFromDefaultFiles() {

        // A pre-generated self-signed certificate and private key pair
        // to be used in the sample by default.
        // This is to be used for ONLY development purpose.
        URL privateKeyURL = SecurityUtil.class.getResource("/cert/sdk.key");
        URL x509CertURL = SecurityUtil.class.getResource("/cert/sdk.crt");

        if (privateKeyURL == null) {
            throw new RuntimeException("Private key not found in resources.");
        }
        if (x509CertURL == null) {
            throw new RuntimeException("X509 certificate not found in resources.");
        }

        String privateKeyFileName = privateKeyURL.getPath();
        String x509CertFileName = x509CertURL.getPath();

        return loadFromFiles(privateKeyFileName, x509CertFileName);
    }

    /**
     * Load the private keys, and the certificate from individual files. This method comes handy when trying to work as
     * a solution user for e.g. vCenter server. The open source "openssl" tool can be leveraged for converting your
     * private key into the PKCS8 format by using the following command:
     *
     * <pre>
     * <b>
     * openssl pkcs8 -topk8 -inform PEM -outform DER -in private_key_file -nocrypt &gt; pkcs8_key
     * </b>
     * </pre>
     *
     * @param privateKeyFileName Path to the file storing the private key in PKCS8 format ONLY
     * @param x509CertFileName Path to the file storing the certificate in X509 format ONLY
     * @return the SecurityUtil with the loaded private key and certificate
     */
    public static SecurityUtil loadFromFiles(String privateKeyFileName, String x509CertFileName) {
        try {
            return new SecurityUtil().loadPrivateKey(privateKeyFileName).loadX509Cert(x509CertFileName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** keytool -genkey -keyalg RSA -alias [userAlias] -keystore [keyStorePath] -storepass [keyStorePassword] */
    private SecurityUtil loadKeystore(String keyStorePath, String keyStorePassword, String userAlias)
            throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException, CertificateException,
                    IOException {
        File file = new File(keyStorePath);
        if (!file.isFile()) {
            log.error("Keystore file not found");
        }
        log.info("Loading KeyStore {}...", file);
        char[] passphrase = keyStorePassword.toCharArray();
        KeyStore ks;
        try (InputStream in = new FileInputStream(file)) {
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(in, passphrase);
        }
        if (ks.isKeyEntry(userAlias)) {
            // get the private key
            KeyStore.PrivateKeyEntry pkEntry =
                    (KeyStore.PrivateKeyEntry) ks.getEntry(userAlias, new KeyStore.PasswordProtection(passphrase));
            privateKey = pkEntry.getPrivateKey();
            if (pkEntry.getCertificate() instanceof X509Certificate) {
                userCert = (X509Certificate) pkEntry.getCertificate();
            }
        }
        return this;
    }

    private SecurityUtil loadPrivateKey(String privateKeyFileName)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Load the private key (in PKCS#8 DER encoding).
        File keyFile = new File(privateKeyFileName);
        byte[] encodedKey = new byte[(int) keyFile.length()];
        try (FileInputStream keyInputStream = new FileInputStream(keyFile)) {
            keyInputStream.read(encodedKey);
        }
        KeyFactory rSAKeyFactory = KeyFactory.getInstance("RSA");
        privateKey = rSAKeyFactory.generatePrivate(new PKCS8EncodedKeySpec(encodedKey));
        return this;
    }

    private SecurityUtil loadX509Cert(String x509CertFileName) throws IOException, CertificateException {
        log.info("Loading X509 Certificate from {}...", x509CertFileName);
        FileInputStream fis = new FileInputStream(x509CertFileName);
        BufferedInputStream bis = new BufferedInputStream(fis);

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        while (bis.available() > 0) {
            userCert = (X509Certificate) cf.generateCertificate(bis);
        }
        return this;
    }

    /** private constructor */
    private SecurityUtil() {}

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public X509Certificate getUserCert() {
        return userCert;
    }
}
