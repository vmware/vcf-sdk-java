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

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;

import oasis.names.tc.saml._2_0.assertion_.AttributeType;

/**
 * Java class for RSAAdviceType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="RSAAdviceType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Attribute" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="AdviceSource" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "RSAAdviceType",
        propOrder = {"attribute"})
public class RSAAdviceType {

    @XmlElement(name = "Attribute", namespace = "urn:oasis:names:tc:SAML:2.0:assertion", required = true)
    protected List<AttributeType> attribute;

    @XmlAttribute(name = "AdviceSource", required = true)
    @XmlSchemaType(name = "anyURI")
    protected String adviceSource;

    /**
     * Gets the value of the attribute property.
     *
     * <p>This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make
     * to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method
     * for the attribute property.
     *
     * <p>For example, to add a new item, do as follows:
     *
     * <pre>
     *    getAttribute().add(newItem);
     * </pre>
     *
     * <p>Objects of the following type(s) are allowed in the list {@link AttributeType }
     */
    public List<AttributeType> getAttribute() {
        if (attribute == null) {
            attribute = new ArrayList<AttributeType>();
        }
        return this.attribute;
    }

    /**
     * Gets the value of the adviceSource property.
     *
     * @return possible object is {@link String }
     */
    public String getAdviceSource() {
        return adviceSource;
    }

    /**
     * Sets the value of the adviceSource property.
     *
     * @param value allowed object is {@link String }
     */
    public void setAdviceSource(String value) {
        this.adviceSource = value;
    }
}
