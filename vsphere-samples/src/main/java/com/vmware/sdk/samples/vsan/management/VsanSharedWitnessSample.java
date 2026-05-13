/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vsan.management;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.sdk.vsphere.utils.vsan.management.VsanManagedObjectsCatalog;
import com.vmware.sdk.vsphere.utils.vsan.management.VsanUtil;
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimClusterVSANStretchedClusterFaultDomainConfig;
import com.vmware.vim25.VimClusterVSANWitnessHostInfo;
import com.vmware.vim25.VimPortType;
import com.vmware.vsan.sdk.ClusterRuntimeInfo;
import com.vmware.vsan.sdk.InvalidArgumentFaultMsg;
import com.vmware.vsan.sdk.InvalidStateFaultMsg;
import com.vmware.vsan.sdk.NotEnoughLicensesFaultMsg;
import com.vmware.vsan.sdk.NotSupportedFaultMsg;
import com.vmware.vsan.sdk.VSANSharedWitnessCompatibilityResult;
import com.vmware.vsan.sdk.VsanFaultFaultMsg;
import com.vmware.vsan.sdk.VsanStretchedClusterConfig;
import com.vmware.vsan.sdk.VsanVcStretchedClusterConfigSpec;
import com.vmware.vsan.sdk.VsanhealthPortType;

/**
 * This sample demonstrates vSAN SharedWitness API by configuring shared witness in the following scenarios:
 *
 * <p>1. For replacing multiple robo clusters of witness into one shared witness in batch:
 *
 * <p>Requirements: one shared witness and one or more robo clusters.
 *
 * <p>API: ReplaceWitnessHostForClusters of the VsanVcStretchedClusterSystem MO.
 *
 * <p>2. For converting one or more regular two-node vSAN clusters to robo clusters sharing the same witness in batch:
 *
 * <p>Requirements: one shared witness and one or more regular two-node vSAN clusters.
 *
 * <p>API: AddWitnessHostForClusters of the VsanVcStretchedClusterSystem MO.
 *
 * <p>Sample Prerequisites:
 *
 * <ol>
 *   <li>A vCenter with one shared witness.
 *   <li>One or more regular two-node vSAN clusters or robo clusters.
 * </ol>
 */
public class VsanSharedWitnessSample {

    private static final Logger log = LoggerFactory.getLogger(VsanSharedWitnessSample.class);

    /** REQUIRED: vCenter FQDN or IP address. */
    public static String serverAddress = "vcenter1.mycompany.com";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";
    /** REQUIRED: Remote witness node to connect to. */
    public static String witness = "witness";

    /** OPTIONAL: Cluster name list of candidate vSAN robo clusters, format: "cluster_1,cluster_2,..." */
    public static String roboClusters = null;
    /** OPTIONAL: Cluster name list of candidate regular two-node vSAN clusters, format: "cluster_1,cluster_2,..." */
    public static String normalClusters = null;
    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (do not leave it empty on production environments).
     */
    public static String trustStorePath = null;

