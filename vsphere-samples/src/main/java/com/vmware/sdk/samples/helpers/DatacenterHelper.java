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

import com.vmware.vcenter.Datacenter;
import com.vmware.vcenter.DatacenterTypes;

public class DatacenterHelper {

    /**
     * Returns the identifier of a datacenter
     *
     * <p>Note: The method assumes only one datacenter with the mentioned name.
     *
     * @param datacenterService Datacenter service stub
     * @param datacenterName name of the datacenter for the placement spec
     * @return identifier of a datacenter
     */
    public static String getDatacenter(Datacenter datacenterService, String datacenterName) {

        Set<String> datacenterNames = Collections.singleton(datacenterName);
        DatacenterTypes.FilterSpec dcFilterSpec = new DatacenterTypes.FilterSpec.Builder()
                .setNames(datacenterNames)
                .build();
        List<DatacenterTypes.Summary> dcSummaries = datacenterService.list(dcFilterSpec);

        if (dcSummaries.size() == 0) {
            throw new RuntimeException("Datacenter with name " + datacenterName + " not found.");
        }

        return dcSummaries.get(0).getDatacenter();
    }
}
