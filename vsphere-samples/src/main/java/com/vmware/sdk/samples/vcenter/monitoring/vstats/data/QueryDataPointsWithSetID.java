/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.monitoring.vstats.data;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.VM;
import com.vmware.vcenter.VMTypes;
import com.vmware.vstats.AcqSpecs;
import com.vmware.vstats.AcqSpecsTypes;
import com.vmware.vstats.CounterSets;
import com.vmware.vstats.CounterSetsTypes;
import com.vmware.vstats.Data;
import com.vmware.vstats.DataTypes;
import com.vmware.vstats.RsrcId;

/**
 * Demonstrates creation of Acquisition Specification using Counter SetId and query for data points filtered by
 * resource.
 */
public class QueryDataPointsWithSetID {
    private static final Logger log = LoggerFactory.getLogger(QueryDataPointsWithSetID.class);
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

    private static final int WAIT_TIME = 30;
    private static final String VM_TYPE = "VM";
    private static final String MEMO = "user-definition of Acquisition " + "Specification";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(QueryDataPointsWithSetID.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            AcqSpecs acqSpecService = client.createStub(AcqSpecs.class);
            Data dataService = client.createStub(Data.class);
            VM vmService = client.createStub(VM.class);
            CounterSets counterSetsService = client.createStub(CounterSets.class);

            AcqSpecsTypes.CreateSpec createSpec = new AcqSpecsTypes.CreateSpec();
            AcqSpecsTypes.CounterSpec counterSpec = new AcqSpecsTypes.CounterSpec();
            List<RsrcId> resourceSpec = new ArrayList<>();

            // Get Counter-set ID of VM counters which is provided as setId.
            List<CounterSetsTypes.Info> counterSets = counterSetsService.list();
            String[] parts;
            String setId = null;
            for (CounterSetsTypes.Info counterSet : counterSets) {
                parts = counterSet.getCounters().get(0).getCid().split("\\.");
                if (parts[parts.length - 1].equals(VM_TYPE)) {
                    setId = counterSet.getId();
                    break;
                }
            }
            counterSpec.setSetId(setId);
            createSpec.setCounters(counterSpec);

            // Choose a random VM from which stats data needs to be collected.
            VMTypes.FilterSpec.Builder builderSpec = new VMTypes.FilterSpec.Builder();
            List<VMTypes.Summary> vmList = vmService.list(builderSpec.build());

            RsrcId vmRsrcId = new RsrcId();
            int randomIndex = (int) (Math.random() * vmList.size());
            vmRsrcId.setIdValue(vmList.get(randomIndex).getVm());
            vmRsrcId.setType(VM_TYPE);

            resourceSpec.add(vmRsrcId);
            createSpec.setResources(resourceSpec);

            // Choose an interval and expiration.
            createSpec.setInterval(interval);
            createSpec.setExpiration(expiration);
            createSpec.setMemo_(MEMO);

            // Create an Acquisition Specification for all the VM counters using "setId" in CreateSpec.
            String acqSpecId = acqSpecService.create(createSpec);
            log.info(
                    "Acquisition Specification created is \n{}\nwith id: {}", acqSpecService.get(acqSpecId), acqSpecId);

            // Wait for 30 seconds for data collection to start.
            TimeUnit.SECONDS.sleep(WAIT_TIME);

            // Query for data points filtered by resource.
            DataTypes.FilterSpec filterSpec = new DataTypes.FilterSpec();
            List<String> resourceFilter = new ArrayList<>();

            String resource = "type." + VM_TYPE + "=" + vmRsrcId.getIdValue();
            resourceFilter.add(resource);
            filterSpec.setResources(resourceFilter);

            DataTypes.DataPointsResult dataPoints = dataService.queryDataPoints(filterSpec);
            log.info("Datapoints collected: \n{}", dataPoints);

            // cleanup
            // Delete the Acquisition Specification.
            acqSpecService.delete(acqSpecId);
            log.info("Cleanup: The Acquisition Specification with id: {} is deleted", acqSpecId);
        }
    }
}
