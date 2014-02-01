package com.livegameengine.web;

import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Source;

import com.livegameengine.model.ClientMessage;

public class ClientMessageSource implements Source {	
	private String systemId_ = "ClientMessageSource"; 
	
	private List<ClientMessage> messages = new ArrayList<ClientMessage>();
	
	public ClientMessageSource(List<ClientMessage> messages) {
		this.messages = messages;
	}
	
	public ClientMessageSource(ClientMessage message) {
		this.messages = new ArrayList<ClientMessage>();
		this.messages.add(message);
	}

	@Override
	public void setSystemId(String systemId) {
		systemId_ = String.format("ClientMessageSource '%s'", systemId);
	}

	@Override
	public String getSystemId() {
		return systemId_;
	}
	
	public List<ClientMessage> getMessages() {
		return this.messages;
	}
	

}
