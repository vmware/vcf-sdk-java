/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.helpers;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.vmware.vcenter.Cluster;
import com.vmware.vcenter.ClusterTypes;
import com.vmware.vcenter.Datacenter;

public class ClusterHelper {

    /**
     * Returns the identifier of a cluster
     *
     * <p>Note: The method assumes that there is only one cluster and datacenter with the mentioned names.
     *
     * @param clusterService Cluster service stub
     * @param clusterName name of the cluster
     * @param datacenterService Datacenter service stub
     * @param datacenterName name of the datacenter
     * @return identifier of a cluster
     */
    public static String getCluster(
            Cluster clusterService, String clusterName, Datacenter datacenterService, String datacenterName) {

        Set<String> clusters = Collections.singleton(clusterName);
        ClusterTypes.FilterSpec.Builder clusterFilterBuilder = new ClusterTypes.FilterSpec.Builder().setNames(clusters);
        if (null != datacenterName) {
            // Get the datacenter
            Set<String> datacenters =
                    Collections.singleton(DatacenterHelper.getDatacenter(datacenterService, datacenterName));
            clusterFilterBuilder.setDatacenters(datacenters);
        }
        List<ClusterTypes.Summary> clusterSummaries = clusterService.list(clusterFilterBuilder.build());
        if (clusterSummaries.size() == 0) {
            throw new RuntimeException("Cluster " + clusterName + "not found in datacenter: " + datacenterName);
        }
        return clusterSummaries.get(0).getCluster();
    }

    public static String getCluster(Cluster clusterService, String clusterName) {
        return getCluster(clusterService, clusterName, null, null);
    }
}
