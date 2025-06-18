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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import jakarta.xml.ws.soap.SOAPFaultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.ssl.InsecureHostnameVerifier;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.HttpNfcLeaseDeviceUrl;
import com.vmware.vim25.HttpNfcLeaseInfo;
import com.vmware.vim25.HttpNfcLeaseState;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.LocalizedMethodFault;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.OvfCreateImportSpecParams;
import com.vmware.vim25.OvfCreateImportSpecResult;
import com.vmware.vim25.OvfFileItem;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VimPortType;

/**
 * This sample demonstrates import or deploy an OVF Appliance from the specified URL.
 *
 * <p>Due to some issue with Jax WS deserialization, "HttpNfcLeaseState" is deserialized as an XML Element and the Value
 * is returned in the ObjectContent as the First Child of Node
 * ObjectContent[0]-&gt;ChangeSet-&gt;ElementData[0]-&gt;val-&gt;firstChild so correct value of HttpNfcLeaseState must
 * be extracted from firstChild node.
 */
public class OVFManagerImportVAppFromUrl {
    private static final Logger log = LoggerFactory.getLogger(OVFManagerImportVAppFromUrl.class);

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

    /** OPTIONAL: Name of the datastore to be used. */
    public static String datastore = null;
    /** REQUIRED: Name of the host system. */
    public static String host = "host";
    /** REQUIRED: OVF File URL Path. */
    public static String urlPath = "file://package.ovf";
    /** REQUIRED: New vApp Name */
    public static String vappName = "VAppName";

    private static boolean vmdkFlag = false;
    private static volatile long totalBytes = 0;
    private static volatile long totalBytesWritten = 0;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(OVFManagerImportVAppFromUrl.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);
            String vimCookie = new String(client.getVimSessionProvider().get());

            ManagedObjectReference hostMoRef = propertyCollectorHelper.getMoRefByName(host, HOST_SYSTEM);
            if (hostMoRef == null) {
                throw new RuntimeException("Host System " + host + " Not Found.");
            }

            Map<String, Object> hostProps = propertyCollectorHelper.fetchProperties(hostMoRef, "datastore", "parent");
            List<ManagedObjectReference> datastoresList =
                    ((ArrayOfManagedObjectReference) hostProps.get("datastore")).getManagedObjectReference();
            if (datastoresList.isEmpty()) {
                throw new RuntimeException("No datastores accessible from host " + host);
            }

            ManagedObjectReference datastoreMoRef = null;
            if (datastore == null) {
                datastoreMoRef = datastoresList.get(0);
            } else {
                for (ManagedObjectReference dsMoRef : datastoresList) {
                    if (datastore.equalsIgnoreCase(propertyCollectorHelper.fetch(dsMoRef, "name"))) {
                        datastoreMoRef = dsMoRef;
                        break;
                    }
                }
            }

            if (datastoreMoRef == null) {
                if (datastore != null) {
                    throw new RuntimeException(
                            "No datastore by name " + datastore + " is accessible from host " + host);
                }
                throw new RuntimeException("No datastores accessible from host " + host);
            }

            ManagedObjectReference resourcePoolMoRef =
                    propertyCollectorHelper.fetch((ManagedObjectReference) hostProps.get("parent"), "resourcePool");

            ManagedObjectReference datacenterMoRef = getDatacenterOfDatastore(vimPort, serviceContent, datastoreMoRef);
            ManagedObjectReference vmFolder = propertyCollectorHelper.fetch(datacenterMoRef, "vmFolder");

            OvfCreateImportSpecParams importSpecParams = createImportSpecParams(hostMoRef, vappName);

            String ovfDescriptor = getOvfDescriptorFromUrl(vimCookie, urlPath);
            if (ovfDescriptor == null || ovfDescriptor.isEmpty()) {
                return;
            }

