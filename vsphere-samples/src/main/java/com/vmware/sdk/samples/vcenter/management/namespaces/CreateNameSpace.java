/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.namespaces;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.helpers.ClusterHelper;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.Cluster;
import com.vmware.vcenter.namespace_management.Clusters;
import com.vmware.vcenter.namespace_management.ClustersTypes;
import com.vmware.vcenter.namespaces.AccessTypes;
import com.vmware.vcenter.namespaces.Instances;
import com.vmware.vcenter.namespaces.InstancesTypes;
import com.vmware.vcenter.storage.Policies;
import com.vmware.vcenter.storage.PoliciesTypes;

/**
 * Demonstrates how to create supervisor Namespace on given Supervisor cluster (either NSX-T or VDS based).
 *
 * <p><a href="https://vThinkBeyondVM.com">vThinkBeyondVM.com</a> <a
 * href="https://vthinkbeyondvm.com/script-to-configure-vsphere-supervisor-cluster-using-rest-apis/">Understand APIs</a>
 *
 * <p>Sample Prerequisites: The sample needs an existing Supervisor cluster enabled cluster name. It can be either NSX-T
 * based or vSphere networking
 *
 * <p><a
 * href="https://developer.vmware.com/docs/vsphere-automation/latest/vcenter/api/vcenter/namespaces/instances/post/">API
 * doc</a>
 */
public class CreateNameSpace {
    private static final Logger log = LoggerFactory.getLogger(CreateNameSpace.class);
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

    /** REQUIRED: Name of the cluster we need to get info about. */
    public static String clusterName = "ClusterName";

    /** REQUIRED: Storage policy name. */
    public static Object storagePolicy = "StoragePolicy";

    /** REQUIRED: Supervisor namespace Name. */
    public static String namespaceName = "NamespaceName";

    /** REQUIRED: Domain name i.e. vsphere.local. */
    public static String domainName = "DomainName";

    /** REQUIRED: Storage limit in MB 10240 for 10 GB. */
    public static String storageLimit = "StorageLimit";

    /** REQUIRED: Role name either EDIT or VIEW. */
    public static String roleName = "RoleName";

    /** REQUIRED: Subject name for namespace i.e. Administrator. */
    public static String subjectName = "SubjectName";

    /** REQUIRED: Subject type i.e. USER or GROUP. */
    public static String subjectType = "SubjectType";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(CreateNameSpace.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            Cluster clusterService = client.createStub(Cluster.class);
            Clusters ppClusterService = client.createStub(Clusters.class);

            String clusterId = ClusterHelper.getCluster(clusterService, clusterName);
            ClustersTypes.Info info = ppClusterService.get(clusterId);

            boolean k8sStatus = !(info.getKubernetesStatus().toString().equals("READY"));
            boolean clusterStatus = !(info.getConfigStatus().toString().equals("RUNNING"));

            if (k8sStatus && clusterStatus) {
                log.info("Cluster is NOT in good condition, exiting from creating namespace");
                System.exit(0);
            }

            // Getting Storage policy ID
            Policies policyService = client.createStub(Policies.class);

            Instances namespaceService = client.createStub(Instances.class);

            List<PoliciesTypes.Summary> summaries = policyService.list(null);

            String storagePolicyId = null;
            for (PoliciesTypes.Summary summary : summaries) {
                // TODO: Add NULL check conditions.
                if (summary != null && summary.getName().equals(storagePolicy)) {
                    storagePolicyId = summary.getPolicy();
                    log.info("Storage policy UUID::{}", summary.getPolicy());
                    break;
                }
            }

            InstancesTypes.CreateSpec spec = new InstancesTypes.CreateSpec();
            spec.setCluster(clusterId);
            spec.setDescription("My first namespace, WOW");
            spec.setNamespace(namespaceName);

            InstancesTypes.StorageSpec storageSpec = new InstancesTypes.StorageSpec();
            storageSpec.setLimit(Long.valueOf(storageLimit));
            storageSpec.setPolicy(storagePolicyId);

            List<InstancesTypes.StorageSpec> storageSpecs = new ArrayList<>();
            storageSpecs.add(storageSpec);

            spec.setStorageSpecs(storageSpecs);

            InstancesTypes.Access accessList = new InstancesTypes.Access();
            accessList.setDomain(domainName);

            if (roleName.equalsIgnoreCase("EDIT")) {
                accessList.setRole(AccessTypes.Role.EDIT);
            } else {
                accessList.setRole(AccessTypes.Role.VIEW);
            }

            accessList.setSubject(subjectName); // Default is Administrator

            if (subjectType.equalsIgnoreCase("USER")) {
                accessList.setSubjectType(AccessTypes.SubjectType.USER);
            } else {
                accessList.setSubjectType(AccessTypes.SubjectType.GROUP);
            }

            List<InstancesTypes.Access> accessLists = new ArrayList<>();
            accessLists.add(accessList);

            spec.setAccessList(accessLists);

            namespaceService.create(spec);
            log.info(
                    "Invocation is successful for creating supervisor namespace, check H5C or call GET API to get status");
        }
    }
}
