/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.compute.vm.guest;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.createInsecureSocketFactory;
import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.ssl.InsecureHostnameVerifier;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayOfByte;
import com.vmware.vim25.FileTransferInformation;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NamePasswordAuthentication;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachinePowerState;

/**
 * This sample downloads a file from the guest to a specified path on the host where the client is running. Since
 * vSphere API 5.0.
 */
public class DownloadGuestFile {
    private static final Logger log = LoggerFactory.getLogger(DownloadGuestFile.class);
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

    /** REQUIRED: Name of the virtual machine. */
    public static String vmName = "vmName";
    /** REQUIRED: Username in the guest. */
    public static String guestUsername = "guestUsername";
    /** REQUIRED: Password in the guest. */
    public static String guestPassword = "guestPassword";

    // TODO: not initialized anywhere
    private static ManagedObjectReference hostMoRef;

    /** REQUIRED: Path of the file in the guest. */
    public static String guestFilePath = "guestFilePath";
    /** REQUIRED: Local file path to download and store the file. */
    public static String localFilePath = "localFilePath";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(DownloadGuestFile.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference vmMoRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);
            VirtualMachinePowerState powerState;
            if (vmMoRef != null) {
                log.info("Virtual Machine {} found", vmName);
                powerState = propertyCollectorHelper.fetch(vmMoRef, "runtime.powerState");
                if (!powerState.equals(VirtualMachinePowerState.POWERED_ON)) {
                    log.error("VirtualMachine: {} needs to be powered on", vmName);
                    return;
                }
            } else {
                log.error("Virtual Machine {} not found.", vmName);
                return;
            }

            String[] opts = new String[] {"guest.guestOperationsReady"};
            String[] opt = new String[] {"guest.guestOperationsReady"};
            Object[] results = propertyCollectorHelper.awaitManagedObjectUpdates(
                    vmMoRef, opts, opt, new Object[][] {new Object[] {true}});

            log.info("Guest Operations are ready for the VM");
            ManagedObjectReference guestOpManger = serviceContent.getGuestOperationsManager();
            ManagedObjectReference fileManagerRef = propertyCollectorHelper.fetch(guestOpManger, "fileManager");

            NamePasswordAuthentication auth = new NamePasswordAuthentication();
            auth.setUsername(guestUsername);
            auth.setPassword(guestPassword);
            auth.setInteractiveSession(false);

            log.info("Executing DownloadFile guest operation");
            FileTransferInformation fileTransferInformation =
                    vimPort.initiateFileTransferFromGuest(fileManagerRef, vmMoRef, auth, guestFilePath);
            log.info("Downloading the file from : {}", fileTransferInformation.getUrl());

            if (hostMoRef != null) {
                opts = new String[] {"config.certificate"};
                opt = new String[] {"config.certificate"};
                results = propertyCollectorHelper.awaitManagedObjectUpdates(hostMoRef, opts, opt, null);

                List<Byte> certificate = ((ArrayOfByte) results[0]).getByte();
                byte[] certificateBytes = new byte[certificate.size()];
                int index = 0;
                for (Byte b : certificate) {
                    certificateBytes[index++] = b;
                }

                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate x509CertificateToTrust =
                        (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certificateBytes));
                log.info("Certificate of the host is successfully retrieved: {}", x509CertificateToTrust);
            }

            getData(fileTransferInformation.getUrl(), localFilePath);
            log.info("Successfully downloaded the file");
        }
    }

    private static void getData(String urlString, String fileName) throws IOException {
        HttpsURLConnection connection = null;

        URL url = URI.create(urlString).toURL();

        connection = (HttpsURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setSSLSocketFactory(createInsecureSocketFactory());
        connection.setHostnameVerifier(new InsecureHostnameVerifier());
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        connection.setRequestMethod("GET");

        try (InputStream in = connection.getInputStream();
                OutputStream out = new FileOutputStream(fileName)) {
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = in.read(buf)) >= 0) {
                out.write(buf, 0, len);
            }
        }

        int returnErrorCode = connection.getResponseCode();

        connection.disconnect();

        if (HttpsURLConnection.HTTP_OK != returnErrorCode) {
            throw new DownloadGuestFileException("File Download is unsuccessful");
        }
    }

    private static class DownloadGuestFileException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public DownloadGuestFileException(String message) {
            super(message);
        }
    }
}
