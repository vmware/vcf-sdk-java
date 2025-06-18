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
import static com.vmware.vim25.ManagedObjectType.CLUSTER_COMPUTE_RESOURCE;
import static com.vmware.vim25.ManagedObjectType.COMPUTE_RESOURCE;
import static com.vmware.vim25.ManagedObjectType.HOST_SYSTEM;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.TraversalSpec;

/**
 * Demonstrates retrieving the list of hosts of the given cluster using {@link com.vmware.vim25.TraversalSpec}
 * specification.
 */
public class HostsInCluster {
    private static final Logger log = LoggerFactory.getLogger(HostsInCluster.class);
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

    /** REQUIRED: Name of the cluster used to list the hosts within it. */
    public static String clusterName = "clusterName";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(HostsInCluster.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            PropertyCollectorHelper propertyCollectorHelper =
                    new PropertyCollectorHelper(client.getVimPort(), client.getVimServiceContent());

            ManagedObjectReference clusterMoRef =
                    propertyCollectorHelper.getMoRefByName(clusterName, CLUSTER_COMPUTE_RESOURCE);

            PropertySpec propertySpec = new PropertySpec();
            propertySpec.setType(HOST_SYSTEM.value());
            propertySpec.setAll(false);
            propertySpec.getPathSet().addAll(Collections.emptyList());

            TraversalSpec traversalSpec = new TraversalSpec();
            traversalSpec.setType(COMPUTE_RESOURCE.value());
            traversalSpec.setPath("host");
            traversalSpec.setName("hosts");

            SelectionSpec selectionSpec = new SelectionSpec();
            selectionSpec.setName(traversalSpec.getName());

            traversalSpec.getSelectSet().add(selectionSpec);

            ObjectSpec objectSpec = new ObjectSpec();
            objectSpec.setObj(clusterMoRef);
            objectSpec.setSkip(true);
            objectSpec.getSelectSet().add(traversalSpec);

            PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
            propertyFilterSpec.getPropSet().add(propertySpec);
            propertyFilterSpec.getObjectSet().add(objectSpec);

            List<ObjectContent> objectContentList =
                    propertyCollectorHelper.retrieveAllProperties(Collections.singletonList(propertyFilterSpec));

            List<String> hosts =
                    objectContentList.stream().map(oc -> oc.getObj().getValue()).collect(Collectors.toList());

            log.info("Found {} hosts in {}: {}", objectContentList.size(), clusterName, hosts);
        }
    }
}
