/*
 * ******************************************************************
 * Copyright (c) 2025-2026 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.auth;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.ops.api.client.controllers.UserAndAuthManagementClient;
import com.vmware.ops.api.model.auth.AuthToken;
import com.vmware.ops.api.model.auth.User;
import com.vmware.ops.api.model.auth.User.UserList;
import com.vmware.ops.api.model.auth.UserGroup;
import com.vmware.ops.api.model.auth.UserRole;
import com.vmware.ops.api.model.auth.UsernamePassword;
import com.vmware.sdk.samples.ops.SampleBaseWithTrustStore;
import com.vmware.sdk.samples.ops.config.auth.UserConfig;
import com.vmware.sdk.samples.ops.config.auth.UserGroupConfig;
import com.vmware.sdk.samples.ops.config.auth.UserManagementConfig;
import com.vmware.sdk.samples.ops.config.auth.UserRoleConfig;
import com.vmware.sdk.samples.ops.helpers.ConfigValidator;

/**
 * Example that illustrates the use of API client bindings to perform
 * user management CRUD operations
 */
public class UserManagementWithTruststore extends SampleBaseWithTrustStore {

    private static final Logger logger = LoggerFactory.getLogger(UserManagementWithTruststore.class);

    private final UserAndAuthManagementClient userAndAuthManagementClient;

    private final UserConfig userConfig;
    private final UserGroupConfig userGroupConfig;
    private final UserRoleConfig userRoleConfig;

    public UserManagementWithTruststore(String clientConfigFile, String sampleConfigClient) throws IOException {
        super(clientConfigFile);

        UserManagementConfig userManagementConfig =
                mapper.readValue(new File(sampleConfigClient), UserManagementConfig.class);
        ConfigValidator.validate(userManagementConfig);

        userConfig = userManagementConfig.getUserConfig();
        userGroupConfig = userManagementConfig.getUserGroupConfig();
        userRoleConfig = userManagementConfig.getUserRoleConfig();
        userAndAuthManagementClient = getClient().userAndAuthManagementClient();
    }

    @Override
    public void run() {
        // INFO: No need to call it manually, it's called automatically from RestClientProxy.
        acquireToken();

        // CRUD for users
        User user = createUser();
        getUser(user.getId());
        getUsers();

        user.setFirstName("New-First");
        user.setLastName("New-Last");
        user.setEmailAddress("last.first@somedomain.com");
        user.setPassword("newPassword@123");

        User updatedUser = modifyUser(user);
        deleteUser(updatedUser.getId());
        UserList userList = getUsers();

        // CRUD for user groups
        UserGroup group = createUserGroup(userList.getUsers());
        getUserGroup(group.getId());
        getUserGroups();

        group.setName("New-Group-Name");
        group.setDescription("New-Group-Description");

        UserGroup updatedUserGroup = modifyUserGroup(group);
        deleteUserGroup(updatedUserGroup.getId());
        getUserGroups();

        // CRUD for user roles
        UserRole userRole = createUserRole();
        getUserRole(userRole.getRoleName());
        getUserRoles();

        userRole.setDescription("New-Role-Description");

        UserRole updatedUserRole = updateUserRole(userRole);
        deleteUserRole(updatedUserRole.getRoleName());
        getUserRoles();
    }

    public AuthToken acquireToken() {
        logger.info("Acquiring token...");
        UsernamePassword usernamePassword =
                new UsernamePassword(baseClientConfig.getUsername(), baseClientConfig.getPassword());
        AuthToken token = userAndAuthManagementClient.acquireToken(usernamePassword);
        logger.info("Acquired token: {}", token.getToken());
        return token;
    }

    public User createUser() {
        logger.info("Creating a local vCenter Operations Manager user account...");
        User user = new User();
        user.setFirstName(userConfig.getFirstname());
        user.setLastName(userConfig.getLastname());
        user.setEmailAddress(userConfig.getEmail());
        user.setUsername(userConfig.getUsername());
        user.setPassword(userConfig.getPassword());
        user = userAndAuthManagementClient.createUser(user);
        logger.info("User ID of the user account created: {}", user.getId());
        return user;
    }

    public User getUser(UUID userId) {
        User user = userAndAuthManagementClient.getUser(userId);
        logger.info("Getting user: {}, first name: {}, last name: {}", userId, user.getFirstName(), user.getLastName());
        return user;
    }

