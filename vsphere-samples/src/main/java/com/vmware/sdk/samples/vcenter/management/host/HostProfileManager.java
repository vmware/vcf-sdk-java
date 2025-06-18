/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.host;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.HOST_SYSTEM;
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayOfComplianceResult;
import com.vmware.vim25.ComplianceFailure;
import com.vmware.vim25.ComplianceResult;
import com.vmware.vim25.DuplicateNameFaultMsg;
import com.vmware.vim25.HostConfigFailedFaultMsg;
import com.vmware.vim25.HostConfigSpec;
import com.vmware.vim25.HostProfileHostBasedConfigSpec;
import com.vmware.vim25.HostProfileManagerConfigTaskList;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.LocalizableMessage;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ManagedObjectType;
import com.vmware.vim25.ProfileExecuteError;
import com.vmware.vim25.ProfileExecuteResult;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.TimedoutFaultMsg;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachinePowerState;

/**
 * This sample demonstrates HostProfileManager and ProfileComplainceManager.
 *
 * <p>NOTE: this sample may place a host into maintenance mode which will require VMs on the host to be suspended.
 */
public class HostProfileManager {
    private static final Logger log = LoggerFactory.getLogger(HostProfileManager.class);

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

    /** REQUIRED: Name of the host. */
    public static String sourceHostName = "sourceHostName";
    /** REQUIRED: Attached Entity Name. */
    public static String entityName = "entityName";
    /** REQUIRED: Attached Entity Type, example: HostSystem or ClusterComputeResource. */
    public static String entityType = "entityType";

    private static ManagedObjectReference hostprofileManager;
    private static ManagedObjectReference profilecomplianceManager;
    private static List<ManagedObjectReference> suspendedVMList;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(HostProfileManager.class, args);

        if (sourceHostName == null || entityName == null || entityType == null) {
            throw new IllegalArgumentException(
                    "Expected --sourcehostname, --entityname, --entitytype arguments properly");
        }

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            hostprofileManager = serviceContent.getHostProfileManager();
            profilecomplianceManager = serviceContent.getComplianceManager();

            ManagedObjectReference createHostMoRef =
                    propertyCollectorHelper.getMoRefByName(sourceHostName, HOST_SYSTEM);

            if (createHostMoRef == null) {
                throw new IllegalStateException("HostSystem " + sourceHostName);
            }

            ManagedObjectReference attachMoRef =
                    propertyCollectorHelper.getMoRefByName(entityName, ManagedObjectType.fromValue(entityType));

            List<ManagedObjectReference> moRefs = new ArrayList<>();
            moRefs.add(attachMoRef);

            ManagedObjectReference hostProfile = createHostProfile(vimPort, createHostMoRef);

            attachProfileWithManagedEntity(vimPort, hostProfile, moRefs);

            printProfilesAssociatedWithEntity(vimPort, attachMoRef);

            if (entityType.equals(HOST_SYSTEM.value())) {
                updateReferenceHost(vimPort, hostProfile, attachMoRef);

                HostConfigSpec hostConfigSpec = executeHostProfile(vimPort, hostProfile, attachMoRef);
                if (hostConfigSpec != null) {
                    configurationTasksToBeAppliedOnHost(vimPort, propertyCollectorHelper, hostConfigSpec, attachMoRef);
                }

                if (checkProfileCompliance(vimPort, propertyCollectorHelper, hostProfile, attachMoRef)) {
                    applyConfigurationToHost(vimPort, propertyCollectorHelper, attachMoRef, hostConfigSpec);
                }
            } else {
                checkProfileCompliance(vimPort, propertyCollectorHelper, hostProfile, attachMoRef);
            }

