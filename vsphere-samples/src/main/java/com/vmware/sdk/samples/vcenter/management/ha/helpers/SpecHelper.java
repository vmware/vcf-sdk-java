/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.ha.helpers;

import java.util.List;

import com.vmware.vcenter.vcha.ClusterTypes;
import com.vmware.vcenter.vcha.ConnectionSpec;
import com.vmware.vcenter.vcha.CredentialsSpec;
import com.vmware.vcenter.vcha.DiskSpec;
import com.vmware.vcenter.vcha.IpFamily;
import com.vmware.vcenter.vcha.IpSpec;
import com.vmware.vcenter.vcha.Ipv4Spec;
import com.vmware.vcenter.vcha.Ipv6Spec;
import com.vmware.vcenter.vcha.NetworkType;
import com.vmware.vcenter.vcha.PlacementSpec;

public class SpecHelper {

    private static final Long SERVER_PORT = 443L;

    private static ConnectionSpec createConnectionSpec(
            String hostname, String username, String password, String sslCertificate) {
        return new ConnectionSpec.Builder(hostname)
                .setUsername(username)
                .setPassword(password.toCharArray())
                .setSslCertificate(sslCertificate)
                .setPort(SERVER_PORT)
                .build();
    }

    public static CredentialsSpec createCredentialsSpec(
            String hostname, String username, String password, String sslCertificate) {
        ConnectionSpec connectionSpec = createConnectionSpec(hostname, username, password, sslCertificate);
        return new CredentialsSpec.Builder(connectionSpec).build();
    }

    public static ClusterTypes.DeploySpec createDeploySpec(
            ClusterTypes.Type deploymentType,
            ClusterTypes.ActiveSpec activeSpec,
            ClusterTypes.PassiveSpec passiveSpec,
            ClusterTypes.WitnessSpec witnessSpec,
            CredentialsSpec vcCredentialsSpec) {
        return new ClusterTypes.DeploySpec.Builder(deploymentType, activeSpec, passiveSpec, witnessSpec)
                .setVcSpec(vcCredentialsSpec)
                .build();
    }

    public static ClusterTypes.UndeploySpec createUndeploySpec(CredentialsSpec credentialsSpec, Boolean forceDelete) {
        return new ClusterTypes.UndeploySpec.Builder()
                .setVcSpec(credentialsSpec)
                .setForceDelete(forceDelete)
                .build();
    }

    public static PlacementSpec createPlacementSpec(
            String name,
            String folder,
            String host,
            String haNetwork,
            NetworkType haNetworkType,
            String datastore,
            String resourcePool,
            String managementNetwork,
            NetworkType managementNetworkType) {
        DiskSpec diskSpec = createDiskSpec(datastore);

        return new PlacementSpec.Builder(name, folder)
                .setHost(host)
                .setHaNetwork(haNetwork)
                .setHaNetworkType(haNetworkType)
                .setStorage(diskSpec)
                .setResourcePool(resourcePool)
                .setManagementNetwork(managementNetwork)
                .setManagementNetworkType(managementNetworkType)
                .build();
    }

    public static IpSpec createIpSpec(
            String address, String subnetMask, Long prefix, String defaultGateway, List<String> dnsServers) {
        Ipv4Spec ipv4Spec = new Ipv4Spec.Builder(address)
                .setSubnetMask(subnetMask)
                .setPrefix(prefix)
                .build();
        return new IpSpec.Builder(IpFamily.IPV4)
                .setIpv4(ipv4Spec)
                .setDefaultGateway(defaultGateway)
                .setDnsServers(dnsServers)
                .build();
    }

    public static IpSpec createIpSpec(String address, Long prefix, String defaultGateway, List<String> dnsServers) {
        Ipv6Spec ipv6Spec = new Ipv6Spec.Builder(address, prefix).build();

        return new IpSpec.Builder(IpFamily.IPV6)
                .setIpv6(ipv6Spec)
                .setDefaultGateway(defaultGateway)
                .setDnsServers(dnsServers)
                .build();
    }

    public static DiskSpec createDiskSpec(String datastore) {
        return new DiskSpec.Builder().setDatastore(datastore).build();
    }

    public static ClusterTypes.ActiveSpec createActiveSpec(IpSpec ipSpec, String haNetwork, NetworkType haNetworkType) {
        return new ClusterTypes.ActiveSpec.Builder(ipSpec)
                .setHaNetwork(haNetwork)
                .setHaNetworkType(haNetworkType)
                .build();
    }

    public static ClusterTypes.PassiveSpec createPassiveSpec(
            IpSpec haIpSpec, IpSpec failoverIpSpec, PlacementSpec placementSpec) {
        return new ClusterTypes.PassiveSpec.Builder(haIpSpec)
                .setFailoverIp(failoverIpSpec)
                .setPlacement(placementSpec)
                .build();
    }

    public static ClusterTypes.WitnessSpec createWitnessSpec(IpSpec haIpSpec, PlacementSpec placementSpec) {
        return new ClusterTypes.WitnessSpec.Builder(haIpSpec)
                .setPlacement(placementSpec)
                .build();
    }
}
