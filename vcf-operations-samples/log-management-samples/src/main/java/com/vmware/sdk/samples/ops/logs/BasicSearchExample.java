/*
 * ******************************************************************
 * Copyright (c) 2025-2026 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * The term "Broadcom" refers to Broadcom Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.logs;

import com.vmware.sdk.ops.logs.api.v2.Search;
import com.vmware.sdk.ops.logs.model.BoolQuery;
import com.vmware.sdk.ops.logs.model.ExistsQuery;
import com.vmware.sdk.ops.logs.model.MatchAllQuery;
import com.vmware.sdk.ops.logs.model.Query;
import com.vmware.sdk.ops.logs.model.QueryRegexp;
import com.vmware.sdk.ops.logs.model.QueryRequest;
import com.vmware.sdk.ops.logs.model.QueryResponse;
import com.vmware.sdk.samples.ops.logs.util.ApiClientUtil;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.vapi.bindings.CompletionStageFuture;
import com.vmware.vapi.data.StringValue;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicSearchExample {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicSearchExample.class.getName());

    private ApiClientUtil clientUtil;
    private Search searchService;

    /** REQUIRED: VCF Log Management FQDN or IP address. */
    public static String logsHost = "<LOGS-ADDRESS>";
    /** REQUIRED: VCF Log Management Port. */
    public static String logsPort = "<port>";
    /** REQUIRED: VCF Operations FQDN or IP address. */
    public static String opsHost = "<VROPS-SUITE-VM-IP-ADDRESS>";
    /** REQUIRED: VCF Operations Username. */
    public static String username = "<username>";
    /** REQUIRED: VCF Operations Password. */
    public static String password = "<password>";

    public static void main(String[] args) {
        BasicSearchExample example = new BasicSearchExample();
        try {
            SampleCommandLineParser.load(BasicSearchExample.class, args);
            example.initialize();
            example.runAllSearchExamples();
        } catch (Exception e) {
            LOGGER.error("Search example failed", e);
        } finally {
            example.cleanup();
        }
    }
    
    /**
     * Initialize the client and search service.
     */
    public void initialize() throws Exception {
        LOGGER.info("Initializing example...");
        clientUtil = new ApiClientUtil();
        clientUtil.initializeClient(opsHost, username, password, logsHost, logsPort);
        searchService = clientUtil.getV2Factory().searchService();
        LOGGER.info("Search service initialized successfully");
    }
    
    /**
     * Run all search examples.
     */
    public void runAllSearchExamples() {
        try {
            // Basic search examples
            performMatchAllQuery();
            performExistsQuery();
            performBooleanQuery();
        } catch (Exception e) {
            LOGGER.error("Error running search examples", e);
        }
    }
    
    /**
     * Example 1: Simple match-all query to retrieve all logs.
     */
    public void performMatchAllQuery() {
        LOGGER.info("=== Match All Query Example ===");
        
        try {
            // Create a match-all query
            MatchAllQuery matchAllQuery = new MatchAllQuery();
            
            Query query = new Query();
            query.setMatchAll(matchAllQuery);
            
            // Build the search request
            QueryRequest request = new QueryRequest.Builder()
                    .setQuery(query)
                    .setSize(10L)  // Limit to 10 results
                    .setFrom(0L)   // Start from first result
                    .build();
            
            // Execute the search
            CompletionStageFuture<QueryResponse> future = searchService.executeLogSearchQuery(request).invoke();
            QueryResponse response = future.get();
            
            LOGGER.info("Match-all query executed successfully");
            LOGGER.info("Total hits: {}", (response.getEvents().getHits() != null ? response.getEvents().getHits().size() : 0));
            
            // Process results
            if (response.getEvents().getHits() != null) {
                response.getEvents().getHits().forEach(hit -> {
                    LOGGER.info("Log entry: {}", hit.toString());
                });
            }
            
        } catch (Exception e) {
            LOGGER.error("Match-all query failed", e);
        }
    }

    public void performExistsQuery() {
        LOGGER.info("=== Exists Query Example ===");

        try {
            // Create a match-all query
            ExistsQuery existsQuery = new ExistsQuery();
            existsQuery.setField("field1");

            Query query = new Query();
            query.setExists(existsQuery);

            // Build the search request
            QueryRequest request = new QueryRequest.Builder()
                    .setQuery(query)
                    .setSize(10L)  // Limit to 10 results
                    .setFrom(0L)   // Start from first result
                    .build();

            // Execute the search
            CompletionStageFuture<QueryResponse> future = searchService.executeLogSearchQuery(request).invoke();
            QueryResponse response = future.get();

            LOGGER.info("Exists query executed successfully");
            LOGGER.info("Total hits: {}", (response.getEvents().getHits() != null ? response.getEvents().getHits().size() : 0));

            // Process results
            if (response.getEvents().getHits() != null) {
                response.getEvents().getHits().forEach(hit -> {
                    LOGGER.info("Log entry: {}", hit.toString());
                });
            }

        } catch (Exception e) {
            LOGGER.error("Exists query failed", e);
        }
    }


    public void performBooleanQuery() {
        LOGGER.info("=== Boolean Query Example ===");

        try {
            QueryRegexp mustQuery = new QueryRegexp();
            StringValue stringValue = new StringValue(".*error.*");
            mustQuery._setDynamicField("message", stringValue);
            Query mQuery = new Query();
            mQuery.setRegexp(mustQuery);
            // Combine into boolean query
            BoolQuery boolQuery = new BoolQuery.Builder()
                    .setMust(Arrays.asList(mQuery))
                    .build();

            Query query = new Query();
            query.setBool(boolQuery);

            // Build the search request
            QueryRequest request = new QueryRequest.Builder()
                    .setQuery(query)
                    .setSize(25L)
                    .build();

            // Execute the search
            CompletionStageFuture<QueryResponse> future = searchService.executeLogSearchQuery(request).invoke();
            QueryResponse response = future.get();

            LOGGER.info("Boolean query executed successfully");
            LOGGER.info("Found logs matching boolean conditions: {}", (response.getEvents().getHits() != null ? response.getEvents().getHits().size() : 0));

        } catch (Exception e) {
            LOGGER.error("Boolean query failed", e);
        }
    }
    
    /**
     * Clean up resources.
     */
    public void cleanup() {
        if (clientUtil != null) {
            clientUtil.cleanup();
        }
    }
}
