/*
 * ******************************************************************
 * Copyright (c) 2010-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.rsa.names._2009._12.std_ext.saml2_;

import java.math.BigInteger;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;

import oasis.names.tc.saml._2_0.assertion_.ConditionAbstractType;

/**
 * Java class for RenewRestrictionType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="RenewRestrictionType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:assertion}ConditionAbstractType">
 *       &lt;sequence>
 *       &lt;/sequence>
 *       &lt;attribute name="Count" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" default="0" />
 *       &lt;attribute name="Postdatable" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="RenewExpired" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RenewRestrictionType")
public class RenewRestrictionType extends ConditionAbstractType {

    @XmlAttribute(name = "Count")
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger count;

    @XmlAttribute(name = "Postdatable")
    protected Boolean postdatable;

    @XmlAttribute(name = "RenewExpired")
    protected Boolean renewExpired;

    /**
     * Gets the value of the count property.
     *
     * @return possible object is {@link BigInteger }
     */
    public BigInteger getCount() {
        if (count == null) {
            return new BigInteger("0");
        } else {
            return count;
        }
    }

    /**
     * Sets the value of the count property.
     *
     * @param value allowed object is {@link BigInteger }
     */
    public void setCount(BigInteger value) {
        this.count = value;
    }

    /**
     * Gets the value of the postdatable property.
     *
     * @return possible object is {@link Boolean }
     */
    public boolean isPostdatable() {
        if (postdatable == null) {
            return false;
        } else {
            return postdatable;
        }
    }

    /**
     * Sets the value of the postdatable property.
     *
     * @param value allowed object is {@link Boolean }
     */
    public void setPostdatable(Boolean value) {
        this.postdatable = value;
    }

    /**
     * Gets the value of the renewExpired property.
     *
     * @return possible object is {@link Boolean }
     */
    public boolean isRenewExpired() {
        if (renewExpired == null) {
            return false;
        } else {
            return renewExpired;
        }
    }

    /**
     * Sets the value of the renewExpired property.
     *
     * @param value allowed object is {@link Boolean }
     */
    public void setRenewExpired(Boolean value) {
        this.renewExpired = value;
    }
}
