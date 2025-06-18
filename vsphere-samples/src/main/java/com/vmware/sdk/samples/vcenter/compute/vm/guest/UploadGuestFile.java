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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.GregorianCalendar;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.datatype.DatatypeFactory;

import jakarta.xml.ws.soap.SOAPFaultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.ssl.InsecureHostnameVerifier;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayOfByte;
import com.vmware.vim25.GuestFileAttributes;
import com.vmware.vim25.GuestPosixFileAttributes;
import com.vmware.vim25.GuestWindowsFileAttributes;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NamePasswordAuthentication;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachinePowerState;

/**
 * This sample uploads a file from the client machine to a specified location inside the guest. Since vSphere API 5.0.
 */
public class UploadGuestFile {
    private static final Logger log = LoggerFactory.getLogger(UploadGuestFile.class);
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

    /** OPTIONAL: Whether to overwrite the file in the guest. Default value is false. */
    public static Boolean overwrite = null;

    /** REQUIRED: Path of the file in the guest. */
    public static String guestFilePath = "guestFilePath";
    /** REQUIRED: Local file path to upload. */
    public static String localFilePath = "localFilePath";
    /** REQUIRED: Type of the guest. (windows or posix). */
    public static String guestType = "guestType";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(UploadGuestFile.class, args);

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
                    log.info("VirtualMachine: {} needs to be powered on", vmName);
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

            GuestFileAttributes guestFileAttributes = null;
            if (guestType.equalsIgnoreCase("windows")) {
                guestFileAttributes = new GuestWindowsFileAttributes();
            } else {
                guestFileAttributes = new GuestPosixFileAttributes();
            }

            guestFileAttributes.setAccessTime(
                    DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));

            guestFileAttributes.setModificationTime(
                    DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
            log.info("Executing UploadGuestFile guest operation");

            File file = new File(localFilePath);
            if (!file.exists()) {
                log.error("Error finding the file: {}", localFilePath);
                return;
            }

            if (file.isDirectory()) {
                log.error("Local file path points to a directory");
                return;
            }

            long fileSize = file.length();
            log.info("Size of the file is : {}", fileSize);
            log.info("Executing UploadFile guest operation");
            String fileUploadUrl;
            try {
                fileUploadUrl = vimPort.initiateFileTransferToGuest(
                        fileManagerRef,
                        vmMoRef,
                        auth,
                        guestFilePath,
                        guestFileAttributes,
                        fileSize,
                        Boolean.TRUE.equals(overwrite));
            } catch (SOAPFaultException e) {
                log.error("SoapException {}", e.getMessage());

                if (e.getMessage().contains(guestFilePath.replaceAll("//", "/") + " already exists")) {
                    log.info("To overwrite the File use --overwrite option");
                }
                return;
            }

            log.info("Uploading the file to : {}", fileUploadUrl);

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
                log.info(
                        "Certificate of the host is successfully retrieved, type {}", x509CertificateToTrust.getType());
            }

            uploadData(fileUploadUrl, localFilePath);
            log.info("Successfully uploaded the file");
        }
    }

    // TODO: both hostMOR and fileSize are not initialized anywhere
    private static ManagedObjectReference hostMoRef;

    private static void uploadData(String urlString, String fileName) throws IOException {
        HttpsURLConnection connection = null;

        URL urlSt = URI.create(urlString).toURL();

        connection = (HttpsURLConnection) urlSt.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setSSLSocketFactory(createInsecureSocketFactory());
        connection.setHostnameVerifier(new InsecureHostnameVerifier());
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        connection.setRequestMethod("PUT");
        long fileSize = new File(fileName).length();
        connection.setRequestProperty("Content-Length", Long.toString(fileSize));

        try (OutputStream out = connection.getOutputStream();
                InputStream in = new FileInputStream(fileName); ) {
            byte[] buf = new byte[102400];
            int len = 0;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }

        int returnErrorCode = connection.getResponseCode();

        connection.disconnect();

        if (HttpsURLConnection.HTTP_OK != returnErrorCode) {
            throw new UploadException("File Upload is unsuccessful");
        }
    }

    private static class UploadException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public UploadException(String message) {
            super(message);
        }
    }
}
