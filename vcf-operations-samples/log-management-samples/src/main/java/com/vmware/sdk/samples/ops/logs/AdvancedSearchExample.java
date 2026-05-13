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
import com.vmware.sdk.ops.logs.model.Aggregation;
import com.vmware.sdk.ops.logs.model.BoolQuery;
import com.vmware.sdk.ops.logs.model.MultiTermLookup;
import com.vmware.sdk.ops.logs.model.MultiTermsAggregation;
import com.vmware.sdk.ops.logs.model.Query;
import com.vmware.sdk.ops.logs.model.QueryMatchPhrase;
import com.vmware.sdk.ops.logs.model.QueryRange;
import com.vmware.sdk.ops.logs.model.QueryRequest;
import com.vmware.sdk.ops.logs.model.QueryResponse;
import com.vmware.sdk.ops.logs.model.RangeQueryValue;
import com.vmware.sdk.samples.ops.logs.util.ApiClientUtil;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.vapi.bindings.CompletionStageFuture;
import com.vmware.vapi.data.StringValue;
import com.vmware.vapi.data.StructValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvancedSearchExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdvancedSearchExample.class.getName());

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
        AdvancedSearchExample example = new AdvancedSearchExample();
        try {
            SampleCommandLineParser.load(AdvancedSearchExample.class, args);
            example.initialize();
            example.runAllAdvancedSearchExamples();
        } catch (Exception e) {
            LOGGER.error("Advanced search example failed", e);
        } finally {
            example.cleanup();
        }
    }

    /**
     * Initialize the client and search services.
     */
    public void initialize() throws Exception {
        LOGGER.info("Initializing example...");
        clientUtil = new ApiClientUtil();
        clientUtil.initializeClient(opsHost, username, password, logsHost, logsPort);
        searchService = clientUtil.getV2Factory().searchService();
        LOGGER.info("Search services initialized successfully");
    }

    /**
     * Run all advanced search examples.
     */
    public void runAllAdvancedSearchExamples() {
        try {
            // Complex query examples
            performComplexBooleanQuery();
        } catch (Exception e) {
            LOGGER.error("Error running advanced search examples", e);
        }
    }

    /**
     * Example 1: Complex boolean query with multiple conditions.
     */
    public void performComplexBooleanQuery() {
        LOGGER.info("=== Complex Boolean Query Example ===");

        try {
            // Build a complex boolean query
            // Must conditions (AND logic)
            List<Query> mustQueries = new ArrayList<>();

            // Time range condition
            RangeQueryValue timeValue = new RangeQueryValue.Builder()
                    .setGte("1000000000000")
                    .setLte("2000000000000")
                    .build();
            StructValue structValue = new StructValue("timestamp");
            structValue.setField("timestamp", timeValue._getDataValue());
            QueryRange timeRange = QueryRange._newInstance(structValue);

            Query timeQuery = new Query();
            timeQuery.setRange(timeRange);
            mustQueries.add(timeQuery);

            // Should conditions (OR logic)
            List<Query> shouldQueries = new ArrayList<>();

            QueryMatchPhrase queryMatchPhrase1 = new QueryMatchPhrase();
            StringValue ERROR = new StringValue("ERROR");
            queryMatchPhrase1._setDynamicField("level", ERROR);

            Query errorQuery = new Query();
            errorQuery.setMatchPhrase(queryMatchPhrase1);
            shouldQueries.add(errorQuery);

            QueryMatchPhrase queryMatchPhrase2 = new QueryMatchPhrase();
            StringValue WARN = new StringValue("WARN");
            queryMatchPhrase2._setDynamicField("level", WARN);

            Query warnQuery = new Query();
            warnQuery.setMatchPhrase(queryMatchPhrase2);
            shouldQueries.add(warnQuery);

            // Combine into boolean query
            BoolQuery boolQuery = new BoolQuery.Builder()
                    .setMust(mustQueries)
                    .setShould(shouldQueries)
                    .build();

            Query mainQuery = new Query();
            mainQuery.setBool(boolQuery);

            // Add aggregations for analysis
            Map<String, Aggregation> aggregations = new HashMap<>();

            // Terms aggregation for log levels
            MultiTermsAggregation levelAgg = new MultiTermsAggregation.Builder()
                    .setTerms(Arrays.asList(
                            new MultiTermLookup.Builder().setField("level").build()
                    ))
                    .setSize(10L)
                    .build();
            aggregations.put("log_levels", new Aggregation.Builder().setMultiTerms(levelAgg).build());
            // Build the search request
            QueryRequest request = new QueryRequest.Builder()
                    .setQuery(mainQuery)
                    .setSize(50L)
                    .setAggregations(aggregations)
                    .setTrackTotalHits(true)
                    .build();

            // Execute the search
            CompletionStageFuture<QueryResponse> future = searchService.executeLogSearchQuery(request).invoke();
            QueryResponse response = future.get();

            LOGGER.info("Complex boolean query executed successfully");
            LOGGER.info("Total hits: " + ((response.getEvents() != null && response.getEvents().getHits() != null )?
                    response.getEvents().getHits().size() : 0));

            if (response.getAggregations() != null) {
                LOGGER.info("Result: " + response.getAggregations().toString());
            }

        } catch (Exception e) {
            LOGGER.error("Complex boolean query failed", e);
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
