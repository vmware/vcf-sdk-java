/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.misc.httpfileaccess;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.createInsecureSocketFactory;
import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.sdk.vsphere.utils.VimClient.getVimServiceInstanceRef;
import static com.vmware.sdk.vsphere.utils.VsphereCookieHelper.VMWARE_SOAP_SESSION_COOKIE;
import static com.vmware.vim25.ManagedObjectType.DATACENTER;
import static com.vmware.vim25.ManagedObjectType.DATASTORE;
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.helpers.KeepAlive;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.ssl.InsecureHostnameVerifier;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.AlreadyExistsFaultMsg;
import com.vmware.vim25.ArrayOfDatastoreHostMount;
import com.vmware.vim25.DatastoreHostMount;
import com.vmware.vim25.DuplicateNameFaultMsg;
import com.vmware.vim25.FileFaultFaultMsg;
import com.vmware.vim25.HostMountInfo;
import com.vmware.vim25.InsufficientResourcesFaultFaultMsg;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidDatastoreFaultMsg;
import com.vmware.vim25.InvalidNameFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NotFoundFaultMsg;
import com.vmware.vim25.OutOfBoundsFaultMsg;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VmConfigFaultFaultMsg;

/**
 * This sample puts VM files in specified Datacenter and Datastore and register and reconfigure the particular VM. The
 * VM you use, should be downloaded from the vSphere you are uploading to. The name of the VM, VM folder, and VM disk
 * files should all be the same. The name of the VM should be unique and unused on the Host. This works best if you use
 * a VM you obtained through{@link GetVMFiles}
 */
public class PutVMFiles {
    private static final Logger log = LoggerFactory.getLogger(PutVMFiles.class);
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

    /** OPTIONAL: Defaults to 'true' and prints more information, set to 'false' to print less. */
    public static Boolean verbose = null;
    /**
     * REQUIRED: Name of the virtual machine to upload. Should be unique and unused. Should be the same as the name of
     * the vm folder and vm file names.
     */
    public static String vmName = "vmName";
    /**
     * REQUIRED: Local path from which files will be copied. This should be the path holding the virtual machine folder
     * but not the vm folder itself.
     */
    public static String localPath = "localPath";
    /** REQUIRED: Name of the target datacenter. */
    public static String datacenterName = "datacenterName";
    /** REQUIRED: Name of the target datastore. */
    public static String datastoreName = "datastoreName";

    private static ManagedObjectReference registeredVMRef = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(PutVMFiles.class, args);

        VcenterClientFactory factory = new VcenterClientFactory(
                serverAddress, 443, 120_000, 120_000, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);
            String vimCookie = new String(client.getVimSessionProvider().get());

