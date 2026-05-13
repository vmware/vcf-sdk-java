/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.resize;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.deployment.Size;
import com.vmware.vcenter.deployment.size.Status;
import com.vmware.vcenter.deployment.size.ResizeState;
import com.vmware.vcenter.deployment.size.StatusTypes.Info;
import com.vmware.vcenter.deployment.DeploymentSize;
import com.vmware.vcenter.deployment.SizeTypes.Spec;
import com.vmware.vcenter.deployment.SizeTypes.Connection;

/**
 * Demonstrates vCenter Scale Up.
 *
 * <p>Sample Prerequisites:
 *
 * <ol>
 *   <li>Vcenter version &gt;= 9.1
 *   <li>Desired deployment size &gt source deployment size
 *   <li>vCenter shouldn't have a snapshot
 *   <li>vCenter High Availability shouldn't be enabled
 *   <li>The host should have sufficient resources
 *   <li>Shouldn't be an ongoing major/minor upgrade, backup/restore
 * </ol>
 */
public class ScaleUpSample {
    private static final Logger log = LoggerFactory.getLogger(ScaleUpSample.class);
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

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(ScaleUpSample.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            log.info("Get vCenter current deployment Size before Scale Up");
            com.vmware.vcenter.deployment.Info sourceInfo = getDeploymentInfo(client);
            printDeploymentSizeInfo(sourceInfo);

            // Perform Scale Up
            log.info("Initiating Vcenter Scale Up");
            reSizeVC(client);

            // Verify reSize status
            log.info("Get vCenter deployment resize status");
            Info statusInfo = getReSizeStatus(client);
            printResizeStatus(statusInfo);

            // verify deployment size after Scale Up
            log.info("Get vCenter current deployment Size after Scale Up");
            com.vmware.vcenter.deployment.Info info = getDeploymentInfo(client);
            printDeploymentSizeInfo(info);
        }
    }

    public static com.vmware.vcenter.deployment.Info getDeploymentInfo(VcenterClient client) {
        Size size = client.createStub(Size.class);

        com.vmware.vcenter.deployment.Info info = size.get();
        log.info("vCenter deployment Size info is: {}", info);
        return info;
    }

    public static void reSizeVC(VcenterClient client){
        Size size = client.createStub(Size.class);

        // Scale Up VC
        Spec spec = createSpec(serverAddress, username, password);
        log.info("vCenter resize spec is: {}", spec);

        size.update(spec);
    }

    public static void printDeploymentSizeInfo(com.vmware.vcenter.deployment.Info info){
        log.info("vCenter current deployment size is: {}", info.getSize());
        log.info("vCenter deployment cpu count is: {}", info.getCpuCount());
        log.info("vCenter deployment memory is: {}", info.getMemory());
        log.info("vCenter deployment total disk space is: {}", info.getTotalDiskSpace());
    }

    public static void printResizeStatus(Info statusInfo){
        log.info("vCenter resize status");
        log.info("Current status is:{}", statusInfo.getCurrentState());
        log.info("Source deployment size name is: {}", statusInfo.getResizeInfo().getSourceDeploymentSizeName());
        log.info("Desired deployment size name is: {}", statusInfo.getResizeInfo().getDesiredDeploymentSizeName());
    }

    public static Spec createSpec(String hostName, String userName, String password){
        Connection conn = new Connection();
        conn.setHostname(hostName);
        conn.setUsername(userName);
        conn.setPassword(password.toCharArray());

        Spec spec = new Spec();
        spec.setDeploymentSize(DeploymentSize.SMALL);
        spec.setConnection(conn);
        spec.setDeferServiceRestart(true);
        return spec;
    }

    public static Info getReSizeStatus(VcenterClient client){
        Status status = client.createStub(Status.class);
        ResizeState currentState = null;
        Info statusInfo;
        do {
            statusInfo = status.get();
            if (statusInfo != null) {
                currentState = statusInfo.getCurrentState();
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } while (currentState != ResizeState.PENDING_SERVICE_RESTART && currentState != ResizeState.RESIZING_FAILED);
        log.info("vCenter deployment Size status is: {}", statusInfo);
        return statusInfo;
    }
}
