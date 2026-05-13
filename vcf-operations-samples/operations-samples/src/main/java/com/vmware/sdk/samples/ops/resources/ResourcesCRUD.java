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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.ops.api.client.controllers.ResourcesClient;
import com.vmware.ops.api.model.common.PageInfo;
import com.vmware.ops.api.model.resource.ResourceDto;
import com.vmware.ops.api.model.resource.ResourceIdentifier;
import com.vmware.ops.api.model.resource.ResourceKey;
import com.vmware.ops.api.model.resource.ResourceQuery;
import com.vmware.sdk.samples.ops.SampleBase;
import com.vmware.sdk.samples.ops.config.resources.ResourceConfig;
import com.vmware.sdk.samples.ops.helpers.ConfigValidator;

/**
 * Example that illustrates the use of API client bindings for resource
 * CRUD operations
 */
public class ResourcesCRUD extends SampleBase {
    private static final Logger logger = LoggerFactory.getLogger(ResourcesCRUD.class);

    private final ResourcesClient resourcesClient;
    private final ResourceConfig resourceConfig;

    public ResourcesCRUD(String clientConfigFile, String sampleConfigFile) throws IOException {
        super(clientConfigFile);

        resourceConfig = mapper.readValue(new File(sampleConfigFile), ResourceConfig.class);
        ConfigValidator.validate(resourceConfig);

        this.resourcesClient = getClient().resourcesClient();
    }

    @Override
    public void run() {
        logger.info("Creating a Resource...");
        ResourceDto resource = createResource();
        UUID resourceIdentifier = resource.getIdentifier();

        logger.info("Sleep for 20 seconds for resource to get created");
        try {
            Thread.sleep(20 * 1000);
        } catch (InterruptedException e) {
            logger.error("Thread interrupted", e);
        }

        ResourceDto resourceDto = getResourcesWithUUID(resourceIdentifier);
        if (resourceDto != null && resourceDto.getResourceKey().getName().equals(resourceConfig.getName())) {
            logger.info("Resource {} is indeed created.", resourceConfig.getName());
        } else {
            logger.warn("Resource {} is not created.", resourceConfig.getName());
            return;
        }

        logger.info("Updating a Resource");
        ResourceKey resourceKey = resource.getResourceKey();
        resourceKey.setName("Bar");
        resource.setResourceKey(resourceKey);
        resourceDto = updateResource(resource);

        logger.info("Sleep for 20 seconds for resource to get updated");
        try {
            Thread.sleep(20 * 1000);
        } catch (InterruptedException e) {
            logger.error("Thread interrupted", e);
        }

        if (resourceDto.getResourceKey().getName().equals("Bar")) {
            logger.info("Resource {} is indeed updated to Bar.", resourceConfig.getName());
        } else {
            logger.warn("Resource {} is not updated to Bar.", resourceConfig.getName());
        }

        logger.info("Deleting previously created resource");
        deleteResource(resourceIdentifier);
        logger.info("Sleep for 20 seconds for resource to get deleted");
        try {
            Thread.sleep(20 * 1000);
        } catch (InterruptedException e) {
            logger.error("Thread interrupted", e);
        }
        resourceDto = getResourcesWithUUID(resource.getIdentifier());
        if (resourceDto != null) {
            logger.warn("Resource is not deleted successfully.");
        } else {
            logger.info("Resource is deleted successfully.");
        }

        logger.info("Getting first 100 resources.");
        getResources();

        logger.info("Getting resources by page.");
        getResourcesByPage();

        logger.info("Getting resources with name.");
        getResourcesWithName(new String[] {"name-1", "name-2"});
    }

    /**
     * Creates a Dummy vSphere Virtual Machine corresponding Resource in VMware Cloud Foundation Operations.<br>
     * Assumes that the vSphere solution pack has already been installed.
     */
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
     * Delete a resource
     */
    public void deleteResource(UUID resourceId) {
        resourcesClient.deleteResource(resourceId);
    }

    public ResourceDto updateResource(ResourceDto resource) {
        return resourcesClient.updateResource(resource);
    }

    public ResourceDto getResourcesWithUUID(UUID uuid) {
        try {
            return resourcesClient.get(uuid);
        } catch (Exception e) {
            return null;
        }
    }

    public ResourceDto.ResourceDtoList getResources() {
        PageInfo pageInfo = new PageInfo();
        pageInfo.setPage(0);
        pageInfo.setPageSize(100);
        ResourceDto.ResourceDtoList resources = this.resourcesClient.lookupResources(new ResourceQuery(), pageInfo);
        logger.info("Total count: {}", resources.getPageInfo().getTotalCount());
        logger.info("Page size: {}", resources.getPageInfo().getPageSize());
        return resources;
    }

    // Example code detailing the use of page and pageSize
    public Map<Integer, ResourceDto.ResourceDtoList> getResourcesByPage() {
        int pageSize = 1000;
        Map<Integer, ResourceDto.ResourceDtoList> pageMap = new HashMap<>();
        ResourceQuery rq = new ResourceQuery();
        PageInfo pageInfo = new PageInfo();
        pageInfo.setPageSize(pageSize);
        ResourceDto.ResourceDtoList resources = this.resourcesClient.lookupResources(rq, pageInfo);
        int totalCount = resources.getPageInfo().getTotalCount();
        for (int i = 0; i < totalCount / pageSize; i++) {
            PageInfo pageInfo1 = new PageInfo();
            pageInfo1.setPage(i);
            pageInfo1.setPageSize(pageSize);
            ResourceDto.ResourceDtoList tempResources = this.resourcesClient.lookupResources(rq, pageInfo1);
            pageMap.put(i, tempResources);
        }
        return pageMap;
    }

    public ResourceDto.ResourceDtoList getResourcesWithName(String[] names) {
        ResourceQuery rq = new ResourceQuery();
        rq.setName(names);
        return resourcesClient.lookupResources(rq, null);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            logger.error(
                    "Please provide the client-config JSON file path first, followed by the resource-config JSON file path, via arguments.");
            System.exit(1);
        }
        new ResourcesCRUD(args[0], args[1]).run();
    }
}