            putVMFiles(vimCookie, vimPort, propertyCollectorHelper);
        } catch (PutVMFilesException cme) {
            log.error("PutVMFilesException:", cme);
        }
    }

    private static boolean customValidation(PropertyCollectorHelper propertyCollectorHelper) {
        boolean validate = false;

        try {
            if (datacenterName != null
                    && !datacenterName.isEmpty()
                    && datastoreName != null
                    && !datastoreName.isEmpty()) {
                ManagedObjectReference datacenterMoRef =
                        propertyCollectorHelper.getMoRefByName(datacenterName, DATACENTER);
                if (datacenterMoRef != null) {
                    ManagedObjectReference datastoreMoRef =
                            propertyCollectorHelper.getMoRefByName(datacenterMoRef, datastoreName, DATASTORE);
                    if (datastoreMoRef == null) {
                        log.error(
                                "Specified Datastore with name {} was not found in specified Datacenter",
                                datastoreName);
                        return validate;
                    }
                    validate = true;
                } else {
                    log.error("Specified Datacenter with name {} not Found", datacenterName);
                    return validate;
                }
            }
        } catch (RuntimeFaultFaultMsg | InvalidPropertyFaultMsg runtimeFaultFaultMsg) {
            throw new PutVMFilesException(runtimeFaultFaultMsg);
        }

        return validate;
    }

    /**
     * Lists out the subdirectories and files under the localDir you specified.
     *
     * @param dir - place on the file system to look
     * @return - list of files under that location
     */
    private static String[] getDirFiles(File dir) {
        if (dir.exists() && dir.isDirectory()) {
            return dir.list();
        } else {
            throw new RuntimeException("Local Path Doesn't Exist: " + dir);
        }
    }

    @SuppressWarnings("unchecked")
    private static void putVMFiles(String vimCookie, VimPortType vimPort, String remoteFilePath, File localFile) {
        String httpUrl = String.format(
                "https://%s/folder/%s?dcPath=%s&dsName=%s",
                serverAddress,
                remoteFilePath,
                URLEncoder.encode(datacenterName, StandardCharsets.UTF_8),
                URLEncoder.encode(datastoreName, StandardCharsets.UTF_8));
        log.info("Putting VM File {} ", httpUrl);

        URL fileURL;
        HttpsURLConnection connection;
        try {
            fileURL = URI.create(httpUrl).toURL();

            connection = (HttpsURLConnection) fileURL.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setSSLSocketFactory(createInsecureSocketFactory());
            connection.setHostnameVerifier(new InsecureHostnameVerifier());
            connection.setAllowUserInteraction(true);
        } catch (IOException e) {
            throw new PutVMFilesException(e);
        }

        connection.setRequestProperty("Cookie", new HttpCookie(VMWARE_SOAP_SESSION_COOKIE, vimCookie).toString());
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        try {
            connection.setRequestMethod("PUT");
        } catch (ProtocolException e) {
            throw new PutVMFilesException(e);
        }
        connection.setRequestProperty("Content-Length", "1024");
        long fileLen = localFile.length();
        System.out.println("File size is: " + fileLen);

        // setChunkedStreamingMode to -1 turns off chunked mode
        // setChunkedStreamingMode to 0 asks for system default
        // NOTE:
        // larger values mean faster connections at the
        // expense of more heap consumption.
        connection.setChunkedStreamingMode(0);

        OutputStream out = null;
        InputStream in = null;
        try {
            out = connection.getOutputStream();
            in = new BufferedInputStream(new FileInputStream(localFile));
            int bufLen = 9 * 1024;
            byte[] buf = new byte[bufLen];
            byte[] tmp = null;
            int len = 0;
            // this can take a very long time, so we do a keep-alive here.
            Thread keepAlive = KeepAlive.keepAlive(vimPort, getVimServiceInstanceRef());
            keepAlive.start();
            final String[] spinner = new String[] {"\u0008/", "\u0008-", "\u0008\\", "\u0008|"};
            System.out.print(".");
            int i = 0;
            while ((len = in.read(buf, 0, bufLen)) != -1) {
                tmp = new byte[len];
                System.arraycopy(buf, 0, tmp, 0, len);
                out.write(tmp, 0, len);
                if (!Boolean.FALSE.equals(verbose)) {
                    System.out.printf("%s", spinner[i++ % spinner.length]);
                }
            }
            System.out.print("\u0008");
            keepAlive.interrupt();
        } catch (IOException e) {
            throw new PutVMFilesException(e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                connection.getResponseCode();
            } catch (IOException e) {
                throw new PutVMFilesException(e);
            }
            connection.disconnect();
        }
    }

    /** Copy contents of this directory up to the datastore. */
    private static void copyDir(String vimCookie, VimPortType vimPort, String dirName) {
        log.info("Copying The Virtual Machine To Host...");
        File dir = new File(localPath, dirName);

        String[] listOfFiles = getDirFiles(dir);
        for (String listOfFile : listOfFiles) {
            String remoteFilePath;
            File localFile = new File(dir, listOfFile);
            if (localFile.getAbsolutePath().contains("vdisk")) {
                remoteFilePath = vmName + "/" + datastoreName + "/" + listOfFile;
            } else {
                remoteFilePath = vmName + "/" + listOfFile;
            }

            putVMFiles(vimCookie, vimPort, remoteFilePath, localFile);

            if (!Boolean.FALSE.equals(verbose)) {
                System.out.print("*");
            }
        }
        log.info("...Done.");
    }

    /** Register the vmx (virtual machine file) we just placed. */
    private static boolean registerVirtualMachine(VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, OutOfBoundsFaultMsg, DuplicateNameFaultMsg,
                    NotFoundFaultMsg, VmConfigFaultFaultMsg, InsufficientResourcesFaultFaultMsg, AlreadyExistsFaultMsg,
                    InvalidDatastoreFaultMsg, FileFaultFaultMsg, InvalidStateFaultMsg, InvalidNameFaultMsg,
                    InvalidCollectorVersionFaultMsg {
        boolean registered = false;
        System.out.print("Registering The Virtual Machine ...");
        ManagedObjectReference hostMoRef = null;
        // Get the Datacenter
        final ManagedObjectReference datacenterMoRef =
                propertyCollectorHelper.getMoRefByName(datacenterName, DATACENTER);

        // Get the Datastore
        final ManagedObjectReference datastoreMoRef =
                propertyCollectorHelper.getMoRefByName(datacenterMoRef, datastoreName, DATASTORE);

        final List<DatastoreHostMount> hostmounts = ((ArrayOfDatastoreHostMount)
                        propertyCollectorHelper.fetch(datastoreMoRef, "host"))
                .getDatastoreHostMount();

        for (DatastoreHostMount datastoreHostMount : hostmounts) {
            if (datastoreHostMount == null) {
                throw new PutVMFilesException("datastore " + datastoreName + " has no host mounts!");
            }
            HostMountInfo mountInfo = datastoreHostMount.getMountInfo();
            if (mountInfo == null) {
                throw new PutVMFilesException("datastoreHostMount on " + datastoreName + " has no info!");
            }

            final Boolean accessible = mountInfo.isAccessible();
            // the values "accessible" and "mounted" need not be set by the server.
            final Boolean mounted = mountInfo.isMounted();
            // if mounted is not set, assume it is true
            if ((accessible != null && accessible) && (mounted == null || mounted)) {
                hostMoRef = datastoreHostMount.getKey();
                break;
            }
            if (!Boolean.FALSE.equals(verbose)) {
                System.out.print(".");
            }
        }
        if (hostMoRef == null) {
            throw new PutVMFilesException("No host connected to the datastore " + datastoreName);
        }

        final ManagedObjectReference computeResourceMoRef = propertyCollectorHelper.fetch(hostMoRef, "parent");

        final ManagedObjectReference resourcePoolMoRef =
                propertyCollectorHelper.fetch(computeResourceMoRef, "resourcePool");

        final ManagedObjectReference vmFolderMoRef = propertyCollectorHelper.fetch(datacenterMoRef, "vmFolder");

        // Get The vmx path
        final String vmxPath = "[" + datastoreName + "] " + vmName + "/" + vmName + ".vmx";

        log.info("...trying to register: {} ...", vmxPath);
        // Registering The Virtual machine
        final ManagedObjectReference taskMoRef =
                vimPort.registerVMTask(vmFolderMoRef, vmxPath, vmName, false, resourcePoolMoRef, hostMoRef);

        if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
            System.out.print("*");
            registered = true;

            registeredVMRef = propertyCollectorHelper.fetch(taskMoRef, "info.result");
            System.out.print("VM registered with value " + registeredVMRef.getValue());
            log.info("...Done.");
        } else {
            System.out.print("Some Exception While Registering The VM");
            registered = false;
            log.error("FAILED!");
        }

        return registered;
    }

    /** Reconfigure the virtual machine we placed on the datastore. */
    private static void reconfigVirtualMachine(VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper) {
        try {
            log.info("ReConfigure The Virtual Machine ..........");

            VirtualMachineFileInfo vmFileInfo = new VirtualMachineFileInfo();
            vmFileInfo.setLogDirectory("[" + datastoreName + "] " + vmName);
            vmFileInfo.setSnapshotDirectory("[" + datastoreName + "] " + vmName);
            vmFileInfo.setSuspendDirectory("[" + datastoreName + "] " + vmName);
            vmFileInfo.setVmPathName("[" + datastoreName + "] " + vmName + "/" + vmName + ".vmx");

            VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
            vmConfigSpec.setFiles(vmFileInfo);

            ManagedObjectReference taskMoRef = vimPort.reconfigVMTask(registeredVMRef, vmConfigSpec);

            if (propertyCollectorHelper.awaitTaskCompletion(taskMoRef)) {
                log.info("ReConfigure The Virtual Machine .......... Done");
            } else {
                log.error("Some Exception While Reconfiguring The VM");
            }
        } catch (Exception e) {
            throw new PutVMFilesException(e);
        }
    }

    /** Put files onto remote datastore. */
    private static void putVMFiles(
            String vimCookie, VimPortType vimPort, PropertyCollectorHelper propertyCollectorHelper)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        boolean validated = customValidation(propertyCollectorHelper);

        if (propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE) != null) {
            throw new PutVMFilesException(String.format("A VM with the name %s already exists!", vmName));
        }

        if (validated) {
            int found = 0;

            String[] listOfDir = getDirFiles(new File(localPath));
            if (listOfDir != null) {
                // Dumping All The Data
                for (String s : listOfDir) {
                    if (!validateDir(s, localPath)) {
                        continue;
                    }

                    // made it here, we found something to upload
                    found++;

                    // go ahead and copy this up to the server
                    copyDir(vimCookie, vimPort, s);

                    // Register The Virtual Machine
                    boolean reconFlag = false;
                    try {
                        reconFlag = registerVirtualMachine(vimPort, propertyCollectorHelper);
                        // Reconfigure the disks
                        if (reconFlag) {
                            reconfigVirtualMachine(vimPort, propertyCollectorHelper);
                        }
                    } catch (Exception e) {
                        throw new PutVMFilesException(e);
                    }
                }
            }
            if (found == 0) {
                log.warn(
                        "There are no suitable VM Directories available at location {}. Did you use GetVMFiles first?",
                        localPath);
            }
        }
    }

    /**
     * Checks a directory name against rules.
     *
     * @param directoryName - directory to examine
     * @return true if usable, false if not
     */
    private static boolean validateDir(String directoryName, String localPath) {
        // short-circuit this method if no name set
        if (directoryName == null) {
            return false;
        }

        // using data-structure to avoid repeated calls
        int message = 0;
        final String[] messages = {
            "",
            String.format(
                    "The directory %s does not contain a matching %s.vmx file to register.%n", directoryName, vmName),
            String.format("Skipping: %s is a hidden name", directoryName),
            String.format("Skipping: %s is not a directory.", directoryName),
            String.format("Skipping: Name %s does not contain the --vmname %s", directoryName, vmName),
        };

        message =
                (!new File(new File(localPath, directoryName), String.format("%s.vmx", vmName)).exists()) ? 1 : message;
        message = (directoryName.startsWith(".")) ? 2 : message;
        message = (!new File(localPath, directoryName).isDirectory()) ? 3 : message;
        message = (!directoryName.contains(vmName)) ? 4 : message;

        if (!Boolean.FALSE.equals(verbose)) {
            System.out.println(messages[message]);
        }

        return message == 0;
    }

    /** For exceptions thrown internal to this sample only. Specifically for internal error handling. */
    private static class PutVMFilesException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public PutVMFilesException(final String message) {
            super(message);
        }

        public PutVMFilesException(final Throwable throwable) {
            super(throwable);
        }
    }
}
