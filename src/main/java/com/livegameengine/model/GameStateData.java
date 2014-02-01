package com.livegameengine.model;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.livegameengine.config.Config;
import com.livegameengine.util.Util;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;

@PersistenceCapable
public class GameStateData implements Scriptable, XmlSerializable { 
	private static final long serialVersionUID = 1;
	
	@NotPersistent private Scriptable parent_, prototype_;
	@NotPersistent private Config config = Config.getInstance();
	
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY) 
	private Key key;
	
	@Persistent
	private String id;
	
	@Persistent
	private Blob value;
	
	@Persistent
	private GameState gameState;
	
	protected Key getKey() {
		return key;
	}
	protected GameStateData(GameState state) {
		setGameState(state);
		value = null;
		id = null;
	}
	public GameStateData() {
		setGameState(null);
		value = null;
		id = null;
	}	

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
	
	public void appendValueInto(Node parent) {
		try {
			Transformer trans = config.newTransformer();
			
			trans.transform(new StreamSource(new ByteArrayInputStream(value.getBytes())), new DOMResult(parent));
		} catch (TransformerConfigurationException e) {
			//TODO: handle this error
		} catch (TransformerException e) {
			//TODO: handle this error
		}		
	}
	
	public Node getValue() {
		if(value == null) return null;
		
		try {
			Document doc = config.newXmlDocument();
			Transformer trans = config.newTransformer();
			
			trans.transform(new StreamSource(new ByteArrayInputStream(value.getBytes())), new DOMResult(doc));
			
			for(int i = 0; i < doc.getChildNodes().getLength(); i++) {
				Node n = doc.getChildNodes().item(i);
				if(n.getNodeType() == Node.ELEMENT_NODE)
					return n;
			}
			
			return null;
		} catch (TransformerConfigurationException e) {
			//TODO: handle this error
		} catch (TransformerException e) {
			//TODO: handle this error
		}
		
		return null;
	}
	/*
	public byte[] getValue() {
		if(value == null) {
			return null;
		}
		else {
			return value.getBytes();
			
		}
	}
	*/
	public void setValue(final byte[] value) {
		if(value == null) {
			this.value = null;
		}
		else {
			this.value = new Blob(value);
		}
	}
	protected void setGameState(GameState gameState) {
		this.gameState = gameState;
	}
	public GameState getGameState() {
		return gameState;
	}
	@Override
	public void delete(String arg0) {}
	@Override
	public void delete(int arg0) {}
	
	@Override
	public void serializeToXml(String elementName, XMLStreamWriter writer) throws XMLStreamException {
		String ns = config.getGameEngineNamespace();
		
		if(value != null) {			
			Util.writeNode(getValue(), writer);
		}
	}
	
	@Override public void serializeToXml(XMLStreamWriter writer) throws XMLStreamException {
		serializeToXml(getDefaultLocalName(), writer);
	}
	
	@Override public String getNamespaceUri() {
		return Config.getInstance().getGameEngineNamespace();
	}
	
	@Override public String getDefaultLocalName() {
		return "data";
	}
	
	@Override
	public Object get(String arg0, Scriptable arg1) {
		if(arg0.equals("id"))
			return getId();
		else if(arg0.equals("key"))
			return KeyFactory.keyToString(getKey());
		//TODO: add valueUrl property pointing to the url of the
		
		return NOT_FOUND;
	}
	@Override
	public Object get(int arg0, Scriptable arg1) {
		return NOT_FOUND;
	}
	@Override
	public String getClassName() {
		return "GameStateData";
	}
	@Override
	public Object getDefaultValue(Class<?> arg0) {
		return "[object GameStateData]";
	}
	@Override
	public Object[] getIds() {
		return new Object[] {"id", "key"};
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
