/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.vsphere.utils.vsan.management;

import static com.vmware.vsan.sdk.ManagedObjectType.HOST_VSAN_HEALTH_SYSTEM;
import static com.vmware.vsan.sdk.ManagedObjectType.VIM_CLUSTER_VSAN_VC_DISK_MANAGEMENT_SYSTEM;
import static com.vmware.vsan.sdk.ManagedObjectType.VIM_CLUSTER_VSAN_VC_STRETCHED_CLUSTER_SYSTEM;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_CAPABILITY_SYSTEM;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_CLUSTER_POWER_SYSTEM;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_DIAGNOSTICS_SYSTEM;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_FILE_SERVICE_SYSTEM;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_IO_INSIGHT_MANAGER;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_ISCSI_TARGET_SYSTEM;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_MASS_COLLECTOR;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_OBJECT_SYSTEM;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_PERFORMANCE_MANAGER;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_PHONE_HOME_SYSTEM;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_REMOTE_DATASTORE_SYSTEM;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_SPACE_REPORT_SYSTEM;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_SYSTEM_EX;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_UPDATE_MANAGER;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_UPGRADE_SYSTEM_EX;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_VCSA_DEPLOYER_SYSTEM;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_VC_CLUSTER_CONFIG_SYSTEM;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_VC_CLUSTER_HEALTH_SYSTEM;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_VDS_SYSTEM;
import static com.vmware.vsan.sdk.ManagedObjectType.VSAN_VUM_SYSTEM;

import com.vmware.vim25.ManagedObjectReference;

/**
 * This class provides methods for obtaining {@code ManagedObjectReference} instances referring which can be used to
 * refer to various Managed Objects from the vSAN API.
 *
 * <p>{@code ManagedObjectReference} is a data structure which is used as input for API operations, typically as the
 * first parameter named {@code _this}.
 */
public class VsanManagedObjectsCatalog {

    private static final String VC_HEALTH_VALUE = "vsan-cluster-health-system";
    private static final String HOST_HEALTH_VALUE = "ha-vsan-health-system";
    private static final String VC_DISK_MGMT_VALUE = "vsan-disk-management-system";
    private static final String STRETCHED_CLUSTER_VALUE = "vsan-stretched-cluster-system";
    private static final String VC_CLUSTER_CONFIG_VALUE = "vsan-cluster-config-system";
    private static final String VC_ISCSI_TARGET_VALUE = "vsan-cluster-iscsi-target-system";
    private static final String PERF_MANAGER_VALUE = "vsan-performance-manager";
    private static final String UPGRADE_SYS_EX_VALUE = "vsan-upgrade-systemex";
    private static final String SPACE_REPORT_VALUE = "vsan-cluster-space-report-system";
    private static final String CLUSTER_OBJECT_SYSTEM_VALUE = "vsan-cluster-object-system";
    private static final String OBJECT_SYSTEM_VALUE = "vsan-object-system";
    private static final String VCSA_DEPLOYER_SYSTEM_VALUE = "vsan-vcsa-deployer-system";
    private static final String SYSTEM_EX_VALUE = "vsanSystemEx";
    private static final String UPDATE_MANAGER_VALUE = "vsan-update-manager";
    private static final String VDS_SYSTEM_VALUE = "vsan-vds-system";
    private static final String VC_CAPABILITY_SYSTEM_VALUE = "vsan-vc-capability-system";
    private static final String CAPABILITY_SYSTEM_VALUE = "vsan-capability-system";
    private static final String MASS_COLLECTOR_VALUE = "vsan-mass-collector";
    private static final String PHONEHOME_SYSTEM_VALUE = "vsan-phonehome-system";
    private static final String VUM_SYSTEM_VALUE = "vsan-vum-system";
    private static final String FILE_SERVICE_SYSTEM_VALUE = "vsan-cluster-file-service-system";
    private static final String REMOTE_DATASTORE_SYSTEM_VALUE = "vsan-remote-datastore-system";
    private static final String VC_IO_INSIGHT_MANAGER_VALUE = "vsan-cluster-ioinsight-manager";
    private static final String IO_INSIGHT_MANAGER_VALUE = "vsan-host-ioinsight-manager";
    private static final String VC_DIAGNOSTIC_SYSTEM_VALUE = "vsan-cluster-diagnostics-system";
    private static final String VC_CLUSTER_POWER_SYSTEM_VALUE = "vsan-cluster-power-system";

