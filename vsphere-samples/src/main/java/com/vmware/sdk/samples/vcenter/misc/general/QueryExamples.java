/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.misc.general;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.helpers.PrettyPrintHelper;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.query.QueryBuilder;
import com.vmware.sdk.vsphere.utils.query.QueryPredicateBuilder;
import com.vmware.sdk.vsphere.utils.query.QueryResult;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.SearchIndexQuerySpecResourceType;
import com.vmware.vim25.SearchIndexResultSet;
import com.vmware.vim25.VimPortType;

/**
 * This sample demonstrates various query examples using the query builder classes from the vsphere-utils library.
 */
public class QueryExamples {
    private static final Logger log = LoggerFactory.getLogger(QueryExamples.class);

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

    /** Example number to run (1-22). Default: 1 */
    public static Integer queryExampleNum = null;
    /** Host name for specific host queries (Examples 3, 4). Default: "host-9" */
    public static String queryHostId = null;
    /** Extra config value for VM queries (Example 6). Default: "FALSE" */
    public static String querySvgaValue = null;
    /** Physical NIC key for host network queries (Examples 8, 9). Default: "key-vim.host.PhysicalNic-vmnic0" */
    public static String queryPnicKey = null;
    /** MAC address for specific host pnic queries (Example 13). Default: "02:00:53:58:79:89" */
    public static String queryMacAddress = null;
    /** Minimum link speed for all pnics query (Example 14, 15). Default: 10 */
    public static Integer queryMinLinkSpeed = null;
    /** VM name pattern for like queries (Examples 16, 17). Default: "*clone*" */
    public static String queryVmNamePattern = null;
    /** List of VM names for IN queries (Examples 18, 19). Default: ["cloneVM-1", "VM-4"] */
    public static String[] queryVmNames = null;
    /** Folder name pattern for like queries (Examples 20, 21, 22). Default: "tst-*" */
    public static String queryFolderNamePattern = null;
    /** Limit for pagination example (Example 21). Default: 50 */
    public static Integer queryPaginationLimit = null;

    public static void main(String[] args) throws Exception {

        SampleCommandLineParser.load(QueryExamples.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();

            // Set default values for optional parameters
            if (queryExampleNum == null) queryExampleNum = 1;
            if (queryHostId == null) queryHostId = "host-9";
            if (querySvgaValue == null) querySvgaValue = "FALSE";
            if (queryPnicKey == null) queryPnicKey = "key-vim.host.PhysicalNic-vmnic0";
            if (queryMacAddress == null) queryMacAddress = "02:00:53:58:79:89";
            if (queryMinLinkSpeed == null) queryMinLinkSpeed = 10;
            if (queryVmNamePattern == null) queryVmNamePattern = "*clone*";
            if (queryVmNames == null) queryVmNames = new String[] {"cloneVM-1", "VM-4"};
            if (queryFolderNamePattern == null) queryFolderNamePattern = "tst-*";
            if (queryPaginationLimit == null) queryPaginationLimit = 50;

            runQueryExample(vimPort, queryExampleNum);
        }
    }

    private static void runQueryExample(VimPortType vimPort, int exampleNum) {
        try {
            switch (exampleNum) {
                case 1:
                    runExample1(vimPort);
                    break;
                case 2:
                    runExample2(vimPort);
                    break;
                case 3:
                    runExample3(vimPort);
                    break;
                case 4:
                    runExample4(vimPort);
                    break;
                case 5:
                    runExample5(vimPort);
                    break;
                case 6:
                    runExample6(vimPort);
                    break;
                case 7:
                    runExample7(vimPort);
                    break;
                case 8:
                    runExample8(vimPort);
                    break;
                case 9:
                    runExample9(vimPort);
                    break;
                case 10:
                    runExample10(vimPort);
                    break;
                case 11:
                    runExample11(vimPort);
                    break;
                case 12:
                    runExample12(vimPort);
                    break;
                case 13:
                    runExample13(vimPort);
                    break;
                case 14:
                    runExample14(vimPort);
                    break;
                case 15:
                    runExample15(vimPort);
                    break;
                case 16:
                    runExample16(vimPort);
                    break;
                case 17:
                    runExample17(vimPort);
                    break;
                case 18:
                    runExample18(vimPort);
                    break;
                case 19:
                    runExample19(vimPort);
                    break;
                case 20:
                    runExample20(vimPort);
                    break;
                case 21:
                    runExample21(vimPort);
                    break;
                case 22:
                    runExample22(vimPort);
                    break;
                default:
                    log.error("Invalid example number: {}. Valid examples: 1-22", exampleNum);
                    return;
            }
        } catch (Exception e) {
            log.error("Error running example {}", exampleNum, e);
        }
    }

