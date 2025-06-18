/*
 * ******************************************************************
 * Copyright (c) 2014-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.cis.authn.json;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import com.vmware.vapi.cis.authn.SamlTokenSecurityContext;
import com.vmware.vapi.cis.authn.SecurityContextFactory;
import com.vmware.vapi.core.ExecutionContext.SecurityContext;
import com.vmware.vapi.internal.security.SecurityContextConstants;
import com.vmware.vapi.protocol.RequestProcessor;
import com.vmware.vapi.saml.SamlToken;
import com.vmware.vapi.saml.exception.InvalidTokenException;
import com.vmware.vapi.security.StdSecuritySchemes;

/** Unit tests for {@link JsonSigningProcessor}. */
public class JsonSigningProcessorTest {

    @Test
    public void testSign() throws InvalidTokenException, IOException, URISyntaxException {
        JsonSigningProcessor proc = new JsonSigningProcessor();
        SamlToken samlToken = SamlTokenUtil.loadSampleToken();
        SecurityContext props =
                SecurityContextFactory.createSamlSecurityContext(samlToken, SamlTokenUtil.loadTokenPrivateKey());
        Map<String, Object> meta = Collections.singletonMap(RequestProcessor.SECURITY_CONTEXT_KEY, (Object) props);
        byte[] res = proc.process(SamlTokenUtil.loadTestMsg(), meta, null);
        String body = new String(res, "UTF-8");
        assertTrue(body.contains(samlToken.getSubject().getName()));
        assertTrue(body.contains(SecurityContextConstants.TIMESTAMP_KEY));
        assertTrue(body.contains(SecurityContextConstants.SIGNATURE_KEY));
        assertTrue(body.contains(SecurityContextConstants.SIG_VALUE_KEY));
        assertTrue(body.contains(SecurityContextConstants.SIGNATURE_ALGORITHM_KEY));
        assertTrue(body.contains(SecurityContextConstants.SAML_TOKEN_KEY));
    }

    @Test
    public void testSignTextToken() throws URISyntaxException, IOException {
        JsonSigningProcessor proc = new JsonSigningProcessor();
        SecurityContext props = SecurityContextFactory.createSamlSecurityCtx(
                SamlTokenUtil.loadSampleTokenXml(), SamlTokenUtil.loadTokenPrivateKey());
        Map<String, Object> meta = Collections.singletonMap(RequestProcessor.SECURITY_CONTEXT_KEY, (Object) props);
        byte[] res = proc.process(SamlTokenUtil.loadTestMsg(), meta, null);
        String body = new String(res, "UTF-8");
        assertTrue(body.contains("root@localos"));
        assertTrue(body.contains(SecurityContextConstants.TIMESTAMP_KEY));
        assertTrue(body.contains(SecurityContextConstants.SIGNATURE_KEY));
        assertTrue(body.contains(SecurityContextConstants.SIG_VALUE_KEY));
        assertTrue(body.contains(SecurityContextConstants.SIGNATURE_ALGORITHM_KEY));
        assertTrue(body.contains(SecurityContextConstants.SAML_TOKEN_KEY));
    }

    @Test
    public void testShouldSign_SamlBearer() {
        JsonSigningProcessor proc = new JsonSigningProcessor();
        final Map<String, Object> props = Collections.<String, Object>singletonMap(
                SecurityContext.AUTHENTICATION_SCHEME_ID, StdSecuritySchemes.SAML_BEARER_TOKEN);

        SecurityContext secCtx = buildTestSecurityContext(props);
        assertFalse(proc.shouldSignRequest(secCtx));
    }

    @Test
    public void testShouldSign_SamlHoK() {
        JsonSigningProcessor proc = new JsonSigningProcessor();
        final Map<String, Object> props = new HashMap<String, Object>(3);
        props.put(SecurityContext.AUTHENTICATION_SCHEME_ID, StdSecuritySchemes.SAML_TOKEN);
        props.put(SamlTokenSecurityContext.SAML_TOKEN_ID, EasyMock.createNiceMock(SamlToken.class));
        props.put(SamlTokenSecurityContext.PRIVATE_KEY_ID, EasyMock.createNiceMock(PrivateKey.class));

        SecurityContext secCtx = buildTestSecurityContext(props);
        assertTrue(proc.shouldSignRequest(secCtx));
    }

    @Test
    public void testShouldSign_SamlHoKText() {
        JsonSigningProcessor proc = new JsonSigningProcessor();
        final Map<String, Object> props = new HashMap<String, Object>(3);
        props.put(SecurityContext.AUTHENTICATION_SCHEME_ID, StdSecuritySchemes.SAML_TOKEN);
        props.put(SamlTokenSecurityContext.SAML_TOKEN_ID, "test token text");
        props.put(SamlTokenSecurityContext.PRIVATE_KEY_ID, EasyMock.createNiceMock(PrivateKey.class));

        SecurityContext secCtx = buildTestSecurityContext(props);
        assertTrue(proc.shouldSignRequest(secCtx));
    }

    @Test
    public void testShouldSign_BrokenCtxt() {
        assertThrows(IllegalStateException.class, () -> {
            JsonSigningProcessor proc = new JsonSigningProcessor();
            final Map<String, Object> props = new HashMap<String, Object>(3);
            props.put(SecurityContext.AUTHENTICATION_SCHEME_ID, StdSecuritySchemes.SAML_TOKEN);

            SecurityContext secCtx = buildTestSecurityContext(props);
            proc.shouldSignRequest(secCtx);
        });
    }

    @Test
    public void testShouldSign_Negative() {
        JsonSigningProcessor proc = new JsonSigningProcessor();
        assertFalse(proc.shouldSignRequest(null));
        assertFalse(proc.shouldSignRequest("not a SecurityContext type"));
        assertFalse(proc.shouldSignRequest(1));

        assertFalse(proc.shouldSignRequest(buildTestSecurityContext(Collections.<String, Object>emptyMap())));
    }

    private SecurityContext buildTestSecurityContext(final Map<String, Object> props) {
        return new SecurityContext() {
            @Override
            public Object getProperty(String key) {
                return props.get(key);
            }

            @Override
            public Map<String, Object> getAllProperties() {
                return props;
            }
        };
    }
}
