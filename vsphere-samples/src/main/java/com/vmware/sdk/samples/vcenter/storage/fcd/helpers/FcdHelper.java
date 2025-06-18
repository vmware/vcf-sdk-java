/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.storage.fcd.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vim25.BaseConfigInfoBackingInfo;
import com.vmware.vim25.BaseConfigInfoFileBackingInfo;
import com.vmware.vim25.DeviceGroupId;
import com.vmware.vim25.FaultDomainId;
import com.vmware.vim25.ID;
import com.vmware.vim25.ReplicationGroupId;
import com.vmware.vim25.ReplicationSpec;
import com.vmware.vim25.VStorageObject;
import com.vmware.vim25.VirtualMachineDefinedProfileSpec;
import com.vmware.vim25.VirtualMachineEmptyProfileSpec;
import com.vmware.vim25.VirtualMachineProfileSpec;

/** Helper class for FCD Samples. */
public class FcdHelper {
    private static final Logger log = LoggerFactory.getLogger(FcdHelper.class);

    /**
     * Utility method to wrap ID string in an ID object.
     *
     * @param idStr The idStr to be wrapped.
     * @return id in ID format.
     */
    public static ID makeId(String idStr) {
        ID id = new ID();
        id.setId(idStr);
        return id;
    }

    /**
     * Utility method to create a {@link VirtualMachineProfileSpec} from profileId, deviceGroupId and faultDomainId.
     *
     * @param pbmProfileId ID of SPBM profile
     * @param deviceGroupId ID of the replication device group
     * @param faultDomainId ID of the fault domain to which the group belongs
     * @return List of {@link VirtualMachineProfileSpec}
     */
    public static List<VirtualMachineProfileSpec> generateVirtualMachineProfileSpec(
            String pbmProfileId, String deviceGroupId, String faultDomainId) {
        List<VirtualMachineProfileSpec> listOfDiskProfileSpec = new ArrayList<>();
        if (pbmProfileId == null && deviceGroupId == null && faultDomainId == null) {
            listOfDiskProfileSpec.add(new VirtualMachineEmptyProfileSpec());
        } else {
            VirtualMachineDefinedProfileSpec definedProfileSpec = new VirtualMachineDefinedProfileSpec();
            if (pbmProfileId != null) {
                definedProfileSpec.setProfileId(pbmProfileId);
            }
            if (deviceGroupId != null) {
                DeviceGroupId devGpId = new DeviceGroupId();
                devGpId.setId(deviceGroupId);

                FaultDomainId faultDomId = new FaultDomainId();
                faultDomId.setId(faultDomainId);

                ReplicationGroupId replicationGroupId = new ReplicationGroupId();
                replicationGroupId.setDeviceGroupId(devGpId);
                replicationGroupId.setFaultDomainId(faultDomId);

                ReplicationSpec replicationSpec = new ReplicationSpec();
                replicationSpec.setReplicationGroupId(replicationGroupId);
                definedProfileSpec.setReplicationSpec(replicationSpec);
                log.info(
                        "Setting replicationSpec with replication group Id :: \n [ Uuid = {} ]\n and Fault domain Id :: \n [ Uuid = {} ]\n ",
                        deviceGroupId,
                        faultDomainId);
            }
            listOfDiskProfileSpec.add(definedProfileSpec);
        }
        return listOfDiskProfileSpec;
    }

    /**
     * Utility method to verify if Fcd ID List&lt;String&gt; is included in fcdIDList.
     *
     * @param fcdStrIdList List of FCD IDs as String
     * @param fcdIdList List of FCD IDs as ID
     * @return true if fcdIDList&lt;ID&gt; contains all the fcds in fcdStrIDList&lt;String&gt;
     */
    public static boolean isFcdIdInFcdList(List<String> fcdStrIdList, List<ID> fcdIdList) {
        Set<String> fcdIDAsStringSet = fcdIdList.stream().map(ID::getId).collect(Collectors.toSet());

        return fcdIDAsStringSet.containsAll(fcdStrIdList);
    }

    /**
     * Utility method to get the FilePath of a given VStorageObject.
     *
     * @param vStorageObject the vStorageObject whose path is to be found
     * @return filePath of vStorageObject
     */
    public static String getFcdFilePath(VStorageObject vStorageObject) {
        BaseConfigInfoBackingInfo backingInfo = vStorageObject.getConfig().getBacking();
        if (backingInfo instanceof BaseConfigInfoFileBackingInfo) {
            BaseConfigInfoFileBackingInfo fileBackingInfo = (BaseConfigInfoFileBackingInfo) backingInfo;
            return fileBackingInfo.getFilePath();
        }
        return null;
    }
}
