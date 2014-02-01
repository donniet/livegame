package com.livegameengine.model;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.ContentHandler;

public interface XmlSerializable {
	public void serializeToXml(String elementName, XMLStreamWriter writer) throws XMLStreamException;
	public void serializeToXml(XMLStreamWriter writer) throws XMLStreamException;
	public String getDefaultLocalName();
	public String getNamespaceUri();
}
