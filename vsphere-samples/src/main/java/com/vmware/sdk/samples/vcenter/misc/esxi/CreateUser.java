/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.misc.esxi;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.ESXiClient;
import com.vmware.sdk.vsphere.utils.ESXiClientFactory;
import com.vmware.vim25.HostAccountSpec;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.Permission;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.UserSearchResult;
import com.vmware.vim25.VimPortType;

/**
 * This sample creates a local user account and password. Afterwards the newly created credentials are used tested.
 *
 * <p>Sample Prerequisites: ESXi only (not vCenter).
 */
public class CreateUser {
    private static final Logger log = LoggerFactory.getLogger(CreateUser.class);
    /** REQUIRED: ESXi FQDN or IP address. */
    public static String serverAddress = "esx1.mycompany.com";
    /** REQUIRED: Username to log in to the ESXi. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the ESXi. */
    public static String password = "password";
    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (do not leave it empty on production environments).
     */
    public static String trustStorePath = null;

    /** OPTIONAL: Provide your own username to create (default is a randomly generated name). */
    public static String user = "user1234";
    /** OPTIONAL: Provide your own password for newly created username (default is a randomly generated password). */
    public static String pass = "C0mPlexPassword123!";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(CreateUser.class, args);

        ESXiClientFactory factory = new ESXiClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (ESXiClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();

            ManagedObjectReference hostLocalAccountManager = serviceContent.getAccountManager();

            ManagedObjectReference hostAuthorizationManager = serviceContent.getAuthorizationManager();

            ManagedObjectReference userDirectoryMoRef = serviceContent.getUserDirectory();
            List<UserSearchResult> userSearchResult =
                    vimPort.retrieveUserGroups(userDirectoryMoRef, null, user, null, null, true, true, false);

            if (userSearchResult.isEmpty()) {
                HostAccountSpec hostAccountSpec = new HostAccountSpec();
                hostAccountSpec.setId(user);
                hostAccountSpec.setPassword(pass);
                hostAccountSpec.setDescription("User Description");

                vimPort.createUser(hostLocalAccountManager, hostAccountSpec);

                ManagedObjectReference rootFolderMoRef = serviceContent.getRootFolder();

                /* For demonstration purposes only, the account is granted
                  the 'administrator' role (-1) on the rootFolder of the inventory.
                 Never give users more privileges than absolutely necessary.
                */

                Permission permission = new Permission();
                permission.setGroup(false);
                permission.setPrincipal(user);
                permission.setRoleId(-1);
                permission.setPropagate(true);
                permission.setEntity(rootFolderMoRef);

                List<Permission> permissions = new ArrayList<>();
                permissions.add(permission);

                vimPort.setEntityPermissions(hostAuthorizationManager, rootFolderMoRef, permissions);

                log.info("Successfully created the '{}' user", user);
                log.info("Authenticating with the newly created user");

                try (ESXiClient client2 = factory.createClient(user, pass, null)) {
                    String serviceName =
                            client2.getVimServiceContent().getAbout().getFullName();
                    log.info("Authentication successful, service name: {}", serviceName);
                }
            } else {
                log.info("User {} already Exist", user);
            }
        }
    }
}