    private static ManagedObjectReference vcHealthRef;
    private static ManagedObjectReference hostHealthRef;
    private static ManagedObjectReference vcDiskMgmtRef;
    private static ManagedObjectReference stretchedClusterRef;
    private static ManagedObjectReference vcClusterConfigRef;
    private static ManagedObjectReference vcIscsiTargetRef;
    private static ManagedObjectReference perfManagerRef;
    private static ManagedObjectReference spaceReportRef;
    private static ManagedObjectReference clusterObjectSystemRef;
    private static ManagedObjectReference objectSystemRef;
    private static ManagedObjectReference upgradeSystemExRef;
    private static ManagedObjectReference vcsaDeployerSystemRef;
    private static ManagedObjectReference systemExRef;
    private static ManagedObjectReference vdsSystemRef;
    private static ManagedObjectReference updateManagerRef;
    private static ManagedObjectReference vcCapabilitySystemRef;
    private static ManagedObjectReference capabilitySystemRef;
    private static ManagedObjectReference massCollectorRef;
    private static ManagedObjectReference phoneHomeSystemRef;
    private static ManagedObjectReference vumSystemRef;
    private static ManagedObjectReference fileServiceSystemRef;
    private static ManagedObjectReference remoteDatastoreSystemRef;
    private static ManagedObjectReference vcIoInsightManagerRef;
    private static ManagedObjectReference ioInsightManagerRef;
    private static ManagedObjectReference vcDiagnosticSystemRef;
    private static ManagedObjectReference vcClusterPowerSystemRef;

    static {
        vcHealthRef = createMoRef(VC_HEALTH_VALUE, VSAN_VC_CLUSTER_HEALTH_SYSTEM.value());
        hostHealthRef = createMoRef(HOST_HEALTH_VALUE, HOST_VSAN_HEALTH_SYSTEM.value());
        vcDiskMgmtRef = createMoRef(VC_DISK_MGMT_VALUE, VIM_CLUSTER_VSAN_VC_DISK_MANAGEMENT_SYSTEM.value());
        stretchedClusterRef =
                createMoRef(STRETCHED_CLUSTER_VALUE, VIM_CLUSTER_VSAN_VC_STRETCHED_CLUSTER_SYSTEM.value());
        vcClusterConfigRef = createMoRef(VC_CLUSTER_CONFIG_VALUE, VSAN_VC_CLUSTER_CONFIG_SYSTEM.value());
        vcIscsiTargetRef = createMoRef(VC_ISCSI_TARGET_VALUE, VSAN_ISCSI_TARGET_SYSTEM.value());
        perfManagerRef = createMoRef(PERF_MANAGER_VALUE, VSAN_PERFORMANCE_MANAGER.value());
        upgradeSystemExRef = createMoRef(UPGRADE_SYS_EX_VALUE, VSAN_UPGRADE_SYSTEM_EX.value());
        spaceReportRef = createMoRef(SPACE_REPORT_VALUE, VSAN_SPACE_REPORT_SYSTEM.value());
        clusterObjectSystemRef = createMoRef(CLUSTER_OBJECT_SYSTEM_VALUE, VSAN_OBJECT_SYSTEM.value());
        objectSystemRef = createMoRef(OBJECT_SYSTEM_VALUE, VSAN_OBJECT_SYSTEM.value());
        vcsaDeployerSystemRef = createMoRef(VCSA_DEPLOYER_SYSTEM_VALUE, VSAN_VCSA_DEPLOYER_SYSTEM.value());
        vcCapabilitySystemRef = createMoRef(VC_CAPABILITY_SYSTEM_VALUE, VSAN_CAPABILITY_SYSTEM.value());
        capabilitySystemRef = createMoRef(CAPABILITY_SYSTEM_VALUE, VSAN_CAPABILITY_SYSTEM.value());
        systemExRef = createMoRef(SYSTEM_EX_VALUE, VSAN_SYSTEM_EX.value());
        vdsSystemRef = createMoRef(VDS_SYSTEM_VALUE, VSAN_VDS_SYSTEM.value());
        updateManagerRef = createMoRef(UPDATE_MANAGER_VALUE, VSAN_UPDATE_MANAGER.value());
        massCollectorRef = createMoRef(MASS_COLLECTOR_VALUE, VSAN_MASS_COLLECTOR.value());
        phoneHomeSystemRef = createMoRef(PHONEHOME_SYSTEM_VALUE, VSAN_PHONE_HOME_SYSTEM.value());
        vumSystemRef = createMoRef(VUM_SYSTEM_VALUE, VSAN_VUM_SYSTEM.value());
        fileServiceSystemRef = createMoRef(FILE_SERVICE_SYSTEM_VALUE, VSAN_FILE_SERVICE_SYSTEM.value());
        remoteDatastoreSystemRef = createMoRef(REMOTE_DATASTORE_SYSTEM_VALUE, VSAN_REMOTE_DATASTORE_SYSTEM.value());
        vcIoInsightManagerRef = createMoRef(VC_IO_INSIGHT_MANAGER_VALUE, VSAN_IO_INSIGHT_MANAGER.value());
        ioInsightManagerRef = createMoRef(IO_INSIGHT_MANAGER_VALUE, VSAN_IO_INSIGHT_MANAGER.value());
        vcDiagnosticSystemRef = createMoRef(VC_DIAGNOSTIC_SYSTEM_VALUE, VSAN_DIAGNOSTICS_SYSTEM.value());
        vcClusterPowerSystemRef = createMoRef(VC_CLUSTER_POWER_SYSTEM_VALUE, VSAN_CLUSTER_POWER_SYSTEM.value());
    }

