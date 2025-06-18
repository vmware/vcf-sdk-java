/*
 * ******************************************************************
 * Copyright (c) 2010-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.saml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vmware.vapi.saml.Advice.AdviceAttribute;

public class AdviceTest {

    private static final String ADVICE_SOURCE = "urn:vc.vmware.com";
    private static final String ATTRIBUTE_NAME1 = "urn:vc:attr:1";
    private static final String ATTRIBUTE_FRIENDLY_NAME1 = "myattribute";
    private static final String ATTRIBUTE_NAME2 = "urn:vc:attr:2";
    private static final String ATTR2_VALUE1 = "advice 1";
    private static final String ATTR2_VALUE2 = "advice 2";

    @Test
    public void createAdviceOneAttributeNoValue() {
        AdviceAttribute attribute = new AdviceAttribute(ATTRIBUTE_NAME1);
        List<AdviceAttribute> attrList = new ArrayList<AdviceAttribute>();
        attrList.add(attribute);

        Advice advice = new Advice(ADVICE_SOURCE, attrList);
        assertEquals(ADVICE_SOURCE, advice.getSource());
        assertEquals(attrList, advice.getAttributes());
    }

    @Test
    public void createAdviceWithAttributes() {
        Advice advice = createAdvice();
        assertEquals(ADVICE_SOURCE, advice.getSource());
        assertEquals(2, advice.getAttributes().size());
    }

    @Test
    public void checkAdviceEquals() {
        Advice advice1 = createAdvice();
        Advice advice2 = createAdvice();
        assertEquals(advice1, advice2);
    }

    @Test
    public void checkAdviceAttributeEquality() {
        AdviceAttribute attr1 = new AdviceAttribute(ATTRIBUTE_NAME1, ATTRIBUTE_FRIENDLY_NAME1, new ArrayList<String>());
        AdviceAttribute attr11 =
                new AdviceAttribute(ATTRIBUTE_NAME1, ATTRIBUTE_FRIENDLY_NAME1, new ArrayList<String>());
        AdviceAttribute attr2 = new AdviceAttribute(ATTRIBUTE_NAME1, null, new ArrayList<String>());
        AdviceAttribute attr3 = new AdviceAttribute(ATTRIBUTE_NAME2, ATTRIBUTE_FRIENDLY_NAME1, new ArrayList<String>());
        List<String> attributeValues = new ArrayList<String>();
        attributeValues.add(ATTR2_VALUE1);
        List<String> attributeValues41 = new ArrayList<String>();
        attributeValues41.add(ATTR2_VALUE1);
        AdviceAttribute attr4 = new AdviceAttribute(ATTRIBUTE_NAME1, ATTRIBUTE_FRIENDLY_NAME1, attributeValues);
        AdviceAttribute attr41 = new AdviceAttribute(ATTRIBUTE_NAME1, ATTRIBUTE_FRIENDLY_NAME1, attributeValues41);

        assertTrue(attr1.equals(attr1), "attribute should equal self");
        assertTrue(attr1.equals(attr11), "attribute should equal another instance with same values");
        assertFalse(attr1.equals(attr2), "attribute should Not equal when friendly name differs");
        assertFalse(attr1.equals(attr3), "attribute should Not equal another attribute");
        assertFalse(attr1.equals(attr4), "attribute should Not equal another attribute");
        assertFalse(attr1.equals(attr4), "attribute should Not equal attribute with different values");
        assertFalse(attr1.equals(attr41), "attribute should Not equal attribute with different values");
        assertTrue(attr4.equals(attr41), "attribute should equal another instance with same values");
    }

    @Test
    public void createAdviceAttribute() {
        AdviceAttribute attribute1 = new AdviceAttribute(ATTRIBUTE_NAME1);
        assertEquals(ATTRIBUTE_NAME1, attribute1.getName());
        assertTrue(attribute1.getValue().isEmpty());
        assertNull(attribute1.getFriendlyName(), "Friendly name should not be set.");

        List<String> attributeValues = new ArrayList<String>();
        attributeValues.add(ATTR2_VALUE1);
        attributeValues.add(ATTR2_VALUE2);
        AdviceAttribute attribute2 = new AdviceAttribute(ATTRIBUTE_NAME2, attributeValues);
        assertEquals(2, attribute2.getValue().size());
    }

    @Test
    public void createAdviceAttributeWithFriendlyName() {
        AdviceAttribute attribute1 =
                new AdviceAttribute(ATTRIBUTE_NAME1, ATTRIBUTE_FRIENDLY_NAME1, new ArrayList<String>());
        assertEquals(ATTRIBUTE_NAME1, attribute1.getName());
        assertTrue(attribute1.getValue().isEmpty());
        assertEquals(ATTRIBUTE_FRIENDLY_NAME1, attribute1.getFriendlyName(), "Friendly name should match.");
    }

    @Test
    public void createAdviceNullSource() {
        assertThrows(IllegalArgumentException.class, () -> {
            AdviceAttribute attribute = new AdviceAttribute(ATTRIBUTE_NAME1);
            List<AdviceAttribute> attrList = new ArrayList<AdviceAttribute>();
            attrList.add(attribute);
            new Advice(null, attrList);
        });
    }

    @Test
    public void createAdviceNoAttributes() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Advice(ADVICE_SOURCE, new ArrayList<AdviceAttribute>());
        });
    }

    @Test
    public void createAdviceNullAttributes() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Advice(ADVICE_SOURCE, null);
        });
    }

    @Test
    public void createAttrbuteNullName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AdviceAttribute(null);
        });
    }

    @Test
    public void createAttributeNullValues() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AdviceAttribute(ATTRIBUTE_NAME1, null);
        });
    }

    private Advice createAdvice() {
        AdviceAttribute attribute1 = new AdviceAttribute(ATTRIBUTE_NAME1);
        List<String> attributeValues = new ArrayList<String>();
        attributeValues.add(ATTR2_VALUE1);
        attributeValues.add(ATTR2_VALUE2);
        AdviceAttribute attribute2 = new AdviceAttribute(ATTRIBUTE_NAME2, attributeValues);

        List<AdviceAttribute> attrList = new ArrayList<AdviceAttribute>();
        attrList.add(attribute1);
        attrList.add(attribute2);
        return new Advice(ADVICE_SOURCE, attrList);
    }
}