    public UserList getUsers() {
        logger.info("List all the local vCenter Operations Manager users in the system...");
        UserList users = userAndAuthManagementClient.getUsers(null, null, null);
        logger.info(
                "Number of local vCenter Operations Manager users in the system: {}",
                users.getUsers().size());
        return users;
    }

    public User modifyUser(User user) {
        logger.info("Modify the local vCenter Operations Manager user account created...");
        user = userAndAuthManagementClient.modifyUser(user);
        logger.info(
                "User Account Details after update: first name: {}, last name: {}",
                user.getFirstName(),
                user.getLastName());
        return user;
    }

    public void deleteUser(UUID userId) {
        logger.info("Deleting user: {}", userId);
        userAndAuthManagementClient.deleteUser(userId);
    }

    public UserGroup createUserGroup(List<User> users) {
        logger.info("Creating a local vCenter Operations Manager group account...");
        UserGroup group = new UserGroup();
        group.setName(userGroupConfig.getName());
        group.setDescription(userGroupConfig.getDescription());
        group.setUserIds(users.stream().map(User::getId).collect(Collectors.toList()));
        group = userAndAuthManagementClient.createUserGroup(group);
        logger.info("Group ID of the user group created: {}", group.getId());
        return group;
    }

    public UserGroup getUserGroup(UUID groupId) {
        UserGroup group = userAndAuthManagementClient.getUserGroup(groupId);
        logger.info(
                "Getting user group: {}, name: {}, description: {}", groupId, group.getName(), group.getDescription());
        return group;
    }

    public UserGroup.UserGroups getUserGroups() {
        logger.info("List all the local vCenter Operations Manager user groups in the system...");
        UserGroup.UserGroups groups = userAndAuthManagementClient.getUserGroups();
        logger.info(
                "Number of local vCenter Operations Manager user groups in the system: {}",
                groups.getUserGroups().size());
        return groups;
    }

    public UserGroup modifyUserGroup(UserGroup group) {
        logger.info("Modify the local vCenter Operations Manager group account created...");
        userAndAuthManagementClient.modifyUserGroup(group);
        logger.info(
                "Group Account Details after update: name: {}, description: {}",
                group.getName(),
                group.getDescription());
        return group;
    }

    public void deleteUserGroup(UUID groupId) {
        logger.info("Deleting group: {}", groupId);
        userAndAuthManagementClient.deleteUserGroup(groupId);
    }

    public UserRole createUserRole() {
        logger.info("Creating a local vCenter Operations Manager userRole account...");
        UserRole userRole = new UserRole();
        userRole.setRoleName(userRoleConfig.getName());
        userRole.setDescription(userRoleConfig.getDescription());
        userRole.setPrivilegeKeys(userRoleConfig.getPrivilegeKeys());
        userRole = userAndAuthManagementClient.createUserRole(userRole);
        logger.info("Name of the user role created: {}", userRole.getRoleName());
        return userRole;
    }

    public UserRole getUserRole(String roleName) {
        UserRole userRole = userAndAuthManagementClient.getRole(roleName);
        logger.info(
                "Getting userRole: {}, name: {}, description: {}",
                roleName,
                userRole.getRoleName(),
                userRole.getDescription());
        return userRole;
    }

    public UserRole.UserRoles getUserRoles() {
        logger.info("List all the local vCenter Operations Manager userRoles in the system...");
        UserRole.UserRoles userRoles = userAndAuthManagementClient.getRoles(null);
        logger.info(
                "Number of local vCenter Operations Manager user roles in the system: {}",
                userRoles.getUserRoles().size());
        return userRoles;
    }

    public UserRole updateUserRole(UserRole userRole) {
        logger.info("Modify the local vCenter Operations Manager userRole account created...");
        userRole = userAndAuthManagementClient.updateUserRole(userRole);
        logger.info(
                "UserRole Account Details after update: name: {}, description: {}",
                userRole.getRoleName(),
                userRole.getDescription());
        return userRole;
    }

    public void deleteUserRole(String roleName) {
        logger.info("Deleting userRole: {}", roleName);
        userAndAuthManagementClient.deleteUserRole(roleName);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new RuntimeException(
                    "Please provide the client-config JSON file path first, followed by the user-management-config JSON file path, via arguments.");
        }

        new UserManagementWithTruststore(args[0], args[1]).run();
    }
}
