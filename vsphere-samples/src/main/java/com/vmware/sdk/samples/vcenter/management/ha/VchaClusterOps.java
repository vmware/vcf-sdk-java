/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.ha;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.samples.vcenter.management.ha.helpers.SpecHelper;
import com.vmware.sdk.samples.vcenter.management.ha.helpers.TaskHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.vcha.Cluster;
import com.vmware.vcenter.vcha.ClusterTypes;
import com.vmware.vcenter.vcha.CredentialsSpec;
import com.vmware.vcenter.vcha.IpSpec;
import com.vmware.vcenter.vcha.NetworkType;
import com.vmware.vcenter.vcha.PlacementSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/**
 * Demonstrates vCenter HA Cluster Deploy, Undeploy operations for a given vCenter server with automatic cluster
 * configuration and IPv4 network configuration.
 *
 * <ul>
 *   <li>Step 1: Deploy a vCenter HA cluster from the given configuration
 *   <li>Step 2: List the Cluster Info
 *   <li>Step 3: Undeploy the vCenter HA cluster
 * </ul>
 *
 * <p>Sample Prerequisites: The sample needs a vCenter server configured to be the active node in the vCenter HA cluster
 * and IPv4 network configuration for cluster networking
 */
public class VchaClusterOps {
    private static final Logger log = LoggerFactory.getLogger(VchaClusterOps.class);
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

    // Management vCenter Connection Spec Options
    /** OPTIONAL: Hostname of the Management vCenter Server. Leave blank if it's a self-managed VC. */
    public static String vcSpecActiveLocationHostname = null;
    /** OPTIONAL: Username to login to the Management vCenter Server. Leave blank if it's a self-managed VC. */
    public static String vcSpecActiveLocationUsername = null;
    /** OPTIONAL: Password to login to the Management vCenter Server. Leave blank if it's a self-managed VC. */
    public static String vcSpecActiveLocationPassword = null;
    /** REQUIRED: SSL Certificate of Management vCenter Server. */
    public static String vcSpecActiveLocationSSLCertificate = "vcSpecActiveLocationSSLCertificate";

    // Active HA Network Spec Options
    /** OPTIONAL: Active HA IP address of the gateway for this interface. */
    public static String activeHaIpDefaultGateway = null;
    /** OPTIONAL: Active HA List of comma separated IP addresses of the DNS servers to configure the interface. */
    public static String[] activeHaIpDnsServers = null;
    /** REQUIRED: Active HA IP address to be used to configure the interface. */
    public static String activeHaIpIpv4Address = "activeHaIpIpv4Address";
    /** REQUIRED: Active HA Subnet mask of the interface. */
    public static String activeHaIpIpv4SubnetMask = "activeHaIpIpv4SubnetMask";
    /** OPTIONAL: Active HA CIDR Prefix for the interface. */
    public static Long activeHaIpIpv4Prefix = null;
    /**
     * OPTIONAL: The identifier of the network object to be used for the HA network. Leave blank if Active node is
     * already configured with HA network.
     */
    public static String activeHaNetwork = null;
    /**
     * OPTIONAL: The type of the network object to be used by the HA network. Leave blank if Active node is already
     * configured with HA network.
     */
    public static NetworkType activeHaNetworkType = null;

    // Passive Failover Network Spec Options
    /** OPTIONAL: Passive failover IP address of the gateway for this interface. */
    public static String passiveFailoverIpDefaultGateway = null;
    /**
     * OPTIONAL: Passive failover List of comma separated IP addresses of the DNS servers to configure the interface.
     */
    public static String[] passiveFailoverIpDnsServers = null;
    /** OPTIONAL: Passive failover IP address to be used to configure the interface. */
    public static String passiveFailoverIpIpv4Address = null;
    /** OPTIONAL: Passive failover Subnet mask of the interface. */
    public static String passiveFailoverIpIpv4SubnetMask = null;
    /** OPTIONAL: Passive failover CIDR Prefix for the interface. */
    public static Long passiveFailoverIpIpv4Prefix = null;

    // Passive HA Network Spec Options
    /** OPTIONAL: Passive HA IP address of the gateway for this interface. */
    public static String passiveHaIpDefaultGateway = null;
    /** OPTIONAL: Passive HA List of comma separated IP addresses of the DNS servers to configure the interface. */
    public static String[] passiveHaIpDnsServers = null;
    /** REQUIRED: Passive HA IP address to be used to configure the interface. */
    public static String passiveHaIpIpv4Address = "passiveHaIpIpv4Address";
    /** REQUIRED: Passive HA Subnet mask of the interface. */
    public static String passiveHaIpIpv4SubnetMask = "passiveHaIpIpv4SubnetMask";
    /** OPTIONAL: Passive HA CIDR Prefix for the interface. */
    public static Long passiveHaIpIpv4Prefix = null;

