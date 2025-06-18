/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.misc.general;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.vim25.ManagedObjectType.DATACENTER;

import jakarta.xml.ws.soap.SOAPFaultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/** This sample demonstrates the SearchIndex API. */
public class SearchIndex {
    private static final Logger log = LoggerFactory.getLogger(SearchIndex.class);
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

    /** REQUIRED: Name of the datacenter. */
    public static String dcName = "dcName";
    /** OPTIONAL: DNS of a virtual machine. */
    public static String vmDnsName = null;
    /** OPTIONAL: Inventory path of a virtual machine. */
    public static String vmPath = null;
    /** OPTIONAL: DNS of the ESX host. */
    public static String hostDnsName = null;
    /** OPTIONAL: IP Address of a virtual machine. */
    public static String vmIP = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(SearchIndex.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            ManagedObjectReference datacenterMoRef = propertyCollectorHelper.getMoRefByName(dcName, DATACENTER);

            if (datacenterMoRef != null) {
                log.info("Found Datacenter with name: {}, MoRef: {}", dcName, datacenterMoRef.getValue());
            } else {
                log.info("Datacenter not Found with name: {}", dcName);
                return;
            }

            if (vmDnsName != null) {
                ManagedObjectReference vmMoRef = null;
                try {
                    vmMoRef = vimPort.findByDnsName(serviceContent.getSearchIndex(), datacenterMoRef, vmDnsName, true);
                } catch (SOAPFaultException sfe) {
                    printSoapFaultException(sfe);
                } catch (RuntimeFaultFaultMsg ex) {
                    log.error("Error Encountered: ", ex);
                }

                if (vmMoRef != null) {
                    log.info("Found VirtualMachine with DNS name: {}, MoRef: {}", vmDnsName, vmMoRef.getValue());
                } else {
                    log.info("VirtualMachine not Found with DNS name: {}", vmDnsName);
                }
            }

            if (vmPath != null) {
                ManagedObjectReference vmMoRef = null;
                try {
                    vmMoRef = vimPort.findByInventoryPath(serviceContent.getSearchIndex(), vmPath);
                } catch (SOAPFaultException sfe) {
                    printSoapFaultException(sfe);
                } catch (RuntimeFaultFaultMsg ex) {
                    log.error("Error Encountered:", ex);
                }

                if (vmMoRef != null) {
                    log.info("Found VirtualMachine with Path: {}, MoRef: {}", vmPath, vmMoRef.getValue());

                } else {
                    log.info("VirtualMachine not found with vmPath address: {}", vmPath);
                }
            }

            if (vmIP != null) {
                ManagedObjectReference vmMoRef = null;
                try {
                    vmMoRef = vimPort.findByIp(serviceContent.getSearchIndex(), datacenterMoRef, vmIP, true);
                } catch (SOAPFaultException sfe) {
                    printSoapFaultException(sfe);
                } catch (RuntimeFaultFaultMsg ex) {
                    log.error("Error Encountered:", ex);
                }

                if (vmMoRef != null) {
                    log.info("Found VirtualMachine with IP address {}, MoRef: {}", vmIP, vmMoRef.getValue());
                } else {
                    log.info("VirtualMachine not found with IP address: {}", vmIP);
                }
            }

            if (hostDnsName != null) {
                ManagedObjectReference hostMoRef = null;
                try {
                    hostMoRef = vimPort.findByDnsName(serviceContent.getSearchIndex(), null, hostDnsName, false);
                } catch (SOAPFaultException sfe) {
                    printSoapFaultException(sfe);
                } catch (RuntimeFaultFaultMsg ex) {
                    log.error("Error Encountered:", ex);
                }

                if (hostMoRef != null) {
                    log.info("Found HostSystem with DNS name {}, MoRef: {}", hostDnsName, hostMoRef.getValue());
                } else {
                    log.info("HostSystem not Found with DNS name:{}", hostDnsName);
                }
            }
        }
    }

    private static void printSoapFaultException(SOAPFaultException sfe) {
        log.error("SOAP Fault -");
        if (sfe.getFault().hasDetail()) {
            log.error("Detail: {}", sfe.getFault().getDetail().getFirstChild().getLocalName());
        }
        if (sfe.getFault().getFaultString() != null) {
            log.error("Message: {}", sfe.getFault().getFaultString());
        }
    }
}
