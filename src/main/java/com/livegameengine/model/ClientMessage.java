package com.livegameengine.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.xml.bind.annotation.XmlList;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.livegameengine.config.Config;
import com.livegameengine.persist.PMF;
import com.livegameengine.util.Util;


@PersistenceCapable
public class ClientMessage implements Scriptable, XmlSerializable {
	private static final long serialVersionUID = 4649802893267737142L;
		
	private static final String LOCAL_NAME = "clientMessage";
	private static final String NAMESPACE_PREFIX = "game";
	private static Config config_ = Config.getInstance();
	
	@NotPersistent
	private Scriptable parent_, prototype_; 
	

	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
	
	@Persistent
	private String name;
	
	@Persistent
	protected List<String> parameterNames;
	
	@Persistent
	protected List<String> parameterValues;
	
	@Persistent
	private Blob content;
	
	@Persistent
	private Date messageDate;
	
	@Persistent
	private Key gameKey;
	
	public static List<ClientMessage> findClientMessagesSince(Game g, Date since) {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		Query q = pm.newQuery(ClientMessage.class);
				
		q.setFilter("gameKey == gameKeyIn && messageDate > since");
		q.declareParameters(Date.class.getName() + " since, " + Key.class.getName() + " gameKeyIn");
		q.setOrdering("messageDate asc");
		q.setRange(0,1000);
		
		return (List<ClientMessage>)q.execute(since, g.getKey());		
	}
	
	public ClientMessage(Game g, String name, Map<String,String> params) {
		this.gameKey = g.getKey();
		this.name = name;
		this.messageDate = new Date();
		
		this.parameterNames = new ArrayList<String>();
		this.parameterValues = new ArrayList<String>();
		
		for(Iterator<String> i = params.keySet().iterator(); i.hasNext();) {
			String k = i.next();
			
			this.parameterNames.add(k);
			
			Object val = params.get(k);
			if(String.class.isAssignableFrom(val.getClass())) {
				this.parameterValues.add((String)val);
			}
			else {
				this.parameterValues.add(val.toString());				
			}
		}
	}
	
	public ClientMessage(Game g, String name, Map<String,String> params, String content) {
		this(g, name, params);
		
		setContent(content);
	}
	
	public ClientMessage(Game g, String name, Map<String,String> params, Node content) {
		this(g, name, params);
		
		setContent(content);
	}

	public Date getMessageDate() {
		return messageDate;
	}

	public Key getKey() {
		return key;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setMessageDate(Date messageDate) {
		this.messageDate = messageDate;
	}
	
	public Key getGameKey() {
		return gameKey;
	}
	public void setGameKey(Key gameKey) {
		this.gameKey = gameKey;
	}
	
	public Iterator<NameValuePair<String>> getParameterIterator() {
		final ClientMessage self = this;
		
		return new Iterator<NameValuePair<String>>() {
			private int index = 0;

			@Override
			public boolean hasNext() {
				return index < parameterNames.size();
			}

			@Override
			public NameValuePair<String> next() {
				return self.getParameter(index++);
			}

			@Override
			public void remove() {}
			
		};
	}
	
	public Map<String,String> getParametersAsMap() {
		Map<String,String> params = new HashMap<String,String>();
		for(Iterator<NameValuePair<String>> j = this.getParameterIterator(); j.hasNext();) {
			NameValuePair<String> p = j.next();
			
			params.put(p.getName(), p.getValue());						
		}
		
		return params;
	}
	
	public NameValuePair<String> getParameter(int i) {
		if(i >= 0 && i < parameterNames.size()) {
			return new NameValuePair<String>(parameterNames.get(i), parameterValues.get(i));
		}
		
		return null;
	}
	public NameValuePair<String> getParameter(String s) {
		//TODO: make this more efficient (if I ever use it...)
		return getParameter(parameterNames.indexOf(s));
	}
	
	// returns either String or Node (or null)
	public Object getContent() {
		try {
			Document doc = config_.newXmlDocument();
			Transformer trans = config_.newTransformer();
			
			trans.transform(new StreamSource(new ByteArrayInputStream(content.getBytes())), new DOMResult(doc));
			
			for(int i = 0; i < doc.getChildNodes().getLength(); i++) {
				Node n = doc.getChildNodes().item(i);
				
				if(n.getNodeType() == Node.ELEMENT_NODE) return n;
			}
			
			return null;
		} catch (TransformerConfigurationException e) {
			return null;
		} catch (TransformerException e) {
			//ignore, just return the string
		}
		
		return new String(content.getBytes());
	}
	public void setContent(Node content) {
		if(content == null) {
			this.content = null;
			return;
		}
		
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			Transformer trans = config_.newTransformer();
			
			trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			
			trans.transform(new DOMSource(content), new StreamResult(bos));
			
			this.content = new Blob(bos.toByteArray());
		} catch (TransformerConfigurationException e) {
			return;
		} catch (TransformerException e) {
			//TODO: add error handling here
			return;
		}
	}
	public void setContent(String content) {
		if(content == null) {
			this.content = null;
			return;
		}
		
		this.content = new Blob(content.getBytes());
	}
	