    // Passive Placement Spec Options
    /** REQUIRED: Passive placement name of the vCenter HA node to be used for the VM name. */
    public static String passivePlacementName = "passivePlacementName";
    /** REQUIRED: Passive placement identifier of the folder to deploy the vCenter HA node to. */
    public static String passivePlacementFolder = "passivePlacementFolder";
    /** OPTIONAL: Passive placement identifier of the network object to be used for the HA network. */
    public static String passivePlacementHaNetwork = null;
    /** OPTIONAL: Passive placement type of the network object to be used by the HA network. */
    public static NetworkType passivePlacementHaNetworkType = null;
    /** REQUIRED: Passive placement identifier of the host to deploy the vCenter HA node to. */
    public static String passivePlacementHost = "passivePlacementHost";
    /** OPTIONAL: Passive placement identifier of the resource pool to deploy the vCenter HA node to. */
    public static String passivePlacementResourcePool = null;
    /** REQUIRED: Passive placement identifier of the datastore to put all the virtual disks on. */
    public static String passivePlacementStorageDatastore = "passivePlacementStorageDatastore";
    /**
     * OPTIONAL: Passive placement identifier of the network object to be used for the Management network. Leave blank
     * if no failoverIp information set.
     */
    public static String passivePlacementManagementNetwork = null;
    /**
     * OPTIONAL: Passive placement type of the network object to be used by the Management network. Leave blank if no
     * failoverIp information set.
     */
    public static NetworkType passivePlacementManagementNetworkType = null;

    // Witness HA Network Spec Options
    /** OPTIONAL: Witness HA IP address of the gateway for this interface. */
    public static String witnessHaIpDefaultGateway = null;
    /** OPTIONAL: Witness HA List of comma separated IP addresses of the DNS servers to configure the interface. */
    public static String[] witnessHaIpDnsServers = null;
    /** REQUIRED: Witness HA IP address to be used to configure the interface. */
    public static String witnessHaIpIpv4Address = "witnessHaIpIpv4Address";
    /** REQUIRED: Witness HA Subnet mask of the interface. */
    public static String witnessHaIpIpv4SubnetMask = "witnessHaIpIpv4SubnetMask";
    /** OPTIONAL: Witness HA CIDR Prefix for the interface. */
    public static Long witnessHaIpIpv4Prefix = null;

