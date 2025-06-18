/*
 * ******************************************************************
 * Copyright (c) 2013-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.cis.authn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.vmware.vapi.core.AsyncHandle;
import com.vmware.vapi.core.ExecutionContext.SecurityContext;
import com.vmware.vapi.internal.security.SecurityUtil;
import com.vmware.vapi.internal.util.Validate;
import com.vmware.vapi.protocol.RequestProcessor;
import com.vmware.vapi.saml.SamlToken;
import com.vmware.vapi.security.AuthenticationHandler;
import com.vmware.vapi.security.PrincipalId;
import com.vmware.vapi.security.StdSecuritySchemes;

/** This class handles authentication by SAML tokens. */
public class SamlTokenAuthnHandler implements AuthenticationHandler {

    private static final String INVALID_AUTHN_MSG = "Authentication data not found";

    /**
     * This key is used to transfer the {@link SamlToken} from the signature verification request processor to the
     * authentication handler
     */
    public static final String SAML_TOKEN_KEY = "saml_token";
    /**
     * This key is used to transfer the {@link Exception} from the signature verification request processor to the
     * authentication handler
     */
    public static final String ERROR_KEY = "saml_error";

    @Override
    public void authenticate(SecurityContext ctx, AsyncHandle<AuthenticationResult> asyncHandle) {
        Validate.notNull(ctx);
        Validate.notNull(asyncHandle);

        @SuppressWarnings("unchecked")
        Map<String, Object> requestData =
                SecurityUtil.narrowType(ctx.getProperty(RequestProcessor.SECURITY_PROC_METADATA_KEY), Map.class);
        if (requestData == null) {
            asyncHandle.setError(new RuntimeException(INVALID_AUTHN_MSG));
            return;
        }
        SamlToken token = SecurityUtil.narrowType(requestData.get(SAML_TOKEN_KEY), SamlToken.class);
        Exception error = SecurityUtil.narrowType(requestData.get(ERROR_KEY), Exception.class);

        if (error != null || token == null) {
            asyncHandle.setError(new RuntimeException(INVALID_AUTHN_MSG, error));
            return;
        }

        asyncHandle.setResult(new AuthnResultImpl(token));
    }

    @Override
    public List<String> supportedAuthenticationSchemes() {
        return Collections.unmodifiableList(
                Arrays.asList(StdSecuritySchemes.SAML_TOKEN, StdSecuritySchemes.SAML_BEARER_TOKEN));
    }

    private final class AuthnResultImpl extends AuthenticationResult {

        private final PrincipalId subject;
        private final List<PrincipalId> groupList = new ArrayList<PrincipalId>();
        private final SamlToken token;

        private AuthnResultImpl(SamlToken token) {
            if (token == null) {
                throw new IllegalArgumentException("token is null");
            }

            this.subject = new PrincipalIdImpl(token.getSubject());
            for (com.vmware.vapi.saml.PrincipalId group : token.getGroupList()) {
                groupList.add(new PrincipalIdImpl(group));
            }
            this.token = token;
        }

        @Override
        public PrincipalId getUser() {
            return subject;
        }

        @Override
        public List<PrincipalId> getGroups() {
            return groupList;
        }

        @Override
        public SecurityContext getSecurityContext() {
            return new SamlTokenSecurityContext(token);
        }
    }

    private static class PrincipalIdImpl implements PrincipalId {

        private final String name;
        private final String domain;

        private PrincipalIdImpl(com.vmware.vapi.saml.PrincipalId principal) {
            this.name = principal.getName();
            this.domain = principal.getDomain();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDomain() {
            return domain;
        }
    }
}
