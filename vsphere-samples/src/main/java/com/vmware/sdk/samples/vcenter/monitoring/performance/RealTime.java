/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.monitoring.performance;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayOfPerfCounterInfo;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfEntityMetric;
import com.vmware.vim25.PerfEntityMetricBase;
import com.vmware.vim25.PerfMetricId;
import com.vmware.vim25.PerfMetricIntSeries;
import com.vmware.vim25.PerfMetricSeries;
import com.vmware.vim25.PerfQuerySpec;
import com.vmware.vim25.PerfSampleInfo;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/** This sample displays performance measurements from the current time at the console. */
public class RealTime {
    private static final Logger log = LoggerFactory.getLogger(RealTime.class);
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

    /** REQUIRED: Name of the virtual machine. */
    public static String vmName = "vmName";
    /**
     * OPTIONAL: Counter types to check. All cpu counters if not present. Example values: swapwait#0, swapwait#1,
     * swapwait, latency, usage, usage.vcpus, readiness, usagemhz, usagemhz, entitlement, demand, ready, costop,
     * maxlimited, used, run, idle, swapwait, demandEntitlementRatio.
     */
    public static String[] counterTypes = null;
    /** OPTIONAL: Number of poll iterations. Default value is 2. */
    public static Integer iterations = null;
    /** OPTIONAL: Pause before each poll in seconds. Default value is 1. */
    public static Integer pauseInSeconds = null;

    private static ManagedObjectReference perfManager;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(RealTime.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            perfManager = serviceContent.getPerfManager();

            ManagedObjectReference vmMoRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);

            if (vmMoRef != null) {
                List<PerfCounterInfo> counterInfos = getPerfCounters(propertyCollectorHelper);
                Map<Integer, PerfCounterInfo> counters = new HashMap<>();
                Set<String> keys = new LinkedHashSet<>();

                if (counterTypes != null) {
                    Collections.addAll(keys, counterTypes);
                }

                for (PerfCounterInfo perfCounterInfo : counterInfos) {
                    if (keys.isEmpty()
                            || keys.contains(perfCounterInfo.getNameInfo().getKey())) {
                        counters.put(perfCounterInfo.getKey(), perfCounterInfo);
                        log.info(
                                "Counter: {} - {} ",
                                perfCounterInfo.getNameInfo().getKey(),
                                perfCounterInfo.getNameInfo().getSummary());
                    }
                }
                List<PerfMetricId> perfMetricIds =
                        vimPort.queryAvailablePerfMetric(perfManager, vmMoRef, null, null, 20);

                List<PerfMetricId> mMetrics = new ArrayList<>();
                if (perfMetricIds != null && !perfMetricIds.isEmpty()) {
                    for (PerfMetricId perfMetricId : perfMetricIds) {
                        if (counters.containsKey(perfMetricId.getCounterId())) {
                            mMetrics.add(perfMetricId);
                        }
                    }
                    monitorPerformance(vimPort, perfManager, vmMoRef, mMetrics, counters);
                } else {
                    log.warn("CPU performance counters disabled for VM {}", vmName);
                    mMetrics.addAll(perfMetricIds);
                }
            } else {
                log.error("Virtual Machine {} not found", vmName);
            }
        }
    }

    private static void displayValues(List<PerfEntityMetricBase> values, Map<Integer, PerfCounterInfo> counters) {
        for (PerfEntityMetricBase value : values) {
            List<PerfMetricSeries> perfMetricSeries = ((PerfEntityMetric) value).getValue();
            List<PerfSampleInfo> perfSampleInfos = ((PerfEntityMetric) value).getSampleInfo();

            System.out.println("Sample time range: "
                    + perfSampleInfos.get(0).getTimestamp().toString() + " - "
                    + perfSampleInfos
                            .get(perfSampleInfos.size() - 1)
                            .getTimestamp()
                            .toString());
            for (PerfMetricSeries metricSeries : perfMetricSeries) {
                PerfCounterInfo pci = counters.get(metricSeries.getId().getCounterId());
                if (pci != null) {
                    System.out.println(pci.getNameInfo().getSummary());
                }

                if (metricSeries instanceof PerfMetricIntSeries) {
                    PerfMetricIntSeries val = (PerfMetricIntSeries) metricSeries;
                    List<Long> longList = val.getValue();
                    for (Long k : longList) {
                        System.out.print(k + " ");
                    }
                    System.out.println();
                }
            }
        }
    }

    /**
     * This method initializes all the performance counters available on the system it is connected to. The performance
     * counters are stored in the hashmap counters with group.counter.rolluptype being the key and id being the value.
     */
    private static List<PerfCounterInfo> getPerfCounters(PropertyCollectorHelper propertyCollectorHelper)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        ArrayOfPerfCounterInfo perfCounterInfo = propertyCollectorHelper.fetch(perfManager, "perfCounter");
        return perfCounterInfo.getPerfCounterInfo();
    }

    private static void monitorPerformance(
            VimPortType vimPort,
            ManagedObjectReference perfManagerMoRef,
            ManagedObjectReference vmMoRef,
            List<PerfMetricId> mMetrics,
            Map<Integer, PerfCounterInfo> counters)
            throws RuntimeFaultFaultMsg, InterruptedException {
        PerfQuerySpec perfQuerySpec = new PerfQuerySpec();
        perfQuerySpec.setEntity(vmMoRef);
        perfQuerySpec.setMaxSample(10);
        perfQuerySpec.getMetricId().addAll(mMetrics);
        perfQuerySpec.setIntervalId(20);

        List<PerfQuerySpec> qSpecs = new ArrayList<>();
        qSpecs.add(perfQuerySpec);

        int iterationsRemaining = iterations != null ? iterations : 1;
        long pauseMillis = pauseInSeconds != null ? pauseInSeconds * 1000 : 1000;
        while (iterationsRemaining-- > 0) {
            List<PerfEntityMetricBase> pValues = vimPort.queryPerf(perfManagerMoRef, qSpecs);
            if (pValues != null) {
                displayValues(pValues, counters);
            }
            if (iterationsRemaining > 0) {
                log.info("Sleeping {} seconds...", pauseMillis / 1000);
                Thread.sleep(pauseMillis);
            }
        }
    }
}
