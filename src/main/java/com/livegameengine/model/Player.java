package com.livegameengine.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.User;
import com.livegameengine.config.Config;
import com.livegameengine.persist.PMF;

@PersistenceCapable
public class Player implements Scriptable, XmlSerializable {
	private static final long serialVersionUID = 1;
	
	@NotPersistent private Scriptable prototype_, parent_;

	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
		
	@Persistent
	private Key gameUserKey;
		
	@Persistent
	private Game game;
	
	@Persistent
	private Date playerJoin;
	
	@Persistent
	private boolean winner;
	
	@Persistent
	private String role;
		
	public Player() {} 
	
	protected Player(Game game, GameUser gameUser, String role) {
		this.game = game;
		this.gameUserKey = gameUser.getKey();
		this.role = role;
		this.playerJoin = new Date();
		this.winner = false;
	}
	protected Player(Game game, GameUser gameUser, String role, Date playerJoin) {
		this.game = game;
		this.gameUserKey = gameUser.getKey();
		this.role = role;
		this.playerJoin = playerJoin;
		this.winner = false;
	}
		
	public Key getKey() { return key; }
	public Key getGameUserKey() { return gameUserKey; }
	public GameUser getGameUser() {
		PersistenceManager pm = JDOHelper.getPersistenceManager(this);
		
		return pm.getObjectById(GameUser.class, this.gameUserKey);
	}
	
	public String getRole() {
		return role;
	}
	public boolean getWinner() {
		return winner;
	}
	public void setWinner(boolean val) {
		winner = val;
	}
		
	//scriptable object stuff
	@Override
	public String getClassName() {
		return "Player";
	}
	
	public GameUser jsGet_gameUser() {
		return getGameUser();
	}
	
	public String jsGet_role() {
		return getRole();
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
		getGameUser().serializeToXml("gameUser", writer);
		writer.writeStartElement(ns, "winner");
		writer.writeCharacters(Boolean.toString(winner));
		writer.writeEndElement();
		writer.writeStartElement(ns, "role");
		writer.writeCharacters(getRole());
		writer.writeEndElement();
		writer.writeStartElement(ns, "playerJoin");
		writer.writeCharacters(Config.getInstance().getDateFormat().format(this.playerJoin));
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
		return "player";
	}
	
	@Override
	public Object get(String arg0, Scriptable arg1) {
		if(arg0.equals("game"))
			return game;
		else if(arg0.equals("gameUser"))
			return getGameUser();
		else if(arg0.equals("key"))
			return KeyFactory.keyToString(getKey());
		else if(arg0.equals("role"))
			return getRole();
		else if(arg0.equals("userid"))
			return getGameUser().getHashedUserId();
		else if(arg0.equals("winner"))
			return getWinner();
		
		return NOT_FOUND;
	}

	@Override
	public Object get(int arg0, Scriptable arg1) {
		return NOT_FOUND;
	}

	@Override
	public Object getDefaultValue(Class<?> arg0) {
		return "[object Player]";
	}

	@Override
	public Object[] getIds() {
		return new Object[] { "game", "gameUser", "key", "role", "userid", "winner" };
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
	public void put(String arg0, Scriptable arg1, Object arg2) {
		if(arg0.equals("winner")) {
			if(arg2 == null) {
				setWinner(false);
			}
			else if(Boolean.class.isAssignableFrom(arg2.getClass())) {
				setWinner((Boolean)arg2);
			}
			else {
				setWinner(false);
			}
		}
			
	}

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
