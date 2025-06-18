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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.ssl.InsecureHostnameVerifier;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.FileTransferInformation;
import com.vmware.vim25.GuestProcessInfo;
import com.vmware.vim25.GuestProgramSpec;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NamePasswordAuthentication;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachinePowerState;

/**
 * This sample runs a specified program inside a virtual machine with output re-directed to a temporary file inside the
 * guest and downloads the output file. Since vSphere API 5.0.
 */
public class RunProgram {
    private static final Logger log = LoggerFactory.getLogger(RunProgram.class);
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

    /** REQUIRED: Fully qualified path of the program inside the guest. */
    public static String guestProgramPath = "guestProgramPath";
    /** REQUIRED: Path to the local file to store the output. */
    public static String localOutputFilePath = "localOutputFilePath";
    /** OPTIONAL: Run the program within an interactive session inside the guest. Default value is false. */
    public static Boolean interactiveSession = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(RunProgram.class, args);

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

            boolean useInteractiveSession = Boolean.TRUE.equals(interactiveSession);
            String[] opts;
            String[] opt;

            if (useInteractiveSession) {
                opts = new String[] {"guest.interactiveGuestOperationsReady"};
                opt = new String[] {"guest.interactiveGuestOperationsReady"};
            } else {
                opts = new String[] {"guest.guestOperationsReady"};
                opt = new String[] {"guest.guestOperationsReady"};
            }
            propertyCollectorHelper.awaitManagedObjectUpdates(vmMoRef, opts, opt, new Object[][] {new Object[] {true}});

            log.info("Guest Operations are ready for the VM");
            ManagedObjectReference guestOpManger = serviceContent.getGuestOperationsManager();

            Map<String, Object> guestOpMgr =
                    propertyCollectorHelper.fetchProperties(guestOpManger, "processManager", "fileManager");

            ManagedObjectReference fileManagerRef = (ManagedObjectReference) guestOpMgr.get("fileManager");
            ManagedObjectReference processManagerRef = (ManagedObjectReference) guestOpMgr.get("processManager");

            NamePasswordAuthentication auth = new NamePasswordAuthentication();
            auth.setUsername(guestUsername);
            auth.setPassword(guestPassword);
            auth.setInteractiveSession(useInteractiveSession);

            log.info("Executing CreateTemporaryFile guest operation");

            String tempFilePath = vimPort.createTemporaryFileInGuest(fileManagerRef, vmMoRef, auth, "", "", "");
            log.info("Successfully created a temporary file at: {} inside the guest", tempFilePath);

            GuestProgramSpec guestProgramSpec = new GuestProgramSpec();
            guestProgramSpec.setProgramPath(guestProgramPath);
            guestProgramSpec.setArguments("> " + tempFilePath + " 2>&1");

            log.info("Starting the specified program inside the guest");

            long pid = vimPort.startProgramInGuest(processManagerRef, vmMoRef, auth, guestProgramSpec);
            log.info("Process ID of the program started is: {}", pid);

            List<Long> pidsList = new ArrayList<>();
            pidsList.add(pid);

            List<GuestProcessInfo> procInfo = null;
            do {
                log.info("Waiting for the process to finish running.");
                procInfo = vimPort.listProcessesInGuest(processManagerRef, vmMoRef, auth, pidsList);
                Thread.sleep(5 * 1000);
            } while (procInfo.get(0).getEndTime() == null);

            log.info("Exit code of the program is {}", procInfo.get(0).getExitCode());

            FileTransferInformation fileTransferInformation = null;
            fileTransferInformation =
                    vimPort.initiateFileTransferFromGuest(fileManagerRef, vmMoRef, auth, tempFilePath);

            String fileDownloadUrl = fileTransferInformation.getUrl().replaceAll("\\*", serverAddress);

            log.info("Downloading the output file from :{}", fileDownloadUrl);

            getData(fileDownloadUrl, localOutputFilePath);
            log.info("Successfully downloaded the file");
        }
    }

    /** pulls data from a url and puts it to the fileName */
    private static void getData(String urlString, String fileName) throws IOException {
        HttpsURLConnection connection = null;

        URL urlSt = URI.create(urlString).toURL();

        connection = (HttpsURLConnection) urlSt.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setSSLSocketFactory(createInsecureSocketFactory());
        connection.setHostnameVerifier(new InsecureHostnameVerifier());
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        connection.setRequestMethod("GET");

        try (InputStream in = connection.getInputStream();
                OutputStream out = new FileOutputStream(fileName)) {
            byte[] buf = new byte[102400];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }

        int returnErrorCode = connection.getResponseCode();

        connection.disconnect();

        if (HttpsURLConnection.HTTP_OK != returnErrorCode) {
            throw new RunProgramException("File Download is unsuccessful");
        }
    }

    private static class RunProgramException extends RuntimeException {
        public RunProgramException(String message) {
            super(message);
        }
    }
}
