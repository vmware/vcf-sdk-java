/*
 * ******************************************************************
 * Copyright (c) 2025-2026 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.resources;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.ops.api.client.controllers.ResourcesClient;
import com.vmware.ops.api.model.property.PropertyContent;
import com.vmware.ops.api.model.property.PropertyContents;
import com.vmware.ops.api.model.resource.ResourceDto;
import com.vmware.ops.api.model.resource.ResourceIdentifier;
import com.vmware.ops.api.model.resource.ResourceKey;
import com.vmware.ops.api.model.stat.StatContent;
import com.vmware.ops.api.model.stat.StatContents;
import com.vmware.sdk.samples.ops.SampleBase;
import com.vmware.sdk.samples.ops.config.resources.ResourceConfig;
import com.vmware.sdk.samples.ops.config.resources.StatAndPropertyConfig;
import com.vmware.sdk.samples.ops.helpers.ConfigValidator;

/**
 * Example that illustrates the use of API client bindings for creating resources
 * and pushing in Stats and Properties.
 */
public class ResourcesAndStats extends SampleBase {
    private static final Logger logger = LoggerFactory.getLogger(ResourcesAndStats.class);

    private final ResourcesClient resourcesClient;
    private final ResourceConfig resourceConfig;
    private final StatAndPropertyConfig statAndPropertyConfig;
    private final int NUMBER_OF_STATS_TO_PUSH;

    public ResourcesAndStats(String clientConfigFile, String sampleConfigFile) throws IOException {
        super(clientConfigFile);

        resourceConfig = mapper.readValue(new File(sampleConfigFile), ResourceConfig.class);
        ConfigValidator.validate(resourceConfig);

        statAndPropertyConfig = resourceConfig.getStatAndPropertyConfig();
        NUMBER_OF_STATS_TO_PUSH = statAndPropertyConfig.getNumberOfStatAndPropertyToPush();

        this.resourcesClient = getClient().resourcesClient();
    }

    @Override
    public void run() {
        logger.info("Creating a Resource...");
        ResourceDto resource = createResource();

        pushData(resource);

        logger.info("Sleep for 2 mins for data to get stored.");
        try {
            Thread.sleep(2 * 60 * 1000);
        } catch (InterruptedException e) {
            logger.error("Thread interrupted", e);
        }

        logger.info("Moving a resource to maintained state and ending the maintained state");
        moveResourceMaintainedState(resource.getIdentifier());

        System.out.println("Deleting previously created resource");
        deleteResource(resource.getIdentifier());
    }

    public void pushData(ResourceDto resource) {
        double[] data = new double[NUMBER_OF_STATS_TO_PUSH];
        long[] timestamps = new long[NUMBER_OF_STATS_TO_PUSH];
        String[] values = new String[NUMBER_OF_STATS_TO_PUSH];
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long currentTime = cal.getTimeInMillis();
        double lastDataPoint;
        for (int i = 0; i < NUMBER_OF_STATS_TO_PUSH; i++) {
            lastDataPoint = Math.random() * 100;
            data[i] = lastDataPoint;
            timestamps[i] = currentTime - ((i + 1) * 300000L);
            values[i] = i % 2 == 0 ? "CONNECTED" : "DISCONNECTED";
        }
        for (int i = 0; i < NUMBER_OF_STATS_TO_PUSH; i++) {
            logger.info("Pushing Numeric Stats data...");
            addStats(null, resource.getIdentifier(), statAndPropertyConfig.getStatKey(), timestamps, data, null, false);

            logger.info("Pushing String Properties data...");
            addProperties(
                    null, resource.getIdentifier(), statAndPropertyConfig.getPropertyKey(), timestamps, null, values);
        }
    }

