/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.storage.sms;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static com.vmware.sdk.vsphere.utils.VcenterClient.getSmsServiceInstanceRef;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim.sms.CertificateNotTrusted;
import com.vmware.vim.sms.SmsPortType;
import com.vmware.vim.sms.SmsTaskInfo;
import com.vmware.vim.sms.VasaProviderInfo;
import com.vmware.vim.sms.VasaProviderSpec;
import com.vmware.vim25.LocalizedMethodFault;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.MethodFault;

/**
 * This sample demonstrates registration, listing and deregistration of a VASA provider.
 *
 * <p>Sample requirements: access to an active VASA provider.
 */
public class SampleProvider {
    private static final Logger log = LoggerFactory.getLogger(SampleProvider.class);

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

    /** REQUIRED: Username for authenticating with the provider. */
    public static String provUsername = "provUsername";
    /** REQUIRED: Password for authenticating with the provider */
    public static String provPassword = "provPassword";
    /** REQUIRED: URL of the VASA provider web service. */
    public static String provUrl = "provUrl";

    private static ManagedObjectReference storageMgr;
    private static ManagedObjectReference provider;

    private static final int MAX_ATTEMPTS = 100;
    private static final int SLEEP_INTERVAL = 1000;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(SampleProvider.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {

            SmsPortType smsPort = client.getSmsPort();

            storageMgr = smsPort.queryStorageManager(getSmsServiceInstanceRef());

            log.info("Querying registered providers...");
            listProviders(smsPort);

            VasaProviderSpec spec = new VasaProviderSpec();
            spec.setName("VasaProvider");
            spec.setUrl(provUrl);
            spec.setUsername(provUsername);
            spec.setPassword(provPassword);

            log.info("Registering provider...");
            registerProvider(smsPort, spec);

            log.info("Querying registered providers...");
            listProviders(smsPort);

            log.info("Unregistering provider...");
            unregisterProvider(smsPort);

            log.info("Querying registered providers...");
            listProviders(smsPort);
        }
    }

    private static class RegistrationResult {
        private ManagedObjectReference provider;
        private MethodFault fault;

        public void setProvider(ManagedObjectReference provider) {
            this.provider = provider;
        }

        public ManagedObjectReference getProvider() {
            return provider;
        }

        public void setFault(MethodFault fault) {
            this.fault = fault;
        }

        public MethodFault getFault() {
            return fault;
        }
    }

    private static void registerProvider(SmsPortType smsPort, VasaProviderSpec spec) throws Exception {
        while (true) {
            RegistrationResult result = registerProviderAndAwait(smsPort, spec);
            provider = result.getProvider();
            if (provider != null) {
                break;
            }

            MethodFault f = result.getFault();
            if (f instanceof CertificateNotTrusted) {
                spec.setCertificate(((CertificateNotTrusted) f).getCertificate());
                continue;
            }

            throw new RuntimeException("Registration failed");
        }
    }

    private static RegistrationResult registerProviderAndAwait(SmsPortType smsPort, VasaProviderSpec spec)
            throws Exception {
        ManagedObjectReference regTaskMoRef = smsPort.registerProviderTask(storageMgr, spec);

        SmsTaskInfo taskInfo = waitForTask(smsPort, regTaskMoRef);

        RegistrationResult result = new RegistrationResult();
        if (taskInfo != null) {
            if (taskInfo.getState().equals("error")) {
                LocalizedMethodFault f = taskInfo.getError();
                result.setFault(f.getFault());
                return result;
            } else if (taskInfo.getState().equals("success")) {
                result.setProvider((ManagedObjectReference) smsPort.querySmsTaskResult(regTaskMoRef));
                return result;
            }
        }

        throw new RuntimeException("Register timed out");
    }

    private static void listProviders(SmsPortType smsPort) throws Exception {
        List<ManagedObjectReference> providers = smsPort.queryProvider(storageMgr);

        if (providers == null || providers.isEmpty()) {
            log.info("No providers found.");
            return;
        }

        for (ManagedObjectReference provider : providers) {
            VasaProviderInfo info = (VasaProviderInfo) smsPort.queryProviderInfo(provider);

            System.out.println("Found provider:");
            System.out.println("Name: " + info.getName());
            System.out.println("Url: " + info.getUrl());
            System.out.println("API Version: " + info.getVasaVersion());
            System.out.println("Status: " + info.getStatus());
        }
    }

    private static void unregisterProvider(SmsPortType smsPort) throws Exception {
        VasaProviderInfo info = (VasaProviderInfo) smsPort.queryProviderInfo(provider);

        ManagedObjectReference unregisterTaskMoRef = smsPort.unregisterProviderTask(storageMgr, info.getUid());

        SmsTaskInfo taskInfo = waitForTask(smsPort, unregisterTaskMoRef);
        if (taskInfo != null) {
            if (taskInfo.getState().equals("error")) {
                throw new RuntimeException("Unregister failed");
            } else if (taskInfo.getState().equals("success")) {
                return;
            }
        }

        throw new RuntimeException("Unregister timed out");
    }

    private static SmsTaskInfo waitForTask(SmsPortType smsPort, ManagedObjectReference smsTask) throws Exception {
        SmsTaskInfo info = null;

        int i = 0;
        while (i < MAX_ATTEMPTS) {
            Thread.sleep(SLEEP_INTERVAL);

            info = smsPort.querySmsTaskInfo(smsTask);

            if (info.getState().equals("running")) {
                ++i;
            } else {
                break;
            }
        }

        return info;
    }
}
