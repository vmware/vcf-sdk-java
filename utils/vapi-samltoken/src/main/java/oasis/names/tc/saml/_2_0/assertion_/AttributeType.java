// This file was initially generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6

package oasis.names.tc.saml._2_0.assertion_;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Section 3.8: The Attribute element is restricted in the following ways:
 *
 * <p>1) It may only contain AttributeValue elements; EncrytpedAttributeValue elements are not supported.
 *
 * <p>2) It may not contain any attributes from other namespaces.
 *
 * <p>3) The value of the Name attribute is restricted to be one of the values from Table 4.
 *
 * <p>4) SAML2.0 spec: If no NameFormat value is provided, the identifier
 * urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified is in effect.
 *
 * <p>5) The value of the type attribute of the AttributeValue element MUST be xsd:string
 *
 * <p>NOTE: This schema does not check that the values of FriendlyName are correct.
 *
 * <p>NOTE: The restrictions on the Name attribute must be restricted if the Assertion contains Advice.
 *
 * <p>Java class for AttributeType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="AttributeType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}AttributeValue" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="NameFormat" type="{http://www.rsa.com/names/2010/04/std-prof/SAML2.0}AttributeNameFormats" default="urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified" />
 *       &lt;attribute name="FriendlyName" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "AttributeType",
        propOrder = {"attributeValue"})
public class AttributeType {

    @XmlElement(name = "AttributeValue", nillable = true)
    protected List<String> attributeValue;

    @XmlAttribute(name = "Name", required = true)
    protected String name;

    @XmlAttribute(name = "NameFormat")
    protected String nameFormat;

    @XmlAttribute(name = "FriendlyName")
    protected String friendlyName;

    /**
     * Gets the value of the attributeValue property.
     *
     * <p>This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make
     * to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method
     * for the attributeValue property.
     *
     * <p>For example, to add a new item, do as follows:
     *
     * <pre>
     *    getAttributeValue().add(newItem);
     * </pre>
     *
     * <p>Objects of the following type(s) are allowed in the list {@link String }
     */
    public List<String> getAttributeValue() {
        if (attributeValue == null) {
            attributeValue = new ArrayList<String>();
        }
        return this.attributeValue;
    }

    /**
     * Gets the value of the name property.
     *
     * @return possible object is {@link String }
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is {@link String }
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the nameFormat property.
     *
     * @return possible object is {@link String }
     */
    public String getNameFormat() {
        if (nameFormat == null) {
            return "urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified";
        } else {
            return nameFormat;
        }
    }

    /**
     * Sets the value of the nameFormat property.
     *
     * @param value allowed object is {@link String }
     */
    public void setNameFormat(String value) {
        this.nameFormat = value;
    }

    /**
     * Gets the value of the friendlyName property.
     *
     * @return possible object is {@link String }
     */
    public String getFriendlyName() {
        return friendlyName;
    }

    /**
     * Sets the value of the friendlyName property.
     *
     * @param value allowed object is {@link String }
     */
    public void setFriendlyName(String value) {
        this.friendlyName = value;
    }
}
