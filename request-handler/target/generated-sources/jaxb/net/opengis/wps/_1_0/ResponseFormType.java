//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.08.10 at 07:34:19 PM AEST 
//


package net.opengis.wps._1_0;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Defines the response type of the WPS, either raw data or XML document
 * 
 * <p>Java class for ResponseFormType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ResponseFormType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;choice&gt;
 *         &lt;element name="ResponseDocument" type="{http://www.opengis.net/wps/1.0.0}ResponseDocumentType"/&gt;
 *         &lt;element name="RawDataOutput" type="{http://www.opengis.net/wps/1.0.0}OutputDefinitionType"/&gt;
 *       &lt;/choice&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ResponseFormType", propOrder = {
    "responseDocument",
    "rawDataOutput"
})
public class ResponseFormType {

    @XmlElement(name = "ResponseDocument")
    protected ResponseDocumentType responseDocument;
    @XmlElement(name = "RawDataOutput")
    protected OutputDefinitionType rawDataOutput;

    /**
     * Gets the value of the responseDocument property.
     * 
     * @return
     *     possible object is
     *     {@link ResponseDocumentType }
     *     
     */
    public ResponseDocumentType getResponseDocument() {
        return responseDocument;
    }

    /**
     * Sets the value of the responseDocument property.
     * 
     * @param value
     *     allowed object is
     *     {@link ResponseDocumentType }
     *     
     */
    public void setResponseDocument(ResponseDocumentType value) {
        this.responseDocument = value;
    }

    /**
     * Gets the value of the rawDataOutput property.
     * 
     * @return
     *     possible object is
     *     {@link OutputDefinitionType }
     *     
     */
    public OutputDefinitionType getRawDataOutput() {
        return rawDataOutput;
    }

    /**
     * Sets the value of the rawDataOutput property.
     * 
     * @param value
     *     allowed object is
     *     {@link OutputDefinitionType }
     *     
     */
    public void setRawDataOutput(OutputDefinitionType value) {
        this.rawDataOutput = value;
    }

}
