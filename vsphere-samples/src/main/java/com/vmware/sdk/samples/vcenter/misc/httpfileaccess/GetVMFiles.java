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
import static com.vmware.sdk.vsphere.utils.VsphereCookieHelper.VMWARE_SOAP_SESSION_COOKIE;
import static com.vmware.vim25.ManagedObjectType.VIRTUAL_MACHINE;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.ssl.InsecureHostnameVerifier;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceFileBackingInfo;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualMachineConfigInfo;

/**
 * This sample gets all the config files, snapshots files, logs files, virtual disk files to the local system.
 *
 * <p>Use with{@link PutVMFiles}
 */
public class GetVMFiles {
    private static final Logger log = LoggerFactory.getLogger(GetVMFiles.class);
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
    /** REQUIRED: Local path to copy files into. */
    public static String localPath = "localPath";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(GetVMFiles.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            String vimCookie = new String(client.getVimSessionProvider().get());

            getVM(vimPort, vimCookie, serviceContent, propertyCollectorHelper);
        }
    }

    /** @return A list of SelectionSpec to navigate from the VM and move upwards to reach the Datacenter. */
    private static List<SelectionSpec> buildTraversalSpecForVMToDatacenter() {
        // For Folder -> Folder recursion
        SelectionSpec visitFoldersSpec = new SelectionSpec();
        visitFoldersSpec.setName("VisitFolders");

        TraversalSpec visitFolders = new TraversalSpec();
        visitFolders.setType("Folder");
        visitFolders.setPath("parent");
        visitFolders.setSkip(Boolean.FALSE);
        visitFolders.setName("VisitFolders");
        visitFolders.getSelectSet().add(visitFoldersSpec);

        // For vApp -> vApp recursion
        SelectionSpec vAppToVappSpec = new SelectionSpec();
        vAppToVappSpec.setName("vAppToVApp");

        SelectionSpec vAppToFolderSpec = new SelectionSpec();
        vAppToFolderSpec.setName("vAppToFolder");

        TraversalSpec vAppToFolder = new TraversalSpec();
        vAppToFolder.setType("VirtualApp");
        vAppToFolder.setPath("parentFolder");
        vAppToFolder.setSkip(Boolean.FALSE);
        vAppToFolder.setName("vAppToFolder");
        vAppToFolder.getSelectSet().add(visitFoldersSpec);

        TraversalSpec vAppToVApp = new TraversalSpec();
        vAppToVApp.setType("VirtualApp");
        vAppToVApp.setPath("parentVApp");
        vAppToVApp.setSkip(Boolean.FALSE);
        vAppToVApp.setName("vAppToVApp");
        vAppToVApp.getSelectSet().add(vAppToVappSpec);
        vAppToVApp.getSelectSet().add(vAppToFolderSpec);

        TraversalSpec vmTovApp = new TraversalSpec();
        vmTovApp.setType("VirtualMachine");
        vmTovApp.setPath("parentVApp");
        vmTovApp.setSkip(Boolean.FALSE);
        vmTovApp.setName("vmTovApp");
        vmTovApp.getSelectSet().add(vAppToVApp);
        vmTovApp.getSelectSet().add(vAppToFolder);

        TraversalSpec vmToFolder = new TraversalSpec();
        vmToFolder.setType("VirtualMachine");
        vmToFolder.setPath("parent");
        vmToFolder.setSkip(Boolean.FALSE);
        vmToFolder.setName("vmToFolder");
        vmToFolder.getSelectSet().add(visitFoldersSpec);

        List<SelectionSpec> speclist = new ArrayList<>();
        speclist.add(vmToFolder);
        speclist.add(vmTovApp);
        speclist.add(visitFolders);
        return speclist;
    }

    /**
     * Method to retrieve the Datacenter under which the specified VM resides.
     *
     * @param vmMoRef {@link ManagedObjectReference} of the VM
     * @return {@link String} name of the datacenter that contains the VM
     */
    private static String getDatacenterOfVM(
            VimPortType vimPort, ServiceContent serviceContent, ManagedObjectReference vmMoRef)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        String datacenterName = "";

        PropertySpec propertySpec = new PropertySpec();
        propertySpec.setAll(Boolean.FALSE);
        propertySpec.setType("Datacenter");
        propertySpec.getPathSet().add("name");

        ObjectSpec objectSpec = new ObjectSpec();
        objectSpec.setObj(vmMoRef);
        objectSpec.setSkip(Boolean.TRUE);
        objectSpec.getSelectSet().addAll(buildTraversalSpecForVMToDatacenter());

        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
        propertyFilterSpec.getPropSet().add(propertySpec);
        propertyFilterSpec.getObjectSet().add(objectSpec);

        List<PropertyFilterSpec> propertyFilterSpecs = new ArrayList<>();
        propertyFilterSpecs.add(propertyFilterSpec);

        RetrieveOptions options = new RetrieveOptions();

        RetrieveResult results =
                vimPort.retrievePropertiesEx(serviceContent.getPropertyCollector(), propertyFilterSpecs, options);

        List<ObjectContent> objectContents = results.getObjects();

        if (objectContents != null) {
            for (ObjectContent oc : objectContents) {
                List<DynamicProperty> dynamicProperties = oc.getPropSet();
                if (dynamicProperties != null) {
                    for (DynamicProperty dp : dynamicProperties) {
                        datacenterName = (String) dp.getVal();
                    }
                }
                log.info("VM is present under {} Datacenter", datacenterName);
                break;
            }
        }
        return datacenterName;
    }

    private static void getVM(
            VimPortType vimPort,
            String vimCookie,
            ServiceContent serviceContent,
            PropertyCollectorHelper propertyCollectorHelper)
            throws IllegalArgumentException, RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, IOException {
        File file = new File(localPath);
        if (!file.exists()) {
            log.error("Wrong or invalid path {}", localPath);
            return;
        }

        ManagedObjectReference vmMoRef = propertyCollectorHelper.getMoRefByName(vmName, VIRTUAL_MACHINE);
        if (vmMoRef != null) {
            log.info("vmMoRef: {}", vmMoRef.getValue());

            getDiskSizeInKB(propertyCollectorHelper, vmMoRef);

            String dataCenterName = getDatacenterOfVM(vimPort, serviceContent, vmMoRef);
            String[] vmDirectory = getVmDirectory(propertyCollectorHelper, vmMoRef);

            Map<String, String> downloadedDir = new HashMap<>();
            if (vmDirectory[0] != null) {
                log.info("vmDirectory-0: {} datacenter as : {}", vmDirectory[0], dataCenterName);
                log.info("Downloading Virtual Machine Configuration Directory");
                String dataStoreName =
                        vmDirectory[0].substring(vmDirectory[0].indexOf("[") + 1, vmDirectory[0].lastIndexOf("]"));
                String configurationDir =
                        vmDirectory[0].substring(vmDirectory[0].indexOf("]") + 2, vmDirectory[0].lastIndexOf("/"));

                boolean success = new File(localPath + "/" + configurationDir + "#vm#" + dataStoreName).mkdir();
                if (!success) {
                    log.info("Could not create {}/{}#vm#{}directory", localPath, configurationDir, dataStoreName);
                }

                downloadDirectory(
                        vimCookie,
                        configurationDir,
                        configurationDir + "#vm#" + dataStoreName,
                        dataStoreName,
                        dataCenterName);

                downloadedDir.put(configurationDir + "#vm#" + dataStoreName, "Directory");
                log.info("Downloading Virtual Machine Configuration Directory Complete");
            }

            if (vmDirectory[1] != null) {
                log.info("Downloading Virtual Machine Snapshot / Suspend / Log Directory");
                for (int i = 1; i < vmDirectory.length; i++) {
                    String dataStoreName =
                            vmDirectory[i].substring(vmDirectory[i].indexOf("[") + 1, vmDirectory[i].lastIndexOf("]"));
                    String configurationDir = "";
                    String apiType = serviceContent.getAbout().getApiType();
                    if (apiType.equalsIgnoreCase("VirtualCenter")) {
                        configurationDir =
                                vmDirectory[i].substring(vmDirectory[i].indexOf("]") + 2, vmDirectory[i].length() - 1);
                    } else {
                        configurationDir = vmDirectory[i].substring(vmDirectory[i].indexOf("]") + 2);
                    }
                    if (!downloadedDir.containsKey(configurationDir + "#vm#" + dataStoreName)) {
                        boolean success = new File(localPath + "/" + configurationDir + "#vm#" + dataStoreName).mkdir();
                        if (!success) {
                            log.info(
                                    "Could not create {}/{}#vm#{}directory",
                                    localPath,
                                    configurationDir,
                                    dataStoreName);
                        }

                        downloadDirectory(
                                vimCookie,
                                configurationDir,
                                configurationDir + "#vm#" + dataStoreName,
                                dataStoreName,
                                dataCenterName);

                        downloadedDir.put(configurationDir + "#vm#" + dataStoreName, "Directory");
                    } else {
                        log.info("Already Downloaded");
                    }
                }
                log.info("Downloading Virtual Machine Snapshot / Suspend / Log Directory Complete");
            }

            String[] virtualDiskLocations = getVDiskLocations(propertyCollectorHelper, vmMoRef);
            if (virtualDiskLocations != null) {
                log.info("Downloading Virtual Disks");
                for (String virtualDiskLocation : virtualDiskLocations) {
                    if (virtualDiskLocation != null) {
                        String dataStoreName = virtualDiskLocation.substring(
                                virtualDiskLocation.indexOf("[") + 1, virtualDiskLocation.lastIndexOf("]"));
                        String configurationDir = virtualDiskLocation.substring(
                                virtualDiskLocation.indexOf("]") + 2, virtualDiskLocation.lastIndexOf("/"));
                        if (!downloadedDir.containsKey(configurationDir + "#vm#" + dataStoreName)) {
                            boolean success =
                                    new File(localPath + "/" + configurationDir + "#vdisk#" + dataStoreName).mkdir();
                            if (!success) {
                                log.info(
                                        "Could not create {}/{}#vdisk#{}directory",
                                        localPath,
                                        configurationDir,
                                        dataStoreName);
                            }

                            downloadDirectory(
                                    vimCookie,
                                    configurationDir,
                                    configurationDir + "#vdisk#" + dataStoreName,
                                    dataStoreName,
                                    dataCenterName);

                            downloadedDir.put(configurationDir + "#vdisk#" + dataStoreName, "Directory");
                        } else {
                            log.info("Already Downloaded");
                        }
                    } else {
                        log.info("Already Downloaded");
                    }
                }
                log.info("Downloading Virtual Disks Complete");
            } else {
                log.info("Downloading Virtual Disks Complete");
            }
        } else {
            throw new IllegalArgumentException("Virtual Machine " + vmName + " Not Found.");
        }
    }

    private static String[] getVmDirectory(
            PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference vmMoRef)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        String[] vmDirectory = new String[4];

        VirtualMachineConfigInfo vmConfigInfo = propertyCollectorHelper.fetch(vmMoRef, "config");
        if (vmConfigInfo != null) {
            vmDirectory[0] = vmConfigInfo.getFiles().getVmPathName();
            vmDirectory[1] = vmConfigInfo.getFiles().getSnapshotDirectory();
            vmDirectory[2] = vmConfigInfo.getFiles().getSuspendDirectory();
            vmDirectory[3] = vmConfigInfo.getFiles().getLogDirectory();
        } else {
            log.info("Cannot Restore VM. Not able to find the virtual machine config info");
        }
        return vmDirectory;
    }

    private static void getDiskSizeInKB(PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference vmMoRef)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        VirtualMachineConfigInfo vmConfigInfo = propertyCollectorHelper.fetch(vmMoRef, "config");
        if (vmConfigInfo != null) {
            List<VirtualDevice> livd = vmConfigInfo.getHardware().getDevice();
            for (VirtualDevice virtualDevice : livd) {
                if (virtualDevice instanceof VirtualDisk) {
                    log.info("Disk size in kb: {}", ((VirtualDisk) virtualDevice).getCapacityInKB());
                }
            }
        }
    }

    private static String[] getVDiskLocations(
            PropertyCollectorHelper propertyCollectorHelper, ManagedObjectReference vmMoRef)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        VirtualMachineConfigInfo vmConfigInfo = propertyCollectorHelper.fetch(vmMoRef, "config");
        log.info("vmconfig info : {}", vmConfigInfo);

        if (vmConfigInfo != null) {
            List<VirtualDevice> virtualDevices = vmConfigInfo.getHardware().getDevice();
            VirtualDevice[] vDevice = virtualDevices.toArray(new VirtualDevice[virtualDevices.size()]);
            int count = 0;
            String[] virtualDisk = new String[vDevice.length];

            for (VirtualDevice virtualDevice : vDevice) {
                if (virtualDevice.getClass().getCanonicalName().equalsIgnoreCase("com.vmware.vim25.VirtualDisk")) {
                    try {
                        long size = ((VirtualDisk) virtualDevice).getCapacityInKB();
                        log.info("Disk size in kb: {}", size);
                        VirtualDeviceFileBackingInfo backingInfo =
                                (VirtualDeviceFileBackingInfo) virtualDevice.getBacking();
                        virtualDisk[count] = backingInfo.getFileName();
                        log.info("virtualDisk : {}", virtualDisk[count]);
                        count++;
                    } catch (ClassCastException e) {
                        log.error("Got Exception :", e);
                    }
                }
            }
            return virtualDisk;
        } else {
            log.info("Cannot Restore VM. Not able to find the virtual machine config info");
            return null;
        }
    }

    private static void downloadDirectory(
            String vimCookie, String directoryName, String localDirectory, String dataStoreName, String dataCenter)
            throws IOException {
        String httpUrl = String.format(
                "https://%s/folder/%s?dcPath=%s&dsName=%s",
                serverAddress,
                URLEncoder.encode(directoryName, StandardCharsets.UTF_8),
                URLEncoder.encode(dataCenter, StandardCharsets.UTF_8),
                URLEncoder.encode(dataStoreName, StandardCharsets.UTF_8));

        httpUrl = httpUrl.replaceAll("\\ ", "%20");
        log.info("httpUrl : {}", httpUrl);

        String[] linkMap = getListFiles(vimCookie, httpUrl);
        for (String s : linkMap) {
            log.info("Downloading VM File {}", s);
            String urlString = "https://" + serverAddress + s;
            String fileName = localDirectory + "/" + s.substring(s.lastIndexOf("/"), s.lastIndexOf("?"));

            urlString = urlString.replaceAll("\\ ", "%20");

            getData(urlString, vimCookie, fileName);
        }
    }

    @SuppressWarnings("unchecked")
    private static String[] getListFiles(String vimCookie, String urlString) throws IOException {
        HttpsURLConnection connection = null;
        URL urlSt = URI.create(urlString).toURL();

        connection = (HttpsURLConnection) urlSt.openConnection();
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

        StringBuilder xmlStringBuilder = new StringBuilder();
        try (BufferedReader in =
                new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                xmlStringBuilder.append(line);
            }
        }

        String xmlString = xmlStringBuilder.toString();
        xmlString = xmlString.replaceAll("&amp;", "&");
        xmlString = xmlString.replaceAll("%2e", ".");
        xmlString = xmlString.replaceAll("%2d", "-");
        xmlString = xmlString.replaceAll("%5f", "_");

        ArrayList<String> list = getFileLinks(xmlString);
        String[] linkMap = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            linkMap[i] = list.get(i);
        }
        return linkMap;
    }

    private static ArrayList<String> getFileLinks(String xmlString) {
        ArrayList<String> linkMap = new ArrayList<>();
        Pattern regex = Pattern.compile("href=\"(.*?)\"");
        Matcher regexMatcher = regex.matcher(xmlString);

        while (regexMatcher.find()) {
            String data = regexMatcher.group(1);
            log.info("fileLinks data : {}", data);
            linkMap.add(data);
        }
        return linkMap;
    }

    @SuppressWarnings("unchecked")
    private static void getData(String urlString, String vimCookie, String fileName) throws IOException {
        HttpsURLConnection connection = null;
        URL urlSt = URI.create(urlString).toURL();

        connection = (HttpsURLConnection) urlSt.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setSSLSocketFactory(createInsecureSocketFactory());
        connection.setHostnameVerifier(new InsecureHostnameVerifier());
        connection.setAllowUserInteraction(true);
        connection.setRequestProperty("Cookie", new HttpCookie(VMWARE_SOAP_SESSION_COOKIE, vimCookie).toString());
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        connection.setRequestProperty("Expect", "100-continue");
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Length", "1024");

        InputStream in = connection.getInputStream();
        int leng = fileName.lastIndexOf("/");
        String dir = fileName.substring(0, leng - 1);
        String fName = fileName.substring(leng + 1);
        fName = fName.replace("%20", " ");
        dir = replaceSpecialChar(dir);
        fileName = localPath + "/" + dir + "/" + fName;

        OutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));
        int bufLen = 9 * 1024;
        byte[] buf = new byte[bufLen];
        byte[] tmp = null;
        int len = 0;
        @SuppressWarnings("unused")
        int bytesRead = 0;

        while ((len = in.read(buf, 0, bufLen)) != -1) {
            bytesRead += len;
            tmp = new byte[len];
            System.arraycopy(buf, 0, tmp, 0, len);
            out.write(tmp, 0, len);
        }
        in.close();
        out.close();
    }

    private static String replaceSpecialChar(String fileName) {
        fileName = fileName.replace(':', '_');
        fileName = fileName.replace('*', '_');
        fileName = fileName.replace('<', '_');
        fileName = fileName.replace('>', '_');
        fileName = fileName.replace('|', '_');
        return fileName;
    }
}