    public ResourceDto createResource() {
        ResourceDto resource = new ResourceDto();
        ResourceKey resourceKey = new ResourceKey();
        resourceKey.setName(resourceConfig.getName());
        resource.setDescription(resourceConfig.getDescription());
        resourceKey.setAdapterKindKey(resourceConfig.getAdapterKindKey());
        resourceKey.setResourceKindKey(resourceConfig.getResourceKindKey());
        List<ResourceIdentifier> resourceIdentifiers = new ArrayList<>();
        for (Map.Entry<String, String> entry : resourceConfig.getIdentifiers().entrySet()) {
            resourceIdentifiers.add(new ResourceIdentifier(entry.getKey(), entry.getValue()));
        }
        resourceKey.setResourceIdentifiers(resourceIdentifiers);
        resource.setResourceKey(resourceKey);
        return resourcesClient.createResource(resource, resourceConfig.getPushAdapterKindKey());
    }

    /**
     * Push stat data for the specified Stat Keys
     *
     * @param adapterSourceId the ID of the adapter kind that will push the stats,
     *          may be null which defaults to SuiteAPI adapter
     * @param resourceUUID VMware Cloud Foundation Operations UUID of the Resource
     * @param statKey Name of the Stat Key
     * @param timestamps Array of long values as timestamps
     * @param data Array of double values as data
     * @param storeOnly true if we want to store the data only, false if we want
     *            analytics processing to be performed
     */
    public void addStats(
            String adapterSourceId,
            UUID resourceUUID,
            String statKey,
            long[] timestamps,
            double[] data,
            String[] values,
            boolean storeOnly) {
        logger.info("Pushing some data for stat key: '{}' ...", statKey);
        StatContents contents = new StatContents();
        StatContent content = new StatContent();
        content.setStatKey(statKey);
        content.setData(data);
        content.setValues(values);
        content.setTimestamps(timestamps);
        contents.getStatContents().add(content);
        if (adapterSourceId == null) {
            resourcesClient.addStats(resourceUUID, contents, true);
        } else {
            resourcesClient.addStats(adapterSourceId, resourceUUID, contents, storeOnly);
        }
    }

    /**
     * Push stat data for the specified Stat Keys
     *
     * @param adapterSourceId the ID of the adapter kind that will push the stats,
     *          may be null which defaults to SuiteAPI adapter
     * @param resourceUUID VMware Cloud Foundation Operations UUID of the Resource
     * @param statKey Name of the Stat Key
     * @param timestamps Array of long values as timestamps
     * @param data Array of double values as data
     */
    public void addProperties(
            String adapterSourceId,
            UUID resourceUUID,
            String statKey,
            long[] timestamps,
            double[] data,
            String[] values) {
        logger.info("Pushing some data for property: '{}' ...", statKey);
        PropertyContents contents = new PropertyContents();
        PropertyContent content = new PropertyContent();
        content.setStatKey(statKey);
        content.setData(data);
        content.setValues(values);
        content.setTimestamps(timestamps);
        contents.getPropertyContents().add(content);
        if (adapterSourceId == null) {
            resourcesClient.addProperties(resourceUUID, contents);
        } else {
            resourcesClient.addProperties(adapterSourceId, resourceUUID, contents);
        }
    }

    public void moveResourceMaintainedState(UUID resourceId) {
        int duration = 1;
        long endTime = System.currentTimeMillis() + (5 * 60 * 1000);
        resourcesClient.markResourcesAsBeingMaintained(
                duration,
                0L,
                resourceId); /* will move the resource to maintained state for the 'duration' time period */
        resourcesClient.markResourcesAsBeingMaintained(
                0, endTime, resourceId); /* will move the resource to maintained state till the end time */
        resourcesClient.markResourcesAsBeingMaintained(
                duration,
                endTime,
                resourceId); /* will move the resource to maintained state till the end time (duration is not considered) */
        resourcesClient.markResourcesAsBeingMaintained(
                0,
                0L,
                resourceId); /* will move the resource to maintained_manual state until 'unmarkResourcesAsBeingMaintained' method is called */
        resourcesClient.unmarkResourcesAsBeingMaintained(
                resourceId); /* will move the resource to back to started state */
    }

    public void deleteResource(UUID resourceId) {
        resourcesClient.deleteResource(resourceId);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            logger.error(
                    "Please provide the client-config JSON file path first, followed by the resource-stat-and-property-config JSON file path, via arguments.");
            System.exit(1);
        }
        new ResourcesAndStats(args[0], args[1]).run();
    }
}
