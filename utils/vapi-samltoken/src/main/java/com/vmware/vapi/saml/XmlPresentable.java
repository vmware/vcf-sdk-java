/*
 * ******************************************************************
 * Copyright (c) 2013-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.saml;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Indicates that an object that can be presented as XML
 *
 * <p>The schema of the XML representation is up to implementation.
 */
public interface XmlPresentable {

    /**
     * @return object presented as a valid XML document string, not null
     *     <p>The XML document will not have a &lt;?xml declaration.
     */
    public String toXml();

    /**
     * Creates a copy of the XML representation of the object, imported into the given document.
     *
     * <p>This method allows the implementations to restore XML metadata which is lost when a node is imported in other
     * document or serialized to string.
     *
     * @param hostDocument required
     * @return the cloned node, owned by the given document, not null
     */
    public Node importTo(Document hostDocument);
}
