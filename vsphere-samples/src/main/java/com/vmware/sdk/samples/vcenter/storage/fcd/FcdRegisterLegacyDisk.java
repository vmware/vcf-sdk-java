/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.storage.fcd;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VStorageObject;
import com.vmware.vim25.VimPortType;

/** This sample registers a given legacy disk as First class disk. */
public class FcdRegisterLegacyDisk {
    private static final Logger log = LoggerFactory.getLogger(FcdRegisterLegacyDisk.class);
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

    /** REQUIRED: File name of the legacy disk. Ex: [sharedVmfs-0] TestVm_REKZ/TestVm_REKZ.vmdk */
    public static String legacyDiskFileName = "legacyDiskFileName";
    /** REQUIRED: Name of the datacenter. */
    public static String dataCenterName = "dataCenterName";
    /** OPTIONAL: Name of the newly created first class disk object. */
    public static String fcdName = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(FcdRegisterLegacyDisk.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();

            ManagedObjectReference vStrObjManagerMoRef = serviceContent.getVStorageObjectManager();

            // Generate the httpUrl from diskPath which the vc recognizes.
            String legacyDiskPathForVc = getDiskPathForVc(legacyDiskFileName);

            // Register the disk as FirstClassDisk.
            log.info("Operation: Register a legacy disk as FCD with disk Path :: {}", legacyDiskPathForVc);
            VStorageObject registeredVStrObj = vimPort.registerDisk(vStrObjManagerMoRef, legacyDiskPathForVc, fcdName);

            log.info(
                    "Success: Registered Disk(now a vStorageObject) : [Uuid = {} ] with Name [ {} ]\n",
                    registeredVStrObj.getConfig().getId().getId(),
                    registeredVStrObj.getConfig().getName());
        }
    }

    /**
     * Util method to get the diskPath recognized by vc for a given disk.
     *
     * @return filePath of vStorageObject
     */
    private static String getDiskPathForVc(String fileNameOfDisk) {
        // Ex: vmdkLocation is :: [sharedVmfs-0] TestVm_3PYN/TestVm_3PYN.vmdk.
        String regex1 = "\\[(.*)\\]\\s(.*)/(.*\\.vmdk)";
        String ds = null;
        String vmFolder = null;
        String vmdk = null;
        if (Pattern.matches(regex1, fileNameOfDisk)) {
            log.info("FileName Pattern matches required pattern.");
            Pattern pattern1 = Pattern.compile(regex1);
            Matcher m = pattern1.matcher(fileNameOfDisk);
            if (m.find()) {
                ds = m.group(1);
                vmFolder = m.group(2);
                vmdk = m.group(3);
            }
        }
        /*
         * diskPath format as recognized by VC:
         * https://<VCIP>/folder/<PathToVmdkInsideDatastore
         * >?dcPath=<DataCenterName>&dsName=<DatastoreName>
         *
         * Ex: diskpath =
         * https://10.160.232.230/folder/TestVm_REKZ/TestVm_REKZ.vmdk
         * ?dcPath=vcqaDC&dsName=sharedVmfs-0
         */

        return "https://" + serverAddress + "/" + "folder/"
                + vmFolder + "/" + vmdk + "?dcPath=" + dataCenterName
                + "&dsName=" + ds;
    }
}
