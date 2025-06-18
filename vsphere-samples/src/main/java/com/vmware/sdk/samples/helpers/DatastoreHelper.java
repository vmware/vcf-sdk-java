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

import com.vmware.content.library.StorageBacking;
import com.vmware.vcenter.Datacenter;
import com.vmware.vcenter.Datastore;
import com.vmware.vcenter.DatastoreTypes;

public class DatastoreHelper {

    /**
     * Returns the identifier of a datastore
     *
     * <p>Note: The method assumes that there is only one datastore and datacenter with the mentioned names.
     *
     * @param datacenterService Datacenter service stub
     * @param datacenterName name of the datacenter for the placement spec
     * @param datastoreService Datastore service stub
     * @param datastoreName name of the datastore for the placement spec
     * @return identifier of a datastore
     */
    public static String getDatastore(
            Datastore datastoreService, String datastoreName, Datacenter datacenterService, String datacenterName) {
        // Get the datastore
        Set<String> datastores = Collections.singleton(datastoreName);
        List<DatastoreTypes.Summary> datastoreSummaries = null;
        DatastoreTypes.FilterSpec datastoreFilterSpec = null;
        if (null != datacenterName) {
            // Get the datacenter
            Set<String> datacenters =
                    Collections.singleton(DatacenterHelper.getDatacenter(datacenterService, datacenterName));
            datastoreFilterSpec = new DatastoreTypes.FilterSpec.Builder()
                    .setNames(datastores)
                    .setDatacenters(datacenters)
                    .build();
            datastoreSummaries = datastoreService.list(datastoreFilterSpec);
            if (datastoreSummaries.size() == 0) {
                throw new RuntimeException(
                        "Datastore " + datastoreName + "not found in datacenter : " + datacenterName);
            }
        } else {
            datastoreFilterSpec =
                    new DatastoreTypes.FilterSpec.Builder().setNames(datastores).build();
            datastoreSummaries = datastoreService.list(datastoreFilterSpec);
            if (!(datastoreSummaries.size() > 0)) {
                throw new RuntimeException("Datastore " + datastoreName + " not found");
            }
        }
        return datastoreSummaries.get(datastoreSummaries.size() - 1).getDatastore();
    }

    public static String getDatastore(Datastore datastoreService, String datastoreName) {
        return getDatastore(datastoreService, datastoreName, null, null);
    }

    /**
     * Creates a datastore storage backing.
     *
     * @param datastoreService Datastore service stub
     * @param dsName name of the datastore for the placement spec
     * @return the storage backing
     */
    public static StorageBacking createStorageBacking(Datastore datastoreService, String dsName) {
        String dsId = getDatastore(datastoreService, dsName);

        // Build the storage backing with the datastore Id
        StorageBacking storageBacking = new StorageBacking();
        storageBacking.setType(StorageBacking.Type.DATASTORE);
        storageBacking.setDatastoreId(dsId);
        return storageBacking;
    }
}
