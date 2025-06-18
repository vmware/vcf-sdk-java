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
import static com.vmware.vim25.ManagedObjectType.HOST_SYSTEM;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayOfPerfCounterInfo;
import com.vmware.vim25.ArrayOfPerfInterval;
import com.vmware.vim25.ElementDescription;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfInterval;
import com.vmware.vim25.PerfMetricId;
import com.vmware.vim25.PerfProviderSummary;
import com.vmware.vim25.PerfSummaryType;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/** This sample displays available performance counters or other data for a requested ESX system. */
public class Basics {
    private static final Logger log = LoggerFactory.getLogger(Basics.class);
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

    /** REQUIRED: Requested info - [interval|counter|host]. */
    public static String info = "info";
    /** OPTIONAL: Required when 'info' is 'host'. */
    public static String hostname = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(Basics.class, args);

        validateTheInput();

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference perfManagerMoRef = serviceContent.getPerfManager();

            if (info.equalsIgnoreCase("interval")) {
                getIntervals(propertyCollectorHelper, perfManagerMoRef);
            } else if (info.equalsIgnoreCase("counter")) {
                getCounters(propertyCollectorHelper, perfManagerMoRef);
            } else if (info.equalsIgnoreCase("host")) {
                ManagedObjectReference hostMoRef = propertyCollectorHelper.getMoRefByName(hostname, HOST_SYSTEM);
                if (hostMoRef == null) {
                    log.error("Host {} not found", hostname);
                    return;
                }

                getQuerySummary(perfManagerMoRef, hostMoRef, vimPort);
                getQueryAvailable(perfManagerMoRef, hostMoRef, vimPort);
            } else {
                log.error("Invalid info argument [host|counter|interval]");
            }
        }
    }

    private static void validateTheInput() {
        if (info.equalsIgnoreCase("host")) {
            if (hostname == null) {
                throw new RuntimeException("Must specify the --hostname" + " parameter when --info is host");
            }
        }
    }

    private static void getIntervals(
            PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference perfManagerMoRef)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        Object property = propertyCollectorHelper.fetch(perfManagerMoRef, "historicalInterval");

        ArrayOfPerfInterval arrayInterval = (ArrayOfPerfInterval) property;

        List<PerfInterval> intervals = arrayInterval.getPerfInterval();
        System.out.println("Performance intervals (" + intervals.size() + "):");
        System.out.println("---------------------");

        int count = 0;
        for (PerfInterval interval : intervals) {
            System.out.print((++count) + ": " + interval.getName());
            System.out.print(" -- period = " + interval.getSamplingPeriod());
            System.out.println(", length = " + interval.getLength());
        }
        System.out.println();
    }

    private static void getCounters(
            PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference perfManagerMoRef)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        Object property = propertyCollectorHelper.fetch(perfManagerMoRef, "perfCounter");

        ArrayOfPerfCounterInfo arrayCounter = (ArrayOfPerfCounterInfo) property;

        List<PerfCounterInfo> counters = arrayCounter.getPerfCounterInfo();
        System.out.println("Performance counters (averages only):");
        System.out.println("-------------------------------------");
        for (PerfCounterInfo counter : counters) {
            if (counter.getRollupType() == PerfSummaryType.AVERAGE) {
                ElementDescription desc = counter.getNameInfo();
                System.out.println(desc.getLabel() + ": " + desc.getSummary());
            }
        }
    }

    private static void getQuerySummary(
            ManagedObjectReference perfManagerMoRef, ManagedObjectReference hostMoRef, VimPortType service)
            throws RuntimeFaultFaultMsg {
        PerfProviderSummary summary = service.queryPerfProviderSummary(perfManagerMoRef, hostMoRef);
        System.out.println("Host perf capabilities:");
        System.out.println("----------------------");
        System.out.println("  Summary supported: " + summary.isSummarySupported());
        System.out.println("  Current supported: " + summary.isCurrentSupported());
        if (summary.isCurrentSupported()) {
            System.out.println("  Current refresh rate: " + summary.getRefreshRate());
        }
    }

    private static void getQueryAvailable(
            ManagedObjectReference perfManagerMoRef, ManagedObjectReference hostMoRef, VimPortType service)
            throws RuntimeFaultFaultMsg {
        PerfProviderSummary perfProviderSummary = service.queryPerfProviderSummary(perfManagerMoRef, hostMoRef);
        List<PerfMetricId> perfMetricIds = service.queryAvailablePerfMetric(
                perfManagerMoRef, hostMoRef, null, null, perfProviderSummary.getRefreshRate());

        List<Integer> idslist = new ArrayList<>();

        for (int i = 0; i != perfMetricIds.size(); ++i) {
            idslist.add(perfMetricIds.get(i).getCounterId());
        }

        List<PerfCounterInfo> perfCounterInfos = service.queryPerfCounter(perfManagerMoRef, idslist);
        System.out.println("Available real-time metrics for host (" + perfMetricIds.size() + "):");
        System.out.println("--------------------------");

        for (int i = 0; i != perfMetricIds.size(); ++i) {
            String label = perfCounterInfos.get(i).getNameInfo().getLabel();
            String instance = perfMetricIds.get(i).getInstance();
            System.out.print("   " + label);
            if (!instance.isEmpty()) {
                System.out.print(" [" + instance + "]");
            }
        }
    }
}
