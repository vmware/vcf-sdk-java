/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.logs.util;

import static com.vmware.vapi.client.Configuration.HTTP_CONFIG_CFG;
import static com.vmware.vapi.client.Configuration.STUB_CONFIG_CFG;
import static com.vmware.vapi.internal.protocol.RestProtocol.REST_REQUEST_AUTHENTICATOR_CFG;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.net.ssl.SSLContext;

import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.ops.logs.api.v2.V2Factory;
import com.vmware.vapi.bindings.StubConfiguration;
import com.vmware.vapi.bindings.StubCreator;
import com.vmware.vapi.client.ApiClient;
import com.vmware.vapi.client.ApiClients;
import com.vmware.vapi.client.Configuration;
import com.vmware.vapi.protocol.HttpConfiguration;


/**
 * This class provides utility methods for working with API clients.
 *
 * <p>It includes methods for creating API client instances and other common API client tasks.
 */
public class ApiClientUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiClientUtil.class.getName());

    public static final int RESPONSE_TIMEOUT = (int) Duration.ofSeconds(180).toMillis();
    public static final int CONNECT_TIMEOUT = (int) Duration.ofSeconds(180).toMillis();

    private String LOGS_HOST;
    private int LOGS_PORT;



    private String OPS_HOST;
    private String username;
    private String password;
    private String OPS_AUTH_URL = "/api/auth/token/acquire";
    private String TOKEN_EXCHANGE_URL = "/api/auth/token/exchange";
    private String TOKEN_EXCHANGE_BODY = "{\"serviceKeys\": [\"ops-li\"]}";


    private V2Factory v2Factory;
    private ApiClient apiClient;

    public String getOpsToken() {
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, (chain, authType) -> true)
                    .build();

            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .sslContext(sslContext)
                    .build();

            String json = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);


            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPS_HOST + OPS_AUTH_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();


            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String jsonString = response.body();
            JSONObject jsonObject = new JSONObject(jsonString);
            return jsonObject.getString("token");
        } catch (Exception e) {
            LOGGER.error("ERROR in API Client getOpsToken");
            e.printStackTrace();
        }

        return "";
    }

    public String getLogsToken(String opsToken) {
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, (chain, authType) -> true) // Trust all certificates
                    .build();

            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .sslContext(sslContext)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPS_HOST + TOKEN_EXCHANGE_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "OpsToken " + opsToken)
                    .POST(HttpRequest.BodyPublishers.ofString(TOKEN_EXCHANGE_BODY))
                    .build();


            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String jsonString = response.body();
            JSONObject jsonObject = new JSONObject(jsonString);
            return jsonObject.getString("jwtToken");
        } catch (Exception e) {
            LOGGER.error("ERROR in API Client getLogsToken");
            e.printStackTrace();
        }

        return "";
    }

    public ApiClient createClient(String hostAddress, int port, HttpConfiguration httpConfig) {
        String token;
        try {
            String opsToken = getOpsToken();
            token = getLogsToken(opsToken);
        } catch (Exception e) {
            LOGGER.error("Could not acquire token pair from VCF Installer");
            throw new RuntimeException(e);
        }

        StubConfiguration stubConfig = new StubConfiguration();

        LogsAuthenticationHeaderAppender appender = new LogsAuthenticationHeaderAppender();
        appender.setToken(token);

        Configuration configuration = new Configuration.Builder()
                .register(HTTP_CONFIG_CFG, httpConfig)
                .register(STUB_CONFIG_CFG, stubConfig)
                .register(REST_REQUEST_AUTHENTICATOR_CFG, appender)
                .build();

        return ApiClients.newRestClient(createUrl(hostAddress, port), configuration);
    }


    private String createUrl(String hostAddress, int port) {
        try {
            return new URIBuilder()
                    .setScheme("https")
                    .setHost(hostAddress)
                    .setPort(port)
                    .build()
                    .toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void initializeClient(String opsHost, String usname, String psw,
            String logHost, String logPort) throws Exception {
        LOGGER.info("Initializing vRLI client...");
        try {
            OPS_HOST = "https://" + opsHost + "/suite-api";
            username = usname;
            password = psw;
            LOGS_HOST = logHost;
            LOGS_PORT = Integer.parseInt(logPort);

            HttpConfiguration.SslConfiguration sslConfig =
                    new HttpConfiguration.SslConfiguration.Builder()
                            .disableCertificateValidation()
                            .disableHostnameVerification()
                            .getConfig();
            // Create HTTP configuration
            HttpConfiguration httpConfig = new HttpConfiguration.Builder()
                    .setSslConfiguration(sslConfig)
                    .setSoTimeout(RESPONSE_TIMEOUT)
                    .setConnectTimeout(CONNECT_TIMEOUT)
                    .getConfig();

            apiClient = this.createClient(LOGS_HOST, LOGS_PORT, httpConfig);

            // Get stub creator from API client
            StubCreator stubCreator = apiClient;

            // Initialize factory instances
            v2Factory = V2Factory.getFactory(stubCreator, new StubConfiguration());

            LOGGER.info("vRLI client initialized successfully");

        } catch (Exception e) {
            LOGGER.error("Failed to initialize vRLI client: {}", e.getMessage());
            throw new RuntimeException("Client initialization failed", e);
        }
    }

    /**
     * Get the initialized V2 factory for use in other examples.
     */
    public V2Factory getV2Factory() {
        return v2Factory;
    }

    public void cleanup() {
        LOGGER.info("Cleaning up resources...");
        try {
            if (apiClient != null) {
                // Close the API client connection
                apiClient.close();
                LOGGER.info("API client closed successfully");
            }
        } catch (Exception e) {
            LOGGER.warn("Error during cleanup: {}", e.getMessage());
        }
    }

}
