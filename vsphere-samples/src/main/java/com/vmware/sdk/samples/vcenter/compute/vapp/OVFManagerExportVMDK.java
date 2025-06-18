/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.compute.vapp;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.createInsecureSocketFactory;
import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.HOST_SYSTEM;
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.ssl.InsecureHostnameVerifier;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.HttpNfcLeaseDeviceUrl;
import com.vmware.vim25.HttpNfcLeaseInfo;
import com.vmware.vim25.HttpNfcLeaseState;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/** This sample demonstrates OVFManager. Exports VMDKs of a VM to the localSystem. */
public class OVFManagerExportVMDK {
    private static final Logger log = LoggerFactory.getLogger(OVFManagerExportVMDK.class);
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

    private static volatile long totalBytes = 0;
    private static volatile boolean vmdkFlag = false;

    /** REQUIRED: Name of the virtual machine. */
    public static String vmName = "vmName";
    /** REQUIRED: Name of Host System. */
    public static String host = "host";
    /** REQUIRED: Absolute path of localSystem folder. */
    public static String localPath = "localPath";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(OVFManagerExportVMDK.class, args);

        File file = new File(localPath);
        if (!file.exists()) {
            log.error("Wrong or invalid path {}", localPath);
            return;
        }

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference sourceHostMoRef = propertyCollectorHelper.getMoRefByName(host, HOST_SYSTEM);
            if (sourceHostMoRef == null) {
                throw new RuntimeException(" Source Host " + host + " Not Found.");
            }

            ManagedObjectReference vmMoRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);
            if (vmMoRef == null) {
                throw new RuntimeException("Virtual Machine " + vmName + " Not Found.");
            }

            log.info("Getting the HTTP NFCLEASE for the VM: {}", vmName);

            try {
                ManagedObjectReference httpNfcLease = vimPort.exportVm(vmMoRef);

                Object[] result = propertyCollectorHelper.awaitManagedObjectUpdates(
                        httpNfcLease, new String[] {"state"}, new String[] {"state"}, new Object[][] {
                            new Object[] {HttpNfcLeaseState.READY, HttpNfcLeaseState.ERROR}
                        });

                if (result[0].equals(HttpNfcLeaseState.READY)) {
                    log.info("HttpNfcLeaseState: {}", result[0]);

                    HttpNfcLeaseInfo httpNfcLeaseInfo = propertyCollectorHelper.fetch(httpNfcLease, "info");
                    httpNfcLeaseInfo.setLeaseTimeout(300000000);

                    printHttpNfcLeaseInfo(httpNfcLeaseInfo, host);

                    long diskCapacity = (httpNfcLeaseInfo.getTotalDiskCapacityInKB()) * 1024;
                    totalBytes = diskCapacity;

                    HttpNfcLeaseExtender leaseExtender = new HttpNfcLeaseExtender(httpNfcLease, vimPort);

                    Thread t = new Thread(leaseExtender);
                    t.start();

                    List<HttpNfcLeaseDeviceUrl> deviceUrlList = httpNfcLeaseInfo.getDeviceUrl();
                    for (HttpNfcLeaseDeviceUrl httpNfcLeaseDeviceUrl : deviceUrlList) {
                        log.info("Downloading Files:");

                        String deviceUrl = httpNfcLeaseDeviceUrl.getUrl();
                        String absoluteFile = deviceUrl.substring(deviceUrl.lastIndexOf("/") + 1);
                        log.info("Absolute File Name: {}", absoluteFile);
                        log.info("VMDK URL: {}", deviceUrl.replace("*", host));

                        writeVMDKFile(absoluteFile, deviceUrl.replace("*", host), vmName);
                    }
                    log.info("Completed Downloading the files");

                    vmdkFlag = true;

                    t.interrupt();

                    vimPort.httpNfcLeaseProgress(httpNfcLease, 100);
                    vimPort.httpNfcLeaseComplete(httpNfcLease);
                } else {
                    log.warn("HttpNfcLeaseState not ready");
                    for (Object o : result) {
                        log.warn("HttpNfcLeaseState: {}", o);
                    }
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void printHttpNfcLeaseInfo(HttpNfcLeaseInfo info, String hostName) {
        System.out.println("########################################################");
        System.out.println("HttpNfcLeaseInfo");
        System.out.println("Lease Timeout: " + info.getLeaseTimeout());
        System.out.println("Total Disk capacity: " + info.getTotalDiskCapacityInKB());

        List<HttpNfcLeaseDeviceUrl> deviceUrlList = info.getDeviceUrl();
        int deviceUrlCount = 1;

        for (HttpNfcLeaseDeviceUrl deviceUrl : deviceUrlList) {
            System.out.println("HttpNfcLeaseDeviceUrl : " + deviceUrlCount++);
            System.out.println("   Device URL Import Key: " + deviceUrl.getImportKey());
            System.out.println("   Device URL Key: " + deviceUrl.getKey());
            System.out.println("   Device URL : " + deviceUrl.getUrl());
            System.out.println("   Updated device URL: " + deviceUrl.getUrl().replace("*", hostName));
            System.out.println("   SSL Thumbprint : " + deviceUrl.getSslThumbprint());
        }
        System.out.println("########################################################");
    }

    public static class HttpNfcLeaseExtender implements Runnable {
        private ManagedObjectReference httpNfcLeaseMoRef;
        private VimPortType vimPort;

        public HttpNfcLeaseExtender(ManagedObjectReference httpNfcLeaseMoRef, VimPortType vimPort) {
            this.httpNfcLeaseMoRef = httpNfcLeaseMoRef;
            this.vimPort = vimPort;
        }

        @Override
        public void run() {
            try {
                log.info("Thread for Checking the HTTP NFCLEASE vmdkFlag: {}", vmdkFlag);
                while (!vmdkFlag) {
                    log.info("#### TOTAL_BYTES {}", totalBytes);
                    try {
                        vimPort.httpNfcLeaseProgress(httpNfcLeaseMoRef, 0);
                        Thread.sleep(290000000);
                    } catch (InterruptedException e) {
                        log.info("Thread interrupted");
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Exception: ", e);
            }
        }
    }

    private static void writeVMDKFile(String absoluteFile, String string, String vmName) throws IOException {
        HttpsURLConnection connection = getHTTPConnection(string);
        String fileName = localPath + "/" + vmName + "-" + absoluteFile;

        try (InputStream in = connection.getInputStream();
                OutputStream out = new FileOutputStream(fileName)) {
            byte[] buf = new byte[102400];
            int len = 0;
            long written = 0;

            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                written = written + len;
            }
            log.info("Exported File {}-{} : {}", vmName, absoluteFile, written);
        }
    }

    static HttpsURLConnection getHTTPConnection(String url) throws IOException {
        URL urlConnection = URI.create(url).toURL();
        HttpsURLConnection connection = (HttpsURLConnection) urlConnection.openConnection();

        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setAllowUserInteraction(true);
        connection.setSSLSocketFactory(createInsecureSocketFactory());
        connection.setHostnameVerifier(new InsecureHostnameVerifier());
        connection.connect();

        return connection;
    }
}
