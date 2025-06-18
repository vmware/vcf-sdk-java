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
import static com.vmware.vim25.ManagedObjectType.RESOURCE_POOL;
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayOfPerfCounterInfo;
import com.vmware.vim25.ElementDescription;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ManagedObjectType;
import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfMetricId;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/** This sample writes available VM, HostSystem or ResourcePool perf counters into the file specified. */
public class PrintCounters {
    private static final Logger log = LoggerFactory.getLogger(PrintCounters.class);
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

    /** REQUIRED: Full path of filename to write to. */
    public static String filename = "filename";
    /** REQUIRED: Name of the managed entity. */
    public static String entityName = "entityName";
    /** REQUIRED: Managed entity [HostSystem|VirtualMachine|ResourcePool]. */
    public static String entityType = "entityType";

    private static ManagedObjectReference perfManager;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(PrintCounters.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            perfManager = serviceContent.getPerfManager();

            String entityType = PrintCounters.entityType;

            if (entityType.equalsIgnoreCase("HostSystem")) {
                printEntityCounters(vimPort, propertyCollectorHelper, HOST_SYSTEM);
            } else if (entityType.equalsIgnoreCase("VirtualMachine")) {
                printEntityCounters(vimPort, propertyCollectorHelper, VIRTUAL_MACHINE);
            } else if (entityType.equals("ResourcePool")) {
                printEntityCounters(vimPort, propertyCollectorHelper, RESOURCE_POOL);
            } else {
                log.error("Entity Argument must be [HostSystem|VirtualMachine|ResourcePool]");
            }
        }
    }

    /**
     * This method initializes all the performance counters available on the system it is connected to. The performance
     * counters are stored in the hashmap counters with group.counter.rolluptype being the key and id being the value.
     */
    private static List<PerfCounterInfo> getPerfCounters(PropertyCollectorHelper propertyCollectorHelper)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        ArrayOfPerfCounterInfo perfCounterInfo = propertyCollectorHelper.fetch(perfManager, "perfCounter");

        return perfCounterInfo.getPerfCounterInfo();
    }

    private static void printEntityCounters(
            VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper, ManagedObjectType entityType)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, IOException {

        ManagedObjectReference moRef = propertyCollectorHelper.getMoRefByName(entityName, entityType);

        List<PerfCounterInfo> counterInfos = getPerfCounters(propertyCollectorHelper);

        if (moRef != null) {
            Set<?> ids = getPerfIdsAvailable(vimPort, perfManager, moRef);

            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename, StandardCharsets.UTF_8)));
            if (counterInfos != null) {
                out.println("<perf-counters>");
                for (PerfCounterInfo pci : counterInfos) {
                    Integer id = pci.getKey();
                    if (ids.contains(id)) {
                        out.print("  <perf-counter key=\"");
                        out.print(id);
                        out.print("\" ");

                        out.print("rollupType=\"");
                        out.print(pci.getRollupType());
                        out.print("\" ");

                        out.print("statsType=\"");
                        out.print(pci.getStatsType());
                        out.println("\">");

                        printElementDescription(out, "groupInfo", pci.getGroupInfo());
                        printElementDescription(out, "nameInfo", pci.getNameInfo());
                        printElementDescription(out, "unitInfo", pci.getUnitInfo());

                        out.println("    <entity type=\"" + entityType + "\"/>");
                        List<Integer> listint = pci.getAssociatedCounterId();
                        int[] ac = new int[listint.size()];
                        for (int i = 0; i < listint.size(); i++) {
                            ac[i] = listint.get(i);
                        }

                        for (int i : ac) {
                            out.println("    <associatedCounter>" + i + "</associatedCounter>");
                        }
                        out.println("  </perf-counter>");
                    }
                }
                out.println("</perf-counters>");
                out.flush();
                out.close();
            }
            log.info("Check {} for Print Counters", filename);
        } else {
            log.error("{} / {} not found.", entityType, entityName);
        }
    }

    private static void printElementDescription(PrintWriter out, String name, ElementDescription elementDescription) {
        out.print("   <" + name + "-key>");
        out.print(elementDescription.getKey());
        out.println("</" + name + "-key>");

        out.print("   <" + name + "-label>");
        out.print(elementDescription.getLabel());
        out.println("</" + name + "-label>");

        out.print("   <" + name + "-summary>");
        out.print(elementDescription.getSummary());
        out.println("</" + name + "-summary>");
    }

    private static Set<Integer> getPerfIdsAvailable(
            VimPortType vimPort, ManagedObjectReference perfMoRef, ManagedObjectReference entityMoRef)
            throws RuntimeFaultFaultMsg {
        Set<Integer> result = new HashSet<>();
        if (entityMoRef != null) {
            List<PerfMetricId> perfMetricIds =
                    vimPort.queryAvailablePerfMetric(perfMoRef, entityMoRef, null, null, 300);
            if (perfMetricIds != null) {
                for (PerfMetricId perfMetricId : perfMetricIds) {
                    result.add(perfMetricId.getCounterId());
                }
            }
        }
        return result;
    }
}
