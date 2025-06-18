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
import static com.vmware.sdk.vsphere.utils.VimClient.getVimServiceInstanceRef;
import static com.vmware.vim25.ManagedObjectType.HOST_SYSTEM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayOfPerfCounterInfo;
import com.vmware.vim25.ArrayOfPerfInterval;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfEntityMetric;
import com.vmware.vim25.PerfEntityMetricBase;
import com.vmware.vim25.PerfInterval;
import com.vmware.vim25.PerfMetricId;
import com.vmware.vim25.PerfMetricIntSeries;
import com.vmware.vim25.PerfMetricSeries;
import com.vmware.vim25.PerfQuerySpec;
import com.vmware.vim25.PerfSampleInfo;
import com.vmware.vim25.PerfStatsType;
import com.vmware.vim25.PerfSummaryType;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/** This sample reads performance measurements from the current time. */
public class History {
    private static final Logger log = LoggerFactory.getLogger(History.class);
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
    public static String hostname = "hostname";
    /** REQUIRED: Sampling interval in seconds. */
    public static String interval = "interval";
    /** OPTIONAL: In minutes, to specify what's start time from which samples needs to be collected. */
    public static Integer startTime = null;
    /** OPTIONAL: Duration for which samples needs to be taken. */
    public static Integer duration = null;
    /** REQUIRED: [cpu|mem]. */
    public static String groupName = "groupName";
    /** REQUIRED: usage (for cpu and mem), overhead (for mem). */
    public static String counterName = "counterName";