    /** Example 1: Basic VM listing */
    private static void runExample1(VimPortType vimPort) {
        log.info("=== Example 1: Basic VM listing ===");
        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name")
                .from(SearchIndexQuerySpecResourceType.VIRTUAL_MACHINE)
                .execute();

        printQueryResult(result);
    }

    /** Example 2: VMs with power state and host */
    private static void runExample2(VimPortType vimPort) {
        log.info("=== Example 2: VMs with power state and host ===");
        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name", "summary.runtime.powerState", "summary.runtime.host")
                .from(SearchIndexQuerySpecResourceType.VIRTUAL_MACHINE)
                .execute();

        printQueryResult(result);
    }

    /** Example 3: VMs on specific host */
    private static void runExample3(VimPortType vimPort) {
        log.info("=== Example 3: VMs on specific host ===");
        ManagedObjectReference hostRef = new ManagedObjectReference();
        hostRef.setType("HostSystem");
        hostRef.setValue(queryHostId);

        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name", "summary.runtime.powerState", "summary.runtime.host")
                .from(SearchIndexQuerySpecResourceType.VIRTUAL_MACHINE)
                .where(new QueryPredicateBuilder("summary.runtime.host").equal(hostRef))
                .execute();

        printQueryResult(result);
    }

    /** Example 4: Powered off VMs on specific host */
    private static void runExample4(VimPortType vimPort) {
        log.info("=== Example 4: Powered off VMs on specific host ===");
        ManagedObjectReference hostRef = new ManagedObjectReference();
        hostRef.setType("HostSystem");
        hostRef.setValue(queryHostId);

        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name", "summary.runtime.powerState", "summary.runtime.host")
                .from(SearchIndexQuerySpecResourceType.VIRTUAL_MACHINE)
                .where(new QueryPredicateBuilder("summary.runtime.host").equal(hostRef))
                .where(new QueryPredicateBuilder("summary.runtime.powerState").equal("poweredOff"))
                .execute();

        printQueryResult(result);
    }

    /** Example 5: VMs with svga.present config */
    private static void runExample5(VimPortType vimPort) {
        log.info("=== Example 5: VMs with svga.present config ===");
        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name", "config.extraConfig[\"svga.present\"]")
                .from(SearchIndexQuerySpecResourceType.VIRTUAL_MACHINE)
                .execute();

        printQueryResult(result);
    }

    /** Example 6: VMs with svga.present=FALSE */
    private static void runExample6(VimPortType vimPort) {
        log.info("=== Example 6: VMs with svga.present=FALSE ===");
        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name", "config.extraConfig[\"svga.present\"]")
                .from(SearchIndexQuerySpecResourceType.VIRTUAL_MACHINE)
                .where(new QueryPredicateBuilder("config.extraConfig[\"svga.present\"]")
                        .equal(querySvgaValue))
                .execute();

        printQueryResult(result);
    }

    /** Example 7: Host network pnic */
    private static void runExample7(VimPortType vimPort) {
        log.info("=== Example 7: Host network pnic ===");
        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name", "config.network.pnic")
                .from(SearchIndexQuerySpecResourceType.HOST_SYSTEM)
                .execute();

        printQueryResult(result);
    }

