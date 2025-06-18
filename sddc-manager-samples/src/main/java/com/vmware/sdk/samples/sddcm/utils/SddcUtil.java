/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.sddcm.utils;

import com.vmware.sdk.samples.sddcm.client.ApiClientUtil;
import com.vmware.sdk.sddcm.SddcmFactory;
import com.vmware.sdk.sddcm.v1.V1Factory;
import com.vmware.vapi.bindings.StubConfiguration;
import com.vmware.vapi.client.ApiClient;

/** Util class to declare the static SddcFactory class to manage the closable resources and ManagerFactory instance. */
public class SddcUtil {

    public static class SddcFactory implements AutoCloseable {
        private final V1Factory v1Factory;
        private final ApiClient apiClient;

        /**
         * Initialization of SDDC Manager client and SDDC Manager stubs used to provide security contexts (e.g.
         * username/password, OAuth tokens) for authentication and authorization.
         *
         * @param sddcManagerIpOrFqdn server IP Address/FQDN
         * @param sddcManagerSsoUserName username in the SSO server
         * @param sddcManagerSsoPassword password in the SSO server
         * @throws Exception if the API call fails
         */
        public SddcFactory(String sddcManagerIpOrFqdn, String sddcManagerSsoUserName, String sddcManagerSsoPassword)
                throws Exception {
            StubConfiguration stubConfiguration = ApiClientUtil.getSddcStubConfiguration(
                    sddcManagerIpOrFqdn, sddcManagerSsoUserName, sddcManagerSsoPassword);
            apiClient = ApiClientUtil.createClient(sddcManagerIpOrFqdn, stubConfiguration);
            v1Factory = SddcmFactory.getFactory(apiClient, stubConfiguration).v1();
        }

        public V1Factory getV1Factory() {
            return v1Factory;
        }

        @Override
        public void close() throws Exception {
            apiClient.close();
        }
    }
}