    private static ManagedObjectReference perfManager;
    private static final Map<String, Map<String, ArrayList<PerfCounterInfo>>> pci = new HashMap<>();

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(History.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            perfManager = serviceContent.getPerfManager();

            displayHistory(
                    vimPort,
                    propertyCollectorHelper,
                    startTime != null ? startTime : 0,
                    duration != null ? duration : 0);
        }
    }

    /**
     * This method initializes all the performance counters available on the system it is connected to. The performance
     * counters are stored in the hashmap counters with group.counter.rolluptype being the key and id being the value.
     */
    private static List<PerfInterval> getPerfInterval(PropertyCollectorHelper propertyCollectorHelper)
            throws Exception {
        ArrayOfPerfInterval perfInterval = propertyCollectorHelper.fetch(perfManager, "historicalInterval");
        return perfInterval.getPerfInterval();
    }

    private static void displayHistory(
            VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper, int startTime, int duration)
            throws Exception {

        if (duration <= 0) {
            duration = 20;
        }
        if (startTime <= 0) {
            startTime = 20;
        }
        if (duration > startTime) {
            throw new RuntimeException("Duration must be less than startime");
        }

        ManagedObjectReference hostMoRef = propertyCollectorHelper.getMoRefByName(hostname, HOST_SYSTEM);
        if (hostMoRef == null) {
            log.error("Host {} not found", hostname);
            return;
        }

        counterInfo(propertyCollectorHelper);
        List<PerfInterval> intervals = getPerfInterval(propertyCollectorHelper);

        boolean valid = checkInterval(intervals, Integer.valueOf(interval));
        if (!valid) {
            log.error("Invalid interval, Specify one from above");
            return;
        }

        PerfCounterInfo perfCounterInfo = getCounterInfo(groupName, counterName, PerfSummaryType.AVERAGE, null);
        if (perfCounterInfo == null) {
            log.error("Incorrect Group Name and Countername specified");
            return;
        }

        XMLGregorianCalendar serverStartTime = vimPort.currentTime(getVimServiceInstanceRef());
        XMLGregorianCalendar serverEndTime = vimPort.currentTime(getVimServiceInstanceRef());

        int minsToAddEnd = duration - (2 * startTime);
        int minsToAddStart = duration - ((2 * startTime) + duration);

        int setTime;
        if (minsToAddStart < 0) {
            setTime = serverStartTime.getMinute() + (60 + minsToAddStart);
            if (setTime >= 60) {
                setTime = setTime - 60;
                serverStartTime.setMinute(setTime);
            } else {
                serverStartTime.setHour(serverStartTime.getHour() - 1);
                serverStartTime.setMinute(setTime);
            }
        } else {
            serverStartTime.setMinute(serverStartTime.getMinute() + (duration - ((2 * startTime) + duration)));
        }
        if (minsToAddEnd < 0) {
            setTime = serverEndTime.getMinute() + (60 + minsToAddEnd);
            if (setTime >= 60) {
                setTime = setTime - 60;
                serverEndTime.setMinute(setTime);
            } else {
                serverEndTime.setHour(serverEndTime.getHour() - 1);
                serverEndTime.setMinute(setTime);
            }
        } else {
            serverEndTime.setMinute(serverEndTime.getMinute() + (duration - (2 * startTime)));
        }

        log.info("Start Time {}", serverStartTime.toGregorianCalendar().getTime());
        log.info("End Time {}", serverEndTime.toGregorianCalendar().getTime());

        List<PerfMetricId> perfMetricIds = vimPort.queryAvailablePerfMetric(
                perfManager, hostMoRef, serverStartTime, serverEndTime, Integer.valueOf(interval));

        PerfMetricId ourCounter = null;

        for (PerfMetricId perfMetricId : perfMetricIds) {
            if (perfMetricId.getCounterId() == perfCounterInfo.getKey()) {
                ourCounter = perfMetricId;
                break;
            }
        }

        if (ourCounter == null) {
            log.info("No data on Host to collect. Has it been running for at least {} minutes", duration);
        } else {
            PerfQuerySpec qSpec = new PerfQuerySpec();
            qSpec.setEntity(hostMoRef);
            qSpec.setStartTime(serverStartTime);
            qSpec.setEndTime(serverEndTime);
            qSpec.getMetricId().addAll(List.of(ourCounter));
            qSpec.setIntervalId(Integer.valueOf(interval));

            List<PerfQuerySpec> perfQuerySpecs = new ArrayList<>(1);
            perfQuerySpecs.add(qSpec);
            List<PerfEntityMetricBase> perfEntityMetricBases = vimPort.queryPerf(perfManager, perfQuerySpecs);

            if (perfEntityMetricBases != null) {
                displayValues(perfEntityMetricBases, perfCounterInfo, ourCounter, Integer.valueOf(interval));
            } else {
                log.info("No Samples Found");
            }
        }
    }

    private static boolean checkInterval(List<PerfInterval> intervals, Integer interv) {
        boolean flag = false;
        for (PerfInterval pi : intervals) {
            if (pi.getSamplingPeriod() == interv) {
                flag = true;
                break;
            }
        }
        if (!flag) {
            log.info("Available summary collection intervals");
            log.info("Period\tLength\tName");
            for (PerfInterval pi : intervals) {
                log.info("Period: {}, Length: {}, Name: {}", pi.getSamplingPeriod(), pi.getLength(), pi.getName());
            }
        }
        return flag;
    }

    /**
     * This method initializes all the performance counters available on the system it is connected to. The performance
     * counters are stored in the hashmap counters with group.counter.rolluptype being the key and id being the value.
     */
    private static List<PerfCounterInfo> getPerfCounters(PropertyCollectorHelper propertyCollectorHelper)
            throws Exception {
        ArrayOfPerfCounterInfo perfCounterInfo = propertyCollectorHelper.fetch(perfManager, "perfCounter");
        return perfCounterInfo.getPerfCounterInfo();
    }

    private static void counterInfo(PropertyCollectorHelper propertyCollectorHelper) throws Exception {
        List<PerfCounterInfo> perfCounterInfos = getPerfCounters(propertyCollectorHelper);
        for (PerfCounterInfo counterInfo : perfCounterInfos) {

            String group = counterInfo.getGroupInfo().getKey();

            Map<String, ArrayList<PerfCounterInfo>> nameMap = null;
            if (!pci.containsKey(group)) {
                nameMap = new HashMap<>();
                pci.put(group, nameMap);
            } else {
                nameMap = pci.get(group);
            }

            String name = counterInfo.getNameInfo().getKey();
            ArrayList<PerfCounterInfo> counters = null;
            if (!nameMap.containsKey(name)) {
                counters = new ArrayList<>();
                nameMap.put(name, counters);
            } else {
                counters = nameMap.get(name);
            }
            counters.add(counterInfo);
        }
    }

    private static ArrayList<PerfCounterInfo> getCounterInfos(String groupName, String counterName) {
        Map<String, ArrayList<PerfCounterInfo>> nameMap = pci.get(groupName);
        if (nameMap != null) {
            return nameMap.get(counterName);
        }
        return null;
    }

    private static PerfCounterInfo getCounterInfo(
            String groupName, String counterName, PerfSummaryType rollupType, PerfStatsType statsType) {
        ArrayList<PerfCounterInfo> counters = getCounterInfos(groupName, counterName);
        if (counters != null) {
            for (PerfCounterInfo pci : counters) {
                if ((statsType == null || statsType.equals(pci.getStatsType()))
                        && (rollupType == null || rollupType.equals(pci.getRollupType()))) {
                    return pci;
                }
            }
        }
        return null;
    }

    private static void displayValues(
            List<PerfEntityMetricBase> values, PerfCounterInfo pci, PerfMetricId perfMetricId, Integer inter) {
        for (PerfEntityMetricBase value : values) {
            List<PerfMetricSeries> metricSeries = ((PerfEntityMetric) value).getValue();
            List<PerfSampleInfo> perfSampleInfos = ((PerfEntityMetric) value).getSampleInfo();

            if (perfSampleInfos == null || perfSampleInfos.isEmpty()) {
                log.info("No Samples available. Continuing.");
                continue;
            }
            log.info(
                    "Sample time range: {} - {}, read every {} seconds",
                    perfSampleInfos.get(0).getTimestamp().toGregorianCalendar().getTime(),
                    (perfSampleInfos.get(perfSampleInfos.size() - 1))
                            .getTimestamp()
                            .toGregorianCalendar()
                            .getTime(),
                    inter);

            for (PerfMetricSeries perfMetricSeries : metricSeries) {
                if (pci != null) {
                    if (pci.getKey() != perfMetricSeries.getId().getCounterId()) {
                        continue;
                    }
                    log.info("{} - Instance: {}", pci.getNameInfo().getSummary(), perfMetricId.getInstance());
                }

                if (perfMetricSeries instanceof PerfMetricIntSeries) {
                    PerfMetricIntSeries val = (PerfMetricIntSeries) perfMetricSeries;
                    List<Long> listlongs = val.getValue();
                    for (int j = 0; j < listlongs.size(); j++) {
                        log.info(
                                "timestamp: {}\tvalue: {}",
                                perfSampleInfos
                                        .get(j)
                                        .getTimestamp()
                                        .toGregorianCalendar()
                                        .getTime(),
                                listlongs.get(j));
                    }
                }
            }
        }
    }
}
