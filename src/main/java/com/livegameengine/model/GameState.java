package com.livegameengine.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.Order;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.scxml.model.Data;
import org.apache.commons.scxml.model.Datamodel;
import com.livegameengine.config.Config;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.xmlimpl.XMLLibImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.livegameengine.persist.PMF;
import com.livegameengine.scxml.js.JsContext;

@PersistenceCapable(detachable="true")
public class GameState implements Scriptable, XmlSerializable { 
	private static final long serialVersionUID = 1;
	
	private static final Config config_ = Config.getInstance();
	
	@NotPersistent private Scriptable parent_, prototype_;
	
	
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
		
	@Persistent
	private Key gameKey;
	
	@Persistent
	private Date stateDate = new Date();
	
	@Persistent
	private boolean important = false;
	
	@Persistent
	@Element(dependent = "true", mappedBy = "gameState", extensions = @Extension(vendorName="datanucleus", key="cascade-persist", value="true"))
	@Order(extensions = @Extension(vendorName="datanucleus", key="list-ordering", value="id asc"))	
	private List<GameStateData> datamodel;
	
	@Persistent
	private Set<String> stateSet;
	
	@Persistent
	private Set<String> validEvents;
	
	protected GameState(Game game) {
		if(game.getKey() == null) {
			throw new IllegalArgumentException("game not persisted");
		}
		this.gameKey = game.getKey();
		stateDate = new Date();
		datamodel = new ArrayList<GameStateData>();
		stateSet = new HashSet<String>();
		validEvents = new HashSet<String>();
	}
	public Key getKey() {
		return key;
	}
	public Key getGameKey() {
		return gameKey;
	}
	protected void setGameKey(Key gameKey) {
		this.gameKey = gameKey;
	}
	public void setStateDate(Date stateDate_) {
		this.stateDate = stateDate_;
	}
	public Date getStateDate() {
		return stateDate;
	}
	public Set<String> getStateSet() {
		return stateSet;
	}
	public Set<String> getValidEvents() {
		return validEvents;
	}
		
	/*
	 * inject the datamodel into the context
	 * 
	 * @param cxt the context to be injected into
	 */
	protected void injectInto(JsContext cxt) {
		Log log = LogFactory.getLog(GameState.class);
		
		DocumentBuilderFactory docbuilderfactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = docbuilderfactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			builder = null;
			log.error("Could not get transformer", e);
			return;
		}
		Document doc = builder.newDocument();
		
		
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer trans = null;
		try {
			trans = factory.newTransformer();
		} catch (TransformerConfigurationException e) {
			trans = null;
			log.error("Could not get transformer", e);
			return;
		}
		
		trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		
		
		for(Iterator<GameStateData> i = getDatamodel().iterator(); i.hasNext(); ) {
			GameStateData d = i.next();
			
			cxt.setLocal(d.getId(), d.getValue());
		}
	}
	
	protected void extractFrom(Datamodel model, JsContext cxt) {
		Log log = LogFactory.getLog(GameState.class);
		
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer trans = null;
		try {
			trans = factory.newTransformer();
		} catch (TransformerConfigurationException e) {
			trans = null;
			log.error("Could not get transformer", e);
			return;
		}
		
		trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		
		for(Iterator<Data> i = model.getData().iterator(); i.hasNext(); ) {
			Data d = i.next();
			
			GameStateData gsd = new GameStateData(this);
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			Object o = cxt.get(d.getId());
			Node n = XMLLibImpl.toDomNode(o);
			
			try {
				trans.transform(new DOMSource(n), new StreamResult(bos));
			} catch(TransformerException e) {
				log.error("Transform failed: " + d.getId(), e);
			}
			
			gsd.setId(d.getId());
			gsd.setValue(bos.toByteArray());
			
			this.getDatamodel().add(gsd);			
		}
	}
	public List<GameStateData> getDatamodel() {
		return datamodel;
	}	
	public boolean isImportant() {
		return important;
	}
	public void setImportant(boolean important) {
		this.important = important;
	}
	@Override
	public void delete(String arg0) {}
	
	@Override
	public void delete(int arg0) {}
	
	Node getValidEventsNode() {
		Document d = config_.newXmlDocument();
		String ns = config_.getGameEngineNamespace();
		
		Node ret = d.createElementNS(ns, "validEvents");
		for(String e : validEvents) {
			Node v = d.createElementNS(ns, "event");
			v.appendChild(d.createTextNode(e));
			ret.appendChild(v);
		}
		
		return ret;
	}

	@Override
	public void serializeToXml(String elementName, XMLStreamWriter writer) throws XMLStreamException {
		String ns = Config.getInstance().getGameEngineNamespace();
		
		writer.writeStartElement(ns, elementName);
		writer.writeAttribute("key", KeyFactory.keyToString(getKey()));
		writer.writeStartElement(ns, "datamodel");
		for(Iterator<GameStateData> i = getDatamodel().iterator(); i.hasNext();) {
			GameStateData gsd = i.next();
			gsd.serializeToXml("data", writer);
		}
		writer.writeEndElement();
		writer.writeStartElement(ns, "important");
		writer.writeCharacters(Boolean.toString(important));
		writer.writeEndElement();		
		writer.writeStartElement(ns, "stateDate");
		writer.writeCharacters(Config.getInstance().getDateFormat().format(getStateDate()));
		writer.writeEndElement();
		writer.writeStartElement(ns, "stateSet");
		for(Iterator<String> i = getStateSet().iterator(); i.hasNext();) {
			String state = i.next();
			writer.writeStartElement(ns, "state");
			writer.writeCharacters(state);
			writer.writeEndElement();
		}
		writer.writeEndElement();
		
		writer.writeStartElement(ns, "validEvents");
		for(String e : validEvents) {
			writer.writeStartElement(ns, "event");
			writer.writeCharacters(e);
			writer.writeEndElement();
		}		
		writer.writeEndElement();
		
		writer.writeEndElement();
	}
	
	@Override public void serializeToXml(XMLStreamWriter writer) throws XMLStreamException {
		serializeToXml(getDefaultLocalName(), writer);
	}
	
	@Override public String getNamespaceUri() {
		return Config.getInstance().getGameEngineNamespace();
	}
	
	@Override public String getDefaultLocalName() {
		return "gameState";
	}
	
	@Override
	public Object get(String arg0, Scriptable arg1) {
		if(arg0.equals("datamodel"))
			return getDatamodel();
		else if(arg0.equals("important"))
			return this.important;
		else if(arg0.equals("key"))
			return KeyFactory.keyToString(getKey());
		else if(arg0.equals("stateDate"))
			return getStateDate();
		else if(arg0.equals("stateSet"))
			return getStateSet();
		
		return NOT_FOUND;
	}
	
	@Override
	public Object get(int arg0, Scriptable arg1) {
		return NOT_FOUND;
	}
	@Override
	public String getClassName() {
		return "GameState";
	}
	@Override
	public Object getDefaultValue(Class<?> arg0) {
		return "[object GameState]";
	}
	@Override
	public Object[] getIds() {
		return new Object[] { "datamodel", "important", "key", "stateDate", "stateSet" };
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
