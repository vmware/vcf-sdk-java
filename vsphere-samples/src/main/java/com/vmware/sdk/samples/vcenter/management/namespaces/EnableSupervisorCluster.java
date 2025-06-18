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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.helpers.ClusterHelper;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.Cluster;
import com.vmware.vcenter.Network;
import com.vmware.vcenter.NetworkTypes;
import com.vmware.vcenter.namespace_management.Clusters;
import com.vmware.vcenter.namespace_management.ClustersTypes;
import com.vmware.vcenter.namespace_management.DistributedSwitchCompatibility;
import com.vmware.vcenter.namespace_management.DistributedSwitchCompatibilityTypes;
import com.vmware.vcenter.namespace_management.EdgeClusterCompatibility;
import com.vmware.vcenter.namespace_management.EdgeClusterCompatibilityTypes;
import com.vmware.vcenter.namespace_management.Ipv4Cidr;
import com.vmware.vcenter.namespace_management.SizingHint;
import com.vmware.vcenter.storage.Policies;
import com.vmware.vcenter.storage.PoliciesTypes;

/**
 * Demonstrates how to enable vSphere supervisor cluster on given cluster (NSX-T should be configured).
 *
 * <p>Sample Prerequisites: All below params need to be passed for this sample
 *
 * <p><a href="https://vthinkbeyondvm.com/script-to-configure-vsphere-supervisor-cluster-using-rest-apis/">Understand
 * APIs</a>
 *
 * <p><a
 * href="https://docs.vmware.com/en/VMware-vSphere/7.0/vmware-vsphere-with-kubernetes/GUID-8D0E905F-9ABB-4CFB-A206-C027F847FAAC.html">Official
 * doc</a>
 */
public class EnableSupervisorCluster {
    private static final Logger log = LoggerFactory.getLogger(EnableSupervisorCluster.class);
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

