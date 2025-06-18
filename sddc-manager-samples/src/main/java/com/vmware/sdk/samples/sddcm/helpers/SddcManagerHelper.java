/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.sddcm.helpers;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.vmware.sdk.sddcm.model.Cluster;
import com.vmware.sdk.sddcm.model.Domain;
import com.vmware.sdk.sddcm.model.Host;
import com.vmware.sdk.sddcm.model.LicenseKey;
import com.vmware.sdk.sddcm.model.PageOfCluster;
import com.vmware.sdk.sddcm.model.PageOfDomain;
import com.vmware.sdk.sddcm.model.PageOfHost;
import com.vmware.sdk.sddcm.model.PageOfLicenseKey;
import com.vmware.sdk.sddcm.v1.V1Factory;

public class SddcManagerHelper {
    /**
     * Utility method to get the Cluster by cluster name.
     *
     * @param v1Factory required to call the V1 API's
     * @param clusterName required to get the specific cluster
     * @return Cluster by name
     * @throws java.util.concurrent.ExecutionException if the there was an error during the API call
     * @throws InterruptedException if the client is interrupted while waiting
     */
    public static Cluster getClusterByName(V1Factory v1Factory, String clusterName)
            throws ExecutionException, InterruptedException {
        PageOfCluster pageOfCluster =
                v1Factory.clustersService().getClusters().invoke().get();
        return pageOfCluster.getElements().stream()
                .filter(cluster -> cluster.getName().equals(clusterName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Utility method to get the Domain by domain name.
     *
     * @param v1Factory required to call the V1 API's
     * @param domainName required to get the specific domain
     * @return Domain by name
     * @throws java.util.concurrent.ExecutionException if the there was an error during the API call
     * @throws InterruptedException if the client is interrupted while waiting
     */
    public static Domain getDomainByName(V1Factory v1Factory, String domainName)
            throws ExecutionException, InterruptedException {
        PageOfDomain pageOfDomain =
                v1Factory.domainsService().getDomains().invoke().get();
        return pageOfDomain.getElements().stream()
                .filter(domain -> domain.getName().equals(domainName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Utility method to get the un-assigned hosts.
     *
     * @param v1Factory required to call the V1 API's
     * @return list of unassigned hosts or free pool hosts from inventory
     * @throws java.util.concurrent.ExecutionException if the there was an error during the API call
     * @throws InterruptedException if the client is interrupted while waiting
     */
    public static List<Host> getHostsFromFreeInventoryPool(V1Factory v1Factory)
            throws ExecutionException, InterruptedException {
        PageOfHost pageOfHost = v1Factory.hostsService().getHosts().invoke().get();
        return pageOfHost.getElements().stream()
                .filter(host -> host.getStatus().equalsIgnoreCase("UNASSIGNED"))
                .collect(Collectors.toList());
    }

    /**
     * Utility method to get the host by host FQDN.
     *
     * @param v1Factory required to call the V1 API's
     * @param hostName host FQDN
     * @return host details
     * @throws java.util.concurrent.ExecutionException if the there was an error during the API call
     * @throws InterruptedException if the client is interrupted while waiting
     */
    public static Host getHostsByName(V1Factory v1Factory, String hostName)
            throws ExecutionException, InterruptedException {
        PageOfHost pageOfHost = v1Factory.hostsService().getHosts().invoke().get();
        return pageOfHost.getElements().stream()
                .filter(host -> host.getFqdn().equals(hostName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Utility method to get the license key by Product type(e.g: ESXi, VC, NSXT, vSAN).
     *
     * @param v1Factory required to call the V1 API's
     * @param productType (e.g: ESXi, VC, NSX, vSAN)
     * @return license key of the respective productType
     * @throws java.util.concurrent.ExecutionException if the there was an error during the API call
     * @throws InterruptedException if the client is interrupted while waiting
     */
    public static String getLicenseKeyByEntityType(V1Factory v1Factory, String productType)
            throws ExecutionException, InterruptedException {
        PageOfLicenseKey pageOfLicenseKey =
                v1Factory.licenseKeysService().getLicenseKeys().invoke().get();
        LicenseKey licenseKey = pageOfLicenseKey.getElements().stream()
                .filter(license -> license.getProductType().equals(productType))
                .findFirst()
                .orElse(null);
        return licenseKey != null ? licenseKey.getKey() : null;
    }
}