    /** Example 8: Host specific pnic */
    private static void runExample8(VimPortType vimPort) {
        log.info("=== Example 8: Host specific pnic ===");
        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name", "config.network.pnic[\"" + queryPnicKey + "\"]")
                .from(SearchIndexQuerySpecResourceType.HOST_SYSTEM)
                .execute();

        printQueryResult(result);
    }

    /** Example 9: Host pnic MAC address */
    private static void runExample9(VimPortType vimPort) {
        log.info("=== Example 9: Host pnic MAC address ===");
        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name", "config.network.pnic[\"" + queryPnicKey + "\"].mac")
                .from(SearchIndexQuerySpecResourceType.HOST_SYSTEM)
                .execute();

        printQueryResult(result);
    }

    /** Example 10: All host pnic MAC addresses */
    private static void runExample10(VimPortType vimPort) {
        log.info("=== Example 10: All host pnic MAC addresses ===");
        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name", "config.network.pnic[*].mac")
                .from(SearchIndexQuerySpecResourceType.HOST_SYSTEM)
                .execute();

        printQueryResult(result);
    }

    /** Example 11: Host pnic keys and MACs */
    private static void runExample11(VimPortType vimPort) {
        log.info("=== Example 11: Host pnic keys and MACs ===");
        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name", "config.network.pnic[*].mac", "config.network.pnic[*].key")
                .from(SearchIndexQuerySpecResourceType.HOST_SYSTEM)
                .execute();

        printQueryResult(result);
    }

    /** Example 12: Host pnic details (key, mac, speed) */
    private static void runExample12(VimPortType vimPort) {
        log.info("=== Example 12: Host pnic details (key, mac, speed) ===");
        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name", "config.network.pnic[*].(key,mac,linkSpeed.speedMb)")
                .from(SearchIndexQuerySpecResourceType.HOST_SYSTEM)
                .execute();

        printQueryResult(result);
    }

    /** Example 13: Host with specific MAC address */
    private static void runExample13(VimPortType vimPort) {
        log.info("=== Example 13: Host with specific MAC address ===");
        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name", "config.network.pnic[*].(key,mac,linkSpeed.speedMb)")
                .from(SearchIndexQuerySpecResourceType.HOST_SYSTEM)
                .where(new QueryPredicateBuilder("config.network.pnic[*].mac")
                        .anyArrayElement()
                        .equal(queryMacAddress))
                .execute();

        printQueryResult(result);
    }

    /** Example 14: Hosts with all pnics > query_min_link_speed Mbps */
    private static void runExample14(VimPortType vimPort) {
        log.info("=== Example 14: Hosts with all pnics > {} Mbps ===", queryMinLinkSpeed);
        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name", "config.network.pnic[*].(key,mac,linkSpeed.speedMb)")
                .from(SearchIndexQuerySpecResourceType.HOST_SYSTEM)
                .where(new QueryPredicateBuilder("config.network.pnic[*].linkSpeed.speedMb")
                        .allArrayElements()
                        .greater(queryMinLinkSpeed))
                .execute();

        printQueryResult(result);
    }

    /** Example 15: Hosts with any pnic > query_min_link_speed Mbps */
    private static void runExample15(VimPortType vimPort) {
        log.info("=== Example 15: Hosts with any pnic > {} Mbps ===", queryMinLinkSpeed);
        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name", "config.network.pnic[*].(key,mac,linkSpeed.speedMb)")
                .from(SearchIndexQuerySpecResourceType.HOST_SYSTEM)
                .where(new QueryPredicateBuilder("config.network.pnic[*].linkSpeed.speedMb")
                        .anyArrayElement()
                        .greater(queryMinLinkSpeed))
                .execute();

        printQueryResult(result);
    }

    /** Example 16: VMs with names like '*clone*' */
    private static void runExample16(VimPortType vimPort) {
        log.info("=== Example 16: VMs with names like '{}' ===", queryVmNamePattern);
        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name")
                .from(SearchIndexQuerySpecResourceType.VIRTUAL_MACHINE)
                .where(new QueryPredicateBuilder("name").like(queryVmNamePattern))
                .execute();

        printQueryResult(result);
    }

