/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.kms;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vapi.std.errors.AlreadyExists;
import com.vmware.vcenter.crypto_manager.kms.Providers;
import com.vmware.vcenter.crypto_manager.kms.ProvidersTypes.CreateSpec;
import com.vmware.vcenter.crypto_manager.kms.ProvidersTypes.ExportResult;
import com.vmware.vcenter.crypto_manager.kms.ProvidersTypes.ExportSpec;
import com.vmware.vcenter.crypto_manager.kms.ProvidersTypes.FilterSpec;
import com.vmware.vcenter.crypto_manager.kms.ProvidersTypes.ImportResult;
import com.vmware.vcenter.crypto_manager.kms.ProvidersTypes.ImportSpec;
import com.vmware.vcenter.crypto_manager.kms.ProvidersTypes.Info;
import com.vmware.vcenter.crypto_manager.kms.ProvidersTypes.Summary;

/**
 * Demonstration of the basic native key provider functionality through Java APIs. This sample includes create, get,
 * list, delete, export and import operations on native key providers.
 */
public class ManageNativeKeyProviders {
    private static final Logger log = LoggerFactory.getLogger(ManageNativeKeyProviders.class);
    /** REQUIRED: vCenter FQDN or IP address. */
    public static String serverAddress = "vcenter1.mycompany.com";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";
    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (do not leave it empty on production environments).
     */
    public static String trustStorePath = null;

    private static final String PASSKEY = "53cur3Pa55w0rd!";
    private static final String TEST_PROVIDER = "test_nkp";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(ManageNativeKeyProviders.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Providers nativeKeyProviders = client.createStub(Providers.class);

            listProviders(nativeKeyProviders);

            createProvider(nativeKeyProviders, TEST_PROVIDER);

            getProviderDetails(nativeKeyProviders, TEST_PROVIDER);

            byte[] backup = backupKeyProvider(nativeKeyProviders, TEST_PROVIDER, PASSKEY.toCharArray());

            getProviderDetails(nativeKeyProviders, TEST_PROVIDER);

            deleteKeyProvider(nativeKeyProviders, TEST_PROVIDER);

            restoreKeyProvider(nativeKeyProviders, backup, PASSKEY.toCharArray());

            deleteKeyProvider(nativeKeyProviders, TEST_PROVIDER);

            listProviders(nativeKeyProviders);
        }
    }

    private static void listProviders(Providers nativeKeyProviders) {
        List<Summary> list = nativeKeyProviders.list(new FilterSpec());

        log.info("List of native key providers got {} items", list.size());
        int i = 1;
        for (Summary s : list) {
            log.info("Provider {} Summary: {}", i++, s);
        }
    }

    private static void createProvider(Providers nativeKeyProviders, String name) {
        CreateSpec providerSpec = new CreateSpec.Builder(name).build();
        try {
            nativeKeyProviders.create(providerSpec);
        } catch (AlreadyExists e) {
            log.info("Provider {} already exists. Continue", name);
        }
    }

    private static void getProviderDetails(Providers nativeKeyProviders, String name) {
        Info info = nativeKeyProviders.get(name);
        log.info("Native key provider details: {}", info);
    }

    /**
     * Backup native key provider data. This is a 2 stage process. First a backup is requested. Second step is to
     * download the backup using token and address returned form the first step.
     *
     * <p>The returned backup data can be used with import to restore a native key provider.
     *
     * @param name name of the native key provider to backup
     * @param pwd password for protection of the backup data
     * @return bytes of the native key provider backup
     */
    private static byte[] backupKeyProvider(Providers nativeKeyProviders, String name, char[] pwd) {
        // Step 1: request backup
        ExportSpec exportSpec = new ExportSpec.Builder(name).setPassword(pwd).build();
        ExportResult exportResult = nativeKeyProviders.export(exportSpec);
        log.info("Backup step one: export result is {}", exportResult);

        // Step 2: download the backup
        URI url = exportResult.getLocation().getUrl();
        char[] token = exportResult.getLocation().getDownloadToken().getToken();

        return downloadBackupData(url, token);
    }

    /**
     * Download backup data from online location.
     *
     * <p>This method used the Java Apache HTTP client to download the backup data.
     *
     * <p>Download is done by making a post request to the url with Authorization Bearer header carrying the supplied
     * token
     *
     * @param url online location
     * @param token access token
     * @return bytes of the native key provider backup
     */
    private static byte[] downloadBackupData(URI url, char[] token) {
        HttpPost request = new HttpPost(url);
        request.addHeader("Authorization", MessageFormat.format("Bearer {0}", new String(token)));

        log.info("Backup request {}", request);
        try (CloseableHttpClient client = createHttpClient();
                CloseableHttpResponse resp = client.execute(request)) {
            int statusCode = resp.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                log.info("Backup failed. HTTP status code {}", statusCode);
                throw new RuntimeException("Cannot backup");
            }
            HttpEntity body = resp.getEntity();

            byte[] backup = new byte[body.getContent().available()];
            body.getContent().read(backup);
            log.info("Backup received {} bytes. Backup completed.", backup.length);

            return backup;
        } catch (IOException e) {
            log.error("IO Exception during backup:", e);
            throw new RuntimeException(e);
        }
    }

    private static CloseableHttpClient createHttpClient() {
        if (trustStorePath == null || trustStorePath.isEmpty()) {
            try {
                SSLContext sslCtx = new SSLContextBuilder()
                        .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                        .build();

                return HttpClients.custom()
                        .setSSLContext(sslCtx)
                        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .build();
            } catch (Exception e) {
                log.error("Cannot create trust all HTTP client", e);
                throw new RuntimeException(e);
            }
        }
        return HttpClients.createDefault();
    }

    private static void deleteKeyProvider(Providers nativeKeyProviders, String name) {
        nativeKeyProviders.delete(name);
        log.info("Deleted key provider {}", name);
    }

    private static void restoreKeyProvider(Providers nativeKeyProviders, byte[] backup, char[] pwd) {
        ImportSpec spec =
                new ImportSpec.Builder().setConfig(backup).setPassword(pwd).build();
        ImportResult importResult = nativeKeyProviders.importProvider(spec);

        log.info("Restored Native Key Provider {}", importResult);
    }
}
