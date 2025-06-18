/*
 * ******************************************************************
 * Copyright (c) 2014-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.cis.authn;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import com.vmware.vapi.core.AsyncHandleSyncAdapter;
import com.vmware.vapi.core.ExecutionContext;
import com.vmware.vapi.core.MethodResult;
import com.vmware.vapi.data.StructValue;
import com.vmware.vapi.internal.protocol.client.rpc.CorrelatingClient;
import com.vmware.vapi.internal.protocol.client.rpc.CorrelatingClient.SendParams;
import com.vmware.vapi.protocol.ClientConfiguration;
import com.vmware.vapi.protocol.HttpConfiguration;
import com.vmware.vapi.protocol.JsonProtocolConnectionFactory;
import com.vmware.vapi.protocol.ProtocolConnection;
import com.vmware.vapi.protocol.ProtocolConnectionFactory;

/** Unit tests for {@link ProtocolFactory}. */
public class ProtocolFactoryTest {

    static class TestCustomProtocolConnectionFactory extends JsonProtocolConnectionFactory {
        CorrelatingClient transport;

        TestCustomProtocolConnectionFactory(CorrelatingClient transport) {
            this.transport = transport;
        }

        @Override
        protected CorrelatingClient createHttpTransport(
                String uri, ClientConfiguration clientConfig, HttpConfiguration httpConfig) {
            return transport;
        }
    }

    @Test
    public void customApacheClientTest() {
        CorrelatingClient customTransport = EasyMock.createNiceMock(CorrelatingClient.class);
        customTransport.send(EasyMock.anyObject(SendParams.class));
        EasyMock.replay(customTransport);

        ProtocolConnectionFactory connFactory = new TestCustomProtocolConnectionFactory(customTransport);

        ProtocolConnection conn = connFactory.getHttpConnection("http://fake.url.com:8080", null, null);

        // perform invoke and then verify the mock, which proves that the
        // custom CorrelatingClient is used as transport
        conn.getApiProvider()
                .invoke(
                        "doesnt_matter-service",
                        "doesnt_matter-operation",
                        new StructValue("doesnt_matter-name"),
                        new ExecutionContext(),
                        new AsyncHandleSyncAdapter<MethodResult>());
        EasyMock.verify(customTransport);
    }
}
