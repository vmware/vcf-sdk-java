/*
 * ******************************************************************
 * Copyright (c) 2022-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.vlcm;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.cis.Tasks;
import com.vmware.cis.TasksTypes;
import com.vmware.cis.task.Info;
import com.vmware.cis.task.Status;
import com.vmware.esx.settings.AddOnSpec;
import com.vmware.esx.settings.BaseImageSpec;
import com.vmware.esx.settings.HostHardwareSpec;
import com.vmware.esx.settings.ImageSelectionSpec;
import com.vmware.esx.settings.ImageSelectionSpec.SelectionType;
import com.vmware.esx.settings.SoftwareInfo;
import com.vmware.esx.settings.clusters.Software;
import com.vmware.esx.settings.clusters.SoftwareTypes;
import com.vmware.esx.settings.clusters.software.Drafts;
import com.vmware.esx.settings.clusters.software.DraftsTypes.CommitSpec;
import com.vmware.esx.settings.clusters.software.DraftsTypes.FilterSpec;
import com.vmware.esx.settings.clusters.software.DraftsTypes.Summary;
import com.vmware.esx.settings.clusters.software.drafts.software.AddOn;
import com.vmware.esx.settings.clusters.software.drafts.software.AlternativeImages;
import com.vmware.esx.settings.clusters.software.drafts.software.AlternativeImagesTypes.CreateSpec;
import com.vmware.esx.settings.clusters.software.drafts.software.BaseImage;
import com.vmware.esx.settings.clusters.software.drafts.software.Components;
import com.vmware.esx.settings.clusters.software.drafts.software.alternative_images.SelectionCriteria;
import com.vmware.esx.settings.clusters.software.drafts.software.alternative_images.software.RemovedComponents;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.services.Service;
import com.vmware.vcenter.services.ServiceTypes;
import com.vmware.vim25.ManagedObjectReference;

/**
 * Demonstrates the vLCM image operations
 *
 * <p>Sample Prerequisites:
 *
 * <ol>
 *   <li>Vcenter version &gt;= 7.0
 *   <li>For running stage cluster task, esx &gt;=8.0
 *   <li>Cluster should be a vLCM cluster
 *   <li>For running methods related to Alternative Images Vcenter version &gt;=9.0 and Base Image &gt;=9.0
 * </ol>
 */
public class ImageOperations {
    private static final Logger log = LoggerFactory.getLogger(ImageOperations.class);
    /** REQUIRED: vCenter FQDN or IP address. */
    public static String serverAddress = "vcenter1.mycompany.com";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";
    /** REQUIRED: Cluster ID to perform Image Operations. */
    public static String clusterId = "clusterId";
    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (do not leave it empty on production environments).
     */
    public static String trustStorePath = null;

    private static Software softwareStub;
    private static Tasks tasks;
    private static Drafts drafts;
    private static AlternativeImages draftsAltImg;
    private static RemovedComponents draftsAltImgRemovedComp;
    private static SelectionCriteria draftsAltImgSelCriteria;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(ImageOperations.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Service serviceApiStub = client.createStub(Service.class);
            log.info("Service API stub {}", serviceApiStub);

            softwareStub = client.createStub(Software.class);
            tasks = client.createStub(Tasks.class);
            drafts = client.createStub(Drafts.class);
            draftsAltImg = client.createStub(AlternativeImages.class);
            draftsAltImgRemovedComp = client.createStub(RemovedComponents.class);
            draftsAltImgSelCriteria = client.createStub(SelectionCriteria.class);

            // Remove any preexisting draft from the cluster
            removeExistingDraftsFromCluster(clusterId);

            // Print current desired state of cluster
            printDesiredImage(clusterId);

            // Below Alternative Images methods are supported only for
            // VC version >= 9.0 and desired ESX Base Image version >= 9.0
            // For Base Image < 9.0 committing draft shall fail.
            log.info("Adding an alternative image to desired image for cluster: {}", clusterId);
            String imageId = createAlternativeImage(clusterId, "TEST_IMAGE");
            printDesiredImage(clusterId);

            if (!imageId.isEmpty()) {
                System.out.println(
                        "Deleting alternative image " + imageId + " from desired image for cluster : " + clusterId);
                deleteAlternativeImage(clusterId, imageId);
                printDesiredImage(clusterId);
            }
        }
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
     * Runs the stage task with the set desired image
     *
     * @param clusterId {@link ManagedObjectReference} of the cluster
     * @return Returns task id of the format
     *     52f9f1b0-160a-25fe-fd1e-e7526df80e0d:com.vmware.esx.settings.clusters.software
     */
    private static String runStageTask(String clusterId) {
        SoftwareTypes.StageSpec.Builder builder = new SoftwareTypes.StageSpec.Builder();
        return softwareStub.stage_Task(clusterId, builder.build());
    }

