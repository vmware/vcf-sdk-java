/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.nsx.helpers;

import com.vmware.sdk.nsx.api.v1.V1Factory;
import com.vmware.sdk.samples.nsx.client.ApiClientHelper;
import com.vmware.vapi.client.ApiClient;

public class NsxHelper {
    public static class NsxFactory implements AutoCloseable {

        private final ApiClient apiClient;
        private final V1Factory v1Factory;

        public NsxFactory(String nsxHostname, String nsxUsername, String nsxPassword) {
            this.apiClient = ApiClientHelper.getBasicAuthApiClient(nsxHostname, nsxUsername, nsxPassword);
            this.v1Factory = V1Factory.getFactory(this.apiClient, null);
        }

        public V1Factory getV1Factory() {
            return v1Factory;
        }

        @Override
        public void close() throws Exception {
            apiClient.close();
        }

        public ApiClient getApiClient() {
            return this.apiClient;
        }
    }
}
