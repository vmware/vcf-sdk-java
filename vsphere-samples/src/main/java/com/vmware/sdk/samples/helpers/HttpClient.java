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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.utils.ssl.InsecureTrustManager;

public class HttpClient {
    private static final Logger log = LoggerFactory.getLogger(HttpClient.class);

    private final CloseableHttpClient client;

    public HttpClient(boolean infiniteSocketTimeout) {
        try {

            SSLContext sslContext = SSLContext.getInstance("TLS");

            // set up a TrustManager that trusts everything
            sslContext.init(null, new TrustManager[] {new InsecureTrustManager()}, new SecureRandom());

            // Create a registry of custom connection socket factories for
            // supported protocol schemes
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("https", new SSLConnectionSocketFactory(sslContext))
                    .build();

            PoolingHttpClientConnectionManager connectionManager =
                    new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            connectionManager.setDefaultMaxPerRoute(500);
            connectionManager.setMaxTotal(600);

            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(5 * 1000)
                    .setConnectionRequestTimeout(5 * 1000)
                    .setSocketTimeout(infiniteSocketTimeout ? -1 : 60 * 1000)
                    .build();

            HttpClientBuilder clientBuilder = HttpClientBuilder.create();
            clientBuilder.setConnectionManager(connectionManager);
            clientBuilder.setDefaultRequestConfig(config);

            // build the HTTP client
            client = clientBuilder.build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    public void upload(File file, String url) {
        upload(file, 0, file.length(), url, null);
    }

    public void upload(File file, long startByte, long endByte, String url, Header header) {
        HttpPut httpPut = new HttpPut(url);
        try {
            FileEntity fileEntity = new FileEntity(file);
            httpPut.setEntity(fileEntity);
            HttpResponse httpResponse = executeRequest(httpPut);
            validateResponse(httpResponse, HttpStatus.SC_OK);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("FileNotFoundException for file" + file.getName(), e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload due to IOException!" + file.getName(), e);
        } catch (RuntimeException e) {
            httpPut.abort();
            throw e;
        }
    }

    public InputStream downloadFile(String url) {

        try {
            URI.create(url).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Url!", e);
        }

        HttpGet httpGet = new HttpGet(url);
        try {

            HttpResponse httpResponse = client.execute(httpGet);
            HttpEntity responseEntity = httpResponse.getEntity();
            InputStream inputStream = null;
            if (responseEntity != null) {
                inputStream = responseEntity.getContent();
            }
            validateResponse(httpResponse, HttpStatus.SC_OK);
            return inputStream;
        } catch (IOException e) {
            throw new RuntimeException("Failed to get url contents", e);
        } catch (RuntimeException e) {
            httpGet.abort();
            throw e;
        }
    }

    /**
     * used to get data from a url and then writes the contents to the file specified.
     *
     * @param url The URL to retrieve
     * @param filename the local file to save to
     * @return response the content from the response
     */
    public String getFile(String url, String filename) {

        try {
            URI.create(url).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Url!", e);
        }

        HttpGet httpGet = new HttpGet(url);
        try {

            HttpResponse httpResponse = client.execute(httpGet);
            HttpEntity responseEntity = httpResponse.getEntity();
            String response = "";
            if (responseEntity != null) {
                response = EntityUtils.toString(responseEntity);
            }
            createFile(filename, response);
            return response;
        } catch (IOException e) {
            throw new RuntimeException("Failed to get url contents", e);
        } catch (RuntimeException e) {
            httpGet.abort();
            throw e;
        }
    }

    private static void createFile(String outfile, String content) throws IOException {
        FileOutputStream fileoutputstream = new FileOutputStream(outfile);
        DataOutputStream dataoutputstream = new DataOutputStream(fileoutputstream);
        dataoutputstream.writeBytes(content);
        dataoutputstream.flush();
        dataoutputstream.close();
    }

    private HttpResponse executeRequest(HttpUriRequest httpRequest) throws IOException {
        int retries = 3;
        boolean shouldBreak = true;
        HttpResponse response = null;
        final String LOGIN_FAILED_MSG = "Failed to login";
        while (retries-- >= 0) {
            try {
                response = client.execute(httpRequest);
            } catch (javax.net.ssl.SSLHandshakeException ex) {
                log.error("SSLHandshakeException: {}", ex.getMessage());
                if (retries <= 0) {
                    shouldBreak = true;
                    throw new RuntimeException(LOGIN_FAILED_MSG, ex);
                }
                shouldBreak = false;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(LOGIN_FAILED_MSG, e);
                }
            } catch (java.io.EOFException ex) {
                log.error("EOFException: {}", ex.getMessage());
                if (retries <= 0) {
                    shouldBreak = true;
                    throw new RuntimeException(LOGIN_FAILED_MSG, ex);
                }
                shouldBreak = false;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(LOGIN_FAILED_MSG, e);
                }
            }
            if (shouldBreak) {
                break;
            }
        }
        return response;
    }

    /**
     * This method does the actual validation of responses.
     *
     * @param response {@link HttpResponse}
     */
    private void validateResponse(HttpResponse response, int statusCode) {
        int actualStatusCode = response.getStatusLine().getStatusCode();
        if (actualStatusCode == statusCode) {
            return;
        }
        String errorMessage = "Invalid return code! Expected: " + statusCode + " Actual: " + actualStatusCode + "; ";
        RuntimeException restEx = new RuntimeException(errorMessage);
        if (response.getEntity() != null) {
            if (response.getEntity().getContentLength() != 0) {
                log.info("Lets do something here");
            }
        }

        throw new RuntimeException(errorMessage, restEx);
    }
}