    /**
     * Runs the apply task with the set desired image
     *
     * @param clusterId {@link ManagedObjectReference} of the cluster
     * @return Returns task id of the format
     *     52f9f1b0-160a-25fe-fd1e-e7526df80e0d:com.vmware.esx.settings.clusters.software
     */
    private static String runApplyTask(String clusterId) {
        com.vmware.esx.settings.clusters.SoftwareTypes.ApplySpec.Builder builder =
                new SoftwareTypes.ApplySpec.Builder();
        builder.setAcceptEula(true);
        return softwareStub.apply_Task(clusterId, builder.build());
    }

    /**
     * Runs the scan task with the set desired image
     *
     * @param clusterId {@link ManagedObjectReference} of the cluster
     * @return Returns task id of the format
     *     52f9f1b0-160a-25fe-fd1e-e7526df80e0d:com.vmware.esx.settings.clusters.software
     */
    private static String runScanTask(String clusterId) {
        return softwareStub.scan_Task(clusterId);
    }

    /**
     * Gets the result of the task ID specified by polling taskId every second.
     *
     * @param taskId task ID of the task for which the result is to be fetched.
     * @return Result of the task
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    private static String getTaskResult(String taskId) throws InterruptedException {
        /*
           Keeps polling till the task either fails or succeeds.
           Sample result of the Task
           com.vmware.esx.settings.cluster_compliance =>
           {incompatible_hosts=[], hosts=[map-entry => {value=com.vmware.esx.settings.host_compliance =>
               {components=[map-entry =>
                   {value=com.vmware.esx.settings.component_compliance =>
                       {current=<unset>, target_source=USER, current_source=<unset>, stage_status=NOT_STAGED,
                       notifications=com.vmware.esx.settings.notifications =>
                       {warnings=<unset>, errors=<unset>, info=<unset>},
                       status=NON_COMPLIANT,
                       target=com.vmware.esx.settings.component_info =>
                       {details=com.vmware.esx.settings.component_details =>
                       {display_version=2.0, vendor=VMware, display_name=test-component-8}, version=2.0-1}},
                        key=test-component-8}],
                       solutions=[], impact=REBOOT_REQUIRED, commit=5,
                       add_on=com.vmware.esx.settings.add_on_compliance =>
                       {current=<unset>, stage_status=<unset>, notifications=com.vmware.esx.settings.notifications =>
                       {warnings=<unset>, errors=<unset>, info=<unset>}, status=COMPLIANT, target=<unset>},
                       stage_status=NOT_STAGED,
                       hardware_support=[], base_image=com.vmware.esx.settings.base_image_compliance =>
                       {current=com.vmware.esx.settings.base_image_info =>
                       {details=com.vmware.esx.settings.base_image_details =>
                       {live_update_compatible_versions=<unset>, release_date=2022-11-09T04:52:15.825Z,
                       display_version=8.0  - 20764643, display_name=ESXi}, version=8.0.1-0.0.20764643},
                       stage_status=<unset>,
                       notifications=com.vmware.esx.settings.notifications =>
                       {warnings=<unset>, errors=<unset>, info=<unset>},
                       status=COMPLIANT, target=com.vmware.esx.settings.base_image_info =>
                       {details=com.vmware.esx.settings.base_image_details =>
                       {live_update_compatible_versions=<unset>,
                       release_date=2022-11-09T04:52:15.825Z, display_version=8.0  - 20764643, display_name=ESXi},
                       version=8.0.1-0.0.20764643}},
                       data_processing_units_compliance=<unset>, solution_impacts=<unset>,
                       scan_time=2022-12-14T03:52:16.999Z,
                       notifications=com.vmware.esx.settings.notifications => {warnings=<unset>, errors=<unset>,
                       info=[com.vmware.esx.settings.notification => {retriable=<unset>,
                       id=com.vmware.vcIntegrity.lifecycle.HostScan.QuickBoot.Supported, originator=<unset>,
                       time=2022-12-14T03:52:16.762Z, message=com.vmware.vapi.std.localizable_message =>
                       {args=[], default_message=Quick Boot is supported on the host.,
                       localized=Quick Boot is supported on the host.,
                       id=com.vmware.vcIntegrity.lifecycle.HostScan.QuickBoot.Supported, params=<unset>},
                       type=INFO, resolution=<unset>},
                       com.vmware.esx.settings.notification =>
                       {retriable=<unset>, id=com.vmware.vcIntegrity.lifecycle.HostScan.RebootImpact,
                       originator=<unset>,
                       time=2022-12-14T03:52:16.999Z, message=com.vmware.vapi.std.localizable_message =>
                       {args=[], default_message=The host will be rebooted during remediation.,
                       localized=The host will be rebooted during remediation.,
                       id=com.vmware.vcIntegrity.lifecycle.HostScan.RebootImpact, params=<unset>},
                       type=INFO, resolution=<unset>}]}, status=NON_COMPLIANT}, key=host-16}],
                       non_compliant_hosts=[host-16],
                       impact=REBOOT_REQUIRED, commit=5, compliant_hosts=[],
                       scan_time=2022-12-14T03:52:16.999Z, stage_status=NOT_STAGED,
                       unavailable_hosts=[], notifications=com.vmware.esx.settings.notifications =>
                       {warnings=<unset>, errors=<unset>, info=<unset>},
                       host_info=[map-entry =>
                       {value=com.vmware.esx.settings.host_info => {is_vsan_witness=<unset>,
                       name=10.10.10.10}, key=host-16}],
                       status=NON_COMPLIANT}
        */
        Info info;
        String res;
        do {
            info = getTaskInfo(taskId);
            res = String.valueOf(info.getResult());
            log.info("Task [{}] current status is:{}", taskId, info.getStatus());
            Thread.sleep(1000);
        } while (info.getStatus() != Status.FAILED && info.getStatus() != Status.SUCCEEDED);
        return res;
    }

    /**
     * Sets the baseImageVersion for the given cluster.
     *
     * @param draftsBaseImage client stub of drafts base image
     * @param clusterId {@link ManagedObjectReference} of the cluster
     * @param baseImageVersion base image version to be set
     * @return draft ID
     */
    public static String setBaseImage(BaseImage draftsBaseImage, String clusterId, String baseImageVersion) {

        removeExistingDraftsFromCluster(clusterId);
        String draftId = drafts.create(clusterId);
        BaseImageSpec baseImageSpec = new BaseImageSpec();
        baseImageSpec.setVersion(baseImageVersion);
        draftsBaseImage.set(clusterId, draftId, baseImageSpec);
        commitSpec(clusterId, draftId, "Committing the base image");
        return draftId;
    }

    /**
     * Adds the specified addon to the desired image of the clusterId.
     *
     * @param draftsComponents client stub of drafts components
     * @param clusterId {@link ManagedObjectReference} of the cluster
     * @param component component name to be set
     * @param compVersion component version to be set
     */
    public static void setComponent(
            Components draftsComponents, String clusterId, String component, String compVersion) {

        removeExistingDraftsFromCluster(clusterId);
        String draftId = drafts.create(clusterId);
        draftsComponents.set(clusterId, draftId, component, compVersion);
        commitSpec(clusterId, draftId, "Setting the component");
    }

    /**
     * Removes the specified component from the desired image of the clusterId.
     *
     * @param draftsComponents client stub of drafts components
     * @param clusterId {@link ManagedObjectReference} of the cluster
     * @param component component name to be removed
     */
    public static void removeComponent(Components draftsComponents, String clusterId, String component) {

        removeExistingDraftsFromCluster(clusterId);
        String draftId = drafts.create(clusterId);
        draftsComponents.delete(clusterId, draftId, component);
        commitSpec(clusterId, draftId, "Removing the component");
    }

    /**
     * Adds the specified addon to the desired image of the clusterId.
     *
     * @param draftsAddon client stub of drafts addon
     * @param clusterId {@link ManagedObjectReference} of the cluster
     * @param addonName name of the addon
     * @param addonVersion version of the addon
     */
    public static void setAddon(AddOn draftsAddon, String clusterId, String addonName, String addonVersion) {

        removeExistingDraftsFromCluster(clusterId);
        String draftId = drafts.create(clusterId);
        AddOnSpec addOnSpec = new AddOnSpec();
        addOnSpec.setName(addonName);
        addOnSpec.setVersion(addonVersion);
        draftsAddon.set(clusterId, draftId, addOnSpec);
        commitSpec(clusterId, draftId, "Committing the addon");
    }

    /**
     * Removes the specified addon from the desired image of the clusterId.
     *
     * @param draftsAddon client stub of drafts addon
     * @param clusterId {@link ManagedObjectReference} of the cluster
     */
    public static void removeAddon(AddOn draftsAddon, String clusterId) {

        removeExistingDraftsFromCluster(clusterId);
        String draftId = drafts.create(clusterId);
        draftsAddon.delete(clusterId, draftId);
        commitSpec(clusterId, draftId, "Removing the addon");
    }

    /**
     * Removes the pending draft for the provided clusterId. One cluster can have only one pending draft. This function
     * removes the pending draft for a new cluster so that we can create a new one.
     *
     * @param clusterId {@link ManagedObjectReference} of the cluster
     */
    public static void removeExistingDraftsFromCluster(String clusterId) {

        FilterSpec filterSpec = new FilterSpec();
        Set<String> owners = new HashSet<>();
        owners.add(username);
        filterSpec.setOwners(owners);
        Map<String, Summary> listOfDrafts = drafts.list(clusterId, filterSpec);
        for (String key : listOfDrafts.keySet()) {
            drafts.delete(clusterId, key);
        }
    }

    /**
     * Commit the vlcm draft with the given draftId.
     *
     * @param clusterId {@link ManagedObjectReference} of the cluster
     * @param draftId draft ID
     * @param commitMessage message to be saved with commit
     * @return true if commit succeeds, false if commit fails
     */
    public static boolean commitSpec(String clusterId, String draftId, String commitMessage) {

        CommitSpec commitSpec = new CommitSpec();
        commitSpec.setMessage(commitMessage);
        String taskId = drafts.commit_Task(clusterId, draftId, commitSpec);
        try {
            String taskResult = getTaskResult(taskId);
            if ("null".equals(taskResult)) {
                return false;
            }
        } catch (InterruptedException e) {
            // Log exception details
            log.error("Commit of draft failed, Reason: {}", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Adds an alternative image with selection criteria to the desired image of the clusterId. Prerequisites for
     * running createAlternativeImage vCenter version &gt;= 9.0 and desired ESX Base Image Version &gt;= 9.0
     *
     * @param clusterId {@link ManagedObjectReference} of the cluster
     * @param imageDisplayName Display name of the Alternative Image
     * @return new imageId created if commit succeeds or empty if commit fails
     */
    public static String createAlternativeImage(String clusterId, String imageDisplayName) {

        removeExistingDraftsFromCluster(clusterId);
        String draftId = drafts.create(clusterId);

        // Add an alternative image to the draft
        CreateSpec spec = new CreateSpec();
        spec.setDisplayName(imageDisplayName);
        String imageId = draftsAltImg.create(clusterId, draftId, spec);

        // Configure selection criteria to the alternative image in the draft
        ImageSelectionSpec selectionSpec = new ImageSelectionSpec();
        selectionSpec.setSelectionType(SelectionType.HOST_HARDWARE_SPEC);
        HostHardwareSpec hostHwSpec = new HostHardwareSpec();
        hostHwSpec.setVendor("TEST_VENDOR");
        selectionSpec.setHostHardwareSpec(hostHwSpec);
        draftsAltImgSelCriteria.set(clusterId, draftId, imageId, selectionSpec);

        if (commitSpec(clusterId, draftId, "Creating an alternative image")) {
            return imageId;
        }

        return "";
    }

    /**
     * Deletes an alternative image from the desired image of the clusterId. Prerequisites for running
     * deleteAlternativeImage vCenter version &gt;= 9.0 and desired ESX Base Image Version &gt;= 9.0
     *
     * @param clusterId {@link ManagedObjectReference} of the cluster
     * @param imageId image ID to be deleted from the desired image
     */
    public static void deleteAlternativeImage(String clusterId, String imageId) {

        removeExistingDraftsFromCluster(clusterId);
        String draftId = drafts.create(clusterId);
        draftsAltImg.delete(clusterId, draftId, imageId);
        commitSpec(clusterId, draftId, "Deleting alternative image");
    }

    /**
     * Removes the specified component from the alternative image in desired image of the clusterId. Prerequisites for
     * running setAlternativeImageRemovedComponent vCenter version &gt;= 9.0 and desired ESX Base Image Version &gt;=
     * 9.0
     *
     * @param clusterId {@link ManagedObjectReference} of the cluster
     * @param imageId image ID on which component is to be removed
     * @param component component name to be removed
     */
    public static void setAlternativeImageRemovedComponent(String clusterId, String imageId, String component) {

        removeExistingDraftsFromCluster(clusterId);
        String draftId = drafts.create(clusterId);
        draftsAltImgRemovedComp.set(clusterId, draftId, imageId, component);
        commitSpec(clusterId, draftId, "Setting the removed component in alternative image");
    }

    /**
     * Gets and print the desired state of the cluster.
     *
     * @param clusterId {@link ManagedObjectReference} of the cluster
     */
    public static void printDesiredImage(String clusterId) {

        SoftwareInfo swInfo = softwareStub.get(clusterId);
        System.out.println("-------------------------------------------------");
        System.out.println("Current desired image for " + clusterId + " : ");
        System.out.println(swInfo);
        System.out.println("-------------------------------------------------");
    }

    protected void formattedOutputDisplay(ServiceTypes.Info info, String serviceName) {
        System.out.println("-----------------------------");
        System.out.println("Service Name : " + serviceName);
        System.out.println("Service Name Key : " + info.getNameKey());
        System.out.println("Service Health : " + info.getHealth());
        System.out.println("Service Status : " + info.getState());
        System.out.println("Service Startup Type : " + info.getStartupType());
    }
}
