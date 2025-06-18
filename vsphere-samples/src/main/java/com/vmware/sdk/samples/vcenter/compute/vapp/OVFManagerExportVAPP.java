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
import static com.vmware.sdk.vsphere.utils.VsphereCookieHelper.VMWARE_SOAP_SESSION_COOKIE;
import static com.vmware.vim25.ManagedObjectType.HOST_SYSTEM;
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_APP;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.vmware.vim25.OvfCreateDescriptorParams;
import com.vmware.vim25.OvfCreateDescriptorResult;
import com.vmware.vim25.OvfFile;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/** This sample demonstrates OVFManager. Exports VMDKs and OVF Descriptor of all VM's in the vApps. */
public class OVFManagerExportVAPP {
    private static final Logger log = LoggerFactory.getLogger(OVFManagerExportVAPP.class);
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
    private static volatile HttpNfcLeaseExtender leaseExtender;
    private static volatile boolean vmdkFlag;

    /** REQUIRED: Name of the host system. */
    public static String host = "host";
    /** REQUIRED: Name of the vApp. */
    public static String vApp = "vApp";
    /** REQUIRED: Local System Folder path. */
    public static String localPath = "localPath";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(OVFManagerExportVAPP.class, args);

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

            ManagedObjectReference hostRef = propertyCollectorHelper.getMoRefByName(host, HOST_SYSTEM);
            if (hostRef == null) {
                log.error("Host Not Found");
            } else {
                ManagedObjectReference vAppMoRef = propertyCollectorHelper.getMoRefByName(vApp, VIRTUAL_APP);
                if (vAppMoRef != null) {
                    OvfCreateDescriptorParams ovfCreateDescriptorParams = new OvfCreateDescriptorParams();
                    ManagedObjectReference httpNfcLease = vimPort.exportVApp(vAppMoRef);
                    log.info("Getting the HTTP NFCLEASE for the vApp: {}", vApp);

                    Object[] result = propertyCollectorHelper.awaitManagedObjectUpdates(
                            httpNfcLease, new String[] {"state"}, new String[] {"state"}, new Object[][] {
                                new Object[] {HttpNfcLeaseState.READY, HttpNfcLeaseState.ERROR}
                            });

                    if (result[0].equals(HttpNfcLeaseState.READY)) {
                        HttpNfcLeaseInfo httpNfcLeaseInfo = propertyCollectorHelper.fetch(httpNfcLease, "info");

                        httpNfcLeaseInfo.setLeaseTimeout(300000000);
                        printHttpNfcLeaseInfo(httpNfcLeaseInfo, host);
                        long diskCapacity = (httpNfcLeaseInfo.getTotalDiskCapacityInKB()) * 1024;
                        log.info("************ Disk capacity: {}", diskCapacity);

                        totalBytes = diskCapacity;
                        leaseExtender = new HttpNfcLeaseExtender(httpNfcLease, vimPort);
                        Thread t = new Thread(leaseExtender);
                        t.start();

                        List<HttpNfcLeaseDeviceUrl> deviceUrlList = httpNfcLeaseInfo.getDeviceUrl();
                        if (deviceUrlList != null) {
                            List<OvfFile> ovfFiles = new ArrayList<>();
                            for (HttpNfcLeaseDeviceUrl httpNfcLeaseDeviceUrl : deviceUrlList) {
                                log.info("Downloading Files:");

                                String deviceId = httpNfcLeaseDeviceUrl.getKey();
                                String deviceUrl = httpNfcLeaseDeviceUrl.getUrl();
                                String absoluteFileName = deviceUrl.substring(deviceUrl.lastIndexOf("/") + 1);
                                log.info("Absolute File Name: {}", absoluteFileName);
                                log.info("VMDK URL: {}", deviceUrl.replace("*", host));

                                long writtenSize = writeVMDKFile(
                                        absoluteFileName,
                                        deviceUrl.replace("*", host),
                                        new String(
                                                client.getVimSessionProvider().get()));

                                OvfFile ovfFile = new OvfFile();
                                ovfFile.setPath(absoluteFileName);
                                ovfFile.setDeviceId(deviceId);
                                ovfFile.setSize(writtenSize);
                                ovfFiles.add(ovfFile);
                            }
                            ovfCreateDescriptorParams.getOvfFiles().addAll(ovfFiles);

                            OvfCreateDescriptorResult ovfCreateDescriptorResult = vimPort.createDescriptor(
                                    serviceContent.getOvfManager(), vAppMoRef, ovfCreateDescriptorParams);

                            String outOVF = localPath + "/" + vApp + ".ovf";
                            File outFile = new File(outOVF);

                            try (FileWriter out = new FileWriter(outFile, StandardCharsets.UTF_8)) {
                                out.write(ovfCreateDescriptorResult.getOvfDescriptor());
                            }
                            log.info("OVF Descriptor Written to file {}.ovf", vApp);
                            log.info("DONE");

                            if (!ovfCreateDescriptorResult.getError().isEmpty()) {
                                log.info("SOME ERRORS");
                            }

                            if (!ovfCreateDescriptorResult.getWarning().isEmpty()) {
                                log.info("SOME WARNINGS");
                            }
                        } else {
                            log.info("No Device URLs");
                        }

                        log.info("Completed Downloading the files");
                        vmdkFlag = true;

                        t.interrupt();

                        vimPort.httpNfcLeaseProgress(httpNfcLease, 100);
                        vimPort.httpNfcLeaseComplete(httpNfcLease);
                    } else {
                        log.warn("HttpNfcLeaseState not ready");
                        log.warn("HttpNfcLeaseState: {}", Arrays.toString(result));
                    }
                } else {
                    log.error("vApp Not Found");
                }
            }
        }
    }

    private static class HttpNfcLeaseExtender implements Runnable {
        private final ManagedObjectReference httpNfcLeaseMoRef;
        private final VimPortType vimPort;

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

    private static void printHttpNfcLeaseInfo(HttpNfcLeaseInfo info, String hostString) {
        System.out.println("########################################################");
        System.out.println("HttpNfcLeaseInfo");
        System.out.println("Lease Timeout: " + info.getLeaseTimeout());
        System.out.println("Total Disk capacity: " + info.getTotalDiskCapacityInKB());

        List<HttpNfcLeaseDeviceUrl> deviceUrlList = info.getDeviceUrl();
        if (deviceUrlList != null) {
            int deviceUrlCount = 1;
            for (HttpNfcLeaseDeviceUrl deviceUrl : deviceUrlList) {
                System.out.println("HttpNfcLeaseDeviceUrl : " + deviceUrlCount++);
                System.out.println("   Device URL Import Key: " + deviceUrl.getImportKey());
                System.out.println("   Device URL Key: " + deviceUrl.getKey());
                System.out.println("   Device URL : " + deviceUrl.getUrl());
                System.out.println(
                        "   Updated device URL: " + deviceUrl.getUrl().replace("*", hostString));
                System.out.println("   SSL Thumbprint : " + deviceUrl.getSslThumbprint());
            }
        } else {
            System.out.println("No Device URLs Found");
            System.out.println("########################################################");
        }
    }

    private static long writeVMDKFile(String absoluteFile, String url, String vimCookie) throws IOException {
        URL urlConnection = URI.create(url).toURL();

        HttpsURLConnection connection = (HttpsURLConnection) urlConnection.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setAllowUserInteraction(true);
        connection.setSSLSocketFactory(createInsecureSocketFactory());
        connection.setHostnameVerifier(new InsecureHostnameVerifier());
        connection.setRequestProperty("Cookie", new HttpCookie(VMWARE_SOAP_SESSION_COOKIE, vimCookie).toString());
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        connection.setRequestProperty("Expect", "100-continue");
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Length", "1024");

        String localpath = localPath + "/" + absoluteFile;
        try (InputStream in = connection.getInputStream();
                OutputStream out = new FileOutputStream(localpath)) {
            byte[] buf = new byte[102400];
            int len = 0;
            long written = 0;

            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                written = written + len;
            }
            log.info("Exported File {} : {}", absoluteFile, written);
            return written;
        }
    }
}
