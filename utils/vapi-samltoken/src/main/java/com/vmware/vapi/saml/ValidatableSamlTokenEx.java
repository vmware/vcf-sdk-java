/*
 * ******************************************************************
 * Copyright (c) 2012-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.saml;

import java.util.List;

/** @see ValidatableSamlToken exposes information about token issuer and extended information on token delegates. */
public interface ValidatableSamlTokenEx extends ValidatableSamlToken {
    /**
     * The issuer element of a token provides information about the issuer of a SAML assertion.
     *
     * @return Assertion's issuer.
     */
    public IssuerNameId getIssuerNameId();

    /**
     * The delegation chain reflects the path of the assertion through one or more intermediaries that act on behalf of
     * the subject of the assertion.
     *
     * @see SamlToken#getDelegationChain() The information is extended with the subject format.
     * @return the delegation chain for this token or empty list if the token has never been delegated.
     */
    public List<TokenDelegateEx> getDelegationChainEx();

    /**
     * Implementations of this class will act as containers for a single intermediary/delegate represented by the
     * assertion.
     */
    public interface TokenDelegateEx extends TokenDelegate {

        /**
         * Identifies the delegate together with its format.
         *
         * @return the delegate
         */
        public SubjectNameId getSubjectNameId();
    }
}
