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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.KeyAnyValue;
import com.vmware.vim25.KeyValue;
import com.vmware.vim25.LicenseAssignmentManagerLicenseAssignment;
import com.vmware.vim25.LicenseEntityNotFoundFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/** Demonstrates uses of the Licensing API using License Manager Reference. */
public class LicenseManager {
    private static final Logger log = LoggerFactory.getLogger(LicenseManager.class);
    /** REQUIRED: vCenter FQDN or IP address. */
    public static String serverAddress = "vcenter1.mycompany.com";

    public static String url = "url";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";
    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (do not leave it empty on production environments).
     */
    public static String trustStorePath = null;

    /** REQUIRED: Action to be performed [browse|setserver|featureinfo]. */
    public static String action = "action";
    /** OPTIONAL: Licensed feature e.g. vMotion. */
    public static String feature = null;
    /** OPTIONAL: License key for KL servers. */
    public static String licenseKey = null;

    private static ManagedObjectReference licManagerRef = null;
    private static ManagedObjectReference licenseAssignmentManagerRef = null;
    private static List<LicenseAssignmentManagerLicenseAssignment> licenses;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(LicenseManager.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            licManagerRef = serviceContent.getLicenseManager();
            licenseAssignmentManagerRef = propertyCollectorHelper.fetch(licManagerRef, "licenseAssignmentManager");
            licenses = vimPort.queryAssignedLicenses(licenseAssignmentManagerRef, null);

            useLicenseManager(vimPort, serviceContent);
        }
    }

    private static void useLicenseManager(VimPortType vimPort, ServiceContent serviceContent)
            throws RuntimeFaultFaultMsg, LicenseEntityNotFoundFaultMsg {
        if (action.equalsIgnoreCase("browse")) {
            System.out.println("Display the license usage. "
                    + "It gives details of license features " + "like license key "
                    + " edition key and entity id.");
            displayLicenseUsage();
        } else if (action.equalsIgnoreCase("setkey")) {
            System.out.println("Set the license key.");
            setLicenseKey(vimPort, serviceContent);
        } else if (action.equalsIgnoreCase("featureinfo")) {
            if (feature != null) {
                displayFeatureInfo();
            } else {
                throw new IllegalArgumentException("Expected --feature argument.");
            }
        } else {
            System.out.println("Invalid Action ");
            System.out.println("Valid Actions [browse|setserver|featureinfo]");
        }
    }

    private static void displayLicenseUsage() {
        print(licenses);
    }

    private static void setLicenseKey(VimPortType vimPort, ServiceContent serviceContent)
            throws RuntimeFaultFaultMsg, LicenseEntityNotFoundFaultMsg {
        boolean flag = true;
        if (licenseKey == null) {
            log.error("For KL servers licensekey is a mandatory option");
            flag = false;
        }
        if (flag) {
            String apiType = serviceContent.getAbout().getApiType();
            if (apiType.equalsIgnoreCase("VirtualCenter")) {
                String entity = serviceContent.getAbout().getInstanceUuid();

                vimPort.updateAssignedLicense(licenseAssignmentManagerRef, entity, licenseKey, null);
                log.info("License key set for VC server");
            } else if (apiType.equalsIgnoreCase("HostAgent")) {
                vimPort.decodeLicense(licManagerRef, licenseKey);
                vimPort.updateLicense(licManagerRef, licenseKey, null);
                log.info("License key set for ESX server");
            }
        }
    }

    private static void displayFeatureInfo() {
        String featureName = feature;
        boolean found = false;
        Map<String, List<KeyValue>> licenseFeatures = new HashMap<>();
        for (LicenseAssignmentManagerLicenseAssignment license : licenses) {
            if (license.getAssignedLicense() != null
                    && license.getAssignedLicense().getProperties() != null) {
                List<KeyValue> licFeatures = new ArrayList<>();
                for (KeyAnyValue property : license.getAssignedLicense().getProperties()) {

                    if (property.getKey().equalsIgnoreCase("feature")) {
                        KeyValue feature = (KeyValue) property.getValue();
                        if (feature != null) {
                            if (feature.getKey().equalsIgnoreCase(featureName)) {
                                found = true;
                                System.out.println("Entity Name: " + license.getEntityDisplayName());
                                System.out.println("License Name: "
                                        + license.getAssignedLicense().getName());
                                System.out.println("Feature Name: " + feature.getKey());
                                System.out.println("Description: " + feature.getValue());
                            }
                            licFeatures.add(feature);
                        }
                    }
                }
                licenseFeatures.put(license.getAssignedLicense().getName(), licFeatures);
            }
        }
        if (!found) {
            System.out.println("Could not find feature " + featureName);
            if (!licenseFeatures.keySet().isEmpty()) {
                System.out.println("Available features are: ");
                for (List<KeyValue> v : licenseFeatures.values()) {
                    for (KeyValue val : v) {
                        System.out.println(val.getKey() + " : " + val.getValue());
                    }
                }
            }
        }
    }

    private static void print(List<LicenseAssignmentManagerLicenseAssignment> licenseAssignment) {
        if (licenseAssignment != null) {
            for (LicenseAssignmentManagerLicenseAssignment la : licenseAssignment) {
                String entityId = la.getEntityId();
                String editionKey = la.getAssignedLicense().getEditionKey();
                String licenseKey = la.getAssignedLicense().getLicenseKey();
                String name = la.getAssignedLicense().getName();
                System.out.println("\nName of the license: " + name
                        + "\n License Key:  " + licenseKey + "\n Edition Key: "
                        + editionKey + "\n EntityID: " + entityId + "\n\n");
            }
        }
    }
}
