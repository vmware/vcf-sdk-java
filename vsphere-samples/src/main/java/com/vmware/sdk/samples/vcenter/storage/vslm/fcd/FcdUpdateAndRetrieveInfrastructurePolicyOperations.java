/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.storage.vslm.fcd;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.DATASTORE;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.storage.fcd.helpers.FcdHelper;
import com.vmware.sdk.samples.vcenter.storage.vslm.fcd.helpers.FcdVslmHelper;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachineProfileSpec;
import com.vmware.vim25.VslmInfrastructureObjectPolicy;
import com.vmware.vim25.VslmInfrastructureObjectPolicySpec;
import com.vmware.vslm.VslmPortType;

/**
 * This sample updates vStorageObject policy, updates and retrieves virtual storage infrastructure object SBPM policy on
 * given datastore from vslm.
 *
 * <p>Sample Prerequisites:
 *
 * <ul>
 *   <li>Existing VStorageObject ID
 *   <li>Existing SPBM profile ID
 * </ul>
 */
public class FcdUpdateAndRetrieveInfrastructurePolicyOperations {
    private static final Logger log = LoggerFactory.getLogger(FcdUpdateAndRetrieveInfrastructurePolicyOperations.class);

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

    /** REQUIRED: UUID of the vstorageobject. */
    public static String vStorageObjectId = "vStorageObjectId";
    /** REQUIRED: SPBM Profile requirement on the new virtual storage object. */
    public static String pbmProfileId = "pbmProfileId";
    /** REQUIRED: Datastore on which policy needs to be retrieved. It only supports VSAN datastore. */
    public static String datastoreName = "datastoreName";
    /** OPTIONAL: ID of the replication device group. */
    public static String deviceGroupId = null;
    /** OPTIONAL: ID of the fault domain to which the group belongs. */
    public static String faultDomainId = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FcdUpdateAndRetrieveInfrastructurePolicyOperations.class, args);

        VcenterClientFactory clientFactory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = clientFactory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);
            VslmPortType vslmPort = client.getVslmPort();
            FcdVslmHelper vslmHelper = new FcdVslmHelper(vslmPort);

            ManagedObjectReference vStorageObjectManager =
                    client.getVslmServiceInstanceContent().getVStorageObjectManager();

            log.info("Invoking infrastructure object SBPM policy related APIs from VSLM ::");

            List<VirtualMachineProfileSpec> profileSpec = null;
            if (pbmProfileId != null) {
                profileSpec = FcdHelper.generateVirtualMachineProfileSpec(pbmProfileId, deviceGroupId, faultDomainId);
            }

            // update VStorageObject Policy
            log.info("Operation: Updating virtual storage object policy from vslm :");
            ManagedObjectReference updateVStorageObjPolicyTask = vslmPort.vslmUpdateVstorageObjectPolicyTask(
                    vStorageObjectManager, FcdHelper.makeId(vStorageObjectId), profileSpec);

            boolean isUpdateVStorageObjPolicySucceeded = vslmHelper.waitForTask(updateVStorageObjPolicyTask);
            if (isUpdateVStorageObjPolicySucceeded) {
                log.info(
                        "Success : vStorage object : [ Name = {} ] updated with profile : [ ProfileId = {} ].\n",
                        vStorageObjectId,
                        pbmProfileId);
            } else {
                log.error(
                        "Error : Failed to update vStorage object : [ Name = {} ] with profile : [ ProfileId = {} ].\n",
                        vStorageObjectId,
                        pbmProfileId);
            }

            // update VStorageObject Infrastructure Object Policy
            log.info("Operation: Updating virtual storage infrastructure object SBPM policy on given from vslm.");
            // Get all the input moRefs required for retrieving VStorageObject.
            ManagedObjectReference datastoreMoRef = propertyCollectorHelper.getMoRefByName(datastoreName, DATASTORE);

            VslmInfrastructureObjectPolicySpec spec = new VslmInfrastructureObjectPolicySpec();
            spec.setDatastore(datastoreMoRef);
            spec.getProfile().addAll(profileSpec);

            vslmPort.vslmUpdateVStorageInfrastructureObjectPolicyTask(vStorageObjectManager, spec);
            log.info(
                    "Success: Updated infrastructure policy for datastore : [ datastore = {} ] from vslm.\n",
                    datastoreMoRef.getValue());

            // Retrieve VStorageObject Infrastructure Object Policy
            log.info("Operation: Retrieving virtual storage infrastructure object SBPM policy on given from vslm.");
            List<VslmInfrastructureObjectPolicy> retrieveVStorageObjectInfraPolicyList =
                    vslmPort.vslmRetrieveVStorageInfrastructureObjectPolicy(vStorageObjectManager, datastoreMoRef);

            log.info(
                    "Success: Retrieved infrastructure policy for datastore : [ datastore = {} ] from vslm.\n",
                    datastoreMoRef.getValue());
            for (VslmInfrastructureObjectPolicy infraPolicy : retrieveVStorageObjectInfraPolicyList) {
                log.info(
                        "Infrastructure object : [ Name = {} ] associated with profile : [ ProfileId = {} ].\n",
                        infraPolicy.getName(),
                        infraPolicy.getProfileId());
            }
        }
    }
}