    /** REQUIRED: Name of the cluster we need to get info about k8scluster. */
    public static String clusterName = "clusterName";
    /** REQUIRED: Cluster size - enum value as either of TINY/SMALL/MEDIUM/LARGE. */
    public static String hintSize = "hintSize";
    /** REQUIRED: Master VM network port group. */
    public static String masterNetwork = "masterNetwork";
    /** REQUIRED: Master starting IP - floating IP used by HA master cluster. */
    public static String floatingIP = "floatingIP";
    /** REQUIRED: Master DNS server IP. */
    public static String masterDnsServer = "masterDnsServer";
    /** REQUIRED: Worker DNS server IP. */
    public static String workerDnsServer = "workerDnsServer";
    /** REQUIRED: NTP server IP. */
    public static String ntpServer = "ntpServer";
    /** REQUIRED: Storage Policy for Master, image and ephermal storage. */
    public static String storagePolicy = "storagePolicy";
    /** REQUIRED: POD cidr network i.e. 10.2.3.4/24 (including prefix). */
    public static String podCidr = "podCidr";
    /** REQUIRED: Service cidr i.e. 10.2.3.4/21 (including prefix). */
    public static String serviceCidr = "serviceCidr";
    /** REQUIRED: Ingress cidr network (including prefix). */
    public static String ingressCidr = "ingressCidr";
    /** REQUIRED: Egress cidr network (including prefix). */
    public static String egressCidr = "egressCidr";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(EnableSupervisorCluster.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            // Supervisor cluster service object
            Clusters ppClusterService = client.createStub(Clusters.class);
            Cluster clusterService = client.createStub(Cluster.class);

            log.info("Getting cluster identifier as part of setup");
            String clusterId = ClusterHelper.getCluster(clusterService, clusterName);

            // Getting policy ID
            log.info("Storage policy name passed was::{}", storagePolicy);
            Policies policyService = client.createStub(Policies.class);

            List<PoliciesTypes.Summary> summaries = policyService.list(null);
            String storagePolicyId = null;
            if (summaries != null && !summaries.isEmpty()) {
                for (PoliciesTypes.Summary summary : summaries) {
                    // TODO: Add NULL check conditions.
                    if (summary.getName().equals(storagePolicy)) {
                        storagePolicyId = summary.getPolicy();
                        log.info("Storage policy UUID::{}", summary.getPolicy());
                        break;
                    }
                }
                if (storagePolicyId == null) {
                    log.info("There is no storage policy matching with the one passed in the enable call");
                }
            } else {
                log.info("Storage policies are not returned, please check the setup");
            }

            // Getting VDS uuid
            DistributedSwitchCompatibility vdsService = client.createStub(DistributedSwitchCompatibility.class);

            DistributedSwitchCompatibilityTypes.FilterSpec vdsFilter =
                    new DistributedSwitchCompatibilityTypes.FilterSpec();
            vdsFilter.setCompatible(true);

            List<DistributedSwitchCompatibilityTypes.Summary> vdsSummary = vdsService.list(clusterId, vdsFilter);

            // Assuming only one DVS compatible in that cluster. Add NULL check
            String dvsSwitchUuid = vdsSummary.get(0).getDistributedSwitch();
            log.info("DVS switch UUID::{}", vdsSummary.get(0).getDistributedSwitch());

            // Getting Edge cluster id
            EdgeClusterCompatibilityTypes.FilterSpec edgeFilter = new EdgeClusterCompatibilityTypes.FilterSpec();
            edgeFilter.setCompatible(true);

            EdgeClusterCompatibility nsxEdgeService = client.createStub(EdgeClusterCompatibility.class);
            List<EdgeClusterCompatibilityTypes.Summary> edgeSummary =
                    nsxEdgeService.list(clusterId, vdsSummary.get(0).getDistributedSwitch(), edgeFilter);

            // Assuming only one Edge compatible in that cluster. Add NULL check
            String edgeClusterId = edgeSummary.get(0).getEdgeCluster();
            log.info("Edge UUID::{}", edgeSummary.get(0).getEdgeCluster());

            // Getting master network id
            NetworkTypes.FilterSpec networkFilter = new NetworkTypes.FilterSpec();

            Set<String> names = new HashSet<>();
            names.add(masterNetwork);

            networkFilter.setNames(names);

            Network networkService = client.createStub(Network.class);
            List<NetworkTypes.Summary> networkSummary = networkService.list(networkFilter);

            String networkId = networkSummary.get(0).getNetwork();
            log.info("Network id::{}", networkSummary.get(0).getNetwork());

            log.info("We are building the Spec for enabling vSphere supervisor cluster");

            ClustersTypes.EnableSpec spec = new ClustersTypes.EnableSpec();
            switch (hintSize) {
                case "TINY":
                    spec.setSizeHint(SizingHint.TINY);
                    break;
                case "SMALL":
                    spec.setSizeHint(SizingHint.SMALL);
                    break;
                case "MEDIUM":
                    spec.setSizeHint(SizingHint.MEDIUM);
                    break;
                case "LARGE":
                    spec.setSizeHint(SizingHint.LARGE);
                    break;
                default:
                    log.error("incorrect hintSize, please enter either of TINY, SMALL, MEDIUM, LARGE");
                    // TODO exit here or anyway call will fail as well.
                    break;
            }

            Ipv4Cidr serCidr = new Ipv4Cidr();
            String[] serviceCidrParts = serviceCidr.split("/");
            serCidr.setAddress(serviceCidrParts[0]);
            serCidr.setPrefix(Long.parseLong(serviceCidrParts[1]));

            spec.setServiceCidr(serCidr);

            spec.setNetworkProvider(ClustersTypes.NetworkProvider.NSXT_CONTAINER_PLUGIN);

            ClustersTypes.NCPClusterNetworkEnableSpec ncpSpec = new ClustersTypes.NCPClusterNetworkEnableSpec();
            // VDS identifier/UUID
            ncpSpec.setClusterDistributedSwitch(dvsSwitchUuid); // Identifier
            // Edge cluster id
            ncpSpec.setNsxEdgeCluster(edgeClusterId); // Edge cluster id

            // Ingress CIDR
            Ipv4Cidr ingressCIDR = new Ipv4Cidr();
            String[] ingressCidrParts = ingressCidr.split("/");
            ingressCIDR.setAddress(ingressCidrParts[0]);
            ingressCIDR.setPrefix(Long.parseLong(ingressCidrParts[1]));

            List<Ipv4Cidr> ingressList = new ArrayList<>();
            ingressList.add(ingressCIDR);

            ncpSpec.setIngressCidrs(ingressList);

            // Egress CIDR
            Ipv4Cidr egressCIDR = new Ipv4Cidr();
            String[] egressCidrParts = egressCidr.split("/");
            egressCIDR.setAddress(egressCidrParts[0]);
            egressCIDR.setPrefix(Long.parseLong(egressCidrParts[1]));

            List<Ipv4Cidr> egressList = new ArrayList<>();
            egressList.add(egressCIDR);

            ncpSpec.setEgressCidrs(egressList);

            // POD CIDR
            Ipv4Cidr podCIDR = new Ipv4Cidr();
            String[] podCidrParts = podCidr.split("/");
            podCIDR.setAddress(podCidrParts[0]);
            podCIDR.setPrefix(Long.parseLong(podCidrParts[1]));

            List<Ipv4Cidr> podList = new ArrayList<>();
            podList.add(podCIDR);
            ncpSpec.setPodCidrs(podList);

            spec.setNcpClusterNetworkSpec(ncpSpec);

            ClustersTypes.NetworkSpec masterNet = new ClustersTypes.NetworkSpec();
            // MasterVM network identifier
            masterNet.setNetwork(networkId);

            masterNet.setMode(ClustersTypes.NetworkSpec.Ipv4Mode.DHCP); // DHCP is only for lab/educational purpose
            masterNet.setFloatingIP(floatingIP);

            // un-comment below code if you want to enter static IP range. Production environment should use it
            /*
             * ClustersTypes.Ipv4Range range=new ClustersTypes.Ipv4Range();
             * range.setStartingAddress(startingIP); range.setAddressCount(5);
             * //Hardcoded, please change as needed range.setGateway(gatewayIP);
             * range.setSubnetMask(masterSubnetMask); masterNet.setAddressRange(range);
             */

            spec.setMasterManagementNetwork(masterNet);

            List<String> masterDNS = new ArrayList<>();
            masterDNS.add(masterDnsServer);
            spec.setMasterDNS(masterDNS);

            List<String> workerDNS = new ArrayList<>();
            workerDNS.add(workerDnsServer); // Using same DNS for worker and Master
            spec.setWorkerDNS(workerDNS);

            List<String> ntpserver = new ArrayList<>();
            ntpserver.add(ntpServer);
            spec.setMasterNTPServers(ntpserver);

            spec.setMasterStoragePolicy(storagePolicyId); // Storage policy identifier
            spec.setEphemeralStoragePolicy(storagePolicyId); // Storage policy identifier
            spec.setLoginBanner("This is your first Project pacific cluster");
            // spec.setMasterDNSNames(masterDNSSearch); //re-using above one

            ClustersTypes.ImageStorageSpec imageSpec = new ClustersTypes.ImageStorageSpec();
            imageSpec.setStoragePolicy(storagePolicyId); // Storage policy identifier
            spec.setImageStorage(imageSpec);

            ppClusterService.enable(clusterId, spec);
            log.info("Invocation is successful for enabling vSphere supervisor cluster, check H5C");
        }
    }
}
