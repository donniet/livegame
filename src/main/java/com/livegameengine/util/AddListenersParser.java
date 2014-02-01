package com.livegameengine.util;

import java.io.IOException;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

import javax.servlet.http.HttpServletRequest;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.collections.ArrayStack;

import com.livegameengine.config.Config;

public class AddListenersParser {
	private Set<String> listeningEvents = null;
	private Config config = Config.getInstance();
	
	public AddListenersParser(HttpServletRequest request) throws XMLStreamException, IOException {
		this.parse(request);
	}
	
	public Set<String> getEvents() {
		return listeningEvents;
	}
	
	private void parse(HttpServletRequest request) throws XMLStreamException, IOException {
		listeningEvents = new HashSet<String>();
		
		XMLInputFactory factory = XMLInputFactory.newFactory();
		XMLStreamReader reader = factory.createXMLStreamReader(request.getInputStream());
						
		while(reader.hasNext()) {
			reader.next();
			
			switch(reader.getEventType()) {
			case XMLStreamReader.START_ELEMENT:
				if(reader.getLocalName() == "addListeners" && reader.getNamespaceURI() == config.getGameEngineNamespace()) {
					parse_addhandler(reader);
					return;
				}
				break;
			}
		}
			
	}
	
	private void parse_addhandler(XMLStreamReader reader) throws XMLStreamException, IOException {
		while(reader.hasNext()) {
			reader.next();
			
			switch(reader.getEventType()) {
			case XMLStreamReader.START_ELEMENT:
				if(reader.getLocalName() == "event" && reader.getNamespaceURI() == config.getGameEngineNamespace()) {
					parse_event(reader);
					
					if(reader.getEventType() != XMLStreamReader.END_ELEMENT)
						throw new IOException("invalid xml format");
				}
				break;
			}
		}
	}
	
	private void parse_event(XMLStreamReader reader) throws XMLStreamException {
		StringBuilder sb = new StringBuilder();
		
		while(reader.next() == XMLStreamReader.CHARACTERS) {
			sb.append(reader.getText());
		}
		
		listeningEvents.add(sb.toString());
	}
}
