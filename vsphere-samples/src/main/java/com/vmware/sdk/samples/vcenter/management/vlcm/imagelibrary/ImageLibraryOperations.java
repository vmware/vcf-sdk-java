/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.vlcm.imagelibrary;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.cis.Tasks;
import com.vmware.cis.TasksTypes;
import com.vmware.cis.task.Status;
import com.vmware.esx.settings.BaseImageSpec;
import com.vmware.esx.settings.Inventory;
import com.vmware.esx.settings.InventoryTypes.AssignEntitiesSpec;
import com.vmware.esx.settings.InventoryTypes.EntitySpec;
import com.vmware.esx.settings.InventoryTypes.ScanSpec;
import com.vmware.esx.settings.OrchestratorSpec;
import com.vmware.esx.settings.clusters.software.SoftwareSpecMetadata;
import com.vmware.esx.settings.depot_content.BaseImages;
import com.vmware.esx.settings.depot_content.BaseImagesTypes.FilterSpec;
import com.vmware.esx.settings.depot_content.BaseImagesTypes.Summary;
import com.vmware.esx.settings.repository.Software;
import com.vmware.esx.settings.repository.SoftwareTypes.Info;
import com.vmware.esx.settings.repository.SoftwareTypes.ListResult;
import com.vmware.esx.settings.repository.SoftwareTypes.Record;
import com.vmware.esx.settings.repository.SoftwareTypes.UpdateSpec;
import com.vmware.esx.settings.repository.software.Drafts;
import com.vmware.esx.settings.repository.software.DraftsTypes.CommitSpec;
import com.vmware.esx.settings.repository.software.DraftsTypes.CreateSpec;
import com.vmware.esx.settings.repository.software.drafts.BaseImage;
import com.vmware.esx.settings.repository.software.drafts.DisplayName;
import com.vmware.esx.settings.repository.software.drafts.DisplayNameTypes.SetSpec;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.services.Service;
import com.vmware.vim25.ManagedObjectReference;

/**
 * Demonstrates the vLCM Image Library operations
 *
 * <p>Sample Prerequisites: Vcenter version &gt;= 9.0
 */
public class ImageLibraryOperations {
    private static final Logger log = LoggerFactory.getLogger(ImageLibraryOperations.class);
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
    /** REQUIRED: Cluster ID to perform Image Library Operations. */
    public static String clusterId = "clusterId";
    /** REQUIRED: Display Name of the image to be created in the Image Library. */
    public static String imageName = "imageName";

    private static final String BASE_IMAGE_VERSION = "8.0.3";
    private static final String OWNER_NAME = "sample-owner";
    private static final String OWNER_ID = UUID.randomUUID().toString();
    private static final String INITIAL_DISPLAY_NAME = "autogen-software-spec-999";
    private static final String ROOT_FOLDER = "group-d1";

