package com.livegameengine.model;

import java.util.Arrays;
import java.util.List;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.mozilla.javascript.Scriptable;


import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.User;
import com.livegameengine.config.Config;
import com.livegameengine.persist.PMF;

@PersistenceCapable
public class GameType implements Scriptable, XmlSerializable { 
	private static final long serialVersionUID = 2;
	
	@NotPersistent private Scriptable parent_, prototype_;
	
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
	
	@Persistent
	private String typeName;
	
	@Persistent
	private Key creator;
	
	@Persistent
	private String description;
	
	@Persistent
	private Blob stateChart;
	
	@Persistent
	private Blob frontEnd;
	
	@Persistent
	private String clientVersion;

	public Key getKey() {
		return key;
	}
	
	public static GameType findByTypeName(final String typeName) {		
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		
		Query q = pm.newQuery(GameType.class);
		q.setFilter("typeName == typeNameIn");
		q.declareParameters("String typeNameIn");
		
		List<GameType> results = (List<GameType>)q.execute(typeName);
		if(results.size() > 0) {
			return results.get(0);
		}
		else {
			return null;
		}	
	}
	public static GameType findByKey(final Key key) {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		
		return pm.getObjectById(GameType.class, key);
	}
	/*
	public void makePersistent() {
		PersistenceManager pm = JDOHelper.getPersistenceManager(this);
		
		pm.makePersistent(this);
	}
	*/
	
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public void setStateChart(byte[] stateChart) {
		this.stateChart = new Blob(stateChart);
	}

	public byte[] getStateChart() {
		return stateChart.getBytes();
	}
	
	public byte[] getFrontEnd() {
		return frontEnd.getBytes();
	}
	public void setFrontEnd(byte[] frontEnd) {
		this.frontEnd = new Blob(frontEnd);
	}

	public void setCreator(GameUser creator) {		
		this.creator = creator.getKey();
	}

	public GameUser getCreator() {
		return GameUser.findByKey(this.creator);
	}
	public String getClientVersion() {
		return clientVersion;
	}
	public void setClientVersion(String version) {
		clientVersion = version;
	}

	@Override
	public void delete(String arg0) {}

	@Override
	public void delete(int arg0) {}

	@Override
	public void serializeToXml(String elementName, XMLStreamWriter writer) throws XMLStreamException {
		String ns = Config.getInstance().getGameEngineNamespace();
		
		writer.writeStartElement(ns, elementName);
		
		writer.writeAttribute("key", KeyFactory.keyToString(getKey()));
		getCreator().serializeToXml("creator", writer);
		writer.writeStartElement(ns, "description");
		writer.writeCharacters(getDescription());
		writer.writeEndElement();
		writer.writeStartElement(ns, "typeName");
		writer.writeCharacters(getTypeName());
		writer.writeEndElement();
		writer.writeStartElement("clientVersion");
		writer.writeCharacters(getClientVersion());
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
		return "gameType";
	}
	
	@Override
	public Object get(String arg0, Scriptable arg1) {
		if(arg0.equals("creator"))
			return this.getCreator();
		else if(arg0.equals("description"))
			return this.getDescription();
		else if(arg0.equals("key"))
			return KeyFactory.keyToString(getKey());
		else if(arg0.equals("typeName"))
			return this.getTypeName();

		//TODO: add statechart, front-end and supporting files as URLs here
		return NOT_FOUND;
	}

	@Override
	public Object get(int arg0, Scriptable arg1) {
		return NOT_FOUND;
	}

	@Override
	public String getClassName() {
		return "GameType";
	}

	@Override
	public Object getDefaultValue(Class<?> arg0) {
		return "[object GameType]";
	}

	@Override
	public Object[] getIds() {
		return new Object[] { "creator", "description", "typeName" };
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
