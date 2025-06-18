/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.monitoring.vstats.acqspecs;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vstats.AcqSpecs;
import com.vmware.vstats.AcqSpecsTypes;
import com.vmware.vstats.CidMid;
import com.vmware.vstats.Providers;
import com.vmware.vstats.ProvidersTypes;
import com.vmware.vstats.RsrcId;

/** Demonstrates create, get, list, update and delete operations of Acquisition Specifications. */
public class LifeCycle {
    private static final Logger log = LoggerFactory.getLogger(LifeCycle.class);
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

    /** REQUIRED: Create an Acquisition Specification to collect data with interval. */
    public static long interval = 10;
    /** REQUIRED: Create an Acquisition Specification that expires within expiration time. */
    public static long expiration = 30;

    private static final String CID = "cpu.capacity.demand.HOST";
    private static final String NEW_COUNTER_ID = "mem.capacity.usage.HOST";
    private static final String HOST_TYPE = "HOST";
    private static final String MEMO = "user-definition of Acquisition " + "Specification";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(LifeCycle.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Providers providerService = client.createStub(Providers.class);
            AcqSpecs acqSpecService = client.createStub(AcqSpecs.class);

            AcqSpecsTypes.CreateSpec createSpec = new AcqSpecsTypes.CreateSpec();
            AcqSpecsTypes.CounterSpec counterSpec = new AcqSpecsTypes.CounterSpec();
            CidMid cidMid = new CidMid();

            // The counter and associated resources can be chosen using discovery
            // APIs. Please refer to samples in discovery package to obtain this
            // metadata. In this sample, we create an Acquisition Specification
            // for a HOST counter.

            cidMid.setCid(CID);
            counterSpec.setCidMid(cidMid);
            createSpec.setCounters(counterSpec);

            // Choose a random host from which stats data needs to be collected.
            List<ProvidersTypes.Summary> providersList = providerService.list();

            int randomIndex = (int) (Math.random() * providersList.size());

            String hostIdValue = providersList.get(randomIndex).getId();
            RsrcId hostRsrcId = new RsrcId();
            hostRsrcId.setIdValue(hostIdValue);
            hostRsrcId.setType(HOST_TYPE);

            List<RsrcId> resourceSpec = new ArrayList<>();
            resourceSpec.add(hostRsrcId);
            createSpec.setResources(resourceSpec);

            // Choose an interval and expiration.
            createSpec.setInterval(interval);
            createSpec.setExpiration(expiration);
            createSpec.setMemo_(MEMO);

            // Create an Acquisition Specification.
            String acqSpecId = acqSpecService.create(createSpec);
            log.info(
                    "----- Acquisition Specification created is \n{}\nwith id: {}",
                    acqSpecService.get(acqSpecId),
                    acqSpecId);

            // List Acquisition Specifications.
            AcqSpecsTypes.ListResult acqSpecsList = acqSpecService.list(null);
            log.info("----- List of Acquisition Specifications: \n{}", acqSpecsList);

            // Update the existing Acquisition Specification by only modifying the
            // intended field in UpdateSpec, keeping all other fields as it is.
            AcqSpecsTypes.UpdateSpec updateSpec = new AcqSpecsTypes.UpdateSpec();
            updateSpec.setResources(createSpec.getResources());
            updateSpec.setCounters(createSpec.getCounters());
            updateSpec.setInterval(createSpec.getInterval());
            updateSpec.setExpiration(createSpec.getExpiration());
            updateSpec.setMemo_(createSpec.getMemo_());

            // Update the cid field in the already created Acquisition Specification previously.
            AcqSpecsTypes.CounterSpec updatedCounterSpec = new AcqSpecsTypes.CounterSpec();
            CidMid updatedCidMid = new CidMid();
            updatedCidMid.setCid(NEW_COUNTER_ID);
            updatedCounterSpec.setCidMid(updatedCidMid);
            updateSpec.setCounters(updatedCounterSpec);

            acqSpecService.update(acqSpecId, updateSpec);
            log.info("----- The updated Acquisition Specification is: \n{}", acqSpecService.get(acqSpecId));

            // Delete the Acquisition Specification.
            acqSpecService.delete(acqSpecId);
            log.info("----- The Acquisition Specification with id: {} is deleted", acqSpecId);
        }
    }
}