            detachHostFromProfile(vimPort, hostProfile, attachMoRef);
            deleteHostProfile(vimPort, hostProfile);
        }
    }

    /**
     * Create a profile from the specified CreateSpec. HostProfileHostBasedConfigSpec is created from the
     * hostEntitymoref (create_host_entity_name) reference. Using this spec a hostProfile is created.
     */
    private static ManagedObjectReference createHostProfile(VimPortType vimPort, ManagedObjectReference hostEntityMoRef)
            throws DuplicateNameFaultMsg, RuntimeFaultFaultMsg {
        HostProfileHostBasedConfigSpec hostProfileHostBasedConfigSpec = new HostProfileHostBasedConfigSpec();
        hostProfileHostBasedConfigSpec.setHost(hostEntityMoRef);
        hostProfileHostBasedConfigSpec.setAnnotation("SDK Sample Host Profile");
        hostProfileHostBasedConfigSpec.setEnabled(true);
        hostProfileHostBasedConfigSpec.setName("SDK Profile " + sourceHostName + " " + new java.util.Date().getTime());
        hostProfileHostBasedConfigSpec.setUseHostProfileEngine(true);

        log.info("===== Creating Host Profile:");
        log.info("Host : {}", hostEntityMoRef.getValue());

        ManagedObjectReference hostProfile = vimPort.createProfile(hostprofileManager, hostProfileHostBasedConfigSpec);

        // Changed from get_value to getValue
        log.info("Profile : {}", hostProfile.getValue());
        return hostProfile;
    }

    /**
     * Associate a profile with a managed entity. The created hostProfile is attached to a hostEntityMoref
     * (ATTACH_HOST_ENTITY_NAME). We attach only one host to the host profile.
     */
    private static void attachProfileWithManagedEntity(
            VimPortType vimPort, ManagedObjectReference hostProfile, List<ManagedObjectReference> attachEntityMoRefs)
            throws RuntimeFaultFaultMsg {
        log.info("===== Associating Host Profile:");
        vimPort.associateProfile(hostProfile, attachEntityMoRefs);
        log.info(
                "Associated {} with {}",
                hostProfile.getValue(),
                attachEntityMoRefs.get(0).getValue());
    }

    /**
     * Get the profile(s) to which this entity is associated. The list of profiles will only include profiles known to
     * this profileManager.
     */
    private static void printProfilesAssociatedWithEntity(VimPortType vimPort, ManagedObjectReference attachMoRef)
            throws RuntimeFaultFaultMsg {
        System.out.println("------------------------------------");
        System.out.println("* Finding Associated Profiles with Host");
        System.out.println("------------------------------------");
        System.out.println("Profiles");
        for (ManagedObjectReference profile : vimPort.findAssociatedProfile(hostprofileManager, attachMoRef)) {
            System.out.println("    " + profile.getValue());
        }
    }

    /** Update the reference host in use by the HostProfile. */
    private static void updateReferenceHost(
            VimPortType vimPort, ManagedObjectReference hostProfile, ManagedObjectReference attachHostMoRef)
            throws RuntimeFaultFaultMsg {
        log.info("===== Updating Reference Host for the Profile:");
        vimPort.updateReferenceHost(hostProfile, attachHostMoRef);
        log.info("Updated Host Profile : {} Reference to {}", hostProfile.getValue(), attachHostMoRef.getValue());
    }

    /** Execute the Profile Engine to calculate the list of configuration changes needed for the host. */
    private static HostConfigSpec executeHostProfile(
            VimPortType vimPort, ManagedObjectReference hostProfile, ManagedObjectReference attachHostMoRef)
            throws RuntimeFaultFaultMsg {

        log.info("===== Executing Profile Against Host:");

        ProfileExecuteResult profileExecuteResult = vimPort.executeHostProfile(hostProfile, attachHostMoRef, null);
        log.info("Status : {}", profileExecuteResult.getStatus());

        if (profileExecuteResult.getStatus().equals("success")) {
            log.info("Valid HostConfigSpec representing Configuration changes to be made on host");
            return profileExecuteResult.getConfigSpec();
        }

        if (profileExecuteResult.getStatus().equals("error")) {
            log.error("List of Errors:");

            for (ProfileExecuteError profileExecuteError : profileExecuteResult.getError()) {
                log.error("Message: {}", profileExecuteError.getMessage().getMessage());
            }
            return null;
        }
        return null;
    }

    /** Generate a list of configuration tasks that will be performed on the host during HostProfile application. */
    private static void configurationTasksToBeAppliedOnHost(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            HostConfigSpec hostConfigSpec,
            ManagedObjectReference attachHostMoRef)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg {

        log.info("===== Config Tasks on the Host during HostProfile Application:");

        ManagedObjectReference taskMoRef =
                vimPort.generateHostProfileTaskListTask(hostprofileManager, hostConfigSpec, attachHostMoRef);
        if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
            log.info("Done....");

            HostProfileManagerConfigTaskList hostProfileManagerConfigTaskList =
                    propertyCollectorHelper.fetch(taskMoRef, "info.result");

            List<LocalizableMessage> taskMessages = hostProfileManagerConfigTaskList.getTaskDescription();
            if (taskMessages != null && !(taskMessages.isEmpty())) {
                for (LocalizableMessage taskMessage : taskMessages) {
                    log.info("Message : {}", taskMessage.getMessage());
                }
            } else {
                log.info("There are no configuration changes to be made");
            }

        } else {
            log.error("Operation Failed");
        }
    }

    /**
     * Checking for the compliance status and results. If compliance is "nonCompliant", it lists all the compliance
     * failures.
     */
    private static boolean complianceStatusAndResults(Object result) {
        List<ComplianceResult> complianceResults = ((ArrayOfComplianceResult) result).getComplianceResult();

        for (ComplianceResult complianceResult : complianceResults) {
            System.out.println("Host : " + complianceResult.getEntity().getValue());
            System.out.println("Profile : " + complianceResult.getProfile().getValue());
            System.out.println("Compliance Status : " + complianceResult.getComplianceStatus());

            if (complianceResult.getComplianceStatus().equals("nonCompliant")) {
                System.out.println("Compliance Failure Reason");
                for (ComplianceFailure complianceFailure : complianceResult.getFailure()) {
                    System.out.println(" " + complianceFailure.getMessage().getMessage());
                }
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    /** Check compliance of an entity against a Profile. */
    private static boolean checkProfileCompliance(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            ManagedObjectReference profiles,
            ManagedObjectReference entities)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg {
        System.out.println("---------------------------------------------");
        System.out.println("* Checking Complaince of Entity against Profile");
        System.out.println("---------------------------------------------");

        List<ManagedObjectReference> profileList = new ArrayList<>();
        List<ManagedObjectReference> entityList = new ArrayList<>();
        profileList.add(profiles);
        entityList.add(entities);

        ManagedObjectReference checkComplianceTaskMoRef =
                vimPort.checkComplianceTask(profilecomplianceManager, profileList, entityList);
        if (propertyCollectorHelper.awaitTaskCompletion(checkComplianceTaskMoRef)) {
            System.out.print("Entity is in Compliance against Profile.");
        } else {
            throw new RuntimeException("Could not check the compliance of the profile with the given entity");
        }

        Object result = propertyCollectorHelper.fetch(checkComplianceTaskMoRef, "info.result");

        return complianceStatusAndResults(result);
    }

    /** Setting the host to maintenance mode and apply the configuration to the host. */
    private static void applyConfigurationToHost(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            ManagedObjectReference attachHostMoRef,
            HostConfigSpec hostConfigSpec)
            throws RuntimeFaultFaultMsg, InvalidStateFaultMsg, TimedoutFaultMsg, InvalidPropertyFaultMsg,
                    InvalidCollectorVersionFaultMsg, HostConfigFailedFaultMsg {
        log.info("====== Applying Configuration changes or HostProfile to Host");
        log.info("Putting Host in Maintenance Mode");

        suspendPoweredOnGuestVMs(vimPort, propertyCollectorHelper, attachHostMoRef);

        ManagedObjectReference mainModeTaskMoRef = vimPort.enterMaintenanceModeTask(attachHostMoRef, 0, null, null);

        Object[] result = propertyCollectorHelper.awaitManagedObjectUpdates(
                mainModeTaskMoRef, new String[] {"info.state", "info.error"}, new String[] {"state"}, new Object[][] {
                    new Object[] {TaskInfoState.SUCCESS, TaskInfoState.ERROR}
                });

        if (result[0].equals(TaskInfoState.SUCCESS)) {
            log.info("Success: Entered Maintenance Mode");
        } else {
            String msg = "Failure: Entering Maintenance Mode "
                    + "Check and/or Configure Host Maintenance Mode Settings. "
                    + "Check that all Virtual Machines on this host are either suspended or powered off.";

            powerOnSuspendedGuestVMs(vimPort, propertyCollectorHelper);
            throw new RuntimeException(msg);
        }
        log.info("Applying Profile to Host");
        ManagedObjectReference applyHostConfTaskMoRef =
                vimPort.applyHostConfigTask(hostprofileManager, attachHostMoRef, hostConfigSpec, null);

        Object[] taskResult = propertyCollectorHelper.awaitManagedObjectUpdates(
                applyHostConfTaskMoRef,
                new String[] {"info.state", "info.error"},
                new String[] {"state"},
                new Object[][] {new Object[] {TaskInfoState.SUCCESS, TaskInfoState.ERROR}});

        if (taskResult[0].equals(TaskInfoState.SUCCESS)) {
            log.info("Success: Apply Configuration to Host");
        } else {
            exitMaintenanceMode(vimPort, propertyCollectorHelper, attachHostMoRef);

            throw new RuntimeException("Failure: Apply configuration to Host");
        }

        exitMaintenanceMode(vimPort, propertyCollectorHelper, attachHostMoRef);
    }

    private static void exitMaintenanceMode(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            ManagedObjectReference attachHostMoRef)
            throws InvalidStateFaultMsg, RuntimeFaultFaultMsg, TimedoutFaultMsg, InvalidPropertyFaultMsg,
                    InvalidCollectorVersionFaultMsg {
        ManagedObjectReference mainModeExitTaskMoRef = vimPort.exitMaintenanceModeTask(attachHostMoRef, 0);

        Object[] results = propertyCollectorHelper.awaitManagedObjectUpdates(
                mainModeExitTaskMoRef,
                new String[] {"info.state", "info.error"},
                new String[] {"state"},
                new Object[][] {new Object[] {TaskInfoState.SUCCESS, TaskInfoState.ERROR}});

        if (results[0].equals(TaskInfoState.SUCCESS)) {
            powerOnSuspendedGuestVMs(vimPort, propertyCollectorHelper);
        } else {
            throw new RuntimeException("Failure exiting maintenance mode.");
        }
    }

    private static void powerOnSuspendedGuestVMs(VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper) {
        for (ManagedObjectReference vmMoRef : suspendedVMList) {
            log.info("Powering ON virtual machine : [{}]", vmMoRef.getValue());
            try {
                ManagedObjectReference taskMoRef = vimPort.powerOnVMTask(vmMoRef, null);
                if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                    log.info("[{}] powered on successfully", vmMoRef.getValue());
                }
            } catch (Exception e) {
                log.error("Unable to power on vm : [{}]", vmMoRef.getValue());
                log.error("Exception:", e);
            }
        }
    }

    private static void suspendPoweredOnGuestVMs(
            VimPortType vimPort,
            PropertyCollectorHelper propertyCollectorHelper,
            ManagedObjectReference attachHostMoRef)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        Map<ManagedObjectReference, Map<String, Object>> vms = propertyCollectorHelper.getObjectProperties(
                attachHostMoRef, VIRTUAL_MACHINE, "name", "runtime.powerState");

        suspendedVMList = new ArrayList<>();
        for (Map.Entry<ManagedObjectReference, Map<String, Object>> entry : vms.entrySet()) {
            ManagedObjectReference vmMoRef = entry.getKey();

            Map<String, Object> vmProp = entry.getValue();

            VirtualMachinePowerState vmPowerState = (VirtualMachinePowerState) vmProp.get("runtime.powerState");

            String vmName = (String) vmProp.get("name");
            if ((vmPowerState.equals(VirtualMachinePowerState.POWERED_ON))) {
                try {
                    ManagedObjectReference taskMoRef = vimPort.suspendVMTask(vmMoRef);
                    if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                        log.info("[{}] suspended successfully: {}", vmName, vmMoRef.getValue());
                        suspendedVMList.add(vmMoRef);
                    }

                } catch (Exception e) {
                    log.error("Unable to suspend vm : {}[{}] / {}", vmName, vmMoRef.getValue(), vmMoRef);
                    log.error("Exception:", e);
                }
            }
        }
    }

    private static void deleteHostProfile(VimPortType vimPort, ManagedObjectReference hostProfile)
            throws RuntimeFaultFaultMsg {
        log.info("===== Deleting Profile");

        vimPort.destroyProfile(hostProfile);
        log.info("Profile : {}", hostProfile.getValue());
    }

    /** Detach a profile from a managed entity. */
    private static void detachHostFromProfile(
            VimPortType vimPort, ManagedObjectReference hostProfile, ManagedObjectReference entity)
            throws RuntimeFaultFaultMsg {
        log.info("====== Detach Host From Profile");

        List<ManagedObjectReference> entityList = new ArrayList<>();
        entityList.add(entity);

        vimPort.dissociateProfile(hostProfile, entityList);
        log.info("Detached Host : {} From Profile : {}", entityList.get(0).getValue(), hostProfile.getValue());
    }
}
