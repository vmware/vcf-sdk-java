/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.host;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.HOST_SYSTEM;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.HostServiceTicket;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/**
 * This sample will acquire a session with VC or ESX and print a cim service ticket and related session information to a
 * file.
 */
public class AcquireSessionInfo {
    private static final Logger log = LoggerFactory.getLogger(AcquireSessionInfo.class);
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

    /** REQUIRED: Name of the host. */
    public static String hostname = "hostname";
    /** REQUIRED: Type of info required only [cimticket] for now. */
    public static String info = "info";
    /** OPTIONAL: Full path of the file to save data to. */
    public static String filename = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(AcquireSessionInfo.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference hostMoRef = propertyCollectorHelper.getMoRefByName(hostname, HOST_SYSTEM);
            if (hostMoRef == null) {
                String msg = "Failure: Host [ " + hostname + "] not found";
                throw new HostFailure(msg);
            }

            if ((info == null) || (info.equalsIgnoreCase("cimticket"))) {
                HostServiceTicket serviceTicket = vimPort.acquireCimServicesTicket(hostMoRef);

                if (serviceTicket != null) {
                    String dataToWrite = stringToWrite(serviceTicket);
                    writeToFile(dataToWrite, filename);
                }
            } else {
                log.info("Support for {} not implemented.", info);
            }
        }
    }

    private static String stringToWrite(HostServiceTicket serviceTicket) {
        String sslThumbprint = "undefined";
        String host = "undefined";
        String port = "undefined";

        String service = serviceTicket.getService();
        String serviceVersion = serviceTicket.getServiceVersion();
        String serviceSessionId = serviceTicket.getSessionId();

        if (serviceTicket.getSslThumbprint() != null) {
            sslThumbprint = serviceTicket.getSslThumbprint();
        }

        if (serviceTicket.getHost() != null) {
            host = serviceTicket.getHost();
        }

        if (serviceTicket.getPort() != null) {
            port = Integer.toString(serviceTicket.getPort());
        }

        String dataToWrite = "CIM Host Service Ticket Information\n"
                + "Service        : "
                + service
                + "\n"
                + "Service Version: "
                + serviceVersion
                + "\n"
                + "Session Id     : "
                + serviceSessionId
                + "\n"
                + "SSL Thumbprint : "
                + sslThumbprint
                + "\n"
                + "Host           : "
                + host
                + "\n"
                + "Port           : "
                + port
                + "\n";
        System.out.println("CIM Host Service Ticket Information\n");
        System.out.println("Service           : " + service);
        System.out.println("Service Version   : " + serviceVersion);
        System.out.println("Session Id        : " + serviceSessionId);
        System.out.println("SSL Thumbprint    : " + sslThumbprint);
        System.out.println("Host              : " + host);
        System.out.println("Port              : " + port);
        return dataToWrite;
    }

    private static void writeToFile(String data, String fileName) throws IOException {
        fileName = fileName == null ? "cimTicketInfo.txt" : fileName;

        File cimFile = new File(fileName);
        if (cimFile.exists()) {
            try (FileOutputStream fop = new FileOutputStream(cimFile)) {
                fop.write(data.getBytes(StandardCharsets.UTF_8));
                log.info("Saved session information at {}", cimFile.getAbsolutePath());
            }
        }
    }

    private static class HostFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public HostFailure(String msg) {
            super(msg);
        }
    }
}
