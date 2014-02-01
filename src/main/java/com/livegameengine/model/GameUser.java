package com.livegameengine.model;

import java.beans.XMLEncoder;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.livegameengine.config.Config;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

import sun.security.provider.MD5;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.User;
import com.livegameengine.persist.PMF;

@PersistenceCapable 
public class GameUser implements Scriptable, XmlSerializable {
	private static final long serialVersionUID = 4649802893267737142L;
	
	private static final String LOCAL_NAME = "gameUser";
	private static final String NAMESPACE_PREFIX = "game";
	
	@NotPersistent
	private Scriptable prototype_; 
	
	@NotPersistent
	private Scriptable parent_;

	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
		
	@Persistent
	private User user; 
	
	@Persistent
	private boolean connected;
			
	public GameUser() {}
	
	private GameUser(final User user) {
		setUser(user);
	}
	
	public static GameUser findByKey(final Key key) {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		
		return pm.getObjectById(GameUser.class, key);		
	}
	
	public static GameUser findByHashedUserId(final String hashedUserId) {
		//String key = Config.getInstance().decryptString(hashedUserId);
		Key key = KeyFactory.stringToKey(hashedUserId);
		
		return findByKey(key);		
	}
	
	public static GameUser findOrCreateGameUserByUser(final User user) {
		PersistenceManager pm = PMF.getInstance().getPersistenceManager();
		
		Query q = pm.newQuery(GameUser.class);
		q.setFilter("user == userIn");
		q.declareParameters(User.class.getName() + " userIn");
		List<GameUser> results = (List<GameUser>)q.execute(user);
		
		if(results.size() > 0) {
			return results.get(0);
		}
		else {
			GameUser ret = new GameUser(user);
			pm.makePersistent(ret);
			
			return ret;
		}
	}
	
	public String getHashedEmail() {
		String email = user.getEmail().trim().toLowerCase();
		
		return Config.getInstance().getMD5Hash(email);
	}
	
	public Key getKey() { return key; }
	public User getUser() { return user; }
	public void setUser(final User user) { 
		this.user = user;
	}
	public String getHashedUserId() {
		return KeyFactory.keyToString(key);
	}
	public String getNickname() {
		return user.getNickname();
	}
	

	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	@Override
	public String getClassName() {
		return "GameUser";
	}

	@Override
	public void delete(String arg0) {
		// not implemented
	}

	@Override
	public void delete(int arg0) {
		// not implemented
	}
	
	@Override
	public void serializeToXml(String elementName, XMLStreamWriter writer) throws XMLStreamException {
		String ns = Config.getInstance().getGameEngineNamespace();
		
		writer.writeStartElement(ns, elementName);
			writer.writeAttribute("key", KeyFactory.keyToString(getKey()));
			writer.writeStartElement(ns, "userid");
				writer.writeCharacters(getHashedUserId());
			writer.writeEndElement();
			writer.writeStartElement(ns, "nickname");
				writer.writeCharacters(getNickname());
			writer.writeEndElement();
			writer.writeStartElement(ns, "hashedEmail");
				writer.writeCharacters(getHashedEmail());
			writer.writeEndElement();
			writer.writeStartElement(ns, "connected");
				writer.writeCharacters(Boolean.toString(isConnected()));
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
		return "gameUser";
	}

	@Override
	public Object get(String arg0, Scriptable arg1) {
		if(arg0.equals("connected"))
			return isConnected();
		else if(arg0.equals("key")) 
			return KeyFactory.keyToString(getKey());
		else if(arg0.equals("userid"))
			return getHashedUserId();
		else if(arg0.equals("nickname"))
			return getNickname();
		else if(arg0.equals("hashedEmail"))
			return getHashedEmail();
		
		return NOT_FOUND;
	}

	@Override
	public Object get(int arg0, Scriptable arg1) {
		return NOT_FOUND;
	}

	@Override
	public Object getDefaultValue(Class<?> arg0) {
		return "[object GameUser]";
	}

	@Override
	public Object[] getIds() {
		return new Object[] {
			"connected", "key", "userid", "nickname", "hashedEmail"
		};
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
		
		for(int i = 0; i < ids.length; i++) {
			if(arg0.equals(ids[i])) return true;
		}
		
		return false;
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
	public void put(String arg0, Scriptable arg1, Object arg2) {
		// read-only
	}

	@Override
	public void put(int arg0, Scriptable arg1, Object arg2) {
		// read-only
	}

	@Override
	public void setParentScope(Scriptable arg0) {
		parent_ = arg0;
	}

	@Override
	public void setPrototype(Scriptable arg0) {
		prototype_ = arg0;
	}


}