    // Witness Placement Spec Options
    /** REQUIRED: Witness placement name of the vCenter HA node to be used for the VM name. */
    public static String witnessPlacementName = "witnessPlacementName";
    /** REQUIRED: Witness placement identifier of the folder to deploy the vCenter HA node to. */
    public static String witnessPlacementFolder = "witnessPlacementFolder";
    /** OPTIONAL: Witness placement identifier of the network object to be used for the HA network. */
    public static String witnessPlacementHaNetwork = null;
    /** OPTIONAL: Witness placement type of the network object to be used by the HA network. */
    public static NetworkType witnessPlacementHaNetworkType = null;
    /** REQUIRED: Witness placement identifier of the host to deploy the vCenter HA node to. */
    public static String witnessPlacementHost = "witnessPlacementHost";
    /** OPTIONAL: Witness placement identifier of the resource pool to deploy the vCenter HA node to. */
    public static String witnessPlacementResourcePool = null;
    /** REQUIRED: Witness placement identifier of the datastore to put all the virtual disks on. */
    public static String witnessPlacementStorageDatastore = "witnessPlacementStorageDatastore";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(VchaClusterOps.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();

            Cluster clusterService = client.createStub(Cluster.class);
            CredentialsSpec mgmtVcCredentialsSpec = SpecHelper.createCredentialsSpec(
                    vcSpecActiveLocationHostname,
                    vcSpecActiveLocationUsername,
                    vcSpecActiveLocationPassword,
                    vcSpecActiveLocationSSLCertificate);

            // Deploy vCenter HA Cluster and List Cluster Info

            // Spec for active node HA network interface
            IpSpec activeHaIpSpec = SpecHelper.createIpSpec(
                    activeHaIpIpv4Address,
                    activeHaIpIpv4SubnetMask,
                    activeHaIpIpv4Prefix,
                    activeHaIpDefaultGateway,
                    activeHaIpDnsServers == null ? List.of() : List.of(activeHaIpDnsServers));

            // Spec for active Node
            ClusterTypes.ActiveSpec activeSpec =
                    SpecHelper.createActiveSpec(activeHaIpSpec, activeHaNetwork, activeHaNetworkType);

            // Spec for passive node HA network interface
            IpSpec passiveHaIpSpec = SpecHelper.createIpSpec(
                    passiveHaIpIpv4Address,
                    passiveHaIpIpv4SubnetMask,
                    passiveHaIpIpv4Prefix,
                    passiveHaIpDefaultGateway,
                    passiveHaIpDnsServers == null ? List.of() : List.of(passiveHaIpDnsServers));

            // Spec for passive node failover network interface
            IpSpec passiveFailoverIpSpec = SpecHelper.createIpSpec(
                    passiveFailoverIpIpv4Address,
                    passiveFailoverIpIpv4SubnetMask,
                    passiveFailoverIpIpv4Prefix,
                    passiveFailoverIpDefaultGateway,
                    passiveFailoverIpDnsServers == null ? List.of() : List.of(passiveFailoverIpDnsServers));

            // Spec for passive node placement
            PlacementSpec passivePlacementSpec = SpecHelper.createPlacementSpec(
                    passivePlacementName,
                    passivePlacementFolder,
                    passivePlacementHost,
                    passivePlacementHaNetwork,
                    passivePlacementHaNetworkType,
                    passivePlacementStorageDatastore,
                    passivePlacementResourcePool,
                    passivePlacementManagementNetwork,
                    passivePlacementManagementNetworkType);

            // Spec for passive node
            ClusterTypes.PassiveSpec passiveSpec =
                    SpecHelper.createPassiveSpec(passiveHaIpSpec, passiveFailoverIpSpec, passivePlacementSpec);

            // Spec for witness node HA network interface
            IpSpec witnessHaIpSpec = SpecHelper.createIpSpec(
                    witnessHaIpIpv4Address,
                    witnessHaIpIpv4SubnetMask,
                    witnessHaIpIpv4Prefix,
                    witnessHaIpDefaultGateway,
                    witnessHaIpDnsServers == null ? List.of() : List.of(witnessHaIpDnsServers));
            // Spec for witness node placement
            PlacementSpec witnessPlacementSpec = SpecHelper.createPlacementSpec(
                    witnessPlacementName,
                    witnessPlacementFolder,
                    witnessPlacementHost,
                    witnessPlacementHaNetwork,
                    witnessPlacementHaNetworkType,
                    witnessPlacementStorageDatastore,
                    witnessPlacementResourcePool,
                    null,
                    null);

            // Spec for witness node
            ClusterTypes.WitnessSpec witnessSpec = SpecHelper.createWitnessSpec(witnessHaIpSpec, witnessPlacementSpec);

            // Spec for vCenter HA cluster deployment
            ClusterTypes.DeploySpec deploySpec = SpecHelper.createDeploySpec(
                    ClusterTypes.Type.AUTO, activeSpec, passiveSpec, witnessSpec, mgmtVcCredentialsSpec);

            log.info("----- DEPLOY vCenter HA CLUSTER: -----");
            try {
                String deployTaskID = clusterService.deploy_Task(deploySpec);
                if (TaskHelper.waitForTask(vimPort, serviceContent, deployTaskID)) {
                    // Wait for cluster to be healthy
                    TaskHelper.sleep(TaskHelper.TASK_SLEEP);
                }
            } catch (Exception e) {
                log.error("Unable to deploy vCenter HA Cluster");
                log.info("Stack trace: ", e);
            }

            ClusterTypes.Info clusterInfo = clusterService.get(mgmtVcCredentialsSpec, false);
            log.info("----- CLUSTER INFO {}", clusterInfo);

            // cleanup
            // Undeploy Cluster and Delete VMs
            ClusterTypes.UndeploySpec undeploySpec = SpecHelper.createUndeploySpec(mgmtVcCredentialsSpec, true);
            log.info("----- UNDEPLOY vCenter HA CLUSTER -----");

            String undeployTaskID = clusterService.undeploy_Task(undeploySpec);
            TaskHelper.waitForTask(vimPort, serviceContent, undeployTaskID);
        }
    }
}