	public void serializeToXml(String elementName, XMLStreamWriter writer, Node replacementContent)
			throws XMLStreamException {
		String ns = config_.getGameEngineNamespace();
		
		if(writer.getPrefix(ns) == null) {
			writer.setPrefix(config_.getGameEngineDefaultNamespacePrefix(), config_.getGameEngineNamespace());
		}
		
		writer.writeStartElement(ns, elementName);
		writer.writeAttribute("key", KeyFactory.keyToString(key));
		
		writer.writeStartElement(ns, "messageDate");
		writer.writeCharacters(config_.getDateFormat().format(getMessageDate()));
		writer.writeEndElement();
		
		writer.writeStartElement(ns, "event");
		writer.writeCharacters(getName());
		writer.writeEndElement();
		
		int min = Math.min(parameterNames.size(), parameterValues.size());
		for(int i = 0; i < min; i++) {
			writer.writeStartElement(ns, "param");
			writer.writeAttribute("name", parameterNames.get(i));
			writer.writeCharacters(parameterValues.get(i));
			writer.writeEndElement();
		}
		
		Object c = null;
		
		if(replacementContent != null) {
			c = replacementContent;
		}
		else if(this.content != null) {
			c = getContent();
		}
		
		if(c != null) {
			writer.writeStartElement(ns, "content");
			
			if(String.class.isAssignableFrom(c.getClass())) {
				writer.writeCharacters((String)c);
			}
			else if(Node.class.isAssignableFrom(c.getClass())) {
				Util.writeNode((Node)c, writer);
			}
						
			writer.writeEndElement();
		}
		
		writer.writeEndElement();
	}
	
	@Override
	public void serializeToXml(String elementName, XMLStreamWriter writer)
			throws XMLStreamException {
		serializeToXml(elementName, writer, null);
	}

	@Override
	public void serializeToXml(XMLStreamWriter writer)
			throws XMLStreamException {
		serializeToXml(getDefaultLocalName(), writer);		
	}

	@Override
	public String getDefaultLocalName() {
		return LOCAL_NAME;
	}

	@Override
	public String getNamespaceUri() {
		return config_.getGameEngineNamespace();
	}

	@Override
	public void delete(String arg0) {}

	@Override
	public void delete(int arg0) {}

	@Override
	public Object get(String arg0, Scriptable arg1) {
		if(arg0.equals("content"))
			return getContent();
		else if(arg0.equals("key"))
			return KeyFactory.keyToString(getKey());
		else if(arg0.equals("messageDate"))
			return getMessageDate();
		else if(arg0.equals("name"))
			return getName();
		else if(arg0.equals("parameters"))
			return new ClientMessageParametersObject(this);
		
		return NOT_FOUND;
	}


	@Override
	public Object get(int arg0, Scriptable arg1) {
		return NOT_FOUND;
	}

	@Override
	public String getClassName() {
		return "ClientMessage";
	}

	@Override
	public Object getDefaultValue(Class<?> arg0) {
		return String.format("[object %s]", getClassName());
	}

	@Override
	public Object[] getIds() {
		return new Object[] { "content", "key", "messageDate", "name", "parameters" };
	}

	@Override
	public Scriptable getParentScope() {
		return parent_;
	}

	@Override
	public Scriptable getPrototype() {
		return prototype_;
	}

	@Override
	public boolean has(String arg0, Scriptable arg1) {
		String[] ids = (String[])getIds();
		
		return Arrays.binarySearch(ids, arg0) >= 0;
	}

	@Override
	public boolean has(int arg0, Scriptable arg1) {
		return false;
	}

	@Override
	public boolean hasInstance(Scriptable value) {
        Scriptable proto = value.getPrototype();
        while (proto != null) {
            if (proto.equals(this))
                return true;
            proto = proto.getPrototype();
        }

        return false;
	}

	@Override
	public void put(String arg0, Scriptable arg1, Object arg2) {}

	@Override
	public void put(int arg0, Scriptable arg1, Object arg2) {}

	@Override
	public void setParentScope(Scriptable arg0) {
		parent_ = arg0;
	}

	@Override
	public void setPrototype(Scriptable arg0) {
		prototype_ = arg0;
	}
	
}
