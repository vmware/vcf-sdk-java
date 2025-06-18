/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.management.appliance.localaccounts;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;
import static java.util.Objects.requireNonNullElse;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appliance.LocalAccounts;
import com.vmware.appliance.LocalAccountsTypes.Config;
import com.vmware.appliance.LocalAccountsTypes.UpdateConfig;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;

/**
 * Demonstrates local accounts workflow as below:
 *
 * <ol>
 *   <li>Create a user
 *   <li>List all users to check if the user is added successfully
 *   <li>Get the user details to check parameters passed in create call, here not passing anything other than mandatory
 *       attributes password and role list. Setting all attributes in set operation ,the workflow is identical and same
 *       can be used in create api as well.
 *   <li>Set certain user details
 *   <li>Get the user details to check for values set
 *   <li>Update certain user details
 *   <li>Get the user details to check for updated values
 *   <li>Delete the user
 *   <li>List all users to check if the user is deleted successfully
 * </ol>
 */
public class LocalAccountWorkflow {
    private static final Logger log = LoggerFactory.getLogger(LocalAccountWorkflow.class);
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

    /** REQUIRED: Specify username. */
    public static String user = "user";
    /** REQUIRED: Specify user password. */
    public static String userPassword = "userPassword";
    /**
     * OPTIONAL: Comma seperated list of roles, valid roles are: operator, admin, superAdmin. Default value is
     * {operator}.
     */
    public static String[] userRoles = null;
    /** OPTIONAL: Number of days after password expiration before the account will be locked. Default value is 10. */
    public static Long daysAfterPasswdExp = null;
    /** OPTIONAL: Email address of the local account. Default value is "usr_email@test.com". */
    public static String userEmail = null;
    /**
     * OPTIONAL: Indicates if the specific user account is enabled. By default, it is enabled, set false to disable the
     * account. Default value is false.
     */
    public static Boolean userAccountEnabled = null;
    /** OPTIONAL: Full name of the user. Default value is "test_user_def". */
    public static String userFullName = null;
    /** OPTIONAL: if the account will be locked after password expiration. Default value is false. */
    public static Boolean inactiveAfterPasswdExpired = null;
    /**
     * OPTIONAL: Maximum number of days between password change, if not specified will pick up from /etc/login.defs.
     * Default value is 70.
     */
    public static Long maxDays = null;
    /**
     * OPTIONAL: Minimum number of days between password change, if not specified will pick up from /etc/login.defs.
     * Default value is 10.
     */
    public static Long minDays = null;
    /**
     * OPTIONAL: Number of days of warning before password expires, if not specified will pick up from /etc/login.defs.
     * Default value is 30.
     */
    public static Long warnDays = null;
    /**
     * OPTIONAL: Old password of the user, required in case of the password change, not required if superAdmin user
     * changes the password of the other. Default value is "Pass123".
     */
    public static String oldPassword = null;
    /**
     * OPTIONAL: Indicates if the account password expires and needs to be reset. By default, it expires after 60
     * days,this can be disabled by setting to false and then password never expires for the account. Default value is
     * false.
     */
    public static Boolean passwordExpires = null;
    /** OPTIONAL: Date when the account's password will expire. Default value is "1/1/28". */
    public static String passwordExpiresAt = null;

    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_SUPERADMIN = "superAdmin";
    private static final String UPDATE_EMAIL = "update@test.com";
    private static final String UPDATE_FULL_NAME = "Updated_full_name";

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(LocalAccountWorkflow.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            LocalAccounts laServiceApiStub = client.createStub(LocalAccounts.class);

            // initializing configuration object to be passed for create and set operations
            Config laConfig = new Config();
            // initializing configuration object to be passed for update operations
            UpdateConfig laUpdateConfig = new UpdateConfig();

            log.info("#### Example: Step1-Creating users");
            char[] oldPasswordCharArray =
                    Objects.requireNonNullElse(oldPassword, "Pass123").toCharArray();

            // setting password and role passed in config
            laConfig.setPassword(oldPasswordCharArray);
            laConfig.setRoles(Arrays.asList(userRoles));

            // create user with basic configuration
            laServiceApiStub.create(user, laConfig);

            // list all users to check if user added in above step is listed
            log.info("#### Example: List Users \n{}", laServiceApiStub.list());

            // get the user details for the created user
            log.info("#### Example:Get user details \n{}", laServiceApiStub.get(user));
            DateFormat formatter = new SimpleDateFormat("MM/dd/yy");
            Calendar cal = Calendar.getInstance();
            cal.setTime(formatter.parse(requireNonNullElse(passwordExpiresAt, "1/1/28")));
            log.info("#### Example: Step2-Setting user attributes");

            /*
             * Populating configuration details for set operation.
             * Note:The same steps can be used to invoke create localaccounts api as well
             */
            // old password is required for set api operation for a non-super admin

            laConfig.setOldPassword(oldPasswordCharArray);
            laConfig.setDaysAfterPasswordExpiration(requireNonNullElse(daysAfterPasswdExp, 10L));
            laConfig.setEmail(requireNonNullElse(userEmail, "usr_email@test.com"));
            laConfig.setEnabled(Boolean.TRUE.equals(userAccountEnabled));
            laConfig.setFullName(requireNonNullElse(userFullName, "test_user_def"));
            laConfig.setInactiveAfterPasswordExpiration(Boolean.TRUE.equals(inactiveAfterPasswdExpired));
            laConfig.setMaxDaysBetweenPasswordChange(requireNonNullElse(maxDays, 70L));
            laConfig.setMinDaysBetweenPasswordChange(requireNonNullElse(minDays, 10L));
            laConfig.setPassword(userPassword.toCharArray());
            laConfig.setPasswordExpires(Boolean.TRUE.equals(passwordExpires));
            laConfig.setRoles(List.of(ROLE_ADMIN));
            laConfig.setWarnDaysBeforePasswordExpiration(requireNonNullElse(warnDays, 30L));

            // invoking set local accounts api passing above configuration
            laServiceApiStub.set(user, laConfig);
            log.info("User details \n{}", laServiceApiStub.get(user));
            // At least one attribute needs to be set for update api
            log.info("#### Example: Step3-Updating user attributes");

            /*
             * Populating configuration details for update operation.
             * 1.Updating role from operator to admin
             * 2.Setting user to enabled, the user was disabled in step2 enabled
             * flag changes from false to true
             * 3.Setting password expires to true and setting a date,max days
             * between password change changes from -1 (password expires false)
             * 4.Setting inactive after password expiration to true:check for
             * inactive at date changes
             * 5.Updating user full name and email id
             * Note:Similarly all other attributes can be updated
             */
            laUpdateConfig.setRoles(List.of(ROLE_SUPERADMIN));
            laUpdateConfig.setEnabled(true);
            laUpdateConfig.setPasswordExpires(true);
            laUpdateConfig.setPasswordExpiresAt(cal);
            laUpdateConfig.setInactiveAfterPasswordExpiration(true);
            laUpdateConfig.setEmail(UPDATE_EMAIL);
            laUpdateConfig.setFullName(UPDATE_FULL_NAME);

            // invoking update local accounts api passing above configuration
            laServiceApiStub.update(user, laUpdateConfig);
            log.info("User details \n{}", laServiceApiStub.get(user));

            // cleanup
            log.info("#### Example: Step4-Delete User");
            // Deleting the user created
            laServiceApiStub.delete(user);

            // list all users to check if user removed in above step is not listed
            log.info("#### Example: List Users \n{}", laServiceApiStub.list());
        }
    }
}