    private static PropertyCollectorHelper propertyCollectorHelper;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VsanSharedWitnessSample.class, args);

        VcenterClientFactory clientFactory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = clientFactory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            // Detecting whether the host is vCenter or ESXi.
            var aboutInfo = serviceContent.getAbout();
            if (!aboutInfo.getApiType().equals("VirtualCenter")) {
                log.error("Remote host should be a Virtual Center.");
                return;
            }

            var witnessMo = getComputeInstance(vimPort, serviceContent, witness);
            if (witnessMo == null) {
                log.error("Given witness host {} is not found in {}", witness, serverAddress);
                return;
            }

            ArrayOfManagedObjectReference hosts = propertyCollectorHelper.fetch(witnessMo, "host");
            List<ManagedObjectReference> hostList = hosts.getManagedObjectReference();
            ManagedObjectReference witnessHost = hostList.get(0);

            List<ManagedObjectReference> allClusters = new ArrayList<>();

            VsanhealthPortType vsanPort = client.getVsanPort();

            if (roboClusters != null && !roboClusters.isEmpty()) {
                List<String> roboClusterNames = Arrays.asList(roboClusters.split(","));
                List<ManagedObjectReference> roboClusterMOs =
                        getClusterInstances(vimPort, serviceContent, roboClusterNames);
                allClusters.addAll(roboClusterMOs);

                var logWitnessStatus = new LogWitnessStatus(witnessHost);
                logWitnessStatus.enter(propertyCollectorHelper, vsanPort);
                replaceWitnessInBatch(vsanPort, witnessHost, allClusters);
                logWitnessStatus.exit(propertyCollectorHelper, vsanPort);
            }

            if (normalClusters != null && !normalClusters.isEmpty()) {
                List<String> twoNodesClusters = Arrays.asList(normalClusters.split(","));
                List<ManagedObjectReference> twoNodesClusterMOs =
                        getClusterInstances(vimPort, serviceContent, twoNodesClusters);
                allClusters.addAll(twoNodesClusterMOs);

                var logWitnessStatus = new LogWitnessStatus(witnessHost);
                logWitnessStatus.enter(propertyCollectorHelper, vsanPort);
                convertToRoboClusterInBatch(vsanPort, witnessHost, twoNodesClusterMOs);
                logWitnessStatus.exit(propertyCollectorHelper, vsanPort);
            }

            var logWitnessStatus = new LogWitnessStatus(witnessHost);
            logWitnessStatus.enter(propertyCollectorHelper, vsanPort);
            removeWitnessForClusters(vsanPort, witnessHost, allClusters);
            logWitnessStatus.exit(propertyCollectorHelper, vsanPort);
        }
    }

    private static ManagedObjectReference getComputeInstance(
            VimPortType vimPort, ServiceContent content, String entityName) {
        var searchIndex = content.getSearchIndex();
        List<ManagedObjectReference> datacenters;
        try {
            datacenters = ((ArrayOfManagedObjectReference)
                            propertyCollectorHelper.fetch(content.getRootFolder(), "childEntity"))
                    .getManagedObjectReference();
        } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e) {
            throw new RuntimeException(e);
        }

        for (var datacenter : datacenters) {
            ManagedObjectReference hostFolder;
            try {
                hostFolder = propertyCollectorHelper.fetch(datacenter, "hostFolder");
            } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e) {
                throw new RuntimeException(e);
            }
            ManagedObjectReference instance;
            try {
                instance = vimPort.findChild(searchIndex, hostFolder, entityName);
            } catch (RuntimeFaultFaultMsg e) {
                throw new RuntimeException(e);
            }
            if (instance != null) {
                return instance;
            }
        }

        return null;
    }

    private static List<ManagedObjectReference> getClusterInstances(
            VimPortType vimPort, ServiceContent content, List<String> clusterNames) {
        List<ManagedObjectReference> clusters = new ArrayList<>();
        for (String clusterName : clusterNames) {
            var cluster = getComputeInstance(vimPort, content, clusterName);
            if (cluster == null) {
                log.error("ERROR: Cluster %s is not found for {}", clusterName);
                System.exit(1);
            }
            clusters.add(cluster);
        }
        return clusters;
    }

    public static void checkCompatibility(
            VsanhealthPortType vsanPort, List<ManagedObjectReference> clusterRefs, ManagedObjectReference witness) {
        VSANSharedWitnessCompatibilityResult compatCheckResult;
        try {
            compatCheckResult = vsanPort.querySharedWitnessCompatibility(
                    VsanManagedObjectsCatalog.getVsanStretchedClusterServiceInstanceReference(), witness, clusterRefs);
        } catch (NotSupportedFaultMsg | com.vmware.vsan.sdk.RuntimeFaultFaultMsg | VsanFaultFaultMsg e) {
            throw new RuntimeException(e);
        }

        String witnessName;
        try {
            witnessName = propertyCollectorHelper.fetch(witness, "name");
        } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e) {
            throw new RuntimeException(e);
        }
        if (!Objects.requireNonNull(compatCheckResult)
                .getWitnessHostCompatibility()
                .isCompatible()) {
            log.error(
                    "ERROR: target host {} doesn't have shared witness capability: {}",
                    witnessName,
                    compatCheckResult
                            .getWitnessHostCompatibility()
                            .getIncompatibleReasons()
                            .get(0)
                            .getMessage());
            System.exit(1);
        }

        for (var clusterCompResult : compatCheckResult.getRoboClusterCompatibility()) {
            if (!clusterCompResult.isCompatible()) {
                String clusterName;
                try {
                    clusterName = propertyCollectorHelper.fetch(clusterCompResult.getEntity(), "name");
                } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e) {
                    throw new RuntimeException(e);
                }
                log.error(
                        "ERROR: cluster {} could not meet shared witness capability requirement: {}",
                        clusterName,
                        clusterCompResult.getIncompatibleReasons());
                System.exit(1);
            }
        }
    }

    /**
     * Convert multiple two-node vsan clusters to robo clusters that share the same witness in batch.
     *
     * <p>Requirements:
     *
     * <ol>
     *   <li>The candidate cluster must be a two-node cluster with vsan enable.
     *   <li>There is no network isolation between witness and the multiple clusters given.
     * </ol>
     *
     * @param vsanPort vSAN Port
     * @param witness witness
     * @param clusterRefs reference of clusters
     */
    public static void convertToRoboClusterInBatch(
            VsanhealthPortType vsanPort, ManagedObjectReference witness, List<ManagedObjectReference> clusterRefs) {
        checkCompatibility(vsanPort, clusterRefs, witness);
        var vscs = VsanManagedObjectsCatalog.getVsanStretchedClusterServiceInstanceReference();
        try {
            for (var clusterRef : clusterRefs) {
                List<VimClusterVSANWitnessHostInfo> infos;
                infos = vsanPort.vsanVcGetWitnessHosts(vscs, clusterRef);
                if (!infos.isEmpty()) {
                    String clusterName = propertyCollectorHelper.fetch(clusterRef, "name");
                    log.error("ERROR: cluster {} is not a regular vSAN cluster", clusterName);
                    System.exit(1);
                }
            }

            String witnessName = propertyCollectorHelper.fetch(witness, "name");
            log.info(
                    "Converting normal vSAN clusters(2 nodes) '{}' to robo clusters with shared witness {}",
                    clusterRefs.stream()
                            .map(c -> {
                                try {
                                    return propertyCollectorHelper.fetch(c, "name");
                                } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .collect(Collectors.toList()),
                    witnessName);

            var spec = new VsanVcStretchedClusterConfigSpec();
            spec.setWitnessHost(witness);

            List<VsanStretchedClusterConfig> clusterConfigs = new ArrayList<>();
            for (var cluster : clusterRefs) {
                var cfg = new VsanStretchedClusterConfig();
                cfg.setCluster(cluster);
                cfg.setPreferredFdName("fd1");
                List<ManagedObjectReference> clusterHosts = ((ArrayOfManagedObjectReference)
                                propertyCollectorHelper.fetch(cluster, "host"))
                        .getManagedObjectReference();

                var faultDomainConfig = new VimClusterVSANStretchedClusterFaultDomainConfig();
                faultDomainConfig.setFirstFdName("fd1");
                faultDomainConfig.getFirstFdHosts().add(clusterHosts.get(0));
                faultDomainConfig.setSecondFdName("fd2");
                faultDomainConfig.getSecondFdHosts().add(clusterHosts.get(1));
                cfg.setFaultDomainConfig(faultDomainConfig);
                clusterConfigs.add(cfg);
            }
            spec.getClusters().addAll(clusterConfigs);

            var addWitnessTask = vsanPort.vsanVcAddWitnessHostForClusters(vscs, spec);
            if (VsanUtil.waitForTasks(propertyCollectorHelper, addWitnessTask)) {
                log.info("vsan vc add witness host for clusters task completed with status: success");
            } else {
                log.error("vsan vc add witness host for clusters task completed with status: failure");
            }
        } catch (InvalidStateFaultMsg
                | com.vmware.vsan.sdk.RuntimeFaultFaultMsg
                | VsanFaultFaultMsg
                | InvalidPropertyFaultMsg
                | RuntimeFaultFaultMsg
                | NotSupportedFaultMsg
                | InvalidArgumentFaultMsg
                | NotEnoughLicensesFaultMsg e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Replace witness with the same Shared witness in batches for multiple robo clusters in one operation.
     *
     * <p>Requirements: 1. The candidate cluster must be vSAN robo cluster. 2. There is no network isolation between
     * witness and the multiple clusters given.
     *
     * @param vsanPort vSAN Port
     * @param witness witness
     * @param clusterRefs reference of clusters
     */
    public static void replaceWitnessInBatch(
            VsanhealthPortType vsanPort, ManagedObjectReference witness, List<ManagedObjectReference> clusterRefs) {
        checkCompatibility(vsanPort, clusterRefs, witness);

        var vscs = VsanManagedObjectsCatalog.getVsanStretchedClusterServiceInstanceReference();
        try {
            for (var clusterRef : clusterRefs) {
                List<VimClusterVSANWitnessHostInfo> infos = vsanPort.vsanVcGetWitnessHosts(vscs, clusterRef);
                if (infos.size() != 1) {
                    String clusterName = propertyCollectorHelper.fetch(clusterRef, "name");
                    log.error("ERROR: cluster {} is not a robo cluster", clusterName);
                    System.exit(1);
                }
            }

            String witnessName = propertyCollectorHelper.fetch(witness, "name");
            log.info(
                    "Replacing the old witness(es) with shared witness {} for clusters: {}",
                    witnessName,
                    clusterRefs.stream()
                            .map(c -> {
                                try {
                                    return propertyCollectorHelper.fetch(c, "name");
                                } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .collect(Collectors.toList()));

            var spec = new VsanVcStretchedClusterConfigSpec();
            spec.setWitnessHost(witness);
            spec.getClusters()
                    .addAll(clusterRefs.stream()
                            .map(c -> {
                                var cfg = new VsanStretchedClusterConfig();
                                cfg.setCluster(c);
                                return cfg;
                            })
                            .collect(Collectors.toList()));

            var task = vsanPort.vsanVcReplaceWitnessHostForClusters(vscs, spec);
            if (VsanUtil.waitForTasks(propertyCollectorHelper, task)) {
                log.info("vsan vc replace witness host for clusters task completed with status: success");
            } else {
                log.error("vsan vc replace witness host for clusters task completed with status: failure");
            }
        } catch (InvalidStateFaultMsg
                | com.vmware.vsan.sdk.RuntimeFaultFaultMsg
                | VsanFaultFaultMsg
                | InvalidPropertyFaultMsg
                | RuntimeFaultFaultMsg
                | NotSupportedFaultMsg
                | InvalidArgumentFaultMsg
                | NotEnoughLicensesFaultMsg e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeWitnessForClusters(
            VsanhealthPortType vsanPort, ManagedObjectReference witness, List<ManagedObjectReference> clusterRefs) {
        List<ManagedObjectReference> totalTasks = new ArrayList<>();
        try {
            String witnessName = propertyCollectorHelper.fetch(witness, "name");
            var vscs = VsanManagedObjectsCatalog.getVsanStretchedClusterServiceInstanceReference();

            for (var clusterRef : clusterRefs) {
                String clusterName = propertyCollectorHelper.fetch(clusterRef, "name");
                log.info("Removing witness {} from cluster {}", witnessName, clusterName);
                var removeTask = vsanPort.vsanVcRemoveWitnessHost(vscs, clusterRef, witness, null);
                totalTasks.add(removeTask);
            }
        } catch (InvalidPropertyFaultMsg
                | RuntimeFaultFaultMsg
                | InvalidArgumentFaultMsg
                | InvalidStateFaultMsg
                | com.vmware.vsan.sdk.RuntimeFaultFaultMsg
                | VsanFaultFaultMsg e) {
            throw new RuntimeException(e);
        }

        totalTasks.forEach(t -> {
            if (VsanUtil.waitForTasks(propertyCollectorHelper, t)) {
                log.info("vsan vc remove witness host task completed with status: success");
            } else {
                log.error("vsan vc remove witness host task completed with status: failure");
            }
        });
    }

    public static List<String> getWitnessClusters(VsanhealthPortType vsanPort, ManagedObjectReference witness) {
        List<String> clusterNames = new ArrayList<>();
        var vscs = VsanManagedObjectsCatalog.getVsanStretchedClusterServiceInstanceReference();
        try {
            List<ClusterRuntimeInfo> witnessClusters = vsanPort.querySharedWitnessClusterInfo(vscs, witness, false);
            for (var cluster : witnessClusters) {
                String clusterName = propertyCollectorHelper.fetch(cluster.getCluster(), "name");
                clusterNames.add(clusterName);
            }
        } catch (NotSupportedFaultMsg
                | com.vmware.vsan.sdk.RuntimeFaultFaultMsg
                | VsanFaultFaultMsg
                | InvalidPropertyFaultMsg
                | RuntimeFaultFaultMsg e) {
            throw new RuntimeException(e);
        }
        return clusterNames;
    }

    static class LogWitnessStatus {
        private ManagedObjectReference witness;

        public LogWitnessStatus(ManagedObjectReference witness) {
            this.witness = witness;
        }

        public void enter(PropertyCollectorHelper propertyCollectorHelper, VsanhealthPortType vsanPort) {
            try {
                log.info(
                        "Before Ops: shared witness {} has joined the following clusters: {}",
                        propertyCollectorHelper.fetch(witness, "name"),
                        getWitnessClusters(vsanPort, witness));
            } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e) {
                throw new RuntimeException(e);
            }
        }

        public void exit(PropertyCollectorHelper propertyCollectorHelper, VsanhealthPortType vsanPort) {
            try {
                log.info(
                        "After Ops: Now shared witness {} has joined the following clusters: {}",
                        propertyCollectorHelper.fetch(witness, "name"),
                        getWitnessClusters(vsanPort, witness));
            } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e) {
                throw new RuntimeException(e);
            }
        }
    }
}