    /** @return the {@link ManagedObjectReference} instance for the ServiceInstance of the vSAN VC health service */
    public static ManagedObjectReference getVsanVcHealthServiceInstanceReference() {
        return vcHealthRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the ServiceInstance of the vSAN host health service */
    public static ManagedObjectReference getVsanHostHealthServiceInstanceReference() {
        return hostHealthRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN VC disk management service */
    public static ManagedObjectReference getVsanVcDiskMgrServiceInstanceReference() {
        return vcDiskMgmtRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN stretched cluster service */
    public static ManagedObjectReference getVsanStretchedClusterServiceInstanceReference() {
        return stretchedClusterRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN VC cluster configuration service */
    public static ManagedObjectReference getVsanVcClusterConfigServiceInstanceReference() {
        return vcClusterConfigRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN VC iSCSI Target service */
    public static ManagedObjectReference getVsanVcIscsiTargetServiceInstanceReference() {
        return vcIscsiTargetRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN space report service */
    public static ManagedObjectReference getVsanSpaceReportServiceInstanceReference() {
        return spaceReportRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN cluster object system service. */
    public static ManagedObjectReference getVsanClusterObjectSystemServiceInstanceReference() {
        return clusterObjectSystemRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN object system service */
    public static ManagedObjectReference getVsanObjectSystemServiceInstanceReference() {
        return objectSystemRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN upgrade system service */
    public static ManagedObjectReference getVsanUpgradeSystemExServiceInstanceReference() {
        return upgradeSystemExRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN performance service */
    public static ManagedObjectReference getVsanPerfMgrServiceInstanceReference() {
        return perfManagerRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN VCSA deployer system */
    public static ManagedObjectReference getVsanVcsaDeployerSystemServiceInstanceReference() {
        return vcsaDeployerSystemRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN system Ex */
    public static ManagedObjectReference getVsanSystemExInstanceReference() {
        return systemExRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN VDS system */
    public static ManagedObjectReference getVsanVdsSystem() {
        return vdsSystemRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN update manager */
    public static ManagedObjectReference getVsanUpdateManager() {
        return updateManagerRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN VC capability system. */
    public static ManagedObjectReference getVsanVcCapabilitySystem() {
        return vcCapabilitySystemRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN capability system. */
    public static ManagedObjectReference getVsanCapabilitySystem() {
        return capabilitySystemRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN mass collector. */
    public static ManagedObjectReference getVsanMassCollector() {
        return massCollectorRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN PhoneHome system. */
    public static ManagedObjectReference getVsanPhoneHomeSystem() {
        return phoneHomeSystemRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN VUM system. */
    public static ManagedObjectReference getVsanVumSystem() {
        return vumSystemRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN File Service system. */
    public static ManagedObjectReference getVsanFileServiceSystem() {
        return fileServiceSystemRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the vSAN Remote Datastore system. */
    public static ManagedObjectReference getVsanRemoteDatastoreSystem() {
        return remoteDatastoreSystemRef;
    }

    /**
     * @return the {@link ManagedObjectReference} instance for the ServiceInstance of the vSAN VC I/O Insight Manager
     *     system.
     */
    public static ManagedObjectReference getVsanVcIoInsightManager() {
        return vcIoInsightManagerRef;
    }

    /**
     * @return the {@link ManagedObjectReference} instance for the ServiceInstance of the vSAN I/O Insight Manager
     *     system.
     */
    public static ManagedObjectReference getVsanIoInsightManager() {
        return ioInsightManagerRef;
    }

    /** @return the {@link ManagedObjectReference} instance for the ServiceInstance of the vSAN VC Diagnostic system. */
    public static ManagedObjectReference getVsanVcDiagnosticSystem() {
        return vcDiagnosticSystemRef;
    }

    /**
     * @return the {@link ManagedObjectReference} instance for the ServiceInstance of the vSAN VC Cluster Power system.
     */
    public static ManagedObjectReference getVsanVcClusterPowerSystem() {
        return vcClusterPowerSystemRef;
    }

    private static ManagedObjectReference createMoRef(String value, String type) {
        ManagedObjectReference ref = new ManagedObjectReference();
        ref.setType(type);
        ref.setValue(value);
        return ref;
    }
}
