/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.monitoring.vstats.list;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.List;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vstats.CounterMetadata;
import com.vmware.vstats.CounterMetadataTypes;
import com.vmware.vstats.CounterSets;
import com.vmware.vstats.CounterSetsTypes;
import com.vmware.vstats.Counters;
import com.vmware.vstats.CountersTypes;
import com.vmware.vstats.Metrics;
import com.vmware.vstats.MetricsTypes;
import com.vmware.vstats.Providers;
import com.vmware.vstats.ProvidersTypes;
import com.vmware.vstats.ResourceAddressSchemas;
import com.vmware.vstats.ResourceAddressSchemasTypes;
import com.vmware.vstats.ResourceTypes;
import com.vmware.vstats.ResourceTypesTypes;

/** Demonstrates all vStats discovery APIs which give current state of the system. */
public class Discovery {
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

    private static final String PRINT_LINE = "\n-------------------------------";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(Discovery.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Counters counterService = client.createStub(Counters.class);
            ResourceAddressSchemas resourceAddrSchemaService = client.createStub(ResourceAddressSchemas.class);
            CounterMetadata counterMetadataService = client.createStub(CounterMetadata.class);
            Providers providerService = client.createStub(Providers.class);
            ResourceTypes resourceTypesService = client.createStub(ResourceTypes.class);
            Metrics metricsService = client.createStub(Metrics.class);
            CounterSets counterSetsService = client.createStub(CounterSets.class);

            List<CountersTypes.Info> countersInfo = counterService.list(null);
            if (countersInfo.isEmpty()) {
                System.out.println("Counter list empty.");
                System.exit(0);
            }
            System.out.println(PRINT_LINE);
            System.out.println("List of vStats supported counters: \n");
            for (CountersTypes.Info counterInfo : countersInfo) {
                System.out.println(counterInfo);
            }
            System.out.println(PRINT_LINE);

            // Choose a random counter.
            int randomIndex = (int) (Math.random() * countersInfo.size());
            CountersTypes.Info counter = countersInfo.get(randomIndex);
            System.out.println("\nThe Counter is: \n" + counter);
            System.out.println(PRINT_LINE);

            String cid = counter.getCid();

            // List of counter metadata associated with that counter.
            List<CounterMetadataTypes.Info> counterMetadata = counterMetadataService.list(cid, null);
            System.out.println("\nList of Counter Metadata: \n" + counterMetadata);
            System.out.println(PRINT_LINE);

            // Get resource address schema associated with that counter.
            String rsrcAddrSchemaID = counter.getResourceAddressSchema();
            ResourceAddressSchemasTypes.Info resourceAddrSchema = resourceAddrSchemaService.get(rsrcAddrSchemaID);
            System.out.println("\nResource Address Schema is: \n" + resourceAddrSchema);
            System.out.println(PRINT_LINE);

            // List of vStats providers connected to vCenter Server.
            List<ProvidersTypes.Summary> providers = providerService.list();
            System.out.println("\nList of vStats providers: \n" + providers);
            System.out.println(PRINT_LINE);

            // List of resource types supported by vStats.
            List<ResourceTypesTypes.Summary> resourceTypes = resourceTypesService.list();
            System.out.println("\nList of vStats supported resource types: \n" + resourceTypes);
            System.out.println(PRINT_LINE);

            // List of metrics supported by vStats.
            List<MetricsTypes.Summary> metrics = metricsService.list();
            System.out.println("\nList of vStats supported metrics: \n" + metrics);
            System.out.println(PRINT_LINE);

            // List of vStats defined Counter-sets.
            List<CounterSetsTypes.Info> counterSets = counterSetsService.list();
            System.out.println("\nList of vStats defined Counter-sets: \n" + counterSets);
            System.out.println(PRINT_LINE);
        }
    }
}
