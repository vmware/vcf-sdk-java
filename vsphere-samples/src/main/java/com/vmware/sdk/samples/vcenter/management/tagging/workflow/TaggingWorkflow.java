/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.tagging.workflow;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.CLUSTER_COMPUTE_RESOURCE;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.cis.tagging.Category;
import com.vmware.cis.tagging.CategoryModel.Cardinality;
import com.vmware.cis.tagging.CategoryTypes;
import com.vmware.cis.tagging.Tag;
import com.vmware.cis.tagging.TagAssociation;
import com.vmware.cis.tagging.TagTypes;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vapi.std.DynamicID;
import com.vmware.vim25.ManagedObjectReference;

/**
 * Demonstrates tagging Create, Read, Update, Delete operations.
 *
 * <ol>
 *   <li>Create a Tag category called Asset.
 *   <li>Create a Tag called "Server" under the category "Asset".
 *   <li>Retrieve an existing Cluster using VIM APIs.
 *   <li>Translates the Cluster's MoRef into vAPI UUID.
 *   <li>Assign "Server" tag to the Cluster using the UUID.
 *       <p>Additional steps when clearData flag is set to TRUE:
 *   <li>Detach the tag from the Cluster.
 *   <li>Delete the tag "Server".
 *   <li>Delete the tag category "Asset".
 * </ol>
 *
 * <p>Sample Prerequisites: The sample needs an existing Cluster
 */
public class TaggingWorkflow {
    private static final Logger log = LoggerFactory.getLogger(TaggingWorkflow.class);
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

    /** REQUIRED: The name of the cluster to be tagged. */
    public static String clusterName = "clusterName";

    private static Category categoryService;
    private static Tag taggingService;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(TaggingWorkflow.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            categoryService = client.createStub(Category.class);
            taggingService = client.createStub(Tag.class);
            TagAssociation tagAssociation = client.createStub(TagAssociation.class);
            PropertyCollectorHelper propertyCollectorHelper =
                    new PropertyCollectorHelper(client.getVimPort(), client.getVimServiceContent());

            // retrieve the Cluster's MoRef
            ManagedObjectReference clusterMoRef =
                    propertyCollectorHelper.getMoRefByName(clusterName, CLUSTER_COMPUTE_RESOURCE);
            if (clusterMoRef == null) {
                throw new RuntimeException("Assertion: Cluster not found, cluster name: " + clusterName);
            }
            log.info("Cluster MoRef: {}", clusterMoRef.getValue());

            // List all existing tag categories
            List<String> categories = categoryService.list();
            log.info("Tag Categories:\n{}\nEnd of tag categories", categories);

            // List all the existing tags
            List<String> tags = taggingService.list();
            log.info("Tags: {}End of tags", tags);

            // create a new tag category "Asset"
            String categoryName = "Asset";
            String categoryDescription = "All data center assets";
            String tagName = "Server";
            String tagDescription = "Cluster running application server";

            String assetCategoryId = createTagCategory(categoryName, categoryDescription, Cardinality.MULTIPLE);
            log.info("Tag category created; Id: {}", assetCategoryId);

            // create a new Tag "Server"
            String serverTagId = createTag(tagName, tagDescription, assetCategoryId);
            log.info("Tag created; Id: {}", serverTagId);

            // update the asset tag
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");
            Date dt = new Date();
            String date = sdf.format(dt); // formats to 09/23/2009 13:53:28.238

            updateTag(serverTagId, "Server Tag updated at " + date);
            log.info("Tag updated; Id: {}", serverTagId);

            // convert the MoRef to vAPI DyanmicID
            DynamicID clusterDynamicId = new DynamicID(clusterMoRef.getType(), clusterMoRef.getValue());

            // list all the tags that can be attached to the Cluster
            List<String> attachableTags = tagAssociation.listAttachableTags(clusterDynamicId);
            log.info("Attachable Tags:\n{}\nEnd of Attachable tags", attachableTags);
            if (!attachableTags.contains(serverTagId)) {
                throw new RuntimeException("Assertion: Tag is not attachable to the Cluster, tag id: " + serverTagId);
            }

            // tag the Cluster
            tagAssociation.attach(serverTagId, clusterDynamicId);

            if (!tagAssociation.listAttachedTags(clusterDynamicId).contains(serverTagId)) {
                throw new RuntimeException("Assertion: Cluster not tagged, tag id: " + serverTagId);
            }
            boolean tagAttached = true;
            log.info("Cluster: {} tagged", clusterDynamicId);

            // cleanup
            if (tagAttached) {
                tagAssociation.detach(serverTagId, clusterDynamicId);
                log.info("Cluster: {} untagged", clusterDynamicId);
            }

            if (serverTagId != null) {
                deleteTag(serverTagId);
                log.info("Tag deleted; Id: {}", serverTagId);
            }

            if (assetCategoryId != null) {
                deleteTagCategory(assetCategoryId);
                log.info("Tag category deleted; Id: {}", assetCategoryId);
            }
        }
    }

    /** API to create a category. User who invokes this needs create category privilege. */
    private static String createTagCategory(String name, String description, Cardinality cardinality) {
        CategoryTypes.CreateSpec createSpec = new CategoryTypes.CreateSpec();
        createSpec.setName(name);
        createSpec.setDescription(description);
        createSpec.setCardinality(cardinality);

        Set<String> associableTypes = new HashSet<>();
        createSpec.setAssociableTypes(associableTypes);

        return categoryService.create(createSpec);
    }

    /** Deletes an existing tag category; User who invokes this API needs delete privilege on the tag category. */
    private static void deleteTagCategory(String categoryId) {
        categoryService.delete(categoryId);
    }

    /**
     * Creates a tag.
     *
     * @param name Display name of the tag
     * @param description Tag description
     * @param categoryId ID of the parent category in which this tag will be created
     * @return Id of the created tag
     */
    private static String createTag(String name, String description, String categoryId) {
        TagTypes.CreateSpec spec = new TagTypes.CreateSpec();
        spec.setName(name);
        spec.setDescription(description);
        spec.setCategoryId(categoryId);

        return taggingService.create(spec);
    }

    /**
     * Update the description of an existing tag. User who invokes this API needs edit privilege on the tag.
     *
     * @param tagId the ID of the input tag
     */
    private static void updateTag(String tagId, String description) {
        TagTypes.UpdateSpec updateSpec = new TagTypes.UpdateSpec();
        updateSpec.setDescription(description);
        taggingService.update(tagId, updateSpec);
    }

    /**
     * Delete an existing tag. User who invokes this API needs delete privilege on the tag.
     *
     * @param tagId the ID of the input tag
     */
    private static void deleteTag(String tagId) {
        taggingService.delete(tagId);
    }
}
