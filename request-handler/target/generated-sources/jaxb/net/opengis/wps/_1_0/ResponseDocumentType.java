//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.08.10 at 07:34:19 PM AEST 
//


package net.opengis.wps._1_0;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ResponseDocumentType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ResponseDocumentType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Output" type="{http://www.opengis.net/wps/1.0.0}DocumentOutputDefinitionType" maxOccurs="unbounded"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="storeExecuteResponse" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt;
 *       &lt;attribute name="lineage" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt;
 *       &lt;attribute name="status" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ResponseDocumentType", propOrder = {
    "output"
})
public class ResponseDocumentType {

    @XmlElement(name = "Output", required = true)
    protected List<DocumentOutputDefinitionType> output;
    @XmlAttribute(name = "storeExecuteResponse")
    protected Boolean storeExecuteResponse;
    @XmlAttribute(name = "lineage")
    protected Boolean lineage;
    @XmlAttribute(name = "status")
    protected Boolean status;

    /**
     * Gets the value of the output property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the output property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOutput().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DocumentOutputDefinitionType }
     * 
     * 
     */
    public List<DocumentOutputDefinitionType> getOutput() {
        if (output == null) {
            output = new ArrayList<DocumentOutputDefinitionType>();
        }
        return this.output;
    }

    /**
     * Gets the value of the storeExecuteResponse property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isStoreExecuteResponse() {
        if (storeExecuteResponse == null) {
            return false;
        } else {
            return storeExecuteResponse;
        }
    }

    /**
     * Sets the value of the storeExecuteResponse property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setStoreExecuteResponse(Boolean value) {
        this.storeExecuteResponse = value;
    }

    /**
     * Gets the value of the lineage property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isLineage() {
        if (lineage == null) {
            return false;
        } else {
            return lineage;
        }
    }

    /**
     * Sets the value of the lineage property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setLineage(Boolean value) {
        this.lineage = value;
    }

    /**
     * Gets the value of the status property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isStatus() {
        if (status == null) {
            return false;
        } else {
            return status;
        }
    }

    /**
     * Sets the value of the status property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setStatus(Boolean value) {
        this.status = value;
    }

}
