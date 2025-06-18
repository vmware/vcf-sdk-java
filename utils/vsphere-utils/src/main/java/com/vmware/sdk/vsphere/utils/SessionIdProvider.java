/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.vsphere.utils;

import com.vmware.cis.Session;
import com.vmware.vapi.bindings.StubConfiguration;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VimPortType;

/**
 * A session provider should provide a vCenter session id that can be used to configure {@link VimPortType} or
 * {@link StubConfiguration} in order to use WSDL/vCenter REST APIs that require authentication.
 *
 * <p>Implementation should use {@link VimPortType#login(ManagedObjectReference, String, String, String)},
 * {@link VimPortType#loginByToken(ManagedObjectReference, String)} {@link Session#create()} and similar.
 */
@FunctionalInterface
public interface SessionIdProvider {
    char[] get();
}