    /** Example 17: VMs with names NOT like '*clone*' */
    private static void runExample17(VimPortType vimPort) {
        log.info("=== Example 17: VMs with names NOT like '{}' ===", queryVmNamePattern);
        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name")
                .from(SearchIndexQuerySpecResourceType.VIRTUAL_MACHINE)
                .where(new QueryPredicateBuilder("name").notLike(queryVmNamePattern))
                .execute();

        printQueryResult(result);
    }

    /** Example 18: VMs with specific names */
    private static void runExample18(VimPortType vimPort) {
        log.info("=== Example 18: VMs with specific names ===");
        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name")
                .from(SearchIndexQuerySpecResourceType.VIRTUAL_MACHINE)
                .where(new QueryPredicateBuilder("name").in((Object[]) queryVmNames))
                .execute();

        printQueryResult(result);
    }

    /** Example 19: VMs with names NOT in specific list */
    private static void runExample19(VimPortType vimPort) {
        log.info("=== Example 19: VMs with names NOT in specific list ===");
        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name")
                .from(SearchIndexQuerySpecResourceType.VIRTUAL_MACHINE)
                .where(new QueryPredicateBuilder("name").notIn((Object[]) queryVmNames))
                .execute();

        printQueryResult(result);
    }

    /** Example 20: Folders with names like 'tst-folder*' */
    private static void runExample20(VimPortType vimPort) {
        log.info("=== Example 20: Folders with names like '{}' ===", queryFolderNamePattern);
        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name")
                .from(SearchIndexQuerySpecResourceType.FOLDER)
                .where(new QueryPredicateBuilder("name").like(queryFolderNamePattern))
                .execute();

        printQueryResult(result);
    }

    /** Example 21: Pagination example */
    private static void runExample21(VimPortType vimPort) {
        log.info("=== Example 21: Pagination example ===");
        QueryResult result = new QueryBuilder(vimPort)
                .select("@moRef", "name")
                .from(SearchIndexQuerySpecResourceType.FOLDER)
                .where(new QueryPredicateBuilder("name").like(queryFolderNamePattern))
                .limit(queryPaginationLimit)
                .execute();

        int pageNo = 0;
        while (true) {
            pageNo++;
            log.info("Page: {}", pageNo);
            printQueryResult(result);
            if (!result.hasNextPage()) {
                break;
            }
            result = result.getNextPage(queryPaginationLimit);
        }
    }

    /** Example 22: Count folders */
    private static void runExample22(VimPortType vimPort) {
        log.info("=== Example 22: Count folders ===");
        QueryResult result = new QueryBuilder(vimPort)
                .select()
                .from(SearchIndexQuerySpecResourceType.FOLDER)
                .limit(0)
                .where(new QueryPredicateBuilder("name").like(queryFolderNamePattern))
                .returnTotalCount()
                .execute();

        printQueryResult(result);
    }

    /** Helper function to print query results. */
    private static void printQueryResult(QueryResult queryResult) {
        SearchIndexResultSet result = queryResult.getResult();

        // Print the actual return value of Execute
        log.info("Query Result:");
        log.info("Properties: {}", result.getProperties());
        log.info("Items count: {}", result.getItems().size());
        log.info("Items:");
        for (int i = 0; i < result.getItems().size(); i++) {
            log.info("  Item {}:", i);
            for (int j = 0; j < result.getItems().get(i).getPropertyValues().size(); j++) {
                Object value =
                        result.getItems().get(i).getPropertyValues().get(j).getValue();
                log.info("    Property {}: Value {}", result.getProperties().get(j), PrettyPrintHelper.formatObjectValue(value));
            }
        }

        if (result.getTotalCount() != null) {
            log.info("Total Count: {}", result.getTotalCount());
        }
        if (queryResult.hasNextPage()) {
            log.info("More pages present");
        }
    }

}
