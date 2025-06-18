/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.monitoring.scsilun;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.List;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.ArrayOfScsiLun;
import com.vmware.vim25.HostScsiDiskPartition;
import com.vmware.vim25.HostVmfsVolume;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ManagedObjectType;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ScsiLun;
import com.vmware.vim25.ScsiLunDurableName;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VmfsDatastoreInfo;

/** This sample displays the CanonicalName, Vendor, Model, Data, Namespace and NamespaceId of the host SCSI LUN name. */
public class SCSILunName {
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

    /** REQUIRED: Host for which SCSI details will be displayed. */
    public static String hostname = "hostname";

    private static ManagedObjectReference host;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(SCSILunName.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            host = propertyCollectorHelper.getMoRefByName(hostname, ManagedObjectType.HOST_SYSTEM);

            ArrayOfScsiLun arrayOfScsiLun = propertyCollectorHelper.fetch(host, "config.storageDevice.scsiLun");

            List<ScsiLun> scsiLun = arrayOfScsiLun.getScsiLun();
            if (scsiLun != null && !scsiLun.isEmpty()) {
                for (int j = 0; j < scsiLun.size(); j++) {
                    System.out.println("\nSCSI LUN " + (j + 1));
                    System.out.println("--------------");

                    String canName = scsiLun.get(j).getCanonicalName();
                    if (scsiLun.get(j).getDurableName() != null) {
                        ScsiLunDurableName scsiLunDurableName = scsiLun.get(j).getDurableName();

                        List<Byte> data = scsiLunDurableName.getData();
                        String namespace = scsiLunDurableName.getNamespace();
                        byte namespaceId = scsiLunDurableName.getNamespaceId();

                        System.out.print("\nData            : ");
                        for (Byte datum : data) {
                            System.out.print(datum + " ");
                        }
                        System.out.println("\nCanonical Name  : " + canName);
                        System.out.println("Namespace       : " + namespace);
                        System.out.println("Namespace ID    : " + namespaceId);
                        System.out.println("\nVMFS Affected ");

                        getVMFS(propertyCollectorHelper, canName);

                        System.out.println("Virtual Machines ");
                        getVMs(propertyCollectorHelper, canName);
                    } else {
                        System.out.println(
                                "\nDurable name for " + scsiLun.get(j).getCanonicalName() + " does not exist");
                    }
                }
            }
        }
    }

    /**
     * This subroutine prints the virtual machine file system volumes affected by the given SCSI LUN.
     *
     * @param canName canonical name of the SCSI logical unit
     */
    private static void getVMFS(PropertyCollectorHelper propertyCollectorHelper, String canName)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        ArrayOfManagedObjectReference ds = propertyCollectorHelper.fetch(host, "datastore");
        List<ManagedObjectReference> dataStoresMoRef = ds.getManagedObjectReference();

        boolean vmfsFlag = false;
        for (ManagedObjectReference managedObjectReference : dataStoresMoRef) {
            var infoClass = propertyCollectorHelper.fetch(managedObjectReference, "info");

            if (infoClass.getClass().getName().equals(VmfsDatastoreInfo.class.getName())) {
                VmfsDatastoreInfo vmfsDatastoreInfo = (VmfsDatastoreInfo) infoClass;

                HostVmfsVolume hostVmfsVolume = vmfsDatastoreInfo.getVmfs();

                String vmfsName = vmfsDatastoreInfo.getName();

                List<HostScsiDiskPartition> diskPartitions = hostVmfsVolume.getExtent();
                for (HostScsiDiskPartition hostScsiDiskPartition : diskPartitions) {
                    if (hostScsiDiskPartition.getDiskName().equals(canName)) {
                        vmfsFlag = true;
                        System.out.println(" " + vmfsName + "\n");
                    }
                }
            }
        }
        if (!vmfsFlag) {
            System.out.println(" None\n");
        }
    }

    /**
     * This subroutine prints the virtual machine affected by the given SCSI LUN.
     *
     * @param canName canonical name of the SCSI logical unit
     */
    private static void getVMs(PropertyCollectorHelper propertyCollectorHelper, String canName)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        ArrayOfManagedObjectReference ds = propertyCollectorHelper.fetch(host, "datastore");
        List<ManagedObjectReference> dataStoresMoRef = ds.getManagedObjectReference();

        boolean vmfsFlag = false;
        for (ManagedObjectReference moRef : dataStoresMoRef) {
            var infoClass = propertyCollectorHelper.fetch(moRef, "info");

            if (infoClass.getClass().getName().equals(VmfsDatastoreInfo.class.getName())) {
                VmfsDatastoreInfo vmfsDatastoreInfo = (VmfsDatastoreInfo) infoClass;

                HostVmfsVolume hostVmfsVolume = vmfsDatastoreInfo.getVmfs();

                List<HostScsiDiskPartition> diskPartitions = hostVmfsVolume.getExtent();
                for (HostScsiDiskPartition hostScsiDiskPartition : diskPartitions) {
                    if (hostScsiDiskPartition.getDiskName().equals(canName)) {
                        ArrayOfManagedObjectReference vms = propertyCollectorHelper.fetch(moRef, "vm");

                        List<ManagedObjectReference> vmsMoRef = vms.getManagedObjectReference();
                        if (vmsMoRef != null) {
                            for (ManagedObjectReference vmMoRef : vmsMoRef) {
                                vmfsFlag = true;

                                String vmname = propertyCollectorHelper.fetch(vmMoRef, "name");
                                System.out.println(" " + vmname);
                            }
                        }
                    }
                }
            }
        }
        if (!vmfsFlag) {
            System.out.println(" None\n");
        }
    }
}