            OvfCreateImportSpecResult ovfImportResult = vimPort.createImportSpec(
                    serviceContent.getOvfManager(), ovfDescriptor, resourcePoolMoRef, datastoreMoRef, importSpecParams);
            if (ovfImportResult.getError() == null || ovfImportResult.getError().isEmpty()) {
                List<OvfFileItem> ovfFileItems = ovfImportResult.getFileItem();
                if (ovfFileItems != null) {
                    for (OvfFileItem fileItem : ovfFileItems) {
                        printOvfFileItem(fileItem);
                        totalBytes += fileItem.getSize();
                    }
                }
                log.info("Total bytes: {}", totalBytes);

                ManagedObjectReference httpNfcLeaseMoRef =
                        vimPort.importVApp(resourcePoolMoRef, ovfImportResult.getImportSpec(), vmFolder, hostMoRef);
                Object[] result = propertyCollectorHelper.awaitManagedObjectUpdates(
                        httpNfcLeaseMoRef, new String[] {"state"}, new String[] {"state"}, new Object[][] {
                            new Object[] {HttpNfcLeaseState.READY, HttpNfcLeaseState.ERROR}
                        });

                if (result[0].equals(HttpNfcLeaseState.READY)) {
                    log.info("HttpNfcLeaseState: {}", result[0]);

                    HttpNfcLeaseInfo httpNfcLeaseInfo = propertyCollectorHelper.fetch(httpNfcLeaseMoRef, "info");

                    printHttpNfcLeaseInfo(httpNfcLeaseInfo);

                    HttpNfcLeaseExtender leaseExtender = new HttpNfcLeaseExtender(httpNfcLeaseMoRef, vimPort);

                    Thread t = new Thread(leaseExtender);
                    t.start();

                    List<HttpNfcLeaseDeviceUrl> deviceUrlList = httpNfcLeaseInfo.getDeviceUrl();
                    for (HttpNfcLeaseDeviceUrl deviceUrl : deviceUrlList) {
                        String deviceKey = deviceUrl.getImportKey();

                        for (OvfFileItem ovfFileItem : ovfFileItems) {
                            if (deviceKey.equals(ovfFileItem.getDeviceId())) {
                                log.info("Import key: {}", deviceKey);
                                log.info("OvfFileItem device id: {}", ovfFileItem.getDeviceId());
                                log.info("HTTP Post file: {}", ovfFileItem.getPath());

                                String absoluteFilePath = urlPath.substring(0, urlPath.lastIndexOf("/"));
                                absoluteFilePath = absoluteFilePath + "/" + ovfFileItem.getPath();
                                log.info("Absolute path: {}", absoluteFilePath);

                                getVMDKFile(
                                        vimCookie,
                                        ovfFileItem.isCreate(),
                                        absoluteFilePath,
                                        deviceUrl.getUrl().replace("*", host),
                                        ovfFileItem.getSize());
                                log.info("Completed uploading the VMDK file");
                            }
                        }
                    }
                    vmdkFlag = true;

                    t.interrupt();

                    vimPort.httpNfcLeaseProgress(httpNfcLeaseMoRef, 100);
                    vimPort.httpNfcLeaseComplete(httpNfcLeaseMoRef);
                } else {
                    log.warn("HttpNfcLeaseState not ready");
                    for (Object o : result) {
                        log.warn("HttpNfcLeaseState: {}", o);
                    }
                }
            } else {
                log.error("Cannot import the vApp because of following:");
                for (LocalizedMethodFault fault : ovfImportResult.getError()) {
                    log.error("Message: {}", fault.getLocalizedMessage());
                }
            }
        }
    }

    private static class HttpNfcLeaseExtender implements Runnable {
        private ManagedObjectReference httpNfcLeaseMoRef;
        private VimPortType vimPort;
        private int progressPercent = 0;

        public HttpNfcLeaseExtender(ManagedObjectReference httpNfcLeaseMoRef, VimPortType vimPort) {
            this.httpNfcLeaseMoRef = httpNfcLeaseMoRef;
            this.vimPort = vimPort;
        }

        @Override
        public void run() {
            try {
                while (!vmdkFlag) {
                    log.info("Thread started with vmdk flag: {}\n\n", vmdkFlag);
                    if (totalBytes != 0) {
                        progressPercent = (int) ((totalBytesWritten * 100) / (totalBytes));
                    }
                    try {
                        vimPort.httpNfcLeaseProgress(httpNfcLeaseMoRef, progressPercent);
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        log.info("Task completed");
                        break;
                    } catch (SOAPFaultException e) {
                        printSoapFaultException(e);
                        break;
                    }
                }
            } catch (SOAPFaultException e) {
                printSoapFaultException(e);
            } catch (Exception e) {
                log.error("Exception: ", e);
            }
        }
    }

    /** @return An array of SelectionSpec to navigate from the Datastore and move upwards to reach the Datacenter */
    private static List<SelectionSpec> buildTraversalSpecForDatastoreToDatacenter() {
        // For Folder -> Folder recursion
        SelectionSpec selectionSpecVisitFolders = new SelectionSpec();
        selectionSpecVisitFolders.setName("VisitFolders");

        TraversalSpec visitFolders = new TraversalSpec();
        visitFolders.setType("Folder");
        visitFolders.setPath("parent");
        visitFolders.setSkip(Boolean.FALSE);
        visitFolders.setName("VisitFolders");
        visitFolders.getSelectSet().add(selectionSpecVisitFolders);

        TraversalSpec datastoreToFolder = new TraversalSpec();
        datastoreToFolder.setType("Datastore");
        datastoreToFolder.setPath("parent");
        datastoreToFolder.setSkip(Boolean.FALSE);
        datastoreToFolder.setName("DatastoreToFolder");
        datastoreToFolder.getSelectSet().add(selectionSpecVisitFolders);

        List<SelectionSpec> speclist = new ArrayList<>();
        speclist.add(datastoreToFolder);
        speclist.add(visitFolders);
        return speclist;
    }

    /**
     * Method to retrieve the Datacenter under which the specified datastore resides.
     *
     * @param datastore {@link ManagedObjectReference} of the datastore
     * @return {@link ManagedObjectReference} of the datacenter that contains the datastore
     */
    private static ManagedObjectReference getDatacenterOfDatastore(
            VimPortType vimPort, ServiceContent serviceContent, ManagedObjectReference datastore)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        PropertySpec propertySpec = new PropertySpec();
        propertySpec.setAll(Boolean.FALSE);
        propertySpec.setType("Datacenter");
        propertySpec.getPathSet().add("name");

        ObjectSpec objectSpec = new ObjectSpec();
        objectSpec.setObj(datastore);
        objectSpec.setSkip(Boolean.TRUE);
        objectSpec.getSelectSet().addAll(buildTraversalSpecForDatastoreToDatacenter());

        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
        propertyFilterSpec.getPropSet().add(propertySpec);
        propertyFilterSpec.getObjectSet().add(objectSpec);

        List<PropertyFilterSpec> propertyFilterSpecs = new ArrayList<>();
        propertyFilterSpecs.add(propertyFilterSpec);

        List<ObjectContent> objectContents = vimPort.retrievePropertiesEx(
                        serviceContent.getPropertyCollector(), propertyFilterSpecs, new RetrieveOptions())
                .getObjects();

        ManagedObjectReference datacenterMoRef = null;

        if (objectContents != null) {
            for (ObjectContent objectContent : objectContents) {
                datacenterMoRef = objectContent.getObj();
                break;
            }
        }
        return datacenterMoRef;
    }

    private static OvfCreateImportSpecParams createImportSpecParams(
            ManagedObjectReference hostMoRef, String newVmName) {
        OvfCreateImportSpecParams importSpecParams = new OvfCreateImportSpecParams();
        importSpecParams.setHostSystem(hostMoRef);
        importSpecParams.setLocale("");
        importSpecParams.setEntityName(newVmName);
        importSpecParams.setDeploymentOption("");

        return importSpecParams;
    }

    private static String getOvfDescriptorFromUrl(String vimCookie, String ovfDescriptorUrl) throws IOException {
        StringBuilder strContent = new StringBuilder();
        int x;

        HttpsURLConnection connection = getHTTPConnection(vimCookie, ovfDescriptorUrl);

        InputStream inputStream = connection.getInputStream();
        while ((x = inputStream.read()) != -1) {
            strContent.append((char) x);
        }
        return String.valueOf(strContent);
    }

    private static HttpsURLConnection getHTTPConnection(String vimCookie, String urlString) throws IOException {
        URL url = URI.create(urlString).toURL();

        HttpsURLConnection httpConnection = (HttpsURLConnection) url.openConnection();
        httpConnection.setDoInput(true);
        httpConnection.setDoOutput(true);
        httpConnection.setAllowUserInteraction(true);
        httpConnection.setSSLSocketFactory(createInsecureSocketFactory());
        httpConnection.setHostnameVerifier(new InsecureHostnameVerifier());
        httpConnection.setRequestProperty("Cookie", new HttpCookie(VMWARE_SOAP_SESSION_COOKIE, vimCookie).toString());
        httpConnection.connect();

        return httpConnection;
    }

    private static void printOvfFileItem(OvfFileItem ovfFileItem) {
        System.out.println("##########################################################");
        System.out.println("OvfFileItem");
        System.out.println("chunkSize: " + ovfFileItem.getChunkSize());
        System.out.println("create: " + ovfFileItem.isCreate());
        System.out.println("deviceId: " + ovfFileItem.getDeviceId());
        System.out.println("path: " + ovfFileItem.getPath());
        System.out.println("size: " + ovfFileItem.getSize());
        System.out.println("##########################################################");
    }

    private static void printHttpNfcLeaseInfo(HttpNfcLeaseInfo info) {
        System.out.println("########################################################");
        System.out.println("HttpNfcLeaseInfo");

        List<HttpNfcLeaseDeviceUrl> deviceUrlList = info.getDeviceUrl();
        for (HttpNfcLeaseDeviceUrl deviceUrl : deviceUrlList) {
            System.out.println("Device URL Import Key: " + deviceUrl.getImportKey());
            System.out.println("Device URL Key: " + deviceUrl.getKey());
            System.out.println("Device URL : " + deviceUrl.getUrl());
            System.out.println("Updated device URL: " + deviceUrl.getUrl().replace("*", "10.20.140.58"));
        }
        System.out.println("Lease Timeout: " + info.getLeaseTimeout());
        System.out.println("Total Disk capacity: " + info.getTotalDiskCapacityInKB());
        System.out.println("########################################################");
    }

    private static void getVMDKFile(
            String vimCookie, boolean put, String readFileLocation, String writeFileLocation, long diskCapacity) {
        HttpsURLConnection writeConnection = null;
        BufferedOutputStream writeBufferedOutputStream = null;

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 64 * 1024;

        try {
            log.info("Destination host URL: {}", writeFileLocation);

            HostnameVerifier hostnameVerifier = (urlHostName, session) -> {
                log.warn("URL Host: {} vs. {}", urlHostName, session.getPeerHost());
                return true;
            };

            HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);

            URL url = URI.create(writeFileLocation).toURL();
            writeConnection = (HttpsURLConnection) url.openConnection();
            writeConnection.setSSLSocketFactory(createInsecureSocketFactory());
            writeConnection.setHostnameVerifier(new InsecureHostnameVerifier());
            writeConnection.setDoInput(true);
            writeConnection.setDoOutput(true);
            writeConnection.setUseCaches(false);
            writeConnection.setChunkedStreamingMode(maxBufferSize);

            if (put) {
                writeConnection.setRequestMethod("PUT");
                log.info("HTTP method: PUT");
            } else {
                writeConnection.setRequestMethod("POST");
                log.info("HTTP method: POST");
            }

            writeConnection.setRequestProperty(
                    "Cookie", new HttpCookie(VMWARE_SOAP_SESSION_COOKIE, vimCookie).toString());
            writeConnection.setRequestProperty("Connection", "Keep-Alive");
            writeConnection.setRequestProperty("Content-Type", "application/x-vnd.vmware-streamVmdk");
            writeConnection.setRequestProperty("Content-Length", String.valueOf(diskCapacity));

            writeBufferedOutputStream = new BufferedOutputStream(writeConnection.getOutputStream());
            log.info("Local file path: {}", readFileLocation);

            HttpsURLConnection readConnection = getHTTPConnection(vimCookie, readFileLocation);

            InputStream readInputStream = readConnection.getInputStream();
            BufferedInputStream readBufferedInputStream = new BufferedInputStream(readInputStream);
            bytesAvailable = readBufferedInputStream.available();
            log.info("vmdk available bytes: {}", bytesAvailable);

            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            bytesRead = readBufferedInputStream.read(buffer, 0, bufferSize);
            long bytesWrote = bytesRead;
            totalBytesWritten += bytesRead;

            while (bytesRead >= 0) {
                writeBufferedOutputStream.write(buffer, 0, bufferSize);
                writeBufferedOutputStream.flush();
                log.info("Bytes Written: {}", bytesWrote);

                bytesAvailable = readBufferedInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesWrote += bufferSize;
                log.info("Total bytes written: {}", totalBytesWritten);

                totalBytesWritten += bufferSize;
                buffer = null;
                buffer = new byte[bufferSize];
                bytesRead = readBufferedInputStream.read(buffer, 0, bufferSize);
                log.info("Bytes Read: {}", bytesRead);

                if ((bytesRead == 0) && (bytesWrote >= diskCapacity)) {
                    log.info("Total bytes written: {}", totalBytesWritten);
                    bytesRead = -1;
                }
            }
            try {
                DataInputStream dataInputStream = new DataInputStream(writeConnection.getInputStream());
                dataInputStream.close();
            } catch (IOException e) {
                log.error("From (ServerResponse): ", e);
            }
            log.info("Writing vmdk to the output stream done");
            readBufferedInputStream.close();
        } catch (IOException e) {
            throw new CheckedExceptionWrapper(e);
        } finally {
            try {
                writeBufferedOutputStream.flush();
                writeBufferedOutputStream.close();
                writeConnection.disconnect();
            } catch (SOAPFaultException e) {
                printSoapFaultException(e);
            } catch (Exception e) {
                log.error("Exception: ", e);
            }
        }
    }

    private static void printSoapFaultException(SOAPFaultException e) {
        log.error("SOAP Fault -");
        if (e.getFault().hasDetail()) {
            log.error("Detail: {}", e.getFault().getDetail().getFirstChild().getLocalName());
        }
        if (e.getFault().getFaultString() != null) {
            log.error("Message: {}", e.getFault().getFaultString());
        }
    }

    private static class CheckedExceptionWrapper extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public CheckedExceptionWrapper(Throwable cause) {
            super(cause);
        }
    }
}
