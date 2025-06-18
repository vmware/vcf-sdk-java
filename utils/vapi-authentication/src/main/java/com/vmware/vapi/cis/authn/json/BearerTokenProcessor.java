/*
 * ******************************************************************
 * Copyright (c) 2013-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.cis.authn.json;

import java.util.HashMap;
import java.util.Map;

import com.vmware.vapi.cis.authn.SamlTokenSecurityContext;
import com.vmware.vapi.core.ExecutionContext.SecurityContext;
import com.vmware.vapi.dsig.json.SecurityContextProcessor;
import com.vmware.vapi.internal.security.SecurityContextConstants;
import com.vmware.vapi.saml.SamlToken;
import com.vmware.vapi.security.StdSecuritySchemes;

/** This class represents a client side processor for bearer token authentication scheme. */
public final class BearerTokenProcessor extends SecurityContextProcessor {

    @Override
    public boolean isSchemeSupported(String requestedScheme) {
        return requestedScheme.equalsIgnoreCase(StdSecuritySchemes.SAML_BEARER_TOKEN);
    }

    @Override
    public Map<String, Object> getSecurityContextProperties(SecurityContext ctx) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(SecurityContextConstants.SCHEME_ID_KEY, StdSecuritySchemes.SAML_BEARER_TOKEN);
        SamlToken samlToken = (SamlToken) ctx.getProperty(SecurityContextConstants.SAML_TOKEN_KEY);
        result.put(SamlTokenSecurityContext.SAML_TOKEN_ID, samlToken.toXml());
        return result;
    }
}
