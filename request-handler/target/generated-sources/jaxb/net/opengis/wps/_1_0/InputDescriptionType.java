//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.08.10 at 07:34:19 PM AEST 
//


package net.opengis.wps._1_0;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * Description of an input to a process. 
 * 
 * In this use, the DescriptionType shall describe this process input. 
 * 
 * <p>Java class for InputDescriptionType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="InputDescriptionType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://www.opengis.net/wps/1.0.0}DescriptionType"&gt;
 *       &lt;sequence&gt;
 *         &lt;group ref="{http://www.opengis.net/wps/1.0.0}InputFormChoice"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="minOccurs" use="required" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" /&gt;
 *       &lt;attribute name="maxOccurs" use="required" type="{http://www.w3.org/2001/XMLSchema}positiveInteger" /&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InputDescriptionType", propOrder = {
    "complexData",
    "literalData",
    "boundingBoxData"
})
public class InputDescriptionType
    extends DescriptionType
{

    @XmlElement(name = "ComplexData", namespace = "")
    protected SupportedComplexDataInputType complexData;
    @XmlElement(name = "LiteralData", namespace = "")
    protected LiteralInputType literalData;
    @XmlElement(name = "BoundingBoxData", namespace = "")
    protected SupportedCRSsType boundingBoxData;
    @XmlAttribute(name = "minOccurs", required = true)
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger minOccurs;
    @XmlAttribute(name = "maxOccurs", required = true)
    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger maxOccurs;

    /**
     * Gets the value of the complexData property.
     * 
     * @return
     *     possible object is
     *     {@link SupportedComplexDataInputType }
     *     
     */
    public SupportedComplexDataInputType getComplexData() {
        return complexData;
    }

    /**
     * Sets the value of the complexData property.
     * 
     * @param value
     *     allowed object is
     *     {@link SupportedComplexDataInputType }
     *     
     */
    public void setComplexData(SupportedComplexDataInputType value) {
        this.complexData = value;
    }

    /**
     * Gets the value of the literalData property.
     * 
     * @return
     *     possible object is
     *     {@link LiteralInputType }
     *     
     */
    public LiteralInputType getLiteralData() {
        return literalData;
    }

    /**
     * Sets the value of the literalData property.
     * 
     * @param value
     *     allowed object is
     *     {@link LiteralInputType }
     *     
     */
    public void setLiteralData(LiteralInputType value) {
        this.literalData = value;
    }

    /**
     * Gets the value of the boundingBoxData property.
     * 
     * @return
     *     possible object is
     *     {@link SupportedCRSsType }
     *     
     */
    public SupportedCRSsType getBoundingBoxData() {
        return boundingBoxData;
    }

    /**
     * Sets the value of the boundingBoxData property.
     * 
     * @param value
     *     allowed object is
     *     {@link SupportedCRSsType }
     *     
     */
    public void setBoundingBoxData(SupportedCRSsType value) {
        this.boundingBoxData = value;
    }

    /**
     * Gets the value of the minOccurs property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getMinOccurs() {
        return minOccurs;
    }

    /**
     * Sets the value of the minOccurs property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setMinOccurs(BigInteger value) {
        this.minOccurs = value;
    }

    /**
     * Gets the value of the maxOccurs property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getMaxOccurs() {
        return maxOccurs;
    }

    /**
     * Sets the value of the maxOccurs property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setMaxOccurs(BigInteger value) {
        this.maxOccurs = value;
    }

}