    private static Software softwareStub;
    private static Tasks tasks;
    private static Drafts drafts;
    private static Inventory inventory;
    private static BaseImages depotBaseImage;
    private static BaseImage draftBaseImage;
    private static DisplayName draftDisplayName;
    private static SoftwareSpecMetadata softwareSpecMetadata;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(ImageLibraryOperations.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Service serviceApiStub = client.createStub(Service.class);
            log.info("Service API stub {}", serviceApiStub);

            softwareStub = client.createStub(Software.class);
            tasks = client.createStub(Tasks.class);
            drafts = client.createStub(Drafts.class);
            inventory = client.createStub(Inventory.class);
            depotBaseImage = client.createStub(BaseImages.class);
            draftBaseImage = client.createStub(BaseImage.class);
            draftDisplayName = client.createStub(DisplayName.class);
            softwareSpecMetadata = client.createStub(SoftwareSpecMetadata.class);

            // Get the list of images in the Image Library
            ListResult imageList = listImages();

            // Get the first image in the Image Library
            if (!imageList.getRecords().isEmpty()) {
                String imageId = imageList.getRecords().get(0).getId();
                printImageDetails(imageId);
            }

            // Get current image of cluster
            String clusterImageId = getCurrentImageAssignedToCluster(clusterId);
            printImageDetails(clusterImageId);

            // Create a new image
            String newImageId = createNewImage();
            printImageDetails(newImageId);

            // Return early if the image could not be created
            if (newImageId.isEmpty()) {
                log.error("Failed to create a new image");
                return;
            }

            // Rename image to the user provided display name
            renameImage(newImageId);

            // Assign new image to cluster
            assignImageToCluster(newImageId, clusterId);

            // Run compliance scan over entire vCenter
            complianceScanAtVcenter();

            // Assign back the original image to cluster
            assignImageToCluster(clusterImageId, clusterId);

            // Cleanup the newly created image
            deleteImage(newImageId);
        }
    }

    /**
     * List and print details of all the images in the Image Library.
     *
     * @return List of all the images in the Image Library
     */
    private static ListResult listImages() {
        ListResult result = softwareStub.list(null, null, null);
        for (Record record : result.getRecords()) {
            log.info("------------------------- Image Name : {} -------------------------", record.getDisplayName());
            log.info("ID : {}", record.getId());
            log.info("Spec : {}", record.getSoftwareSpec());
            log.info("Assigned Entities : {}", record.getAssignedEntities().toString());
        }
        return result;
    }

    /**
     * Print the details of the image identified by the imageId
     *
     * @param imageId The unique identifier of an image in the Image Library
     */
    private static void printImageDetails(String imageId) {
        log.info("Get the details of : {}", imageId);
        Info record = softwareStub.get(imageId);
        log.info("Name : {}", record.getDisplayName());
        log.info("Spec : {}", record.getSoftwareSpec());
        log.info("Assigned Entities : {}", record.getAssignedEntities().toString());
    }

    /**
     * Create a new image in the Image Library. The function creates a draft, sets the base image version and display
     * names in that new draft and the commits the draft.
     *
     * @return Returns the unique identifier of the new image. If the image could not be created, returns an empty
     *     string.
     */
    private static String createNewImage() {
        // Create a draft
        String draftId = createDraftInImageLibrary();

        // Get a BaseImage from depot
        String baseImageVersion = getBaseImageFromDepot(BASE_IMAGE_VERSION);
        if (baseImageVersion.isEmpty()) {
            return "";
        }

        // Set the BaseImage in the draft.
        setBaseImageInDraft(draftId, baseImageVersion);
        // Set an initial display name
        setDisplayNameInDraft(draftId, INITIAL_DISPLAY_NAME);

        // Commit the draft in to the Image Library
        return commitDraftInImageLibrary(draftId);
    }

    /**
     * Rename an existing image in the Image Library to the display name passed by the user.
     *
     * @param imageId The unique identifier of the image in the Image Library
     */
    private static void renameImage(String imageId) {
        OrchestratorSpec orchSpec = new OrchestratorSpec();
        orchSpec.setOwner(OWNER_NAME);
        orchSpec.setOwnerData(OWNER_ID);

        UpdateSpec updateSpec = new UpdateSpec();
        updateSpec.setDisplayName(imageName);
        updateSpec.setOrchestrator(orchSpec);

        log.info("Renaming the image : {}", updateSpec);
        softwareStub.update(imageId, updateSpec);
        log.info("Renamed : {}", imageId);
    }

    /**
     * Delete an existing image in the Image Library
     *
     * @param imageId The unique identifier of the image in the Image Library
     */
    private static void deleteImage(String imageId) {
        log.info("Deleting the image : {}", imageId);
        softwareStub.delete(imageId);
        log.info("Deleted the image : {}", imageId);
    }

    /**
     * Get the current image assigned to the cluster.
     *
     * @param clusterId {@link ManagedObjectReference} of the cluster
     * @return The unique identifier of the image assigned to the cluster
     */
    private static String getCurrentImageAssignedToCluster(String clusterId) {
        com.vmware.esx.settings.SoftwareSpecMetadata metadata = softwareSpecMetadata.get(clusterId);
        String clusterImageId = metadata.getSoftwareSpecId();
        log.info("The cluster {} has existing image: {}", clusterId, clusterImageId);
        return clusterImageId;
    }

    /**
     * Assign an existing image in the Image Library to the cluster provided by the user
     *
     * @param imageId The unique identifier of the image in the Image Library
     * @param clusterId {@link ManagedObjectReference} of the cluster
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    private static void assignImageToCluster(String imageId, String clusterId) throws InterruptedException {
        log.info("Assigning the image : {} to cluster: {}", imageId, clusterId);

        // Create a spec of entities to which the image should be assigned
        EntitySpec entitySpec = new EntitySpec();
        Set<String> clusters = new HashSet<>();
        clusters.add(clusterId);
        entitySpec.setClusters(clusters);
        entitySpec.setType(EntitySpec.InventoryType.CLUSTER);

        // Add the orchestrator details
        OrchestratorSpec orchSpec = new OrchestratorSpec();
        orchSpec.setOwner(OWNER_NAME);
        orchSpec.setOwnerData(OWNER_ID);

        // Create the assign spec
        AssignEntitiesSpec assignSpec = new AssignEntitiesSpec();
        assignSpec.setEntities(entitySpec);
        assignSpec.setOrchestrator(orchSpec);
        assignSpec.setSoftwareSpecId(imageId);
        log.info("Assign Spec: {}", assignSpec);

        // Assign the image
        String taskId = inventory.assignEntities_Task(assignSpec);
        String result = getTaskResult(taskId);
        log.info("Result of Assign: {}", result);
    }

    /**
     * Run a compliance scan on all the clusters in the vCenter. The vCenter is identified by group-d1 in the vCenter
     * inventory. Print the result of the scan operation.
     *
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    private static void complianceScanAtVcenter() throws InterruptedException {
        log.info("Compliance Check at {}", ROOT_FOLDER);

        // Add the list of entities on which compliance needs to run
        EntitySpec entitySpec = new EntitySpec();
        Set<String> folders = new HashSet<>();
        folders.add(ROOT_FOLDER);
        entitySpec.setFolders(folders);
        entitySpec.setType(EntitySpec.InventoryType.FOLDER);

        // Create a scan spec
        ScanSpec scanSpec = new ScanSpec();
        scanSpec.setEntities(entitySpec);
        log.info("Scan Spec: {}", scanSpec);

        // Run the scan task
        String taskId = inventory.scan_Task(scanSpec);
        String result = getTaskResult(taskId);
        log.info("Scan result: {}", result);
    }

    /** Create an empty draft in the Image Library */
    private static String createDraftInImageLibrary() {
        CreateSpec createSpec = new CreateSpec();
        createSpec.setDeleteExistingDraft(true);
        String draftId = drafts.create(createSpec);
        log.info("Created a draft: {}", draftId);
        return draftId;
    }

    /**
     * Set the BaseImage in a draft in the Image Library
     *
     * @param draftId The identifier of a draft in the Image Library
     * @param baseImageVersion The version of the BaseImage to set in the draft
     */
    private static void setBaseImageInDraft(String draftId, String baseImageVersion) {
        BaseImageSpec baseImageSpec = new BaseImageSpec();
        baseImageSpec.setVersion(baseImageVersion);
        log.info("Set the base image: {}", baseImageSpec);
        draftBaseImage.set(draftId, baseImageSpec);
    }

    /**
     * Set the display name in a draft in the Image Library
     *
     * @param draftId The identifier of a draft in the Image Library
     */
    private static void setDisplayNameInDraft(String draftId, String displayName) {
        SetSpec spec = new SetSpec();
        spec.setDisplayName(displayName);
        log.info("Set the display name in draft: {}", spec);
        draftDisplayName.set(draftId, spec);
    }

    /**
     * Commit the given draft into the Image Library as a new image.
     *
     * @param draftId The identifier of a draft in the Image Library
     * @return Returns the unique identifier of the new image. If the image could not be created, returns an empty
     *     string.
     */
    private static String commitDraftInImageLibrary(String draftId) {
        OrchestratorSpec orchSpec = new OrchestratorSpec();
        orchSpec.setOwner(OWNER_NAME);
        orchSpec.setOwnerData(OWNER_ID);

        CommitSpec commitSpec = new CommitSpec();
        commitSpec.setMessage("Save Image to Library");
        commitSpec.setOrchestrator(orchSpec);

        log.info("Save the draft: {} with commit spec: {}", draftId, commitSpec);
        try {
            String taskId = drafts.commit_Task(draftId, commitSpec);
            String newImageId = getTaskResult(taskId);
            log.info("Commit created the image: {}", newImageId);
            return newImageId;
        } catch (Exception e) {
            log.error("Commit failed: ", e);
            return "";
        }
    }

    /**
     * Given a minimum version string, get the BaseImage version of the oldest build present in the vLCM depot for that
     * version or greater
     *
     * @param minimumVersion The minimum version of the BaseImage to look up in the vLCM depot
     * @return BaseImage version of the oldest build equal to or greater than the minimum version. If the BaseImage is
     *     not found, an empty string is returned.
     */
    private static String getBaseImageFromDepot(String minimumVersion) {
        FilterSpec filterSpec = new FilterSpec();
        filterSpec.setMinVersion(minimumVersion);

        List<Summary> listBaseImages = depotBaseImage.list(filterSpec);
        Summary summary = listBaseImages.stream()
                .min(new Comparator<Summary>() {
                    @Override
                    public int compare(Summary o1, Summary o2) {
                        return o1.getVersion().compareTo(o2.getVersion());
                    }
                })
                .orElse(null);

        if (summary == null) {
            return "";
        }

        log.info("Found Base Image: {}", summary);
        return summary.getVersion();
    }

    /**
     * Gets the result of the task ID specified
     *
     * @param taskId task ID of the task for which the result is to be fetched.
     * @return Info structure which contains the status/result of the task.
     */
    private static com.vmware.cis.task.Info getTaskInfo(String taskId) {
        TasksTypes.GetSpec.Builder taskSpecBuilder = new TasksTypes.GetSpec.Builder();
        return tasks.get(taskId, taskSpecBuilder.build());
    }

    /**
     * Gets the result of the task ID specified by polling taskId every second.
     *
     * @param taskId task ID of the task for which the result is to be fetched.
     * @return Result of the task
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    private static String getTaskResult(String taskId) throws InterruptedException {
        com.vmware.cis.task.Info info;
        String res;
        do {
            info = getTaskInfo(taskId);
            res = String.valueOf(info.getResult());
            log.info("Task [{}] current status is: {}", taskId, info.getStatus());
            Thread.sleep(5000);
        } while (info.getStatus() != Status.FAILED && info.getStatus() != Status.SUCCEEDED);
        return res;
    }
}
